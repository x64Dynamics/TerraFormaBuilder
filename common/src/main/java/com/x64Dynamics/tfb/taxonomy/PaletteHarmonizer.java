package com.x64Dynamics.tfb.taxonomy;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialFamilyResolver;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.List;

public final class PaletteHarmonizer {
    private PaletteHarmonizer() {}

    public static float scoreHarmony(MaterialPalette palette) {
        if (palette == null) return 0.0f;

        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier roof = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);

        BlockTaxonomyEntry wallEntry = BlockTaxonomyRegistry.get(wall.namespacedId());
        BlockTaxonomyEntry roofEntry = BlockTaxonomyRegistry.get(roof.namespacedId());
        BlockTaxonomyEntry fndEntry = BlockTaxonomyRegistry.get(foundation.namespacedId());

        float score = 0.5f;

        // Contrast between wall and roof is desirable
        if (wallEntry != null && roofEntry != null) {
            if (wallEntry.primaryTone() != roofEntry.primaryTone()) {
                score += 0.25f;
            }
            // Material family matching (stair exists)
            if (roof.path().contains("stairs")) {
                score += 0.15f;
            }
        }

        // Foundation should be robust (stone, masonry, deepslate)
        if (fndEntry != null) {
            if (fndEntry.substance() == MaterialSubstance.STONE || fndEntry.substance() == MaterialSubstance.DEEPSLATE || fndEntry.substance() == MaterialSubstance.BRICK) {
                score += 0.1f;
            }
        }

        return Math.min(1.0f, score);
    }

    public static MaterialPalette completePalette(String wallBlockId) {
        MaterialPalette palette = new MaterialPalette("harmonized_" + wallBlockId);
        palette.set(MaterialRole.WALL_PRIMARY, wallBlockId);

        com.x64Dynamics.tfb.material.MaterialFamily family = MaterialFamilyResolver.resolve(BlockIdentifier.of(wallBlockId));
        BlockIdentifier stair = family.stair();
        palette.set(MaterialRole.ROOF_SLOPE, stair != null ? stair.namespacedId() : "minecraft:stone_brick_stairs");

        BlockIdentifier slab = family.slab();
        palette.set(MaterialRole.ROOF_RIDGE, slab != null ? slab.namespacedId() : "minecraft:stone_brick_slab");

        palette.set(MaterialRole.WALL_ACCENT, "minecraft:spruce_planks");
        palette.set(MaterialRole.PILLAR, "minecraft:stripped_spruce_log");
        palette.set(MaterialRole.FOUNDATION, "minecraft:cobblestone");
        palette.set(MaterialRole.FLOOR, "minecraft:spruce_planks");
        palette.set(MaterialRole.DOOR, "minecraft:spruce_door");
        palette.set(MaterialRole.WINDOW, "minecraft:glass_pane");
        palette.set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

        return palette;
    }
}