package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.city.SettlementPattern;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprets natural-language architectural prompts (VoxelDreamer-style)
 * into structured building/city parameters, style presets, roof forms,
 * foundation strategies, and detailing toggles.
 */
public final class PromptBuildingInterpreter {
    private static final Pattern STORIES_PATTERN = Pattern.compile("\\b(\\d+)\\s*[- ]?(?:story|stories|floor|floors)\\b");
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\b(\\d+)\\s*[x×]\\s*(\\d+)(?:\\s*[x×]\\s*(\\d+))?\\b");

    public record PromptInterpretation(
        String normalizedPrompt,
        boolean cityMode,
        SettlementPattern settlementPattern,
        StylePreset stylePreset,
        RoofGenerator.RoofType roofType,
        WingConfiguration wingConfiguration,
        FoundationStrategy foundationStrategy,
        int width,
        int height,
        int depth,
        int stories,
        boolean porchAndGarden,
        boolean chimney,
        boolean balconies,
        boolean dormers,
        boolean shutters,
        boolean furnishInterior,
        String summary
    ) {}

    private PromptBuildingInterpreter() {}

    public static PromptInterpretation interpret(String rawPrompt) {
        String p = rawPrompt == null ? "" : rawPrompt.trim().toLowerCase(Locale.ROOT);

        boolean cityMode = containsAny(p, "city", "town", "village", "settlement", "harbor", "port", "district");
        SettlementPattern pattern = SettlementPattern.MEDIEVAL_TOWN;
        if (containsAny(p, "village", "hamlet", "organic")) pattern = SettlementPattern.ORGANIC_VILLAGE;
        else if (containsAny(p, "grid", "modern", "planned")) pattern = SettlementPattern.PLANNED_GRID;
        else if (containsAny(p, "suburb", "neighborhood")) pattern = SettlementPattern.SUBURBAN_NEIGHBORHOOD;
        else if (containsAny(p, "urban", "downtown", "skyline")) pattern = SettlementPattern.URBAN_DISTRICT;
        else if (containsAny(p, "industrial", "factory", "port", "harbor", "dock")) pattern = SettlementPattern.INDUSTRIAL_SECTOR;

        StylePreset style = detectStyle(p);
        RoofGenerator.RoofType roof = detectRoof(p, style);
        WingConfiguration wings = detectWings(p);
        FoundationStrategy foundation = detectFoundation(p);

        int width = 15, height = 11, depth = 15, stories = 0;
        if (containsAny(p, "manor", "mansion", "villa", "palace", "estate")) {
            width = 21; height = 14; depth = 19; stories = 2;
            if (wings == WingConfiguration.SINGLE_RECTANGLE) wings = WingConfiguration.L_SHAPED;
        } else if (containsAny(p, "tower", "wizard", "watchtower", "spire", "lighthouse", "pagoda")) {
            width = 11; height = 19; depth = 11; stories = 3;
            if (!containsAny(p, "gable", "flat", "mansard")) {
                roof = style == StylePreset.JAPANESE ? RoofGenerator.RoofType.GABLE : RoofGenerator.RoofType.CONICAL;
            }
        } else if (containsAny(p, "cottage", "cabin", "hut", "shack")) {
            width = 11; height = 9; depth = 11; stories = 1;
        } else if (containsAny(p, "tavern", "inn", "hall", "barn")) {
            width = 19; height = 13; depth = 15; stories = 2;
        }

        if (cityMode) {
            width = 56; height = 24; depth = 56;
        }

        Matcher sm = STORIES_PATTERN.matcher(p);
        if (sm.find()) {
            stories = Math.max(1, Math.min(4, Integer.parseInt(sm.group(1))));
            height = Math.max(height, stories * 4 + 5);
        }

        Matcher dm = SIZE_PATTERN.matcher(p);
        if (dm.find()) {
            int a = Math.max(7, Math.min(96, Integer.parseInt(dm.group(1))));
            int b = Math.max(7, Math.min(96, Integer.parseInt(dm.group(2))));
            if (dm.group(3) != null) {
                int c = Math.max(7, Math.min(96, Integer.parseInt(dm.group(3))));
                width = a;
                height = b;
                depth = c;
            } else {
                width = a;
                depth = b;
            }
        }

        boolean porch = !containsAny(p, "no porch", "no garden");
        boolean chimney = containsAny(p, "chimney", "fireplace") || (style != StylePreset.MODERN && style != StylePreset.MINIMALIST);
        boolean balconies = containsAny(p, "balcony", "balconies", "terrace") || stories >= 2;
        boolean dormers = containsAny(p, "dormer", "attic") || (roof == RoofGenerator.RoofType.GABLE && width >= 13);
        boolean shutters = !containsAny(p, "no shutter") && style != StylePreset.MODERN;
        boolean furnish = !containsAny(p, "unfurnished", "empty shell", "no interior");

        String summary = cityMode
            ? String.format("%s %s (%dx%d)", style.name(), pattern.name(), width, depth)
            : String.format("%s %s %s (%dx%dx%d)", style.name(), wings.name(), roof.name(), width, height, depth);

        return new PromptInterpretation(
            p, cityMode, pattern, style, roof, wings, foundation,
            width, height, depth, stories, porch, chimney, balconies, dormers, shutters, furnish, summary
        );
    }

    private static StylePreset detectStyle(String p) {
        if (containsAny(p, "modern", "glass", "concrete", "contemporary", "minimalist")) return StylePreset.MODERN;
        if (containsAny(p, "nordic", "viking", "spruce", "log cabin", "cabin")) return StylePreset.NORDIC;
        if (containsAny(p, "japanese", "pagoda", "zen", "cherry", "oriental")) return StylePreset.JAPANESE;
        if (containsAny(p, "minimalist", "desert", "sandstone", "adobe", "clean")) return StylePreset.MINIMALIST;
        if (containsAny(p, "fantasy", "wizard", "magic", "elven", "enchanted")) return StylePreset.FANTASY;
        if (containsAny(p, "classical", "roman", "greek", "marble", "temple")) return StylePreset.CLASSICAL;
        if (containsAny(p, "industrial", "factory", "steampunk", "brick", "warehouse")) return StylePreset.INDUSTRIAL;
        if (containsAny(p, "gothic", "cathedral", "dark", "vampire", "castle")) return StylePreset.GOTHIC;
        if (containsAny(p, "traditional", "coastal", "colonial", "suburban")) return StylePreset.TRADITIONAL;
        if (containsAny(p, "victorian", "ornate", "manor")) return StylePreset.VICTORIAN;
        if (containsAny(p, "rustic", "cottage", "farm", "barn")) return StylePreset.RUSTIC;
        return StylePreset.MEDIEVAL;
    }

    private static RoofGenerator.RoofType detectRoof(String p, StylePreset style) {
        if (containsAny(p, "mansard")) return RoofGenerator.RoofType.MANSARD;
        if (containsAny(p, "hip")) return RoofGenerator.RoofType.HIP;
        if (containsAny(p, "flat", "rooftop")) return RoofGenerator.RoofType.FLAT;
        if (containsAny(p, "conical", "cone", "spire")) return RoofGenerator.RoofType.CONICAL;
        if (containsAny(p, "dome", "cupola")) return RoofGenerator.RoofType.DOME;
        if (containsAny(p, "shed", "slanted")) return RoofGenerator.RoofType.SHED;
        if (containsAny(p, "gable", "pitched")) return RoofGenerator.RoofType.GABLE;
        if (style == StylePreset.MODERN || style == StylePreset.MINIMALIST) return RoofGenerator.RoofType.FLAT;
        if (style == StylePreset.VICTORIAN) return RoofGenerator.RoofType.MANSARD;
        return RoofGenerator.RoofType.GABLE;
    }

    private static WingConfiguration detectWings(String p) {
        if (containsAny(p, "l-shape", "l shape", "l-wing")) return WingConfiguration.L_SHAPED;
        if (containsAny(p, "t-shape", "t shape")) return WingConfiguration.T_SHAPED;
        if (containsAny(p, "u-shape", "u shape", "courtyard")) return WingConfiguration.U_SHAPED;
        if (containsAny(p, "cross", "cruciform", "cathedral")) return WingConfiguration.CROSS_SHAPED;
        return WingConfiguration.SINGLE_RECTANGLE;
    }

    private static FoundationStrategy detectFoundation(String p) {
        if (containsAny(p, "stilt", "pillar", "elevated", "swamp", "over water")) return FoundationStrategy.PILLARS_STILTS;
        if (containsAny(p, "basement", "cellar", "solid foundation")) return FoundationStrategy.SOLID_SUB_BASEMENT;
        if (containsAny(p, "cut", "excavate", "level ground", "flatten")) return FoundationStrategy.CUT_AND_FILL;
        if (containsAny(p, "cliff", "cantilever", "overhang")) return FoundationStrategy.CLIFF_CANTILEVER;
        if (containsAny(p, "cave", "mountain", "embedded", "hobbit")) return FoundationStrategy.MOUNTAIN_CAVE_EMBED;
        return FoundationStrategy.STEPPED_TERRACE;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
