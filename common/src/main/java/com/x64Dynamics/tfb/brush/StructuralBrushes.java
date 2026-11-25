package com.x64Dynamics.tfb.brush;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.material.*;

import java.util.*;

import com.x64Dynamics.tfb.architectural.facade.PillarGenerator;
import com.x64Dynamics.tfb.architectural.facade.WallGenerator;
import com.x64Dynamics.tfb.architectural.layout.FloorGenerator;
import com.x64Dynamics.tfb.architectural.layout.StaircaseGenerator;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
public final class StructuralBrushes {
    private StructuralBrushes() { }

    public static final class WallBrush implements IConstructionBrush {
        public String id() { return "wall"; }
        public String displayName() { return "Wall"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            int thickness = context.getProperty("thickness", 1);
            boolean crenellations = context.getProperty("crenellations", false);
            return WallGenerator.generateBoxWalls(context.bounds(), thickness, crenellations, context.palette());
        }
    }

    public static final class FloorBrush implements IConstructionBrush {
        public String id() { return "floor"; }
        public String displayName() { return "Floor"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            return FloorGenerator.generateFloor(context.bounds(), context.bounds().min().y(), true, context.palette());
        }
    }

    public static final class RoofBrush implements IConstructionBrush {
        public String id() { return "roof"; }
        public String displayName() { return "Roof"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            return RoofGenerator.generateRoof(context.bounds(), RoofGenerator.RoofType.GABLE, Axis.X, 0, context.palette());
        }
    }

    public static final class CeilingBrush implements IConstructionBrush {
        public String id() { return "ceiling"; }
        public String displayName() { return "Ceiling"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            return FloorGenerator.generateCeiling(context.bounds(), context.bounds().max().y(), 3, context.palette());
        }
    }

    public static final class PillarBrush implements IConstructionBrush {
        public String id() { return "pillar"; }
        public String displayName() { return "Pillar"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            Vec3i base = context.bounds().min();
            return PillarGenerator.generatePillar(base, context.bounds().sizeY(), true, context.palette());
        }
    }

    public static final class StairBrush implements IConstructionBrush {
        public String id() { return "stair"; }
        public String displayName() { return "Stair"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            BoundingBox3i b = context.bounds();
            StaircaseGenerator.StaircaseStyle style = (b.sizeX() >= 7 && b.sizeY() >= 6)
                ? StaircaseGenerator.StaircaseStyle.BIFURCATED_IMPERIAL
                : (b.sizeX() >= 4 && b.sizeY() >= 4)
                    ? StaircaseGenerator.StaircaseStyle.DOG_LEG
                    : StaircaseGenerator.StaircaseStyle.STRAIGHT;
            return StaircaseGenerator.generate(style, b, Direction.NORTH, context.palette());
        }
    }

    public static final class ArchBrush implements IConstructionBrush {
        public String id() { return "arch"; }
        public String displayName() { return "Arch"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            Vec3i left = new Vec3i(box.min().x(), box.min().y(), box.min().z());
            Vec3i right = new Vec3i(box.max().x(), box.min().y(), box.min().z());

            for (Vec3i pos : ShapeRasterizer.arch(left, right, box.sizeY(), box.sizeZ(), Direction.SOUTH)) {
                targets.add(new PlacementTarget(pos, context.palette().get(MaterialRole.ARCH), MaterialRole.ARCH));
            }
            return targets;
        }
    }

    public static final class BridgeBrush implements IConstructionBrush {
        public String id() { return "bridge"; }
        public String displayName() { return "Bridge"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            MaterialPalette palette = context.palette();

            // Bridge deck
            int deckY = box.min().y() + Math.max(1, box.sizeY() / 3);
            targets.addAll(FloorGenerator.generateFloor(box, deckY, true, palette));

            // Parapets/Fences
            int minX = box.min().x(), maxX = box.max().x();
            int minZ = box.min().z(), maxZ = box.max().z();
            for (int x = minX; x <= maxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, deckY + 1, minZ), palette.get(MaterialRole.FENCE), MaterialRole.FENCE));
                targets.add(new PlacementTarget(new Vec3i(x, deckY + 1, maxZ), palette.get(MaterialRole.FENCE), MaterialRole.FENCE));
            }

            // Support pillars underneath
            targets.addAll(PillarGenerator.generatePillar(new Vec3i(minX + 2, box.min().y(), (minZ + maxZ) / 2), deckY - box.min().y(), false, palette));
            targets.addAll(PillarGenerator.generatePillar(new Vec3i(maxX - 2, box.min().y(), (minZ + maxZ) / 2), deckY - box.min().y(), false, palette));

            return targets;
        }
    }

    public static final class FoundationBrush implements IConstructionBrush {
        public String id() { return "foundation"; }
        public String displayName() { return "Foundation"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            for (Vec3i pt : ShapeRasterizer.box(context.bounds(), false)) {
                targets.add(new PlacementTarget(pt, context.palette().get(MaterialRole.FOUNDATION), MaterialRole.FOUNDATION));
            }
            return targets;
        }
    }

    public static final class FenceBrush implements IConstructionBrush {
        public String id() { return "fence"; }
        public String displayName() { return "Fence"; }
        public BrushCategory category() { return BrushCategory.STRUCTURAL; }

        public List<PlacementTarget> generate(BrushContext context) {
            List<PlacementTarget> targets = new ArrayList<>();
            BoundingBox3i box = context.bounds();
            int minX = box.min().x(), maxX = box.max().x();
            int minZ = box.min().z(), maxZ = box.max().z();
            int y = box.min().y();

            for (int x = minX; x <= maxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, y, minZ), context.palette().get(MaterialRole.FENCE), MaterialRole.FENCE));
                targets.add(new PlacementTarget(new Vec3i(x, y, maxZ), context.palette().get(MaterialRole.FENCE), MaterialRole.FENCE));
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                targets.add(new PlacementTarget(new Vec3i(minX, y, z), context.palette().get(MaterialRole.FENCE), MaterialRole.FENCE));
                targets.add(new PlacementTarget(new Vec3i(maxX, y, z), context.palette().get(MaterialRole.FENCE), MaterialRole.FENCE));
            }
            return targets;
        }
    }
}
