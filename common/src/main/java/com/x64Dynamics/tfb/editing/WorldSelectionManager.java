package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class WorldSelectionManager {
    public enum SelectionMode {
        ENTIRE_BUILDING,
        COMPONENT_LEVEL,
        REGION_BOX
    }

    private SelectionMode mode = SelectionMode.ENTIRE_BUILDING;
    private BoundingBox3i primaryBox;
    private final List<SelectableComponent> components = new ArrayList<>();
    private final List<SelectableComponent> selectedComponents = new ArrayList<>();
    private int gridSnap = 1;
    private Consumer<WorldSelectionManager> changeListener;

    public WorldSelectionManager(BoundingBox3i initialBox) {
        this.primaryBox = initialBox;
    }

    public SelectionMode mode() { return mode; }
    public void setMode(SelectionMode mode) {
        this.mode = mode;
        notifyChange();
    }

    public BoundingBox3i primaryBox() { return primaryBox; }
    public void setPrimaryBox(BoundingBox3i box) {
        this.primaryBox = box;
        notifyChange();
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
        notifyChange();
    }

    public void translatePrimaryBox(Vec3i offset) {
        int snappedX = (offset.x() / gridSnap) * gridSnap;
        int snappedY = (offset.y() / gridSnap) * gridSnap;
        int snappedZ = (offset.z() / gridSnap) * gridSnap;
        Vec3i snappedOffset = new Vec3i(snappedX, snappedY, snappedZ);

        this.primaryBox = primaryBox.offset(snappedOffset);
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