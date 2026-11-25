package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Public plaza, market square, park, or waterfront dock within a WorldProject.
public final class PublicSpace {
    public enum PublicSpaceType {
        PLAZA,
        MARKET_SQUARE,
        PARK_GARDEN,
        WATERFRONT_DOCK
    }

    private final String id;
    private String name;
    private PublicSpaceType type;
    private BoundingBox3i bounds;
    private boolean locked;
    private final List<PlacementTarget> targets = new ArrayList<>();

    public PublicSpace(String id, String name, PublicSpaceType type, BoundingBox3i bounds) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bounds = bounds;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public PublicSpaceType type() { return type; }
    public void setType(PublicSpaceType type) { this.type = type; }
    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public List<PlacementTarget> targets() { return Collections.unmodifiableList(targets); }
    public void setTargets(List<PlacementTarget> newTargets) {
        targets.clear();
        if (newTargets != null) targets.addAll(newTargets);
    }
}
