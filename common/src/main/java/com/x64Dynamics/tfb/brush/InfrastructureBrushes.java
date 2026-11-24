package com.x64Dynamics.tfb.brush;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
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
            List<PlacementTarget> targets = new ArrayList<>();
            MaterialPalette palette = context.palette();
            int width = context.getProperty("width", 3);

            List<Vec3i> points = context.pathPoints();
            if (points.size() >= 2) {
                for (Vec3i pos : ShapeRasterizer.catmullRomPath(points, 6, width)) {
                    targets.add(new PlacementTarget(pos, palette.get(MaterialRole.FLOOR), MaterialRole.FLOOR));
                }
            } else {
                BoundingBox3i box = context.bounds();
                for (int x = box.min().x(); x <= box.max().x(); x++) {
                    for (int z = box.min().z(); z <= box.max().z(); z++) {
                        targets.add(new PlacementTarget(new Vec3i(x, box.min().y(), z), palette.get(MaterialRole.FLOOR), MaterialRole.FLOOR));
                    }
                }
            }
            return targets;
        }
    }

    public static final class PathBrush implements IConstructionBrush {
        public String id() { return "path"; }
        public String displayName() { return "Path"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            int y = box.min().y();

            BlockIdentifier pathBlock = BlockIdentifier.of("minecraft:dirt_path");
            for (int x = box.min().x(); x <= box.max().x(); x++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), pathBlock, MaterialRole.FLOOR));
                }
            }
            return targets;
        }
    }

    public static final class StreetBrush implements IConstructionBrush {
        public String id() { return "street"; }
        public String displayName() { return "Street"; }
        public BrushCategory category() { return BrushCategory.INFRASTRUCTURE; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            int y = box.min().y();

            BlockIdentifier asphalt = BlockIdentifier.of("minecraft:gray_concrete");
            BlockIdentifier line = BlockIdentifier.of("minecraft:yellow_concrete");

            boolean runsAlongX = box.sizeX() >= box.sizeZ();
            int midZ = (box.min().z() + box.max().z()) / 2;
            int midX = (box.min().x() + box.max().x()) / 2;

            for (int x = box.min().x(); x <= box.max().x(); x++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    boolean isCenter = runsAlongX ? (z == midZ && x % 4 < 2) : (x == midX && z % 4 < 2);
                    BlockIdentifier block = isCenter ? line : asphalt;
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), block, MaterialRole.FLOOR));
                }
            }
            return targets;
        }
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
