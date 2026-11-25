package com.x64Dynamics.tfb.architectural.layout;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
public final class FloorGenerator {
    private FloorGenerator() { }

    public static List<PlacementTarget> generateFloor(BoundingBox3i bounds, int yLevel, boolean borderTrim, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                boolean onBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                MaterialRole role = (borderTrim && onBorder) ? MaterialRole.BEAM : MaterialRole.FLOOR;
                targets.add(new PlacementTarget(new Vec3i(x, yLevel, z), palette.get(role), role));
            }
        }
        return targets;
    }

    public static List<PlacementTarget> generateCeiling(BoundingBox3i bounds, int yLevel, int beamSpacing, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                boolean isBeam = (beamSpacing > 0) && ((z - minZ) % beamSpacing == 0 || (x - minX) % beamSpacing == 0);
                MaterialRole role = isBeam ? MaterialRole.BEAM : MaterialRole.CEILING;
                targets.add(new PlacementTarget(new Vec3i(x, yLevel, z), palette.get(role), role));
            }
        }
        return targets;
    }
}
