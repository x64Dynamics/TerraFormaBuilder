package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class AdvancedRoofFeatures {
    private AdvancedRoofFeatures() { }

    public static List<PlacementTarget> generateDormer(Vec3i roofSlopePoint, Direction outwardFacing, int width, int height, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        Direction cross = outwardFacing.rotateY();
        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier window = palette.get(MaterialRole.WINDOW);

        int halfW = width / 2;

        // Front window & walls
        for (int h = 0; h < height; h++) {
            for (int w = -halfW; w <= halfW; w++) {
                Vec3i pos = roofSlopePoint.offset(cross, w).add(0, h, 0);
                if (w == 0 && h > 0 && h < height - 1) {
                    targets.add(new PlacementTarget(pos, window, MaterialRole.WINDOW));
                } else {
                    targets.add(new PlacementTarget(pos, wall, MaterialRole.WALL_PRIMARY));
                }
            }
        }

        // Gabled micro-roof over dormer
        int topY = roofSlopePoint.y() + height;
        BlockIdentifier leftStair = stair.withProperty("facing", cross.name().toLowerCase()).withProperty("half", "bottom");
        BlockIdentifier rightStair = stair.withProperty("facing", cross.opposite().name().toLowerCase()).withProperty("half", "bottom");

        for (int d = 0; d < 3; d++) {
            Vec3i centerTop = roofSlopePoint.offset(outwardFacing.opposite(), d).add(0, height, 0);
            targets.add(new PlacementTarget(centerTop.offset(cross, -halfW - 1), leftStair, MaterialRole.ROOF_SLOPE));
            targets.add(new PlacementTarget(centerTop.offset(cross, halfW + 1), rightStair, MaterialRole.ROOF_SLOPE));
            targets.add(new PlacementTarget(centerTop, palette.get(MaterialRole.ROOF_RIDGE), MaterialRole.ROOF_RIDGE));
        }

        return targets;
    }

    public static List<PlacementTarget> generateChimney(Vec3i baseRoofPos, int heightAboveRoof, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier brick = BlockIdentifier.of("minecraft:bricks");
        BlockIdentifier campfire = BlockIdentifier.of("minecraft:campfire");
        BlockIdentifier trapdoor = BlockIdentifier.of("minecraft:spruce_trapdoor");

        // Flue column
        for (int y = 0; y < heightAboveRoof; y++) {
            targets.add(new PlacementTarget(baseRoofPos.add(0, y, 0), brick, MaterialRole.WALL_PRIMARY));
        }

        // Smoky chimney top
        Vec3i top = baseRoofPos.add(0, heightAboveRoof, 0);
        targets.add(new PlacementTarget(top, campfire, MaterialRole.DECORATION));

        // Chimney pot / surround
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            targets.add(new PlacementTarget(top.offset(d), trapdoor, MaterialRole.DECORATION));
        }
        return targets;
    }
}
