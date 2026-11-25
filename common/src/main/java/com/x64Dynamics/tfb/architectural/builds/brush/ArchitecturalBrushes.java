package com.x64Dynamics.tfb.architectural.builds.brush;

import com.x64Dynamics.tfb.architectural.builds.HomeGenerator;
import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;

import com.x64Dynamics.tfb.architectural.builds.*;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.material.*;
import com.x64Dynamics.tfb.architectural.exterior.terrain.TerrainAdapter;

import java.util.*;

import com.x64Dynamics.tfb.architectural.exterior.facades.OpeningGenerator;
import com.x64Dynamics.tfb.architectural.exterior.facades.PillarGenerator;
import com.x64Dynamics.tfb.architectural.exterior.wall.WallGenerator;
import com.x64Dynamics.tfb.architectural.exterior.wall.WallSegmenter;
import com.x64Dynamics.tfb.architectural.interior.floor.FloorGenerator;
import com.x64Dynamics.tfb.architectural.interior.layouts.TowerGenerator;
import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public final class ArchitecturalBrushes {
    private ArchitecturalBrushes() { }

    public static final class RoomBrush implements IConstructionBrush {
        public String id() { return "room"; }
        public String displayName() { return "Room"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            targets.addAll(FloorGenerator.generateFloor(bounds, bounds.min().y(), true, palette));
            targets.addAll(FloorGenerator.generateCeiling(bounds, bounds.max().y(), 3, palette));
            targets.addAll(WallGenerator.generateBoxWalls(bounds, 1, false, palette));

            Vec3i doorCenter = new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), bounds.min().z());
            targets.addAll(OpeningGenerator.carveDoorway(doorCenter, Direction.SOUTH, 1, 2, palette));

            return targets;
        }
    }

    public static final class HouseBrush implements IConstructionBrush {
        public String id() { return "house"; }
        public String displayName() { return "House"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();
            int stories = context.getProperty("stories", 0);
            boolean furnish = context.getProperty("furnish", true);
            boolean porch = context.getProperty("porch", true);
            boolean chimney = context.getProperty("chimney", true);
            float density = context.getProperty("density", 1.0f);
            long seed = context.getProperty("seed", 42L);

            HomeGenerator.HomeConfig config = new HomeGenerator.HomeConfig(
                bounds,
                palette,
                RoofGenerator.RoofType.GABLE,
                stories,
                WingConfiguration.SINGLE_RECTANGLE,
                porch,
                chimney,
                furnish,
                density,
                seed
            );
            List<PlacementTarget> targets = new ArrayList<>(HomeGenerator.generateHome(config).targets());
            if (context.heightmap() != null) {
                targets.addAll(TerrainAdapter.adapt(bounds, context.heightmap(), context.foundationStrategy(), palette).foundationPlacements());
            }
            return targets;
        }
    }

    public static final class BuildingBrush implements IConstructionBrush {
        public String id() { return "building"; }
        public String displayName() { return "Building"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();
            int stories = Math.max(1, context.getProperty("stories", Math.max(1, bounds.sizeY() / 4)));
            int storyHeight = Math.max(3, bounds.sizeY() / stories);
            int totalWallHeight = Math.min(bounds.sizeY(), stories * storyHeight);

            int minX = bounds.min().x(), maxX = bounds.max().x();
            int minZ = bounds.min().z(), maxZ = bounds.max().z();
            int y = bounds.min().y();
            int lenX = bounds.sizeX();
            int lenZ = bounds.sizeZ();

            // Composed Multi-Story Facades
            targets.addAll(WallSegmenter.composeFacade(new Vec3i(minX, y, maxZ), Direction.EAST, Direction.SOUTH, lenX, totalWallHeight, true, palette));
            targets.addAll(WallSegmenter.composeFacade(new Vec3i(maxX, y, minZ), Direction.WEST, Direction.NORTH, lenX, totalWallHeight, false, palette));
            targets.addAll(WallSegmenter.composeFacade(new Vec3i(maxX, y, maxZ), Direction.NORTH, Direction.EAST, lenZ, totalWallHeight, false, palette));
            targets.addAll(WallSegmenter.composeFacade(new Vec3i(minX, y, minZ), Direction.SOUTH, Direction.WEST, lenZ, totalWallHeight, false, palette));

            // Interior floors for each story
            for (int s = 0; s < stories; s++) {
                int floorY = bounds.min().y() + (s * storyHeight);
                targets.addAll(FloorGenerator.generateFloor(bounds, floorY, true, palette));
            }

            // Top Roof with parapet
            BoundingBox3i roofBounds = BoundingBox3i.fromOriginAndSize(bounds.min().add(0, totalWallHeight, 0), bounds.sizeX(), Math.max(1, bounds.sizeY() - totalWallHeight), bounds.sizeZ());
            targets.addAll(RoofGenerator.generateRoof(roofBounds, RoofGenerator.RoofType.FLAT, Axis.X, 0, palette));
            return targets;
        }
    }

    public static final class HallwayBrush implements IConstructionBrush {
        public String id() { return "hallway"; }
        public String displayName() { return "Hallway"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            targets.addAll(FloorGenerator.generateFloor(bounds, bounds.min().y(), true, palette));
            targets.addAll(FloorGenerator.generateCeiling(bounds, bounds.max().y(), 2, palette));

            boolean runsAlongX = bounds.sizeX() >= bounds.sizeZ();
            int minY = bounds.min().y() + 1, maxY = bounds.max().y() - 1;

            if (runsAlongX) {
                for (int x = bounds.min().x(); x <= bounds.max().x(); x++) {
                    for (int y = minY; y <= maxY; y++) {
                        targets.add(new PlacementTarget(new Vec3i(x, y, bounds.min().z()), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                        targets.add(new PlacementTarget(new Vec3i(x, y, bounds.max().z()), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                    }
                }
            } else {
                for (int z = bounds.min().z(); z <= bounds.max().z(); z++) {
                    for (int y = minY; y <= maxY; y++) {
                        targets.add(new PlacementTarget(new Vec3i(bounds.min().x(), y, z), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                        targets.add(new PlacementTarget(new Vec3i(bounds.max().x(), y, z), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                    }
                }
            }
            return targets;
        }
    }

    public static final class TowerBrush implements IConstructionBrush {
        public String id() { return "tower"; }
        public String displayName() { return "Tower"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            int radius = Math.max(3, Math.min(bounds.sizeX(), bounds.sizeZ()) / 2);
            Vec3i center = new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), (bounds.min().z() + bounds.max().z()) / 2);
            TowerGenerator.TowerShape shape = (bounds.sizeX() == bounds.sizeZ() && bounds.sizeX() >= 10)
                ? TowerGenerator.TowerShape.OCTAGONAL
                : TowerGenerator.TowerShape.CIRCULAR;
            var config = new TowerGenerator.TowerConfig(shape, radius, Math.max(8, bounds.sizeY()), 2, true, true, true);
            targets.addAll(TowerGenerator.generateTower(center, config, palette));
            return targets;
        }
    }

    public static final class CastleBrush implements IConstructionBrush {
        public String id() { return "castle"; }
        public String displayName() { return "Castle"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            // Castle curtain walls with crenellations
            targets.addAll(FloorGenerator.generateFloor(bounds, bounds.min().y(), true, palette));
            targets.addAll(WallGenerator.generateBoxWalls(bounds, 2, true, palette));

            // Corner towers
            int towerRadius = Math.max(2, Math.min(bounds.sizeX(), bounds.sizeZ()) / 6);
            int towerHeight = bounds.sizeY() + 3;

            Vec3i[] corners = {
                bounds.min(),
                new Vec3i(bounds.max().x(), bounds.min().y(), bounds.min().z()),
                new Vec3i(bounds.min().x(), bounds.min().y(), bounds.max().z()),
                new Vec3i(bounds.max().x(), bounds.min().y(), bounds.max().z())
            };

            for (Vec3i corner : corners) {
                for (Vec3i pt : ShapeRasterizer.cylinder(corner, towerRadius, towerRadius, towerHeight, true)) {
                    targets.add(new PlacementTarget(pt, palette.get(MaterialRole.WALL_CORNER), MaterialRole.WALL_CORNER));
                }
            }
            return targets;
        }
    }

    public static final class StableBrush implements IConstructionBrush {
        public String id() { return "stable"; }
        public String displayName() { return "Stable"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            targets.addAll(FloorGenerator.generateFloor(bounds, bounds.min().y(), false, palette));
            targets.addAll(RoofGenerator.generateRoof(bounds, RoofGenerator.RoofType.SHED, Axis.Z, 1, palette));

            // Wooden fence partitions every 4 blocks
            for (int x = bounds.min().x(); x <= bounds.max().x(); x += 4) {
                for (int z = bounds.min().z(); z <= bounds.max().z(); z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, bounds.min().y() + 1, z), palette.get(MaterialRole.FENCE), MaterialRole.FENCE));
                }
            }
            return targets;
        }
    }

    public static final class BarnBrush implements IConstructionBrush {
        public String id() { return "barn"; }
        public String displayName() { return "Barn"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            int wallH = bounds.sizeY() / 2;
            BoundingBox3i wallBounds = BoundingBox3i.fromOriginAndSize(bounds.min(), bounds.sizeX(), wallH, bounds.sizeZ());
            BoundingBox3i roofBounds = BoundingBox3i.fromOriginAndSize(bounds.min().add(0, wallH, 0), bounds.sizeX(), bounds.sizeY() - wallH, bounds.sizeZ());

            targets.addAll(FloorGenerator.generateFloor(wallBounds, wallBounds.min().y(), true, palette));
            targets.addAll(WallGenerator.generateBoxWalls(wallBounds, 1, false, palette));
            targets.addAll(RoofGenerator.generateRoof(roofBounds, RoofGenerator.RoofType.MANSARD, Axis.X, 1, palette));
            return targets;
        }
    }

    public static final class CourtyardBrush implements IConstructionBrush {
        public String id() { return "courtyard"; }
        public String displayName() { return "Courtyard"; }
        public BrushCategory category() { return BrushCategory.ARCHITECTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i bounds = context.bounds();
            MaterialPalette palette = context.palette();

            // Paved courtyard floor with perimeter colonnade
            targets.addAll(FloorGenerator.generateFloor(bounds, bounds.min().y(), true, palette));

            int minX = bounds.min().x(), maxX = bounds.max().x();
            int minZ = bounds.min().z(), maxZ = bounds.max().z();
            int colonnadeY = bounds.min().y() + 1;
            int pillarH = Math.min(4, bounds.sizeY() - 1);

            for (int x = minX + 2; x <= maxX - 2; x += 3) {
                targets.addAll(PillarGenerator.generatePillar(new Vec3i(x, colonnadeY, minZ + 2), pillarH, true, palette));
                targets.addAll(PillarGenerator.generatePillar(new Vec3i(x, colonnadeY, maxZ - 2), pillarH, true, palette));
            }
            return targets;
        }
    }
}
