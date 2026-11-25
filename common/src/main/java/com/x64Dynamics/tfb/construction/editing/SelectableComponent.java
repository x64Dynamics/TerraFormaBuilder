package com.x64Dynamics.tfb.construction.editing;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SelectableComponent {
    private final String id;
    private final ArchitecturalComponentType type;
    private BoundingBox3i bounds;
    private final List<PlacementTarget> targets;
    private boolean selected = false;

    public SelectableComponent(String id, ArchitecturalComponentType type, BoundingBox3i bounds, List<PlacementTarget> targets) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.type = type;
        this.bounds = bounds;
        this.targets = new ArrayList<>(targets != null ? targets : Collections.emptyList());
    }

    public String id() { return id; }
    public ArchitecturalComponentType type() { return type; }
    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; }

    public List<PlacementTarget> targets() { return Collections.unmodifiableList(targets); }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public boolean containsBlock(Vec3i pos) {
        return bounds.contains(pos);
    }
}