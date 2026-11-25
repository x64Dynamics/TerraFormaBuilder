package com.x64Dynamics.tfb.architectural.city;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Graph-based urban street and path network connecting districts, parcels, and public spaces.
public final class RoadNetwork {
    public enum RoadTier {
        ARTERIAL(5),
        STREET(3),
        ALLEY(2),
        BOARDWALK(3);

        private final int defaultWidth;
        RoadTier(int defaultWidth) { this.defaultWidth = defaultWidth; }
        public int defaultWidth() { return defaultWidth; }
    }

    public record RoadSegment(
        String id,
        RoadTier tier,
        List<Vec3i> waypoints,
        int width
    ) { }

    private final List<RoadSegment> segments = new ArrayList<>();
    private final List<Vec3i> intersections = new ArrayList<>();
    private final List<PlacementTarget> resolvedTargets = new ArrayList<>();

    public List<RoadSegment> segments() { return Collections.unmodifiableList(segments); }
    public List<Vec3i> intersections() { return Collections.unmodifiableList(intersections); }
    public List<PlacementTarget> resolvedTargets() { return Collections.unmodifiableList(resolvedTargets); }

    public void addSegment(RoadSegment segment) { segments.add(segment); }
    public void addIntersection(Vec3i center) { intersections.add(center); }
    public void setResolvedTargets(List<PlacementTarget> targets) {
        resolvedTargets.clear();
        if (targets != null) resolvedTargets.addAll(targets);
    }

    public void clear() {
        segments.clear();
        intersections.clear();
        resolvedTargets.clear();
    }
}
