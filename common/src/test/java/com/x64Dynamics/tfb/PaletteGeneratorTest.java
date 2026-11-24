package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.*;
import org.junit.Test;

import static org.junit.Assert.*;

public final class PaletteGeneratorTest {
    @Test
    public void testDynamicHarmonicGeneration() {
        BlockIdentifier seed = BlockIdentifier.of("minecraft:deepslate_bricks");
        MaterialPalette palette = HarmonicPaletteGenerator.generateFromSeed("dynamic_deepslate", seed);

        assertEquals("dynamic_deepslate", palette.id());
        assertEquals("deepslate_bricks", palette.get(MaterialRole.WALL_PRIMARY).path());
        assertNotNull(palette.get(MaterialRole.ROOF_SLOPE));
        assertNotNull(palette.get(MaterialRole.WALL_ACCENT));
        assertNotNull(palette.get(MaterialRole.FLOOR));
    }

    @Test
    public void testBiomeContextMapping() {
        BiomeArchitecturalContext desert = BiomeArchitecturalContext.fromBiomeName("minecraft:desert_lakes");
        assertEquals(BiomeArchitecturalContext.DESERT_ARID, desert);
        assertEquals("minecraft:sandstone", desert.primaryBlockId());

        BiomeArchitecturalContext snowy = BiomeArchitecturalContext.fromBiomeName("minecraft:snowy_taiga");
        assertEquals(BiomeArchitecturalContext.TAIGA_COLD, snowy);
    }
}
