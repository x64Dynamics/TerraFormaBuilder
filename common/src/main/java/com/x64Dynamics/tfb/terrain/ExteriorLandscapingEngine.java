package com.x64Dynamics.tfb.terrain;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates terrain-hugging walkways, stepped approaches, foundation weathering,
 * and organic foliage/lantern accents around procedural buildings.
 */
public final class ExteriorLandscapingEngine {
    private ExteriorLandscapingEngine() {}

    public static List<PlacementTarget> generateTerrainApproachAndYard(
            BoundingBox3i bounds,
            int doorX,
            int doorZ,
            HeightmapGrid heightmap,
            MaterialPalette palette,
            StylePreset style,
            long seed) {
        List<PlacementTarget> targets = new ArrayList<>();
        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);

        int baseY = bounds.min().y();
        BlockIdentifier dirtPath = style == StylePreset.MINIMALIST || style == StylePreset.FANTASY
            ? BlockIdentifier.of("minecraft:sandstone")
            : BlockIdentifier.of("minecraft:dirt_path");
        BlockIdentifier cobble = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

        int prevY = baseY;
        for (int pz = doorZ + 3; pz <= doorZ + 8; pz++) {
            int groundY = heightmap != null ? heightmap.getElevation(doorX, pz) : baseY;
            int stepY = clampStep(prevY, groundY);
            for (int dx = -1; dx <= 1; dx++) {
                int px = doorX + dx;
                BlockIdentifier surface = (dx == 0) ? dirtPath : ((pz + dx) % 2 == 0 ? dirtPath : cobble);
                targets.add(new PlacementTarget(new Vec3i(px, stepY, pz), surface, MaterialRole.FLOOR));
                // Fill below step so walkway never floats over slopes
                int localGround = heightmap != null ? heightmap.getElevation(px, pz) : baseY;
                for (int fy = localGround; fy < stepY; fy++) {
                    targets.add(new PlacementTarget(new Vec3i(px, fy, pz), cobble, MaterialRole.FOUNDATION));
                }
                if (stepY > prevY && dx == 0) {
                    targets.add(new PlacementTarget(
                        new Vec3i(px, stepY + 1, pz),
                        stair.withProperty("facing", "north"),
                        MaterialRole.STAIR
                    ));
                } else if (stepY < prevY && dx == 0) {
                    targets.add(new PlacementTarget(
                        new Vec3i(px, prevY, pz),
                        slab.withProperty("type", "bottom"),
                        MaterialRole.SLAB
                    ));
                }
            }
            prevY = stepY;
        }

        // Lantern post at walkway entrance
        int postX = doorX + 2;
        int postZ = doorZ + 6;
        int postGround = heightmap != null ? heightmap.getElevation(postX, postZ) : baseY;
        targets.add(new PlacementTarget(new Vec3i(postX, postGround + 1, postZ), fence, MaterialRole.FENCE));
        targets.add(new PlacementTarget(new Vec3i(postX, postGround + 2, postZ), fence, MaterialRole.FENCE));
        targets.add(new PlacementTarget(new Vec3i(postX, postGround + 3, postZ), lantern, MaterialRole.LIGHT_SOURCE));

        // Organic corner bushes & flower clusters hugging terrain
        BlockIdentifier leafBlock = style == StylePreset.JAPANESE
            ? BlockIdentifier.of("minecraft:cherry_leaves")
            : style == StylePreset.MINIMALIST
                ? BlockIdentifier.of("minecraft:azalea_leaves")
                : BlockIdentifier.of("minecraft:flowering_azalea_leaves");

        int[][] bushAnchors = {
            {bounds.min().x() - 1, bounds.min().z() - 1},
            {bounds.max().x() + 1, bounds.min().z() - 1},
            {bounds.min().x() - 1, bounds.max().z() + 1},
            {bounds.max().x() + 1, bounds.max().z() + 1}
        };
        for (int[] anchor : bushAnchors) {
            int bx = anchor[0];
            int bz = anchor[1];
            int gy = heightmap != null ? heightmap.getElevation(bx, bz) : baseY;
            targets.add(new PlacementTarget(new Vec3i(bx, gy + 1, bz), leafBlock, MaterialRole.DECORATION));
            if (rng.nextBoolean()) {
                targets.add(new PlacementTarget(new Vec3i(bx, gy + 2, bz), leafBlock, MaterialRole.DECORATION));
            }
        }

        return targets;
    }

    private static int clampStep(int prevY, int targetY) {
        if (targetY > prevY + 1) return prevY + 1;
        if (targetY < prevY - 1) return prevY - 1;
        return targetY;
    }
}
