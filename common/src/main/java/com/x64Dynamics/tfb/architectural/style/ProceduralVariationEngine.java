package com.x64Dynamics.tfb.architectural.style;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ProceduralVariationEngine {
    private ProceduralVariationEngine() {}

    public static List<PlacementTarget> applyFacadeVariation(
        List<PlacementTarget> targets,
        MaterialPalette palette,
        long seed,
        float variationRate
    ) {
        if (variationRate <= 0.0f) return new ArrayList<>(targets);

        Random random = new Random(seed);
        List<PlacementTarget> result = new ArrayList<>(targets.size());
        BlockIdentifier wallAccent = palette.get(MaterialRole.WALL_ACCENT);

        for (PlacementTarget target : targets) {
            if (target.role() == MaterialRole.WALL_PRIMARY && random.nextFloat() < variationRate) {
                result.add(new PlacementTarget(target.pos(), wallAccent, MaterialRole.WALL_ACCENT));
            } else {
                result.add(target);
            }
        }
        return result;
    }
}