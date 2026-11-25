package com.x64Dynamics.tfb.brush;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoadInfrastructureEngine;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class InfrastructureBrushes {
    private InfrastructureBrushes() { }

    public static final class RoadBrush implements IConstructionBrush {
        public String id() { return "road"; }
        public String displayName() { return "Road"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            MaterialPalette palette = context.palette();
            int width = Math.max(3, context.getProperty("width", 3));

            List<Vec3i> points = buildPathPoints(context);
            if (points.size() >= 2) {
                SplinePath spline = SplinePath.fromIntegerPoints(SplinePath.SplineType.CATMULL_ROM, points, false);
                RoadInfrastructureEngine.RoadProfile profile = new RoadInfrastructureEngine.RoadProfile(
                    RoadInfrastructureEngine.RoadStyle.PAVED_STONE,
                    width, 1, true, false, true, 12, true, true
                );
                return RoadInfrastructureEngine.generateSplineRoad(spline, profile, palette);
            }
            return Collections.emptyList();
        }
    }

    public static final class PathBrush implements IConstructionBrush {
        public String id() { return "path"; }
        public String displayName() { return "Path"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            Map<Vec3i, PlacementTarget> map = new HashMap<>();
            List<Vec3i> points = buildPathPoints(context);

            if (points.size() < 2) return targets;

            int width = Math.max(2, context.getProperty("width", 3));
            SplinePath spline = SplinePath.fromIntegerPoints(SplinePath.SplineType.CATMULL_ROM, points, false);
            List<SplinePath.PathSample> samples = spline.sample(8);

            BlockIdentifier dirtPath = BlockIdentifier.of("minecraft:dirt_path");
            BlockIdentifier coarseDirt = BlockIdentifier.of("minecraft:coarse_dirt");
            BlockIdentifier gravel = BlockIdentifier.of("minecraft:gravel");
            BlockIdentifier rootedDirt = BlockIdentifier.of("minecraft:rooted_dirt");
            BlockIdentifier cobble = BlockIdentifier.of("minecraft:cobblestone");
            BlockIdentifier mossyCobble = BlockIdentifier.of("minecraft:mossy_cobblestone");
            BlockIdentifier fence = BlockIdentifier.of("minecraft:oak_fence");
            BlockIdentifier lantern = BlockIdentifier.of("minecraft:lantern");

            int halfW = width / 2;
            int sampleCount = 0;

            for (SplinePath.PathSample sample : samples) {
                sampleCount++;
                Vec3d pos = sample.position();
                Vec3d normal = sample.normal();
                int baseY = (int) Math.round(pos.y());

                for (int offset = -halfW - 1; offset <= halfW + 1; offset++) {
                    double tx = pos.x() + normal.x() * offset;
                    double tz = pos.z() + normal.z() * offset;
                    Vec3i p = new Vec3i((int) Math.round(tx), baseY, (int) Math.round(tz));

                    int absOffset = Math.abs(offset);
                    int hash = Math.abs((p.x() * 3129871) ^ (p.z() * 6187919) ^ (sampleCount * 7919));

                    if (absOffset <= halfW) {
                        // Natural path surface variation
                        int roll = hash % 100;
                        BlockIdentifier blk;
                        if (roll < 60) {
                            blk = dirtPath;
                        } else if (roll < 80) {
                            blk = coarseDirt;
                        } else if (roll < 90) {
                            blk = gravel;
                        } else {
                            blk = rootedDirt;
                        }
                        map.put(p, new PlacementTarget(p, blk, MaterialRole.FLOOR));
                    } else if (absOffset == halfW + 1) {
                        // Irregular soft border (cobblestone / coarse dirt edging)
                        if ((hash % 10) < 6) {
                            BlockIdentifier edgeBlk = ((hash % 2) == 0) ? cobble : mossyCobble;
                            map.put(p, new PlacementTarget(p, edgeBlk, MaterialRole.FOUNDATION));
                        }
                    }
                }

                // Periodic lantern posts along path edge
                if (sampleCount % 12 == 0) {
                    double lx = pos.x() + normal.x() * (halfW + 1);
                    double lz = pos.z() + normal.z() * (halfW + 1);
                    Vec3i postPos = new Vec3i((int) Math.round(lx), baseY + 1, (int) Math.round(lz));
                    map.put(postPos, new PlacementTarget(postPos, fence, MaterialRole.PILLAR));
                    map.put(postPos.add(0, 1, 0), new PlacementTarget(postPos.add(0, 1, 0), lantern, MaterialRole.LIGHT_SOURCE));
                }
            }

            targets.addAll(map.values());
            return targets;
        }
    }

    public static final class StreetBrush implements IConstructionBrush {
        public String id() { return "street"; }
        public String displayName() { return "Street"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            MaterialPalette palette = context.palette();
            int width = Math.max(5, context.getProperty("width", 5));

            List<Vec3i> points = buildPathPoints(context);
            if (points.size() >= 2) {
                SplinePath spline = SplinePath.fromIntegerPoints(SplinePath.SplineType.CATMULL_ROM, points, false);
                RoadInfrastructureEngine.RoadProfile profile = new RoadInfrastructureEngine.RoadProfile(
                    RoadInfrastructureEngine.RoadStyle.ASPHALT_MODERN,
                    width, 2, true, true, true, 14, true, true
                );
                return RoadInfrastructureEngine.generateSplineRoad(spline, profile, palette);
            }
            return Collections.emptyList();
        }
    }

    private static List<Vec3i> buildPathPoints(BrushContext context) {
        List<Vec3i> points = new ArrayList<>(context.pathPoints());
        if (points.size() < 2) {
            BoundingBox3i box = context.bounds();
            int y = box.min().y();
            if (box.sizeX() >= box.sizeZ()) {
                int midZ = (box.min().z() + box.max().z()) / 2;
                points.add(new Vec3i(box.min().x(), y, midZ));
                points.add(new Vec3i(box.max().x(), y, midZ));
            } else {
                int midX = (box.min().x() + box.max().x()) / 2;
                points.add(new Vec3i(midX, y, box.min().z()));
                points.add(new Vec3i(midX, y, box.max().z()));
            }
        }
        return points;
    }

    public static final class SidewalkBrush implements IConstructionBrush {
        public String id() { return "sidewalk"; }
        public String displayName() { return "Sidewalk"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            BlockIdentifier slab = BlockIdentifier.of("minecraft:smooth_stone_slab[half=bottom]");

            for (int x = box.min().x(); x <= box.max().x(); x++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, box.min().y(), z), slab, MaterialRole.SLAB));
                }
            }
            return targets;
        }
    }

    public static final class TunnelBrush implements IConstructionBrush {
        public String id() { return "tunnel"; }
        public String displayName() { return "Tunnel"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            MaterialPalette palette = context.palette();

            // Cylindrical / vaulted tunnel bore
            int radius = Math.min(box.sizeX(), box.sizeY()) / 2;
            Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, box.min().y(), box.min().z());

            for (int z = box.min().z(); z <= box.max().z(); z++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = 0; dy <= radius * 2; dy++) {
                        double dist = (dx * dx) + Math.pow(dy - radius, 2);
                        Vec3i pt = new Vec3i(center.x() + dx, center.y() + dy, z);
                        if (dist <= radius * radius) {
                            if (dist >= Math.pow(radius - 1, 2) || dy == 0) {
                                targets.add(new PlacementTarget(pt, palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                            } else {
                                targets.add(new PlacementTarget(pt, BlockIdentifier.AIR, MaterialRole.DECORATION));
                            }
                        }
                    }
                }
            }
            return targets;
        }
    }

    public static final class RailwayBrush implements IConstructionBrush {
        public String id() { return "railway"; }
        public String displayName() { return "Railway"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            int y = box.min().y();

            BlockIdentifier ballast = BlockIdentifier.of("minecraft:gravel");
            BlockIdentifier ties = BlockIdentifier.of("minecraft:oak_slab[half=bottom]");
            BlockIdentifier rail = BlockIdentifier.of("minecraft:rail");

            boolean runsAlongX = box.sizeX() >= box.sizeZ();
            int midZ = (box.min().z() + box.max().z()) / 2;
            int midX = (box.min().x() + box.max().x()) / 2;

            for (int x = box.min().x(); x <= box.max().x(); x++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), ballast, MaterialRole.FOUNDATION));

                    boolean isRailLine = runsAlongX ? (z == midZ) : (x == midX);
                    if (isRailLine) {
                        targets.add(new PlacementTarget(new Vec3i(x, y + 1, z), rail, MaterialRole.DECORATION));
                    }
                }
            }
            return targets;
        }
    }

    public static final class DrainageBrush implements IConstructionBrush {
        public String id() { return "drainage"; }
        public String displayName() { return "Drainage"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            int y = box.min().y();

            BlockIdentifier grate = BlockIdentifier.of("minecraft:iron_bars");
            BlockIdentifier canal = context.palette().get(MaterialRole.FOUNDATION);

            for (int x = box.min().x(); x <= box.max().x(); x++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y - 1, z), canal, MaterialRole.FOUNDATION));
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), grate, MaterialRole.DECORATION));
                }
            }
            return targets;
        }
    }
}
