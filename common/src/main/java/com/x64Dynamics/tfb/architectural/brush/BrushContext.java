package com.x64Dynamics.tfb.architectural.brush;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.architectural.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.architectural.terrain.HeightmapGrid;

import java.util.*;

public final class BrushContext {
    private final BoundingBox3i bounds;
    private final List<Vec3i> pathPoints;
    private final MaterialPalette palette;
    private final HeightmapGrid heightmap;
    private final FoundationStrategy foundationStrategy;
    private final Map<String, Object> properties;

    public BrushContext(
        BoundingBox3i bounds,
        List<Vec3i> pathPoints,
        MaterialPalette palette,
        HeightmapGrid heightmap,
        FoundationStrategy foundationStrategy,
        Map<String, Object> properties
    ) {
        this.bounds = bounds;
        this.pathPoints = pathPoints == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(pathPoints));
        this.palette = palette;
        this.heightmap = heightmap;
        this.foundationStrategy = foundationStrategy == null ? FoundationStrategy.PILLARS_STILTS : foundationStrategy;
        this.properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(properties));
    }

    public BoundingBox3i bounds() { return bounds; }
    public List<Vec3i> pathPoints() { return pathPoints; }
    public MaterialPalette palette() { return palette; }
    public HeightmapGrid heightmap() { return heightmap; }
    public FoundationStrategy foundationStrategy() { return foundationStrategy; }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object val = properties.get(key);
        return val != null ? (T) val : defaultValue;
    }
}
