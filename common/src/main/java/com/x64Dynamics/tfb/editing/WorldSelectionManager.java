package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WorldSelectionManager {
    public enum SelectionMode {
        ENTIRE_BUILDING,
        COMPONENT_LEVEL,
        REGION_BOX
    }

    private SelectionMode mode = SelectionMode.ENTIRE_BUILDING;
    private Vec3i pos1 = null;
    private Vec3i pos2 = null;
    private BoundingBox3i primaryBox;
    private final List<SelectableComponent> components = new ArrayList<>();
    private final List<SelectableComponent> selectedComponents = new ArrayList<>();
    private final Map<String, BoundingBox3i> savedSelections = new LinkedHashMap<>();
    private int gridSnap = 1;
    private Consumer<WorldSelectionManager> changeListener;

    public WorldSelectionManager(BoundingBox3i initialBox) {
        this.primaryBox = initialBox;
        if (initialBox != null) {
            this.pos1 = initialBox.min();
            this.pos2 = initialBox.max();
        }
    }

    public SelectionMode mode() { return mode; }
    public void setMode(SelectionMode mode) {
        this.mode = mode;
        notifyChange();
    }

    public Vec3i pos1() { return pos1; }
    public Vec3i pos2() { return pos2; }
    public boolean hasPos1() { return pos1 != null; }
    public boolean hasPos2() { return pos2 != null; }
    public boolean hasCompleteSelection() { return pos1 != null && pos2 != null; }

    public void setPos1(Vec3i pos) {
        this.pos1 = pos;
        if (pos != null) {
            if (pos2 != null) {
                this.primaryBox = BoundingBox3i.fromCorners(pos1, pos2);
            } else if (primaryBox != null) {
                this.primaryBox = BoundingBox3i.fromOriginAndSize(pos, primaryBox.sizeX(), primaryBox.sizeY(), primaryBox.sizeZ());
                this.pos2 = primaryBox.max();
            } else {
                this.primaryBox = new BoundingBox3i(pos, pos);
            }
        }
        notifyChange();
    }

    public void setPos2(Vec3i pos) {
        this.pos2 = pos;
        if (pos != null) {
            if (pos1 != null) {
                this.primaryBox = BoundingBox3i.fromCorners(pos1, pos2);
            } else if (primaryBox != null) {
                this.pos1 = primaryBox.min();
                this.primaryBox = BoundingBox3i.fromCorners(pos1, pos);
            } else {
                this.pos1 = pos;
                this.primaryBox = new BoundingBox3i(pos, pos);
            }
        }
        notifyChange();
    }

    public void clearPositions() {
        this.pos1 = null;
        this.pos2 = null;
        notifyChange();
    }

    public BoundingBox3i primaryBox() { return primaryBox; }
    public void setPrimaryBox(BoundingBox3i box) {
        this.primaryBox = box;
        if (box != null) {
            this.pos1 = box.min();
            this.pos2 = box.max();
        }
        notifyChange();
    }

    public void expandSelection(int amount) {
        if (primaryBox == null) return;
        this.primaryBox = primaryBox.inflate(amount);
        this.pos1 = primaryBox.min();
        this.pos2 = primaryBox.max();
        notifyChange();
    }

    public void contractSelection(int amount) {
        if (primaryBox == null) return;
        if (primaryBox.sizeX() > amount * 2 && primaryBox.sizeY() > amount * 2 && primaryBox.sizeZ() > amount * 2) {
            this.primaryBox = primaryBox.inflate(-amount);
            this.pos1 = primaryBox.min();
            this.pos2 = primaryBox.max();
            notifyChange();
        }
    }

    public Vec3i center() {
        if (primaryBox != null) {
            Vec3d c = primaryBox.center();
            return new Vec3i((int) Math.floor(c.x()), (int) Math.floor(c.y()), (int) Math.floor(c.z()));
        }
        return pos1 != null ? pos1 : Vec3i.ZERO;
    }

    public long volume() {
        return primaryBox != null ? primaryBox.volume() : 0;
    }

    public Vec3i dimensions() {
        return primaryBox != null ? new Vec3i(primaryBox.sizeX(), primaryBox.sizeY(), primaryBox.sizeZ()) : Vec3i.ZERO;
    }

    public void saveSelection(String name) {
        if (primaryBox != null && name != null && !name.trim().isEmpty()) {
            savedSelections.put(name.trim(), primaryBox);
        }
    }

    public boolean loadSelection(String name) {
        BoundingBox3i box = savedSelections.get(name);
        if (box != null) {
            setPrimaryBox(box);
            return true;
        }
        return false;
    }

    public Map<String, BoundingBox3i> savedSelections() {
        return Collections.unmodifiableMap(savedSelections);
    }

    public int gridSnap() { return gridSnap; }
    public void setGridSnap(int snap) { this.gridSnap = Math.max(1, snap); }

    public void setChangeListener(Consumer<WorldSelectionManager> listener) {
        this.changeListener = listener;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.accept(this);
        }
    }

    public List<SelectableComponent> allComponents() { return Collections.unmodifiableList(components); }
    public List<SelectableComponent> selectedComponents() { return Collections.unmodifiableList(selectedComponents); }

    public void setComponents(List<SelectableComponent> comps) {
        this.components.clear();
        this.selectedComponents.clear();
        if (comps != null) {
            this.components.addAll(comps);
        }
        notifyChange();
    }

    public void selectComponent(SelectableComponent comp, boolean additive) {
        if (!additive) {
            clearSelection();
        }
        if (comp != null && !selectedComponents.contains(comp)) {
            comp.setSelected(true);
            selectedComponents.add(comp);
            notifyChange();
        }
    }

    public void deselectComponent(SelectableComponent comp) {
        if (comp != null && selectedComponents.remove(comp)) {
            comp.setSelected(false);
            notifyChange();
        }
    }

    public void clearSelection() {
        for (SelectableComponent comp : selectedComponents) {
            comp.setSelected(false);
        }
        selectedComponents.clear();
        notifyChange();
    }

    public void resizePrimaryBox(Direction face, int deltaAmount) {
        int snappedDelta = (deltaAmount / gridSnap) * gridSnap;
        if (snappedDelta == 0) return;

        Vec3i min = primaryBox.min();
        Vec3i max = primaryBox.max();

        switch (face) {
            case EAST -> max = new Vec3i(max.x() + snappedDelta, max.y(), max.z());
            case WEST -> min = new Vec3i(min.x() - snappedDelta, min.y(), min.z());
            case UP -> max = new Vec3i(max.x(), max.y() + snappedDelta, max.z());
            case DOWN -> min = new Vec3i(min.x(), min.y() - snappedDelta, min.z());
            case SOUTH -> max = new Vec3i(max.x(), max.y(), max.z() + snappedDelta);
            case NORTH -> min = new Vec3i(min.x(), min.y(), min.z() - snappedDelta);
        }

        // Ensure minimum dimensions
        int minX = Math.min(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxX = Math.max(min.x(), max.x());
        int maxY = Math.max(min.y(), max.y());
        int maxZ = Math.max(min.z(), max.z());

        this.primaryBox = new BoundingBox3i(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
        this.pos1 = this.primaryBox.min();
        this.pos2 = this.primaryBox.max();
        notifyChange();
    }

    public void translatePrimaryBox(Vec3i offset) {
        int snappedX = (offset.x() / gridSnap) * gridSnap;
        int snappedY = (offset.y() / gridSnap) * gridSnap;
        int snappedZ = (offset.z() / gridSnap) * gridSnap;
        Vec3i snappedOffset = new Vec3i(snappedX, snappedY, snappedZ);

        this.primaryBox = primaryBox.offset(snappedOffset);
        this.pos1 = this.primaryBox.min();
        this.pos2 = this.primaryBox.max();
        notifyChange();
    }

    public SelectableComponent findComponentAt(Vec3i point) {
        for (SelectableComponent comp : components) {
            if (comp.containsBlock(point)) {
                return comp;
            }
        }
        return null;
    }
}