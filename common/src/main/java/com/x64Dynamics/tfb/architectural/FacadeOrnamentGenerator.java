package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class FacadeOrnamentGenerator {
    private FacadeOrnamentGenerator() { }

    public static List<PlacementTarget> generatePilasters(BoundingBox3i wallSpan, Direction wallFacing, int spacing, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        Direction alongWall = wallFacing.rotateY();

        int minX = wallSpan.min().x(), maxX = wallSpan.max().x();
        int minZ = wallSpan.min().z(), maxZ = wallSpan.max().z();
        int minY = wallSpan.min().y(), maxY = wallSpan.max().y();

        boolean runsX = (maxX - minX) >= (maxZ - minZ);
        int length = runsX ? (maxX - minX + 1) : (maxZ - minZ + 1);

        for (int offset = 0; offset < length; offset += spacing) {
            Vec3i base = runsX ? new Vec3i(minX + offset, minY, minZ) : new Vec3i(minX, minY, minZ + offset);
            Vec3i protruded = base.offset(wallFacing);

            for (int y = minY; y <= maxY; y++) {
                targets.add(new PlacementTarget(new Vec3i(protruded.x(), y, protruded.z()), pillar, MaterialRole.PILLAR));
            }
        }
        return targets;
    }

    public static List<PlacementTarget> addWindowShuttersAndFlowerBox(Vec3i windowBottomCenter, Direction wallFacing, int windowWidth, int windowHeight, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        Direction cross = wallFacing.rotateY();
        BlockIdentifier trapdoor = palette.get(MaterialRole.FENCE); // Fallback to decorative

        int halfW = windowWidth / 2;
        Vec3i leftSide = windowBottomCenter.offset(cross, -halfW - 1);
        Vec3i rightSide = windowBottomCenter.offset(cross, halfW + 1);

        // Shutters along height
        for (int y = 0; y < windowHeight; y++) {
            targets.add(new PlacementTarget(leftSide.add(0, y, 0), trapdoor, MaterialRole.DECORATION));
            targets.add(new PlacementTarget(rightSide.add(0, y, 0), trapdoor, MaterialRole.DECORATION));
        }

        // Flower box beneath window sill
        Vec3i sillBase = windowBottomCenter.add(0, -1, 0).offset(wallFacing);
        BlockIdentifier dirt = BlockIdentifier.of("minecraft:dirt");
        for (int w = -halfW; w <= halfW; w++) {
            Vec3i boxPos = sillBase.offset(cross, w);
            targets.add(new PlacementTarget(boxPos, dirt, MaterialRole.DECORATION));
            targets.add(new PlacementTarget(boxPos.add(0, 1, 0), BlockIdentifier.of("minecraft:poppy"), MaterialRole.DECORATION));
        }
        return targets;
    }

    public static List<PlacementTarget> generateBalcony(Vec3i doorwayCenter, Direction outwardFacing, int width, int depth, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        Direction cross = outwardFacing.rotateY();
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier railing = palette.get(MaterialRole.FENCE);
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);

        int halfW = width / 2;
        int floorY = doorwayCenter.y() - 1;

        // Balcony floor & corbel supports
        for (int d = 1; d <= depth; d++) {
            for (int w = -halfW; w <= halfW; w++) {
                Vec3i floorPos = doorwayCenter.offset(outwardFacing, d).offset(cross, w).add(0, -1, 0);
                targets.add(new PlacementTarget(floorPos, slab, MaterialRole.SLAB));

                // Corbel inverted stair support underneath first row
                if (d == 1 && (w == -halfW || w == halfW || w == 0)) {
                    BlockIdentifier corbel = stair.withProperty("facing", outwardFacing.name().toLowerCase()).withProperty("half", "top");
                    targets.add(new PlacementTarget(floorPos.add(0, -1, 0), corbel, MaterialRole.DECORATION));
                }

                // Railings around perimeter
                boolean isEdge = (d == depth || w == -halfW || w == halfW);
                if (isEdge) {
                    targets.add(new PlacementTarget(floorPos.add(0, 1, 0), railing, MaterialRole.BALUSTRADE));
                }
            }
        }
        return targets;
    }
}
