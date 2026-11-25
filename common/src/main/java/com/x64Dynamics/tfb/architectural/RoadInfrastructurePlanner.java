package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.ShapeRasterizer;
import com.x64Dynamics.tfb.geometry.SplinePath;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class RoadInfrastructurePlanner {
    public enum RoadStyle {
        PAVED_STONE,
        ASPHALT_MODERN,
        DIRT_COBBLE_RURAL,
        WOODEN_BOARDWALK
    }

    public enum IntersectionType {
        T_JUNCTION,
        FOUR_WAY,
        ROUNDABOUT,
        Y_BRANCH
    }

    public record RoadProfile(
        RoadStyle style,
        int roadwayWidth,
        int sidewalkWidth,
        boolean hasCurbs,
        boolean hasCenterline,
        boolean hasStreetlights,
        int streetlightSpacing,
        boolean autoBridgeSupports,
        boolean autoTunnelArch
    ) {
        public static RoadProfile defaultUrban() {
            return new RoadProfile(RoadStyle.ASPHALT_MODERN, 5, 2, true, true, true, 14, true, true);
        }

        public static RoadProfile defaultRural() {
            return new RoadProfile(RoadStyle.PAVED_STONE, 3, 1, false, false, true, 16, true, true);
        }
    }

    private RoadInfrastructurePlanner() { }

    // Generates a complete road ribbon along a SplinePath with road surface, curbs,
    // sidewalks, markings, streetlights, and bridge/tunnel features.
    public static List<PlacementTarget> generateSplineRoad(
        SplinePath path,
        RoadProfile profile,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        Map<Vec3i, PlacementTarget> targetMap = new HashMap<>();

        List<SplinePath.PathSample> samples = path.sample(8);
        if (samples.isEmpty()) return targets;

        BlockIdentifier roadSurface = getRoadSurfaceBlock(profile.style, palette);
        BlockIdentifier curbBlock = BlockIdentifier.of("minecraft:stone_brick_stairs");
        BlockIdentifier sidewalkBlock = palette.get(MaterialRole.FLOOR);
        BlockIdentifier centerlineBlock = BlockIdentifier.of("minecraft:yellow_concrete");
        BlockIdentifier airBlock = BlockIdentifier.of("minecraft:air");

        int halfRoad = profile.roadwayWidth() / 2;
        int totalHalf = halfRoad + (profile.hasCurbs() ? 1 : 0) + profile.sidewalkWidth();

        int sampleCounter = 0;

        for (SplinePath.PathSample sample : samples) {
            sampleCounter++;
            Vec3d pos = sample.position();
            Vec3d normal = sample.normal();
            int baseY = (int) Math.round(pos.y());

            boolean isMarkingStep = (sampleCounter % 6 < 3);

            for (int offset = -totalHalf; offset <= totalHalf; offset++) {
                double targetX = pos.x() + normal.x() * offset;
                double targetZ = pos.z() + normal.z() * offset;
                Vec3i blockPos = new Vec3i((int) Math.round(targetX), baseY, (int) Math.round(targetZ));

                int absOffset = Math.abs(offset);

                if (absOffset == 0 && profile.hasCenterline() && isMarkingStep && profile.roadwayWidth() >= 3) {
                    // Centerline marking
                    targetMap.put(blockPos, new PlacementTarget(blockPos, centerlineBlock, MaterialRole.FLOOR));
                } else if (absOffset <= halfRoad) {
                    // Main roadway surface
                    targetMap.put(blockPos, new PlacementTarget(blockPos, roadSurface, MaterialRole.FLOOR));
                } else if (profile.hasCurbs() && absOffset == halfRoad + 1) {
                    // Curb
                    Vec3i curbPos = blockPos.add(0, 0, 0);
                    targetMap.put(curbPos, new PlacementTarget(curbPos, curbBlock, MaterialRole.SLAB));
                } else if (absOffset <= totalHalf) {
                    // Sidewalk (elevated by 0 or 1 block relative to road)
                    Vec3i walkPos = profile.hasCurbs() ? blockPos.add(0, 0, 0) : blockPos;
                    targetMap.put(walkPos, new PlacementTarget(walkPos, sidewalkBlock, MaterialRole.FLOOR));
                }

                // Auto tunnel arch clearance above road
                if (profile.autoTunnelArch()) {
                    for (int h = 1; h <= 4; h++) {
                        Vec3i clearPos = blockPos.add(0, h, 0);
                        // Air clearance target
                        targetMap.put(clearPos, new PlacementTarget(clearPos, airBlock, MaterialRole.AIR));
                    }
                }
            }

            // Periodic streetlights along sidewalk edge
            if (profile.hasStreetlights() && (sampleCounter % profile.streetlightSpacing() == 0)) {
                double lightX = pos.x() + normal.x() * totalHalf;
                double lightZ = pos.z() + normal.z() * totalHalf;
                Vec3i lightBase = new Vec3i((int) Math.round(lightX), baseY + 1, (int) Math.round(lightZ));
                addStreetlight(lightBase, palette, targetMap);
            }
        }

        targets.addAll(targetMap.values());
        return targets;
    }

    // Generates a road intersection (T-Junction, 4-Way Cross, Roundabout, or Y-Branch).
    public static List<PlacementTarget> generateIntersection(
        Vec3i center,
        IntersectionType type,
        RoadProfile profile,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        Map<Vec3i, PlacementTarget> map = new HashMap<>();

        BlockIdentifier roadSurface = getRoadSurfaceBlock(profile.style, palette);
        BlockIdentifier sidewalkBlock = palette.get(MaterialRole.FLOOR);
        int radius = profile.roadwayWidth() + profile.sidewalkWidth() + 2;

        switch (type) {
            case ROUNDABOUT -> {
                int islandRadius = Math.max(3, profile.roadwayWidth());
                int outerRadius = islandRadius + profile.roadwayWidth() + 2;

                // 1. Central decorative island
                for (Vec3i p : ShapeRasterizer.cylinder(center, islandRadius, islandRadius, 1, false)) {
                    BlockIdentifier islandFill = (p.distance(center) < islandRadius - 1)
                        ? BlockIdentifier.of("minecraft:grass_block")
                        : BlockIdentifier.of("minecraft:stone_brick_slab[type=bottom]");
                    map.put(p, new PlacementTarget(p, islandFill, MaterialRole.FLOOR));
                }
                // Center fountain or statue monument
                map.put(center.add(0, 1, 0), new PlacementTarget(center.add(0, 1, 0), BlockIdentifier.of("minecraft:stone_brick_wall"), MaterialRole.PILLAR));
                map.put(center.add(0, 2, 0), new PlacementTarget(center.add(0, 2, 0), BlockIdentifier.of("minecraft:lantern"), MaterialRole.LIGHT_SOURCE));

                // 2. Circular roadway ring
                for (Vec3i p : ShapeRasterizer.cylinder(center, outerRadius, outerRadius, 1, false)) {
                    if (p.distance(center) >= islandRadius) {
                        map.put(p, new PlacementTarget(p, roadSurface, MaterialRole.FLOOR));
                    }
                }

                // 3. Outer sidewalk ring
                int sidewalkOuter = outerRadius + profile.sidewalkWidth();
                for (Vec3i p : ShapeRasterizer.cylinder(center, sidewalkOuter, sidewalkOuter, 1, false)) {
                    if (p.distance(center) >= outerRadius) {
                        map.put(p, new PlacementTarget(p, sidewalkBlock, MaterialRole.FLOOR));
                    }
                }
            }
            case FOUR_WAY, T_JUNCTION, Y_BRANCH -> {
                int armLength = radius + 4;
                int halfRoad = profile.roadwayWidth() / 2;

                // Central junction square
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Vec3i p = center.add(dx, 0, dz);
                        boolean onArm = (Math.abs(dx) <= halfRoad) || (Math.abs(dz) <= halfRoad);
                        if (type == IntersectionType.T_JUNCTION && dz < -halfRoad) {
                            onArm = Math.abs(dx) <= halfRoad; // No north arm for T
                        }
                        if (onArm) {
                            map.put(p, new PlacementTarget(p, roadSurface, MaterialRole.FLOOR));
                        } else if (Math.abs(dx) <= radius && Math.abs(dz) <= radius) {
                            map.put(p, new PlacementTarget(p, sidewalkBlock, MaterialRole.FLOOR));
                        }
                    }
                }

                // Streetlights at 4 corners
                int cornerOffset = halfRoad + 2;
                addStreetlight(center.add(cornerOffset, 1, cornerOffset), palette, map);
                addStreetlight(center.add(-cornerOffset, 1, cornerOffset), palette, map);
                addStreetlight(center.add(-cornerOffset, 1, -cornerOffset), palette, map);
                if (type != IntersectionType.T_JUNCTION) {
                    addStreetlight(center.add(cornerOffset, 1, -cornerOffset), palette, map);
                }

                // Crosswalks & traffic lights for urban intersections
                if (profile.style() == RoadStyle.ASPHALT_MODERN) {
                    addCrosswalk(center.add(0, 0, halfRoad + 2), true, halfRoad, map);
                    addCrosswalk(center.add(halfRoad + 2, 0, 0), false, halfRoad, map);
                    addCrosswalk(center.add(-halfRoad - 2, 0, 0), false, halfRoad, map);
                    if (type != IntersectionType.T_JUNCTION) {
                        addCrosswalk(center.add(0, 0, -halfRoad - 2), true, halfRoad, map);
                        addTrafficLight(center.add(cornerOffset, 1, -cornerOffset), Direction.WEST, map);
                    }
                    addTrafficLight(center.add(-cornerOffset, 1, cornerOffset), Direction.EAST, map);
                }
            }
        }

        targets.addAll(map.values());
        return targets;
    }

    private static void addCrosswalk(Vec3i lineCenter, boolean runsAlongX, int halfRoad, Map<Vec3i, PlacementTarget> map) {
        BlockIdentifier stripe = BlockIdentifier.of("minecraft:white_concrete");
        for (int i = -halfRoad; i <= halfRoad; i++) {
            if (Math.abs(i) % 2 == 0) {
                Vec3i p1 = runsAlongX ? lineCenter.add(i, 0, 0) : lineCenter.add(0, 0, i);
                Vec3i p2 = runsAlongX ? lineCenter.add(i, 0, 1) : lineCenter.add(1, 0, i);
                map.put(p1, new PlacementTarget(p1, stripe, MaterialRole.WALL_ACCENT));
                map.put(p2, new PlacementTarget(p2, stripe, MaterialRole.WALL_ACCENT));
            }
        }
    }

    private static void addTrafficLight(Vec3i base, Direction armDir, Map<Vec3i, PlacementTarget> map) {
        BlockIdentifier pole = BlockIdentifier.of("minecraft:iron_bars");
        BlockIdentifier housing = BlockIdentifier.of("minecraft:black_concrete");
        map.put(base, new PlacementTarget(base, BlockIdentifier.of("minecraft:polished_andesite"), MaterialRole.PILLAR));
        for (int y = 1; y <= 4; y++) {
            map.put(base.add(0, y, 0), new PlacementTarget(base.add(0, y, 0), pole, MaterialRole.PILLAR));
        }
        Vec3i armStart = base.add(0, 4, 0);
        map.put(armStart.offset(armDir, 1), new PlacementTarget(armStart.offset(armDir, 1), pole, MaterialRole.PILLAR));
        Vec3i head = armStart.offset(armDir, 2);
        map.put(head, new PlacementTarget(head, housing, MaterialRole.DECORATION));
        map.put(head.add(0, 1, 0), new PlacementTarget(head.add(0, 1, 0), BlockIdentifier.of("minecraft:red_concrete"), MaterialRole.LIGHT_SOURCE));
        map.put(head.add(0, -1, 0), new PlacementTarget(head.add(0, -1, 0), BlockIdentifier.of("minecraft:lime_concrete"), MaterialRole.LIGHT_SOURCE));
    }

    private static void addStreetlight(Vec3i base, MaterialPalette palette, Map<Vec3i, PlacementTarget> map) {
        BlockIdentifier post = BlockIdentifier.of("minecraft:cobblestone_wall");
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

        map.put(base, new PlacementTarget(base, post, MaterialRole.PILLAR));
        map.put(base.add(0, 1, 0), new PlacementTarget(base.add(0, 1, 0), fence, MaterialRole.PILLAR));
        map.put(base.add(0, 2, 0), new PlacementTarget(base.add(0, 2, 0), fence, MaterialRole.PILLAR));
        map.put(base.add(0, 3, 0), new PlacementTarget(base.add(0, 3, 0), lantern, MaterialRole.LIGHT_SOURCE));
    }

    private static BlockIdentifier getRoadSurfaceBlock(RoadStyle style, MaterialPalette palette) {
        return switch (style) {
            case ASPHALT_MODERN -> BlockIdentifier.of("minecraft:gray_concrete");
            case PAVED_STONE -> palette.get(MaterialRole.FOUNDATION);
            case DIRT_COBBLE_RURAL -> BlockIdentifier.of("minecraft:coarse_dirt");
            case WOODEN_BOARDWALK -> palette.get(MaterialRole.FLOOR);
        };
    }
}
