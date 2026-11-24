package com.x64Dynamics.tfb.persistence;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;

public record BlueprintMetadata(
    String name,
    String author,
    long creationEpochMs,
    String brushId,
    BoundingBox3i bounds,
    String stylePreset
) { }
