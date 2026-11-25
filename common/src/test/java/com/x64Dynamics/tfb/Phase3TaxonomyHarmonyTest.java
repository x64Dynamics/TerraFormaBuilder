package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.taxonomy.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class Phase3TaxonomyHarmonyTest {

    @Test
    public void testModdedFurnitureAndLightSourceTagging() {
        BlockTaxonomyEntry chair = BlockTaxonomyClassifier.analyze(BlockIdentifier.of("cfm:oak_chair"));
        assertTrue(chair.traits().contains(BlockTrait.FURNITURE));
        assertTrue(chair.traits().contains(BlockTrait.DECORATIVE));

        BlockTaxonomyEntry sconce = BlockTaxonomyClassifier.analyze(BlockIdentifier.of("supplementaries:brass_sconce"));
        assertTrue(sconce.traits().contains(BlockTrait.LANTERN));
        assertTrue(sconce.traits().contains(BlockTrait.LIGHT_EMITTING));
        assertTrue(sconce.luminance() > 0);
    }

    @Test
    public void testRgbToHsvAndComplementaryHarmony() {
        float[] redHsv = PaletteHarmonizer.rgbToHsv(255, 0, 0);
        assertEquals(0f, redHsv[0], 0.5f);
        assertEquals(1f, redHsv[1], 0.01f);
        assertEquals(1f, redHsv[2], 0.01f);

        float comp = PaletteHarmonizer.hsvHarmonyScore(200, 90, 40, 40, 150, 200);
        assertTrue("Complementary colors should score high harmony", comp >= 0.65f);

        float score = PaletteHarmonizer.scoreHarmony(StylePreset.MEDIEVAL.createPalette());
        assertTrue("Medieval palette harmony should be >= 0.7", score >= 0.7f);
    }
}
