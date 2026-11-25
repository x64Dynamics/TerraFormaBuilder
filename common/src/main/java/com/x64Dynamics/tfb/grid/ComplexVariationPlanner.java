package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.Random;

/**
 * Generates intentional, rule-driven architectural variations between buildings
 * sharing a complex so they maintain shared identity without looking like identical clones.
 */
public final class ComplexVariationPlanner {
    public record VariedSpec(
        RoofGenerator.RoofType roofType,
        int storyHeight,
        boolean addPorch,
        boolean addChimney,
        MaterialPalette palette,
        long buildingSeed
    ) { }

    private ComplexVariationPlanner() { }

    public static VariedSpec deriveSpec(GridBuilding building, int indexInComplex) {
        Random rng = new Random(building.seed() ^ (indexInComplex * 0x9E3779B97F4A7C15L));

        RoofGenerator.RoofType roof = building.roofType();
        if (indexInComplex > 0 && building.function() != GridBuilding.BuildingFunction.TOWER) {
            RoofGenerator.RoofType[] alt = {
                building.roofType(),
                RoofGenerator.RoofType.GABLE,
                RoofGenerator.RoofType.HIP,
                RoofGenerator.RoofType.MANSARD
            };
            if (building.roofType() != RoofGenerator.RoofType.FLAT) {
                roof = alt[rng.nextInt(alt.length)];
            }
        }

        // Subtle story-height variation (4 vs 5) for adjacent buildings unless stacked
        int storyH = building.storyHeight();
        if (indexInComplex % 2 == 1 && storyH == 4 && rng.nextBoolean()) {
            storyH = 5;
        }

        MaterialPalette base = building.palette();
        MaterialPalette varied = new MaterialPalette(base.id() + "_v" + indexInComplex);
        for (MaterialRole role : MaterialRole.values()) {
            varied.set(role, base.get(role));
        }
        // Swap primary & accent wall on alternating complex buildings for cohesive variety
        if (indexInComplex % 2 == 1) {
            varied.set(MaterialRole.WALL_PRIMARY, base.get(MaterialRole.WALL_ACCENT));
            varied.set(MaterialRole.WALL_ACCENT, base.get(MaterialRole.WALL_PRIMARY));
        }

        boolean addPorch = building.function() == GridBuilding.BuildingFunction.RESIDENTIAL || (indexInComplex == 0);
        boolean addChimney = building.function() != GridBuilding.BuildingFunction.COURTYARD && (indexInComplex % 2 == 0);

        return new VariedSpec(roof, storyH, addPorch, addChimney, varied, building.seed() + indexInComplex * 31L);
    }
}
