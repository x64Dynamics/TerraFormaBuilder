package com.x64Dynamics.tfb.architectural.plan;

import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.Objects;

/**
 * Deterministic architectural specification for a procedural building.
 */
public record BuildingDefinition(
    String id,
    BoundingBox3i footprintBounds,
    WingConfiguration wingConfiguration,
    StylePreset style,
    MaterialPalette palette,
    RoofGenerator.RoofType roofType,
    int requestedStories,
    boolean addPorchAndGarden,
    boolean addChimney,
    boolean furnishInterior,
    float furnishingDensity,
    long seed
) {
    public BuildingDefinition {
        Objects.requireNonNull(footprintBounds, "footprintBounds");
        Objects.requireNonNull(palette, "palette");
        if (id == null || id.isBlank()) {
            id = "home-" + Long.toHexString(seed);
        }
        wingConfiguration = wingConfiguration != null ? wingConfiguration : WingConfiguration.SINGLE_RECTANGLE;
        style = style != null ? style : StylePreset.MEDIEVAL;
        roofType = roofType != null ? roofType : RoofGenerator.RoofType.GABLE;
        furnishingDensity = Math.max(0.0f, Math.min(1.0f, furnishingDensity));
    }
}
