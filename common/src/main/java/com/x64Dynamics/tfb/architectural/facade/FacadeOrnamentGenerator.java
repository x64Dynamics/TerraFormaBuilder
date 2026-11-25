package com.x64Dynamics.tfb.architectural.facade;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
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

    /**
     * Generates fluted classical pilasters with plinth bases and stair-corbel capitals.
     */
    public static List<PlacementTarget> generateFlutedPilasters(BoundingBox3i wallSpan, Direction wallFacing, int spacing, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        BlockIdentifier plinth = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier capital = palette.get(MaterialRole.STAIR)
            .withProperty("facing", wallFacing.opposite().name().toLowerCase())
            .withProperty("half", "top");

        int minX = wallSpan.min().x(), maxX = wallSpan.max().x();
        int minZ = wallSpan.min().z(), maxZ = wallSpan.max().z();
        int minY = wallSpan.min().y(), maxY = wallSpan.max().y();
        boolean runsX = (maxX - minX) >= (maxZ - minZ);
        int length = runsX ? (maxX - minX + 1) : (maxZ - minZ + 1);

        for (int offset = 0; offset < length; offset += Math.max(2, spacing)) {
            Vec3i base = runsX ? new Vec3i(minX + offset, minY, minZ) : new Vec3i(minX, minY, minZ + offset);
            Vec3i protruded = base.offset(wallFacing);
            for (int y = minY; y <= maxY; y++) {
                Vec3i p = new Vec3i(protruded.x(), y, protruded.z());
                if (y == minY) {
                    targets.add(new PlacementTarget(p, plinth, MaterialRole.FOUNDATION));
                } else if (y == maxY) {
                    targets.add(new PlacementTarget(p, capital, MaterialRole.BEAM));
                } else {
                    targets.add(new PlacementTarget(p, pillar, MaterialRole.PILLAR));
                }
            }
        }
        return targets;
    }

    /**
     * Generates a triangular classical pediment cornice above a doorway or window bay.
     */
    public static List<PlacementTarget> generatePedimentCornice(Vec3i centerTop, Direction outwardFacing, int width, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        Direction cross = outwardFacing.rotateY();
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        int halfW = Math.max(1, width / 2);

        for (int w = -halfW; w <= halfW; w++) {
            targets.add(new PlacementTarget(centerTop.offset(cross, w).offset(outwardFacing), slab, MaterialRole.ROOF_OVERHANG));
        }
        int rise = Math.max(1, halfW);
        for (int h = 1; h <= rise; h++) {
            int span = halfW - h;
            if (span < 0) break;
            if (span == 0) {
                targets.add(new PlacementTarget(centerTop.add(0, h, 0).offset(outwardFacing), slab, MaterialRole.ROOF_RIDGE));
            } else {
                targets.add(new PlacementTarget(centerTop.offset(cross, -span).add(0, h, 0).offset(outwardFacing),
                    stair.withProperty("facing", cross.name().toLowerCase()).withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
                targets.add(new PlacementTarget(centerTop.offset(cross, span).add(0, h, 0).offset(outwardFacing),
                    stair.withProperty("facing", cross.opposite().name().toLowerCase()).withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
            }
        }
        return targets;
    }

    /**
     * Generates a rhythmic facade window array with sills, lintels, shutters, and intervening fluted pilasters.
     */
    public static List<PlacementTarget> generateRhythmicWindowArray(
        BoundingBox3i wallSpan,
        Direction wallFacing,
        int baySpacing,
        int windowHeight,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        targets.addAll(generateFlutedPilasters(wallSpan, wallFacing, Math.max(3, baySpacing), palette));

        int minX = wallSpan.min().x(), maxX = wallSpan.max().x();
        int minZ = wallSpan.min().z(), maxZ = wallSpan.max().z();
        int midY = wallSpan.min().y() + 1;
        boolean runsX = (maxX - minX) >= (maxZ - minZ);
        int length = runsX ? (maxX - minX + 1) : (maxZ - minZ + 1);
        int step = Math.max(3, baySpacing);

        for (int offset = step / 2; offset < length - 1; offset += step) {
            Vec3i winBase = runsX ? new Vec3i(minX + offset, midY, minZ) : new Vec3i(minX, midY, minZ + offset);
            for (int h = 0; h < Math.max(1, windowHeight); h++) {
                targets.add(new PlacementTarget(winBase.add(0, h, 0), palette.get(MaterialRole.WINDOW), MaterialRole.WINDOW));
            }
            targets.addAll(addWindowShuttersAndFlowerBox(winBase, wallFacing, 1, Math.max(1, windowHeight), palette));
        }
        return targets;
    }
}
