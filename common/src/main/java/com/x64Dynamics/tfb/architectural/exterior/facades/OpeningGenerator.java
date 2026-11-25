package com.x64Dynamics.tfb.architectural.exterior.facades;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
public final class OpeningGenerator {
    private OpeningGenerator() { }

    public static List<PlacementTarget> carveDoorway(Vec3i baseCenter, Direction wallFacing, int doorWidth, int doorHeight, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        Direction cross = wallFacing.rotateY();
        BlockIdentifier door = palette.get(MaterialRole.DOOR);
        BlockIdentifier frame = palette.get(MaterialRole.BEAM);

        int startW = -(Math.max(1, doorWidth) - 1) / 2;
        int endW = startW + Math.max(1, doorWidth) - 1;
        for (int y = 0; y < doorHeight; y++) {
            for (int w = startW; w <= endW; w++) {
                Vec3i pos = baseCenter.offset(cross, w).add(0, y, 0);
                if (y == 0) {
                    BlockIdentifier orientedDoor = door.withProperty("facing", wallFacing.name().toLowerCase()).withProperty("half", "lower");
                    targets.add(new PlacementTarget(pos, orientedDoor, MaterialRole.DOOR));
                } else if (y == 1) {
                    BlockIdentifier orientedDoor = door.withProperty("facing", wallFacing.name().toLowerCase()).withProperty("half", "upper");
                    targets.add(new PlacementTarget(pos, orientedDoor, MaterialRole.DOOR));
                } else {
                    targets.add(new PlacementTarget(pos, BlockIdentifier.AIR, MaterialRole.DOOR));
                }
            }
        }

        // Surrounding lintel/frame
        int topY = doorHeight;
        for (int w = startW - 1; w <= endW + 1; w++) {
            Vec3i lintelPos = baseCenter.offset(cross, w).add(0, topY, 0);
            targets.add(new PlacementTarget(lintelPos, frame, MaterialRole.FRAME));
        }
        return targets;
    }

    public static List<PlacementTarget> generateWindows(BoundingBox3i wallSpan, int windowSpacing, int windowWidth, int windowHeight, int bottomOffset, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        if (windowWidth <= 0 || windowHeight <= 0 || windowSpacing < 0) {
            return targets;
        }
        BlockIdentifier glass = palette.get(MaterialRole.WINDOW);

        int minX = wallSpan.min().x(), maxX = wallSpan.max().x();
        int minZ = wallSpan.min().z(), maxZ = wallSpan.max().z();
        int baseY = wallSpan.min().y() + bottomOffset;

        boolean runsAlongX = (maxX - minX) >= (maxZ - minZ);

        if (runsAlongX) {
            int length = maxX - minX + 1;
            for (int offset = windowSpacing; offset + windowWidth <= length - windowSpacing; offset += (windowWidth + windowSpacing)) {
                for (int w = 0; w < windowWidth; w++) {
                    int x = minX + offset + w;
                    for (int h = 0; h < windowHeight; h++) {
                        targets.add(new PlacementTarget(new Vec3i(x, baseY + h, minZ), glass, MaterialRole.WINDOW));
                    }
                }
            }
        } else {
            int length = maxZ - minZ + 1;
            for (int offset = windowSpacing; offset + windowWidth <= length - windowSpacing; offset += (windowWidth + windowSpacing)) {
                for (int w = 0; w < windowWidth; w++) {
                    int z = minZ + offset + w;
                    for (int h = 0; h < windowHeight; h++) {
                        targets.add(new PlacementTarget(new Vec3i(minX, baseY + h, z), glass, MaterialRole.WINDOW));
                    }
                }
            }
        }
        return targets;
    }
}
