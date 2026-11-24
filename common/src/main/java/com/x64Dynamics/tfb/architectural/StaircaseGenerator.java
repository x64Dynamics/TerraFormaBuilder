package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class StaircaseGenerator {
    private StaircaseGenerator() { }

    public static List<PlacementTarget> generateStraight(Vec3i bottom, Direction ascendDirection, int height, int width, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        Direction stairFacing = ascendDirection.opposite();
        Direction cross = ascendDirection.rotateY();

        BlockIdentifier orientedStair = stair.withProperty("facing", stairFacing.name().toLowerCase()).withProperty("half", "bottom");

        for (int step = 0; step < height; step++) {
            Vec3i stepPos = bottom.offset(ascendDirection, step).add(0, step, 0);
            for (int w = 0; w < width; w++) {
                Vec3i targetPos = stepPos.offset(cross, w);
                targets.add(new PlacementTarget(targetPos, orientedStair, MaterialRole.STAIR));
            }
        }
        return targets;
    }

    public static List<PlacementTarget> generateSpiral(Vec3i centerBottom, int height, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);

        Direction[] cycle = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        for (int y = 0; y < height; y++) {
            // Central core
            targets.add(new PlacementTarget(centerBottom.add(0, y, 0), pillar, MaterialRole.PILLAR));

            // Spiral tread
            Direction dir = cycle[y % 4];
            Direction facing = dir.rotateY().opposite();
            BlockIdentifier tread = stair.withProperty("facing", facing.name().toLowerCase()).withProperty("half", "bottom");

            targets.add(new PlacementTarget(centerBottom.add(0, y, 0).offset(dir), tread, MaterialRole.STAIR));
        }
        return targets;
    }
}
