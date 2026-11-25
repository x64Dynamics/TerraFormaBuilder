package com.x64Dynamics.tfb.construction.editing;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy;

import java.util.Collections;
import java.util.Map;

public record DesignHistorySnapshot(
    String brushId,
    BoundingBox3i bounds,
    String stylePreset,
    FoundationStrategy foundationStrategy,
    Map<String, String> paletteOverrides
) {
    public DesignHistorySnapshot {
        paletteOverrides = paletteOverrides == null ? Collections.emptyMap() : Collections.unmodifiableMap(paletteOverrides);
    }
}
