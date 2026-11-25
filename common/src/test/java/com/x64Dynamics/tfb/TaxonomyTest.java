package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.taxonomy.*;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public final class TaxonomyTest {
    @Test
    public void testInference() {
        BlockTaxonomyEntry entry = BlockTaxonomyClassifier.analyze(BlockIdentifier.of("minecraft:deepslate_tile_stairs[facing=north,half=bottom]"));
        assertTrue(entry.hasTrait(BlockTrait.STAIR));
        assertEquals(MaterialSubstance.DEEPSLATE, entry.substance());
        assertEquals(ColorTone.BLACK, entry.primaryTone());
    }

    @Test
    public void testModdedBlockDiscovery() {
        BlockTaxonomyEntry modded = BlockTaxonomyClassifier.analyze(BlockIdentifier.of("biomesoplenty:fir_planks"));
        assertTrue(modded.hasTrait(BlockTrait.SOLID_CUBE));
        assertEquals(MaterialSubstance.WOOD, modded.substance());
    }

    @Test
    public void testVariantLookup() {
        BlockTaxonomyRegistry registry = BlockTaxonomyRegistry.getInstance();
        BlockIdentifier base = BlockIdentifier.of("minecraft:stone_bricks");
        Optional<BlockIdentifier> stair = registry.findMatchingVariant(base, BlockTrait.STAIR);
        assertTrue(stair.isPresent());
        assertTrue(stair.get().path().contains("stair"));
    }
}
