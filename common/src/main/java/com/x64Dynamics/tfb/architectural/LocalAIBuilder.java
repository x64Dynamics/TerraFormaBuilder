package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.architectural.interior.layouts.TowerGenerator;
import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.architectural.grid.ModularGridSession;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;

import java.util.*;

// Local context-aware AI architectural builder.
// Scans surrounding terrain, water, slope, and nearby man-made structures via ILevelAccess,
// interprets natural-language architectural prompts locally, and generates complete detailed builds.
public final class LocalAIBuilder {
    public enum Archetype {
        COTTAGE,
        MANOR,
        TAVERN,
        TOWER,
        WORKSHOP,
        MODULAR_COMPOUND,
        PIER_HOUSE
    }

    public record WorldContextReport(
        AutonomousArchitect.DetectedBiomeTheme biomeTheme,
        StylePreset harmonizedNeighborhoodStyle,
        int minSurfaceY,
        int maxSurfaceY,
        int slopeDelta,
        float waterRatio,
        boolean hasNearbyStructures,
        Direction bestApproachFacing,
        FoundationStrategy recommendedFoundation
    ) { }

    public record DesignIntent(
        Archetype archetype,
        StylePreset style,
        int width,
        int depth,
        int stories,
        int storyHeight,
        RoofGenerator.RoofType roofType,
        WingConfiguration wings,
        boolean addPorch,
        boolean addChimney,
        boolean modularCompound
    ) { }

    public record AIBuildResult(
        WorldContextReport context,
        DesignIntent intent,
        List<PlacementTarget> targets,
        BuildingPlan plan,
        String summary
    ) { }

    private LocalAIBuilder() { }

    public static WorldContextReport analyzeLocalWorld(Vec3i origin, ILevelAccess level) {
        Vec3i org = origin != null ? origin : new Vec3i(0, 64, 0);
        var biome = AutonomousArchitect.detectBiomeTheme(org, level, null);
        if (level == null) {
            return new WorldContextReport(
                biome, StylePreset.MEDIEVAL, org.y(), org.y(), 0, 0f, false,
                Direction.SOUTH, FoundationStrategy.SOLID_SUB_BASEMENT
            );
        }

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int totalSamples = 0;
        int waterSamples = 0;
        int manMadeHits = 0;

        int stoneBrickScore = 0, modernScore = 0, japaneseScore = 0, rusticScore = 0, gothicScore = 0;
        Map<Direction, Integer> approachScore = new EnumMap<>(Direction.class);
        for (Direction d : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            approachScore.put(d, 0);
        }

        for (int dx = -14; dx <= 14; dx += 2) {
            for (int dz = -14; dz <= 14; dz += 2) {
                int x = org.x() + dx;
                int z = org.z() + dz;
                int sy = level.getSurfaceHeight(x, z);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy);
                totalSamples++;

                BlockIdentifier top = level.getBlockState(new Vec3i(x, sy, z));
                String path = top.path().toLowerCase(Locale.ROOT);
                boolean isWater = path.contains("water") || path.contains("kelp");
                if (isWater) waterSamples++;

                if (path.contains("stone_brick") || path.contains("cobblestone")) {
                    manMadeHits++;
                    stoneBrickScore += 2;
                } else if (path.contains("deepslate") || path.contains("blackstone")) {
                    manMadeHits++;
                    gothicScore += 2;
                } else if (path.contains("quartz") || path.contains("concrete")) {
                    manMadeHits++;
                    modernScore += 2;
                } else if (path.contains("cherry") || path.contains("bamboo")) {
                    manMadeHits++;
                    japaneseScore += 2;
                } else if (path.contains("planks") || path.contains("brick") || path.contains("lantern") || path.contains("path")) {
                    manMadeHits++;
                    rusticScore += 2;
                }

                // Score cardinal approaches (prefer flat, dry, path-adjacent)
                int delta = Math.abs(sy - org.y());
                int score = (isWater ? -15 : 4) - delta * 2 + (path.contains("path") ? 12 : 0);
                if (dz > 4 && Math.abs(dx) <= 6) approachScore.merge(Direction.SOUTH, score, Integer::sum);
                if (dz < -4 && Math.abs(dx) <= 6) approachScore.merge(Direction.NORTH, score, Integer::sum);
                if (dx > 4 && Math.abs(dz) <= 6) approachScore.merge(Direction.EAST, score, Integer::sum);
                if (dx < -4 && Math.abs(dz) <= 6) approachScore.merge(Direction.WEST, score, Integer::sum);
            }
        }

        int slopeDelta = (minY == Integer.MAX_VALUE) ? 0 : (maxY - minY);
        float waterRatio = totalSamples > 0 ? (float) waterSamples / totalSamples : 0f;
        boolean hasNearby = manMadeHits >= 3;

        StylePreset neighborStyle = switch (biome) {
            case SNOWY_ALPINE -> StylePreset.NORDIC;
            case DESERT_ARID -> StylePreset.TRADITIONAL;
            case CHERRY_EAST_ASIAN -> StylePreset.JAPANESE;
            case COASTAL_WATERFRONT -> StylePreset.RUSTIC;
            case STONY_HIGHLAND -> StylePreset.GOTHIC;
            case TEMPERATE_FOREST -> StylePreset.MEDIEVAL;
        };
        if (hasNearby) {
            int best = Math.max(Math.max(stoneBrickScore, modernScore), Math.max(japaneseScore, Math.max(rusticScore, gothicScore)));
            if (best == modernScore && modernScore > 0) neighborStyle = StylePreset.MODERN;
            else if (best == gothicScore && gothicScore > 0) neighborStyle = StylePreset.GOTHIC;
            else if (best == japaneseScore && japaneseScore > 0) neighborStyle = StylePreset.JAPANESE;
            else if (best == rusticScore && rusticScore > 0) neighborStyle = StylePreset.RUSTIC;
            else if (best == stoneBrickScore && stoneBrickScore > 0) neighborStyle = StylePreset.MEDIEVAL;
        }

        Direction bestFacing = Direction.SOUTH;
        int bestDirScore = Integer.MIN_VALUE;
        for (var e : approachScore.entrySet()) {
            if (e.getValue() > bestDirScore) {
                bestDirScore = e.getValue();
                bestFacing = e.getKey();
            }
        }

        FoundationStrategy foundation = waterRatio >= 0.30f
            ? FoundationStrategy.PILLARS_STILTS
            : (slopeDelta >= 5 ? FoundationStrategy.STEPPED_TERRACE : FoundationStrategy.SOLID_SUB_BASEMENT);

        return new WorldContextReport(
            biome, neighborStyle, minY == Integer.MAX_VALUE ? org.y() : minY,
            maxY == Integer.MIN_VALUE ? org.y() : maxY, slopeDelta, waterRatio,
            hasNearby, bestFacing, foundation
        );
    }

    public static DesignIntent parseDesignPrompt(String prompt, WorldContextReport ctx) {
        String p = (prompt == null ? "" : prompt).toLowerCase(Locale.ROOT).trim();

        Archetype archetype = ctx.waterRatio() >= 0.35f ? Archetype.PIER_HOUSE : Archetype.COTTAGE;
        if (p.contains("modular") || p.contains("grid") || p.contains("compound") || p.contains("connected")) archetype = Archetype.MODULAR_COMPOUND;
        else if (p.contains("tower") || p.contains("watchtower") || p.contains("keep")) archetype = Archetype.TOWER;
        else if (p.contains("tavern") || p.contains("inn") || p.contains("hall")) archetype = Archetype.TAVERN;
        else if (p.contains("manor") || p.contains("mansion") || p.contains("villa") || p.contains("estate")) archetype = Archetype.MANOR;
        else if (p.contains("workshop") || p.contains("smithy") || p.contains("forge")) archetype = Archetype.WORKSHOP;

        StylePreset style = ctx.harmonizedNeighborhoodStyle();
        if (p.contains("modern") || p.contains("contemporary")) style = StylePreset.MODERN;
        else if (p.contains("gothic") || p.contains("dark") || p.contains("cathedral")) style = StylePreset.GOTHIC;
        else if (p.contains("japanese") || p.contains("asian") || p.contains("cherry") || p.contains("pagoda")) style = StylePreset.JAPANESE;
        else if (p.contains("nordic") || p.contains("viking") || p.contains("alpine")) style = StylePreset.NORDIC;
        else if (p.contains("victorian")) style = StylePreset.VICTORIAN;
        else if (p.contains("rustic") || p.contains("cabin") || p.contains("cottage")) style = StylePreset.RUSTIC;
        else if (p.contains("industrial") || p.contains("factory")) style = StylePreset.INDUSTRIAL;
        else if (p.contains("fantasy") || p.contains("wizard") || p.contains("magic")) style = StylePreset.FANTASY;
        else if (p.contains("classical") || p.contains("roman") || p.contains("greek")) style = StylePreset.CLASSICAL;
        else if (p.contains("minimal")) style = StylePreset.MINIMALIST;
        else if (p.contains("medieval") || p.contains("castle")) style = StylePreset.MEDIEVAL;

        int stories = switch (archetype) {
            case MANOR, TAVERN -> 2;
            case TOWER -> 3;
            default -> 2;
        };
        if (p.contains("1-story") || p.contains("1 story") || p.contains("single story") || p.contains("1 floor")) stories = 1;
        else if (p.contains("2-story") || p.contains("2 story") || p.contains("two story") || p.contains("2 floor")) stories = 2;
        else if (p.contains("3-story") || p.contains("3 story") || p.contains("three story") || p.contains("3 floor")) stories = 3;
        else if (p.contains("4-story") || p.contains("4 story") || p.contains("four story") || p.contains("4 floor")) stories = 4;

        int width = switch (archetype) {
            case MANOR -> 20;
            case TAVERN -> 18;
            case TOWER -> 11;
            case WORKSHOP -> 13;
            default -> 14;
        };
        int depth = switch (archetype) {
            case MANOR -> 16;
            case TAVERN -> 14;
            case TOWER -> 11;
            case WORKSHOP -> 12;
            default -> 12;
        };
        if (p.contains("small") || p.contains("tiny") || p.contains("cozy")) {
            width = Math.max(9, width - 4);
            depth = Math.max(9, depth - 4);
        } else if (p.contains("large") || p.contains("grand") || p.contains("big") || p.contains("huge")) {
            width += 6;
            depth += 4;
        }

        WingConfiguration wings = switch (archetype) {
            case MANOR -> WingConfiguration.U_SHAPED;
            case TAVERN -> WingConfiguration.L_SHAPED;
            default -> WingConfiguration.SINGLE_RECTANGLE;
        };
        if (p.contains("u-shape") || p.contains("u shape") || p.contains("courtyard")) wings = WingConfiguration.U_SHAPED;
        else if (p.contains("l-shape") || p.contains("l shape")) wings = WingConfiguration.L_SHAPED;

        RoofGenerator.RoofType roof = switch (style) {
            case MODERN, MINIMALIST -> RoofGenerator.RoofType.FLAT;
            case GOTHIC, VICTORIAN -> RoofGenerator.RoofType.MANSARD;
            case JAPANESE -> RoofGenerator.RoofType.HIP;
            default -> RoofGenerator.RoofType.GABLE;
        };
        if (archetype == Archetype.TOWER) roof = RoofGenerator.RoofType.CONICAL;
        if (p.contains("flat roof")) roof = RoofGenerator.RoofType.FLAT;
        else if (p.contains("hip")) roof = RoofGenerator.RoofType.HIP;
        else if (p.contains("mansard")) roof = RoofGenerator.RoofType.MANSARD;
        else if (p.contains("dome")) roof = RoofGenerator.RoofType.DOME;
        else if (p.contains("gable")) roof = RoofGenerator.RoofType.GABLE;

        boolean addPorch = p.contains("porch") || p.contains("veranda") || archetype == Archetype.PIER_HOUSE || archetype == Archetype.COTTAGE || archetype == Archetype.TAVERN;
        boolean addChimney = p.contains("chimney") || p.contains("fireplace") || style == StylePreset.MEDIEVAL || style == StylePreset.RUSTIC || style == StylePreset.NORDIC;
        int storyHeight = (style == StylePreset.GOTHIC || archetype == Archetype.MANOR) ? 5 : 4;

        return new DesignIntent(
            archetype, style, width, depth, stories, storyHeight,
            roof, wings, addPorch, addChimney, archetype == Archetype.MODULAR_COMPOUND
        );
    }

    public static AIBuildResult designAndBuild(Vec3i origin, String prompt, ILevelAccess level, long seed) {
        Vec3i org = origin != null ? origin : new Vec3i(0, 64, 0);
        WorldContextReport ctx = analyzeLocalWorld(org, level);
        DesignIntent intent = parseDesignPrompt(prompt, ctx);
        MaterialPalette palette = intent.style().createPalette();

        if (intent.modularCompound()) {
            ModularGridSession session = new ModularGridSession(org, 5, 5);
            // Build an L-shaped connected 5x5x5 ground floor + 2nd-story tower/wing
            session.placeModule(0, 0, 0, intent.style(), palette, intent.roofType());
            session.placeModule(1, 0, 0, intent.style(), palette, intent.roofType());
            session.placeModule(0, 0, 1, intent.style(), palette, intent.roofType());
            for (int f = 1; f < Math.min(3, intent.stories()); f++) {
                session.placeModule(0, f, 0, intent.style(), palette, intent.roofType());
            }
            List<PlacementTarget> targets = session.buildPlacementTargets();
            String summary = String.format("AI Modular %s (%d modules, %d blocks, facing %s)",
                intent.style().name(), session.modules().size(), targets.size(), ctx.bestApproachFacing().name());
            return new AIBuildResult(ctx, intent, targets, null, summary);
        }

        if (intent.archetype() == Archetype.TOWER) {
            int radius = Math.max(4, intent.width() / 2);
            int totalHeight = Math.max(8, intent.stories() * intent.storyHeight());
            var towerCfg = new TowerGenerator.TowerConfig(
                TowerGenerator.TowerShape.OCTAGONAL, radius, totalHeight, intent.stories(),
                true, true, true
            );
            List<PlacementTarget> targets = TowerGenerator.generateTower(org.add(radius, 0, radius), towerCfg, palette);
            String summary = String.format("AI %s %dF Tower (%d blocks, facing %s)",
                intent.style().name(), intent.stories(), targets.size(), ctx.bestApproachFacing().name());
            return new AIBuildResult(ctx, intent, targets, null, summary);
        }

        BoundingBox3i box = new BoundingBox3i(org, org.add(intent.width() - 1, intent.stories() * intent.storyHeight() + 8, intent.depth() - 1));
        var cfg = new HomeGenerator.HomeConfig(
            box, palette, intent.roofType(), intent.stories(), intent.storyHeight(),
            intent.style(), ctx.bestApproachFacing(), intent.wings(),
            intent.addPorch(), intent.addChimney(), true, 0.90f, seed
        );
        var res = HomeGenerator.generateHome(cfg);
        String summary = String.format("AI %s %dF %s (%d blocks, %s, facing %s)",
            intent.style().name(), res.stories(), intent.archetype().name(),
            res.targets().size(), ctx.recommendedFoundation().name(), ctx.bestApproachFacing().name());
        return new AIBuildResult(ctx, intent, res.targets(), res.plan(), summary);
    }
}
