package com.x64Dynamics.tfb.taxonomy;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;

public final class BlockTaxonomyEngine {
    private BlockTaxonomyEngine() { }

    public static BlockTaxonomyEntry analyze(BlockIdentifier block) {
        return analyze(block, EnumSet.noneOf(BlockTrait.class), Collections.emptySet(), 0);
    }

    public static BlockTaxonomyEntry analyze(
        BlockIdentifier block,
        Set<BlockTrait> discoveredTraits,
        Set<String> propertyNames,
        int discoveredLuminance
    ) {
        String path = block.path().toLowerCase(Locale.ROOT);
        EnumSet<BlockTrait> traits = EnumSet.noneOf(BlockTrait.class);
        if (discoveredTraits != null) {
            traits.addAll(discoveredTraits);
        }

        // Detect structural traits from path if not already provided by runtime class inspection
        if (traits.isEmpty() || (traits.size() == 1 && traits.contains(BlockTrait.WATERLOGGABLE))) {
            if (path.contains("stairs")) traits.add(BlockTrait.STAIR);
            else if (path.contains("slab")) traits.add(BlockTrait.SLAB);
            else if (path.contains("wall") && !path.contains("wall_torch") && !path.contains("wall_sign")) traits.add(BlockTrait.WALL);
            else if (path.contains("fence_gate")) traits.add(BlockTrait.FENCE_GATE);
            else if (path.contains("fence")) traits.add(BlockTrait.FENCE);
            else if (path.contains("door") && !path.contains("trapdoor")) traits.add(BlockTrait.DOOR);
            else if (path.contains("trapdoor")) traits.add(BlockTrait.TRAPDOOR);
            else if (path.contains("pillar") || path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae")) traits.add(BlockTrait.PILLAR);
            else if (path.contains("pane") || path.contains("iron_bars")) traits.add(BlockTrait.GLASS_PANE);
            else if (path.contains("lantern")) { traits.add(BlockTrait.LANTERN); traits.add(BlockTrait.LIGHT_EMITTING); }
            else if (path.contains("torch")) { traits.add(BlockTrait.TORCH); traits.add(BlockTrait.LIGHT_EMITTING); }
            else if (path.contains("carpet")) traits.add(BlockTrait.CARPET);
            else traits.add(BlockTrait.SOLID_CUBE);
        }

        // Waterloggable check
        if (block.property("waterlogged") != null || (propertyNames != null && propertyNames.contains("waterlogged"))) {
            traits.add(BlockTrait.WATERLOGGABLE);
        }
        if (propertyNames != null && (propertyNames.contains("open") || propertyNames.contains("powered"))) {
            traits.add(BlockTrait.INTERACTABLE);
        }

        // Detect substance
        MaterialSubstance substance = inferSubstance(path);

        // Detect color tones
        ColorTone primaryTone = inferPrimaryTone(path, substance);
        ColorTone accentTone = primaryTone.complementary();

        int luminance = discoveredLuminance > 0 ? discoveredLuminance : (traits.contains(BlockTrait.LIGHT_EMITTING) ? 15 : 0);
        if (luminance > 0) {
            traits.add(BlockTrait.LIGHT_EMITTING);
        }

        return new BlockTaxonomyEntry(block, traits, substance, primaryTone, accentTone, luminance);
    }

    private static MaterialSubstance inferSubstance(String path) {
        if (path.contains("deepslate") || path.contains("blackstone")) return MaterialSubstance.DEEPSLATE;
        if (path.contains("sandstone")) return MaterialSubstance.SANDSTONE;
        if (path.contains("brick")) return MaterialSubstance.BRICK;
        if (path.contains("quartz")) return MaterialSubstance.QUARTZ;
        if (path.contains("prismarine")) return MaterialSubstance.PRISMARINE;
        if (path.contains("copper")) return MaterialSubstance.COPPER;
        if (path.contains("iron")) return MaterialSubstance.IRON;
        if (path.contains("nether") || path.contains("crimson") || path.contains("warped")) return MaterialSubstance.NETHER;
        if (path.contains("glass")) return MaterialSubstance.GLASS;
        if (path.contains("planks") || path.contains("log") || path.contains("wood") || path.contains("stem")) return MaterialSubstance.WOOD;
        if (path.contains("leaves") || path.contains("grass") || path.contains("vine")) return MaterialSubstance.ORGANIC;
        return MaterialSubstance.STONE;
    }

    private static ColorTone inferPrimaryTone(String path, MaterialSubstance substance) {
        if (path.contains("white") || path.contains("snow") || path.contains("quartz")) return ColorTone.WHITE;
        if (path.contains("gray") || path.contains("stone") || path.contains("andesite")) return ColorTone.LIGHT_GRAY;
        if (path.contains("dark") || path.contains("tuff") || path.contains("basalt")) return ColorTone.DARK_GRAY;
        if (path.contains("black") || path.contains("deepslate") || path.contains("obsidian")) return ColorTone.BLACK;
        if (path.contains("spruce") || path.contains("dark_oak") || path.contains("dirt")) return ColorTone.BROWN_COOL;
        if (path.contains("oak") || path.contains("wood") || path.contains("planks")) return ColorTone.BROWN_WARM;
        if (path.contains("red") || path.contains("crimson") || path.contains("brick")) return ColorTone.RED_WARM;
        if (path.contains("orange") || path.contains("acacia") || path.contains("terracotta")) return ColorTone.ORANGE;
        if (path.contains("yellow") || path.contains("sand") || path.contains("gold")) return ColorTone.YELLOW;
        if (path.contains("green") || path.contains("moss") || path.contains("leaf")) return ColorTone.GREEN;
        if (path.contains("blue") || path.contains("cyan") || path.contains("prismarine") || path.contains("warped")) return ColorTone.BLUE_AQUA;
        if (path.contains("purple") || path.contains("amethyst")) return ColorTone.PURPLE;

        return switch (substance) {
            case STONE -> ColorTone.LIGHT_GRAY;
            case DEEPSLATE -> ColorTone.BLACK;
            case WOOD -> ColorTone.BROWN_WARM;
            case SANDSTONE -> ColorTone.YELLOW;
            case BRICK -> ColorTone.RED_WARM;
            case QUARTZ -> ColorTone.WHITE;
            case PRISMARINE -> ColorTone.BLUE_AQUA;
            case COPPER -> ColorTone.ORANGE;
            case IRON -> ColorTone.LIGHT_GRAY;
            case NETHER -> ColorTone.RED_WARM;
            case GLASS -> ColorTone.WHITE;
            case ORGANIC -> ColorTone.GREEN;
        };
    }
}
