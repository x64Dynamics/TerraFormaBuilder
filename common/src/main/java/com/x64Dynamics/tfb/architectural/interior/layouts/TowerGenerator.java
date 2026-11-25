package com.x64Dynamics.tfb.architectural.interior.layouts;

import com.x64Dynamics.tfb.geometry.ShapeRasterizer;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
// Generates multi-segment circular and polygonal towers with stepped plinths,
// intermediate floor plates, arrow-slit windows, corbelled parapets, battlements,
// and optional curtain-wall connections.
public final class TowerGenerator {
    public enum TowerShape {
        CIRCULAR(0),
        HEXAGONAL(6),
        OCTAGONAL(8);

        private final int sides;
        TowerShape(int sides) { this.sides = sides; }
        public int sides() { return sides; }
    }

    public record TowerConfig(
        TowerShape shape,
        int baseRadius,
        int totalHeight,
        int segments,
        boolean includeBattlements,
        boolean includeSpiralStair,
        boolean includeArrowSlits
    ) {
        public static TowerConfig defaultKeep(int radius, int height) {
            return new TowerConfig(TowerShape.CIRCULAR, Math.max(3, radius), Math.max(8, height), 2, true, true, true);
        }
    }

    private TowerGenerator() { }

    public static List<PlacementTarget> generateTower(Vec3i baseCenter, TowerConfig config, MaterialPalette palette) {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        int radius = Math.max(2, config.baseRadius());
        int height = Math.max(5, config.totalHeight());
        int segments = Math.max(1, config.segments());

        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier trim = palette.get(MaterialRole.BEAM);
        BlockIdentifier floor = palette.get(MaterialRole.FLOOR);
        BlockIdentifier window = palette.get(MaterialRole.WINDOW);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier light = palette.get(MaterialRole.LIGHT_SOURCE);

        // 1. Stepped plinth base (2 courses)
        for (Vec3i p : rasterizeRingOrFill(baseCenter, radius + 1, 1, config.shape(), false)) {
            map.put(p, new PlacementTarget(p, foundation, MaterialRole.FOUNDATION));
        }
        for (Vec3i p : rasterizeRingOrFill(baseCenter.add(0, 1, 0), radius, 1, config.shape(), false)) {
            map.put(p, new PlacementTarget(p, foundation, MaterialRole.FOUNDATION));
        }

        // 2. Multi-segment shaft with belt courses, interior floors, and arrow slits
        int shaftHeight = height - 2;
        int tierHeight = Math.max(3, shaftHeight / segments);

        for (int seg = 0; seg < segments; seg++) {
            int segStartY = 2 + seg * tierHeight;
            int segEndY = (seg == segments - 1) ? height - 1 : Math.min(height - 1, segStartY + tierHeight);
            int segH = Math.max(1, segEndY - segStartY);
            int segRadius = Math.max(2, radius - (seg >= 2 ? 1 : 0));

            Vec3i tierOrigin = baseCenter.add(0, segStartY, 0);
            for (Vec3i p : rasterizeRingOrFill(tierOrigin, segRadius, segH, config.shape(), true)) {
                map.put(p, new PlacementTarget(p, wall, MaterialRole.WALL_PRIMARY));
            }

            // Interior floor plate at bottom of each upper segment
            if (seg > 0) {
                for (Vec3i p : rasterizeRingOrFill(tierOrigin, segRadius - 1, 1, config.shape(), false)) {
                    map.put(p, new PlacementTarget(p, floor, MaterialRole.FLOOR));
                }
                // Exterior stringcourse ring
                for (Vec3i p : rasterizeRingOrFill(tierOrigin, segRadius + 1, 1, config.shape(), true)) {
                    map.put(p, new PlacementTarget(p, trim, MaterialRole.BEAM));
                }
            }

            // Cardinal arrow-slit windows at mid-tier
            if (config.includeArrowSlits() && segH >= 3) {
                int midY = baseCenter.y() + segStartY + segH / 2;
                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : dirs) {
                    Vec3i slitLower = new Vec3i(baseCenter.x() + d[0] * segRadius, midY, baseCenter.z() + d[1] * segRadius);
                    Vec3i slitUpper = slitLower.add(0, 1, 0);
                    map.put(slitLower, new PlacementTarget(slitLower, window, MaterialRole.WINDOW));
                    map.put(slitUpper, new PlacementTarget(slitUpper, window, MaterialRole.WINDOW));
                }
            }
        }

        // 3. Top deck & corbelled battlements
        int topY = baseCenter.y() + height - 1;
        int topRadius = Math.max(2, radius - (segments >= 3 ? 1 : 0));
        Vec3i deckOrigin = new Vec3i(baseCenter.x(), topY, baseCenter.z());

        for (Vec3i p : rasterizeRingOrFill(deckOrigin, topRadius, 1, config.shape(), false)) {
            map.put(p, new PlacementTarget(p, floor, MaterialRole.FLOOR));
        }

        if (config.includeBattlements()) {
            int parapetRadius = topRadius + 1;
            List<Vec3i> parapetRing = rasterizeRingOrFill(deckOrigin, parapetRadius, 1, config.shape(), true);
            for (Vec3i p : parapetRing) {
                // Corbel base ring
                map.put(p, new PlacementTarget(p, trim, MaterialRole.BEAM));
                // Lower parapet wall
                Vec3i parapetWall = p.add(0, 1, 0);
                map.put(parapetWall, new PlacementTarget(parapetWall, wall, MaterialRole.WALL_PRIMARY));

                // Alternating merlons and crenels
                boolean isMerlon = ((Math.abs(p.x() - baseCenter.x()) + Math.abs(p.z() - baseCenter.z())) % 2 == 0);
                if (isMerlon) {
                    Vec3i merlon = p.add(0, 2, 0);
                    map.put(merlon, new PlacementTarget(merlon, wall, MaterialRole.WALL_PRIMARY));
                    Vec3i cap = p.add(0, 3, 0);
                    map.put(cap, new PlacementTarget(cap, slab, MaterialRole.SLAB));
                }
            }

            // Cardinal parapet lanterns
            Vec3i[] lanternSpots = {
                deckOrigin.add(topRadius, 2, 0),
                deckOrigin.add(-topRadius, 2, 0),
                deckOrigin.add(0, 2, topRadius),
                deckOrigin.add(0, 2, -topRadius)
            };
            for (Vec3i ls : lanternSpots) {
                map.put(ls, new PlacementTarget(ls, light, MaterialRole.LIGHT_SOURCE));
            }
        }

        // 4. Optional interior spiral staircase
        if (config.includeSpiralStair() && height > 4) {
            for (PlacementTarget st : StaircaseGenerator.generateSpiral(baseCenter.add(0, 1, 0), height - 2, palette)) {
                map.putIfAbsent(st.pos(), st);
            }
        }

        return new ArrayList<>(map.values());
    }

    // Generates a connected crenellated curtain wall between two tower centers.
    public static List<PlacementTarget> generateConnectedBattlementWall(Vec3i start, Vec3i end, int height, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier walkway = palette.get(MaterialRole.FLOOR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);

        List<Vec3i> line = ShapeRasterizer.bresenhamLine(start, end);
        for (int i = 0; i < line.size(); i++) {
            Vec3i base = line.get(i);
            for (int y = 0; y < height; y++) {
                targets.add(new PlacementTarget(base.add(0, y, 0), wall, MaterialRole.WALL_PRIMARY));
            }
            targets.add(new PlacementTarget(base.add(0, height, 0), walkway, MaterialRole.FLOOR));
            if (i % 2 == 0) {
                targets.add(new PlacementTarget(base.add(0, height + 1, 0), wall, MaterialRole.WALL_PRIMARY));
                targets.add(new PlacementTarget(base.add(0, height + 2, 0), slab, MaterialRole.SLAB));
            }
        }
        return targets;
    }

    private static List<Vec3i> rasterizeRingOrFill(Vec3i center, int radius, int height, TowerShape shape, boolean hollow) {
        return switch (shape) {
            case CIRCULAR -> ShapeRasterizer.cylinder(center, radius, radius, height, hollow);
            case HEXAGONAL -> ShapeRasterizer.regularPolygon(center, radius, 6, height, 0.0, hollow);
            case OCTAGONAL -> ShapeRasterizer.regularPolygon(center, radius, 8, height, Math.PI / 8.0, hollow);
        };
    }
}
