package com.x64Dynamics.tfb.architectural.exterior.facades;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
public final class PillarGenerator {
    private PillarGenerator() { }

    public static List<PlacementTarget> generatePillar(Vec3i base, int height, boolean capitalAndBase, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        if (height <= 0) return targets;

        BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);
        BlockIdentifier stairBlock = palette.get(MaterialRole.STAIR);

        for (int y = 0; y < height; y++) {
            Vec3i pos = base.add(0, y, 0);
            targets.add(new PlacementTarget(pos, pillarBlock, MaterialRole.PILLAR));
        }

        if (capitalAndBase && height >= 3) {
            Vec3i foot = base;
            Vec3i top = base.add(0, height - 1, 0);

            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockIdentifier baseStair = stairBlock.withProperty("facing", dir.opposite().name().toLowerCase()).withProperty("half", "bottom");
                BlockIdentifier capStair = stairBlock.withProperty("facing", dir.opposite().name().toLowerCase()).withProperty("half", "top");

                targets.add(new PlacementTarget(foot.offset(dir), baseStair, MaterialRole.DECORATION));
                targets.add(new PlacementTarget(top.offset(dir), capStair, MaterialRole.DECORATION));
            }
        }
        return targets;
    }
}
