package com.x64Dynamics.tfb.architectural.editor;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.terrain.HeightmapGrid;

import java.util.*;

/**
 * Persistent, hierarchical architectural project holding editable {@link BuildingSection}s,
 * spatial dependency tracking, incremental dirty-region regeneration, and undo/redo snapshots.
 */
public final class ArchitecturalProject {
    private final String id;
    private String name;
    private final List<BuildingSection> sections = new ArrayList<>();
    private final Deque<List<BuildingSection>> undoStack = new ArrayDeque<>();
    private final Deque<List<BuildingSection>> redoStack = new ArrayDeque<>();
    private int nextSectionSeq = 1;

    public ArchitecturalProject(String id, String name) {
        this.id = id != null && !id.isBlank() ? id : "proj-" + Long.toHexString(System.nanoTime());
        this.name = name != null && !name.isBlank() ? name : "Untitled Architecture";
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { if (name != null && !name.isBlank()) this.name = name; }

    public List<BuildingSection> sections() {
        return Collections.unmodifiableList(sections);
    }

    public Optional<BuildingSection> findSection(String sectionId) {
        for (BuildingSection s : sections) {
            if (s.id().equals(sectionId)) return Optional.of(s);
        }
        return Optional.empty();
    }

    public Optional<BuildingSection> findSectionAt(Vec3i pos) {
        for (int i = sections.size() - 1; i >= 0; i--) {
            BuildingSection s = sections.get(i);
            BoundingBox3i b = s.bounds();
            if (pos.x() >= b.min().x() && pos.x() <= b.max().x()
                    && pos.y() >= b.min().y() && pos.y() <= b.max().y() + (s.stories() * 4) + 5
                    && pos.z() >= b.min().z() && pos.z() <= b.max().z()) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public BuildingSection addSection(
            BuildingSection.SectionKind kind,
            BoundingBox3i bounds,
            int stories,
            StylePreset style,
            RoofGenerator.RoofType roofType,
            Direction facing,
            long seed) {
        pushUndo();
        String sid = "sec_" + (nextSectionSeq++);
        BuildingSection sec = new BuildingSection(sid, kind, bounds, stories, style, roofType, facing, seed);
        sections.add(sec);
        markAdjacentDirty(sec);
        return sec;
    }

    public boolean removeSection(String sectionId) {
        Optional<BuildingSection> opt = findSection(sectionId);
        if (opt.isEmpty() || opt.get().isLocked()) return false;
        pushUndo();
        BuildingSection removed = opt.get();
        sections.remove(removed);
        markAdjacentDirty(removed);
        return true;
    }

    public boolean extendSection(String sectionId, Direction dir, int amount) {
        Optional<BuildingSection> opt = findSection(sectionId);
        if (opt.isEmpty() || opt.get().isLocked() || amount == 0) return false;
        pushUndo();
        BuildingSection s = opt.get();
        BoundingBox3i b = s.bounds();
        Vec3i min = b.min();
        Vec3i max = b.max();
        switch (dir) {
            case NORTH -> min = new Vec3i(min.x(), min.y(), Math.min(max.z() - 4, min.z() - amount));
            case SOUTH -> max = new Vec3i(max.x(), max.y(), Math.max(min.z() + 4, max.z() + amount));
            case WEST -> min = new Vec3i(Math.min(max.x() - 4, min.x() - amount), min.y(), min.z());
            case EAST -> max = new Vec3i(Math.max(min.x() + 4, max.x() + amount), max.y(), max.z());
            case UP -> {
                s.setStories(s.stories() + (amount > 0 ? 1 : -1));
                markAdjacentDirty(s);
                return true;
            }
            case DOWN -> min = new Vec3i(min.x(), min.y() - amount, min.z());
        }
        s.setBounds(BoundingBox3i.fromCorners(min, max));
        markAdjacentDirty(s);
        return true;
    }

    public boolean stackFloor(String sectionId) {
        Optional<BuildingSection> opt = findSection(sectionId);
        if (opt.isEmpty() || opt.get().isLocked()) return false;
        pushUndo();
        BuildingSection s = opt.get();
        s.setStories(s.stories() + 1);
        markAdjacentDirty(s);
        return true;
    }

    public boolean moveSection(String sectionId, Vec3i delta) {
        Optional<BuildingSection> opt = findSection(sectionId);
        if (opt.isEmpty() || opt.get().isLocked()) return false;
        pushUndo();
        BuildingSection s = opt.get();
        markAdjacentDirty(s);
        s.translate(delta);
        for (BuildingSection child : sections) {
            if (sectionId.equals(child.parentSectionId()) && !child.isLocked()) {
                child.translate(delta);
            }
        }
        markAdjacentDirty(s);
        return true;
    }

    public boolean toggleLock(String sectionId) {
        Optional<BuildingSection> opt = findSection(sectionId);
        if (opt.isEmpty()) return false;
        opt.get().setLocked(!opt.get().isLocked());
        return true;
    }

    public void regenerateDirtySections(HeightmapGrid heightmap, FoundationStrategy foundationStrategy) {
        for (BuildingSection s : sections) {
            if (s.isDirty() && !s.isLocked()) {
                boolean hasTallerNeighbor = hasTallerAdjacentSection(s);
                List<PlacementTarget> composed = SectionComposer.compose(
                    s, sections, hasTallerNeighbor, heightmap, foundationStrategy
                );
                s.setCachedTargets(composed);
            }
        }
    }

    public List<PlacementTarget> resolveAllTargets() {
        Map<Vec3i, PlacementTarget> merged = new LinkedHashMap<>();
        for (BuildingSection s : sections) {
            for (PlacementTarget t : s.cachedTargets()) {
                merged.put(t.pos(), t);
            }
        }
        return new ArrayList<>(merged.values());
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        redoStack.push(snapshotSections());
        restoreSections(undoStack.pop());
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        undoStack.push(snapshotSections());
        restoreSections(redoStack.pop());
        return true;
    }

    private void pushUndo() {
        undoStack.push(snapshotSections());
        while (undoStack.size() > 32) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private List<BuildingSection> snapshotSections() {
        List<BuildingSection> snap = new ArrayList<>(sections.size());
        for (BuildingSection s : sections) snap.add(s.copy());
        return snap;
    }

    private void restoreSections(List<BuildingSection> snap) {
        sections.clear();
        sections.addAll(snap);
    }

    private void markAdjacentDirty(BuildingSection changed) {
        BoundingBox3i cb = changed.bounds();
        for (BuildingSection other : sections) {
            if (other == changed || other.isLocked()) continue;
            BoundingBox3i ob = other.bounds();
            boolean nearX = ob.min().x() <= cb.max().x() + 2 && ob.max().x() >= cb.min().x() - 2;
            boolean nearZ = ob.min().z() <= cb.max().z() + 2 && ob.max().z() >= cb.min().z() - 2;
            if (nearX && nearZ) {
                other.markDirty();
            }
        }
    }

    private boolean hasTallerAdjacentSection(BuildingSection target) {
        BoundingBox3i tb = target.bounds();
        int targetTop = tb.min().y() + target.stories() * 4;
        for (BuildingSection other : sections) {
            if (other == target) continue;
            BoundingBox3i ob = other.bounds();
            int otherTop = ob.min().y() + other.stories() * 4;
            boolean touchesX = ob.min().x() <= tb.max().x() + 1 && ob.max().x() >= tb.min().x() - 1;
            boolean touchesZ = ob.min().z() <= tb.max().z() + 1 && ob.max().z() >= tb.min().z() - 1;
            if (touchesX && touchesZ && otherTop > targetTop) {
                return true;
            }
        }
        return false;
    }
}
