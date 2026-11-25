package com.x64Dynamics.tfb.architectural.terrain;

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

// Generates terrain-hugging walkways, stepped approaches, foundation weathering,
// and organic foliage/lantern accents around procedural buildings.
public final class ExteriorLandscaper {
    private ExteriorLandscaper() {}

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

        com.x64Dynamics.tfb.geometry.Direction facing = inferFacing(bounds, doorX, doorZ);
        com.x64Dynamics.tfb.geometry.Direction cross = facing.rotateY();
        Vec3i doorOrigin = new Vec3i(doorX, baseY, doorZ);

        int prevY = baseY;
        for (int dist = 3; dist <= 8; dist++) {
            Vec3i centerStep = doorOrigin.offset(facing, dist);
            int groundY = heightmap != null ? heightmap.getElevation(centerStep.x(), centerStep.z()) : baseY;
            int stepY = clampStep(prevY, groundY);
            for (int dCross = -1; dCross <= 1; dCross++) {
                Vec3i col = centerStep.offset(cross, dCross);
                int px = col.x();
                int pz = col.z();
                BlockIdentifier surface = (dCross == 0) ? dirtPath : ((dist + dCross) % 2 == 0 ? dirtPath : cobble);
                targets.add(new PlacementTarget(new Vec3i(px, stepY, pz), surface, MaterialRole.FLOOR));
                // Fill below step so walkway never floats over slopes
                int localGround = heightmap != null ? heightmap.getElevation(px, pz) : baseY;
                for (int fy = localGround; fy < stepY; fy++) {
                    targets.add(new PlacementTarget(new Vec3i(px, fy, pz), cobble, MaterialRole.FOUNDATION));
                }
                if (stepY > prevY && dCross == 0) {
                    targets.add(new PlacementTarget(
                        new Vec3i(px, stepY + 1, pz),
                        stair.withProperty("facing", facing.name().toLowerCase()),
                        MaterialRole.STAIR
                    ));
                } else if (stepY < prevY && dCross == 0) {
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
        Vec3i postCol = doorOrigin.offset(facing, 6).offset(cross, 2);
        int postX = postCol.x();
        int postZ = postCol.z();
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

    private static com.x64Dynamics.tfb.geometry.Direction inferFacing(BoundingBox3i bounds, int doorX, int doorZ) {
        int distN = Math.abs(doorZ - bounds.min().z());
        int distS = Math.abs(bounds.max().z() - doorZ);
        int distW = Math.abs(doorX - bounds.min().x());
        int distE = Math.abs(bounds.max().x() - doorX);
        int min = Math.min(Math.min(distN, distS), Math.min(distW, distE));
        if (min == distN) return com.x64Dynamics.tfb.geometry.Direction.NORTH;
        if (min == distW) return com.x64Dynamics.tfb.geometry.Direction.WEST;
        if (min == distE) return com.x64Dynamics.tfb.geometry.Direction.EAST;
        return com.x64Dynamics.tfb.geometry.Direction.SOUTH;
    }
}
