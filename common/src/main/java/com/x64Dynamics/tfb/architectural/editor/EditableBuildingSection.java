package com.x64Dynamics.tfb.architectural.editor;

import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Editable architectural volume within an {@link ArchitecturalProject}.
 * Preserves stable ID, spatial bounds, style, roof form, lock state,
 * and cached resolved placements for incremental dirty-region regeneration.
 */
public final class EditableBuildingSection {
    public enum SectionKind {
        MAIN_WING,
        SIDE_WING,
        TOWER,
        PORCH,
        BALCONY,
        ROOFTOP_TERRACE,
        STAIR_APPROACH
    }

    private final String id;
    private SectionKind kind;
    private BoundingBox3i bounds;
    private int stories;
    private StylePreset style;
    private RoofGenerator.RoofType roofType;
    private Direction facing;
    private boolean locked;
    private boolean dirty = true;
    private long seed;
    private String parentSectionId;
    private List<PlacementTarget> cachedTargets = new ArrayList<>();

    public EditableBuildingSection(
            String id,
            SectionKind kind,
            BoundingBox3i bounds,
            int stories,
            StylePreset style,
            RoofGenerator.RoofType roofType,
            Direction facing,
            long seed) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = kind != null ? kind : SectionKind.MAIN_WING;
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.stories = Math.max(1, Math.min(5, stories));
        this.style = style != null ? style : StylePreset.MEDIEVAL;
        this.roofType = roofType != null ? roofType : RoofGenerator.RoofType.GABLE;
        this.facing = facing != null ? facing : Direction.SOUTH;
        this.seed = seed;
    }

    public String id() { return id; }
    public SectionKind kind() { return kind; }
    public void setKind(SectionKind kind) { if (kind != null) { this.kind = kind; markDirty(); } }

    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) {
        if (bounds != null && !bounds.equals(this.bounds)) {
            this.bounds = bounds;
            markDirty();
        }
    }

    public int stories() { return stories; }
    public void setStories(int stories) {
        int clamped = Math.max(1, Math.min(5, stories));
        if (this.stories != clamped) {
            this.stories = clamped;
            markDirty();
        }
    }

    public StylePreset style() { return style; }
    public void setStyle(StylePreset style) {
        if (style != null && this.style != style) {
            this.style = style;
            markDirty();
        }
    }

    public RoofGenerator.RoofType roofType() { return roofType; }
    public void setRoofType(RoofGenerator.RoofType roofType) {
        if (roofType != null && this.roofType != roofType) {
            this.roofType = roofType;
            markDirty();
        }
    }

    public Direction facing() { return facing; }
    public void setFacing(Direction facing) {
        if (facing != null && this.facing != facing) {
            this.facing = facing;
            markDirty();
        }
    }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isDirty() { return dirty; }
    public void markDirty() { if (!locked) this.dirty = true; }
    public void clearDirty() { this.dirty = false; }

    public long seed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; markDirty(); }

    public String parentSectionId() { return parentSectionId; }
    public void setParentSectionId(String parentSectionId) { this.parentSectionId = parentSectionId; }

    public List<PlacementTarget> cachedTargets() {
        return Collections.unmodifiableList(cachedTargets);
    }

    public void setCachedTargets(List<PlacementTarget> targets) {
        this.cachedTargets = targets != null ? new ArrayList<>(targets) : new ArrayList<>();
        this.dirty = false;
    }

    public void translate(Vec3i delta) {
        if (delta == null || (delta.x() == 0 && delta.y() == 0 && delta.z() == 0)) return;
        this.bounds = BoundingBox3i.fromCorners(bounds.min().add(delta), bounds.max().add(delta));
        List<PlacementTarget> shifted = new ArrayList<>(cachedTargets.size());
        for (PlacementTarget t : cachedTargets) {
            shifted.add(new PlacementTarget(t.pos().add(delta), t.block(), t.role()));
        }
        this.cachedTargets = shifted;
    }

    public EditableBuildingSection copy() {
        EditableBuildingSection c = new EditableBuildingSection(id, kind, bounds, stories, style, roofType, facing, seed);
        c.locked = this.locked;
        c.dirty = this.dirty;
        c.parentSectionId = this.parentSectionId;
        c.cachedTargets = new ArrayList<>(this.cachedTargets);
        return c;
    }
}
