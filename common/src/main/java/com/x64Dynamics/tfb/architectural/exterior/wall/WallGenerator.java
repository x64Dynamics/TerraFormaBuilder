package com.x64Dynamics.tfb.architectural.exterior.wall;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
public final class WallGenerator {
    private WallGenerator() { }

    public static List<PlacementTarget> generateBoxWalls(BoundingBox3i bounds, int thickness, boolean crenellations, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minY = bounds.min().y(), maxY = bounds.max().y();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();

        for (int y = minY; y <= maxY; y++) {
            boolean isTop = (y == maxY);
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int distFromOuter = Math.min(
                        Math.min(x - minX, maxX - x),
                        Math.min(z - minZ, maxZ - z)
                    );

                    if (distFromOuter < thickness) {
                        boolean isCorner = (x == minX || x == maxX) && (z == minZ || z == maxZ);

                        if (crenellations && isTop) {
                            int perimeterStep = (x - minX) + (z - minZ);
                            if (perimeterStep % 2 != 0 && !isCorner) {
                                continue;
                            }
                        }

                        MaterialRole role = isCorner ? MaterialRole.WALL_CORNER : MaterialRole.WALL_PRIMARY;
                        targets.add(new PlacementTarget(new Vec3i(x, y, z), palette.get(role), role));
                    }
                }
            }
        }
        return targets;
    }
}
