package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;

public final class LayeredFacadeComposer {
    private LayeredFacadeComposer() {}

    public static List<PlacementTarget> generateCorniceTrim(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        int topY = bounds.max().y();

        int minX = bounds.min().x() - 1, maxX = bounds.max().x() + 1;
        int minZ = bounds.min().z() - 1, maxZ = bounds.max().z() + 1;

        // Inverted stairs facing outward as eave corbeling
        for (int x = minX; x <= maxX; x++) {
            targets.add(new PlacementTarget(new Vec3i(x, topY, minZ), stair.withProperty("facing", "north").withProperty("half", "top"), MaterialRole.ROOF_OVERHANG));
            targets.add(new PlacementTarget(new Vec3i(x, topY, maxZ), stair.withProperty("facing", "south").withProperty("half", "top"), MaterialRole.ROOF_OVERHANG));
        }
        for (int z = minZ; z <= maxZ; z++) {
            targets.add(new PlacementTarget(new Vec3i(minX, topY, z), stair.withProperty("facing", "west").withProperty("half", "top"), MaterialRole.ROOF_OVERHANG));
            targets.add(new PlacementTarget(new Vec3i(maxX, topY, z), stair.withProperty("facing", "east").withProperty("half", "top"), MaterialRole.ROOF_OVERHANG));
        }

        return targets;
    }

    public static List<PlacementTarget> generateRusticatedBase(BoundingBox3i bounds, int baseHeight, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier foundationBlock = palette.get(MaterialRole.FOUNDATION);

        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int baseY = bounds.min().y();

        for (int y = baseY; y < baseY + baseHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, y, minZ), foundationBlock, MaterialRole.FOUNDATION));
                targets.add(new PlacementTarget(new Vec3i(x, y, maxZ), foundationBlock, MaterialRole.FOUNDATION));
            }
            for (int z = minZ; z <= maxZ; z++) {
                targets.add(new PlacementTarget(new Vec3i(minX, y, z), foundationBlock, MaterialRole.FOUNDATION));
                targets.add(new PlacementTarget(new Vec3i(maxX, y, z), foundationBlock, MaterialRole.FOUNDATION));
            }
        }
        return targets;
    }

    public static List<PlacementTarget> generateEntrancePortico(Vec3i entranceCenter, Direction outwardFacing, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        BlockIdentifier slab = palette.get(MaterialRole.ROOF_RIDGE);

        Direction cross = outwardFacing.rotateY();
        Vec3i leftColBase = entranceCenter.offset(cross, -2).offset(outwardFacing, 2);
        Vec3i rightColBase = entranceCenter.offset(cross, 2).offset(outwardFacing, 2);

        // 2 Pillars
        for (int y = 0; y < 4; y++) {
            targets.add(new PlacementTarget(leftColBase.add(0, y, 0), pillar, MaterialRole.PILLAR));
            targets.add(new PlacementTarget(rightColBase.add(0, y, 0), pillar, MaterialRole.PILLAR));
        }

        // Portico pediment roof on top
        int roofY = entranceCenter.y() + 4;
        for (int c = -2; c <= 2; c++) {
            for (int d = 0; d <= 2; d++) {
                targets.add(new PlacementTarget(entranceCenter.offset(cross, c).offset(outwardFacing, d).add(0, 4, 0), slab, MaterialRole.ROOF_SLOPE));
            }
        }

        return targets;
    }
}