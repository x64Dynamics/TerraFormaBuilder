package com.x64Dynamics.tfb.architectural.exterior.terrain;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;

public final class LandscapeAdapter {
    private LandscapeAdapter() {}

    public static List<PlacementTarget> generateCourtyard(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier paver = palette.get(MaterialRole.FLOOR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        int y = bounds.min().y();

        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int cx = (minX + maxX) / 2;
        int cz = (minZ + maxZ) / 2;

        // Paved paths in cross pattern
        for (int x = minX; x <= maxX; x++) {
            targets.add(new PlacementTarget(new Vec3i(x, y, cz), paver, MaterialRole.FLOOR));
        }
        for (int z = minZ; z <= maxZ; z++) {
            targets.add(new PlacementTarget(new Vec3i(cx, y, z), paver, MaterialRole.FLOOR));
        }

        // Central fountain basin
        BlockIdentifier water = BlockIdentifier.of("minecraft:water");
        BlockIdentifier rim = palette.get(MaterialRole.ROOF_RIDGE);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Vec3i pos = new Vec3i(cx + dx, y, cz + dz);
                if (dx == 0 && dz == 0) {
                    targets.add(new PlacementTarget(pos, water, MaterialRole.DECORATION));
                    targets.add(new PlacementTarget(pos.add(0, 1, 0), BlockIdentifier.of("minecraft:sea_lantern"), MaterialRole.LIGHT_SOURCE));
                } else {
                    targets.add(new PlacementTarget(pos, rim, MaterialRole.SLAB));
                }
            }
        }

        return targets;
    }

    public static List<PlacementTarget> generateRetainingWall(BoundingBox3i span, int height, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier wallBlock = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier slab = palette.get(MaterialRole.ROOF_RIDGE);

        int minX = span.min().x(), maxX = span.max().x();
        int minZ = span.min().z(), maxZ = span.max().z();
        int baseY = span.min().y();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int h = 0; h < height; h++) {
                    targets.add(new PlacementTarget(new Vec3i(x, baseY + h, z), wallBlock, MaterialRole.FOUNDATION));
                }
                // Wall coping on top
                targets.add(new PlacementTarget(new Vec3i(x, baseY + height, z), slab, MaterialRole.SLAB));
            }
        }
        return targets;
    }
}