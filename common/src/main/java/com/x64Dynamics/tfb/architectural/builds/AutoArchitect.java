package com.x64Dynamics.tfb.architectural.builds;

import com.x64Dynamics.tfb.architectural.builds.plan.BuildingDefinition;
import com.x64Dynamics.tfb.architectural.builds.plan.BuildingPlan;
import com.x64Dynamics.tfb.architectural.builds.style.StyleRuleSetRegistry;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.architectural.builds.grid.GridPresets;
import com.x64Dynamics.tfb.architectural.builds.grid.StructurePlacementController;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.architectural.exterior.terrain.HeightmapGrid;

import java.util.Locale;

import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
// Zero-input autonomous architectural director.
// Inspects the surrounding world blocks, terrain heightmap, and optional biome hint
// to automatically select architectural style, harmonized material palette, roof type,
// story count/height, wing layout, entrance orientation, foundation strategy, and interiors.
public final class AutoArchitect {
    public enum DetectedBiomeTheme {
        SNOWY_ALPINE,
        DESERT_ARID,
        CHERRY_EAST_ASIAN,
        COASTAL_WATERFRONT,
        STONY_HIGHLAND,
        TEMPERATE_FOREST
    }

    public record AutoDesignReport(
        DetectedBiomeTheme biomeTheme,
        StylePreset chosenStyle,
        Direction chosenFacing,
        RoofGenerator.RoofType chosenRoof,
        WingConfiguration chosenWings,
        FoundationStrategy chosenFoundation,
        BuildingPlan plan
    ) { }

    private AutoArchitect() { }

    public static DetectedBiomeTheme detectBiomeTheme(Vec3i origin, ILevelAccess level, String biomeHint) {
        if (biomeHint != null && !biomeHint.isBlank()) {
            String b = biomeHint.toLowerCase(Locale.ROOT);
            if (b.contains("cherry") || b.contains("bamboo") || b.contains("jungle")) {
                return DetectedBiomeTheme.CHERRY_EAST_ASIAN;
            }
            if (b.contains("snow") || b.contains("ice") || b.contains("frozen") || b.contains("taiga") || b.contains("grove")) {
                return DetectedBiomeTheme.SNOWY_ALPINE;
            }
            if (b.contains("desert") || b.contains("badlands") || b.contains("savanna")) {
                return DetectedBiomeTheme.DESERT_ARID;
            }
            if (b.contains("ocean") || b.contains("beach") || b.contains("river")) {
                return DetectedBiomeTheme.COASTAL_WATERFRONT;
            }
            if (b.contains("stony") || b.contains("peak") || b.contains("mountain") || b.contains("dripstone")) {
                return DetectedBiomeTheme.STONY_HIGHLAND;
            }
        }

        if (level == null) return DetectedBiomeTheme.TEMPERATE_FOREST;

        int snowScore = 0, desertScore = 0, cherryScore = 0, waterScore = 0, stoneScore = 0;
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                int x = origin.x() + dx;
                int z = origin.z() + dz;
                int y = level.getSurfaceHeight(x, z);
                BlockIdentifier surface = level.getBlockState(new Vec3i(x, y, z));
                String p = surface.path().toLowerCase(Locale.ROOT);
                if (p.contains("snow") || p.contains("ice") || p.contains("spruce") || p.contains("podzol")) snowScore++;
                else if (p.contains("sand") || p.contains("terracotta") || p.contains("red_sand")) desertScore++;
                else if (p.contains("cherry") || p.contains("bamboo") || p.contains("moss") || p.contains("jungle")) cherryScore++;
                else if (p.contains("water") || p.contains("kelp")) waterScore++;
                else if (p.contains("stone") || p.contains("deepslate") || p.contains("andesite") || p.contains("granite")) stoneScore++;
            }
        }

        if (snowScore >= 3 || origin.y() >= 110) return DetectedBiomeTheme.SNOWY_ALPINE;
        if (desertScore >= 3) return DetectedBiomeTheme.DESERT_ARID;
        if (cherryScore >= 3) return DetectedBiomeTheme.CHERRY_EAST_ASIAN;
        if (waterScore >= 4) return DetectedBiomeTheme.COASTAL_WATERFRONT;
        if (stoneScore >= 5) return DetectedBiomeTheme.STONY_HIGHLAND;
        return DetectedBiomeTheme.TEMPERATE_FOREST;
    }

    public static AutoDesignReport autoDesign(
        Vec3i origin,
        BoundingBox3i optionalBounds,
        ILevelAccess level,
        String biomeHint,
        long seed
    ) {
        DetectedBiomeTheme theme = detectBiomeTheme(origin, level, biomeHint);

        StylePreset style = switch (theme) {
            case SNOWY_ALPINE -> StylePreset.NORDIC;
            case DESERT_ARID -> StylePreset.MINIMALIST;
            case CHERRY_EAST_ASIAN -> StylePreset.JAPANESE;
            case COASTAL_WATERFRONT -> StylePreset.RUSTIC;
            case STONY_HIGHLAND -> StylePreset.GOTHIC;
            case TEMPERATE_FOREST -> ((seed & 1L) == 0L) ? StylePreset.MEDIEVAL : StylePreset.TRADITIONAL;
        };

        MaterialPalette palette = style.createPalette();
        if (theme == DetectedBiomeTheme.DESERT_ARID) {
            palette.set(MaterialRole.WALL_PRIMARY, "minecraft:smooth_sandstone")
                .set(MaterialRole.WALL_ACCENT, "minecraft:terracotta")
                .set(MaterialRole.FOUNDATION, "minecraft:cut_sandstone")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:sandstone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:sandstone_slab");
        }

        BoundingBox3i bounds = optionalBounds != null
            ? optionalBounds
            : BoundingBox3i.fromOriginAndSize(origin, 13, 18, 11);

        HeightmapGrid heightmap = (level != null) ? HeightmapGrid.sample(bounds, level) : null;
        int relief = heightmap != null ? heightmap.maxY() - heightmap.minY() : 0;

        Direction facing = pickBestApproachDirection(bounds, level);
        FoundationStrategy foundation = (theme == DetectedBiomeTheme.COASTAL_WATERFRONT || relief >= 5)
            ? FoundationStrategy.PILLARS_STILTS
            : (relief >= 2 ? FoundationStrategy.STEPPED_TERRACE : FoundationStrategy.SOLID_SUB_BASEMENT);

        WingConfiguration wings = (bounds.sizeX() >= 13 && bounds.sizeZ() >= 13 && relief <= 2)
            ? WingConfiguration.L_SHAPED
            : WingConfiguration.SINGLE_RECTANGLE;

        var rules = StyleRuleSetRegistry.forPreset(style);
        RoofGenerator.RoofType roof = rules.preferredRoofType();
        int stories = (bounds.sizeX() >= 11 && bounds.sizeZ() >= 11) ? 2 : 1;
        int storyHeight = (style == StylePreset.MODERN || style == StylePreset.FANTASY) ? 5 : 4;

        BuildingDefinition def = new BuildingDefinition(
            "auto-" + theme.name().toLowerCase(Locale.ROOT),
            bounds,
            wings,
            style,
            palette,
            roof,
            stories,
            storyHeight,
            facing,
            true,
            theme != DetectedBiomeTheme.DESERT_ARID,
            true,
            1.0f,
            seed
        );

        BuildingPlan plan = HomeGenerator.generatePlan(def, heightmap, foundation);
        return new AutoDesignReport(theme, style, facing, roof, wings, foundation, plan);
    }

    public static AutoDesignReport autoPopulateComplex(
        Vec3i origin,
        StructurePlacementController controller,
        ILevelAccess level,
        String biomeHint,
        long seed
    ) {
        AutoDesignReport report = autoDesign(origin, null, level, biomeHint, seed);
        GridPresets.PresetKind kind = switch (report.biomeTheme()) {
            case DESERT_ARID, COASTAL_WATERFRONT -> GridPresets.PresetKind.COMMERCIAL_ROW;
            case TEMPERATE_FOREST, CHERRY_EAST_ASIAN -> GridPresets.PresetKind.VILLAGE_SQUARE;
            case SNOWY_ALPINE, STONY_HIGHLAND -> GridPresets.PresetKind.MANOR_ESTATE;
        };
        controller.grid().setOrigin(origin);
        GridPresets.populate(kind, controller, origin.y(), report.chosenStyle(), seed);
        return report;
    }

    private static Direction pickBestApproachDirection(BoundingBox3i bounds, ILevelAccess level) {
        if (level == null) return Direction.SOUTH;
        int midX = (bounds.min().x() + bounds.max().x()) / 2;
        int midZ = (bounds.min().z() + bounds.max().z()) / 2;
        int baseY = bounds.min().y();

        Direction best = Direction.SOUTH;
        int bestDelta = Integer.MAX_VALUE;
        for (Direction d : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
            Vec3i probe = new Vec3i(midX, baseY, midZ).offset(d, Math.max(bounds.sizeX(), bounds.sizeZ()) / 2 + 2);
            int surfY = level.getSurfaceHeight(probe.x(), probe.z());
            int delta = Math.abs(surfY - baseY);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = d;
            }
        }
        return best;
    }
}
