package com.x64Dynamics.tfb.material;

import com.x64Dynamics.tfb.material.taxonomy.*;

import java.util.*;

public final class HarmonicPaletteGenerator {
    private HarmonicPaletteGenerator() { }

    public static MaterialPalette generateFromSeed(String paletteId, BlockIdentifier seedBlock) {
        BlockTaxonomyRegistry registry = BlockTaxonomyRegistry.getInstance();
        BlockTaxonomyEntry seedEntry = registry.getOrRegister(seedBlock);

        MaterialPalette palette = new MaterialPalette(paletteId);

        // 1. Primary wall
        palette.set(MaterialRole.WALL_PRIMARY, seedBlock);

        // 2. Matching variants for primary
        BlockIdentifier stair = registry.findMatchingVariant(seedBlock, BlockTrait.STAIR).orElse(seedBlock);
        BlockIdentifier slab = registry.findMatchingVariant(seedBlock, BlockTrait.SLAB).orElse(seedBlock);
        BlockIdentifier wall = registry.findMatchingVariant(seedBlock, BlockTrait.WALL).orElse(seedBlock);
        BlockIdentifier fence = registry.findMatchingVariant(seedBlock, BlockTrait.FENCE).orElse(seedBlock);
        BlockIdentifier door = registry.findMatchingVariant(seedBlock, BlockTrait.DOOR).orElse(BlockIdentifier.of("minecraft:oak_door"));

        palette.set(MaterialRole.ROOF_SLOPE, stair);
        palette.set(MaterialRole.ROOF_RIDGE, slab);
        palette.set(MaterialRole.STAIR, stair);
        palette.set(MaterialRole.SLAB, slab);
        palette.set(MaterialRole.FENCE, fence);
        palette.set(MaterialRole.DOOR, door);

        // 3. Contrast accent and corners
        ColorTone accentTone = seedEntry.accentTone();
        List<BlockTaxonomyEntry> accentPool = registry.queryByTone(accentTone, BlockTrait.SOLID_CUBE);
        BlockIdentifier accentBlock = !accentPool.isEmpty() ? accentPool.get(0).block() : seedBlock;
        palette.set(MaterialRole.WALL_ACCENT, accentBlock);

        // 4. Pillars & Beams
        List<BlockTaxonomyEntry> pillarPool = registry.query(MaterialSubstance.WOOD, BlockTrait.PILLAR);
        BlockIdentifier pillarBlock = !pillarPool.isEmpty() ? pillarPool.get(0).block() : seedBlock;
        palette.set(MaterialRole.PILLAR, pillarBlock);
        palette.set(MaterialRole.BEAM, pillarBlock);
        palette.set(MaterialRole.WALL_CORNER, pillarBlock);

        // 5. Floors & Ceilings
        List<BlockTaxonomyEntry> woodPool = registry.query(MaterialSubstance.WOOD, BlockTrait.SOLID_CUBE);
        BlockIdentifier floorBlock = !woodPool.isEmpty() ? woodPool.get(0).block() : slab;
        palette.set(MaterialRole.FLOOR, floorBlock);
        palette.set(MaterialRole.CEILING, slab);

        // 6. Foundations
        List<BlockTaxonomyEntry> stonePool = registry.query(MaterialSubstance.STONE, BlockTrait.SOLID_CUBE);
        BlockIdentifier stoneBlock = !stonePool.isEmpty() ? stonePool.get(0).block() : seedBlock;
        palette.set(MaterialRole.FOUNDATION, stoneBlock);
        palette.set(MaterialRole.SUB_FOUNDATION, BlockIdentifier.of("minecraft:deepslate"));

        // 7. Glass, Lights & Details
        palette.set(MaterialRole.WINDOW, BlockIdentifier.of("minecraft:glass_pane"));
        palette.set(MaterialRole.LIGHT_SOURCE, BlockIdentifier.of("minecraft:lantern"));

        return palette;
    }
}
