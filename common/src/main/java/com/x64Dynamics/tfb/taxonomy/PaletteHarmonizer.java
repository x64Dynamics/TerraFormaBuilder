package com.x64Dynamics.tfb.taxonomy;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialFamilyResolver;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

public final class PaletteHarmonizer {
    private PaletteHarmonizer() {}

    /**
     * Converts 8-bit RGB [0..255] to HSV: hue in [0..360), saturation in [0..1], value in [0..1].
     */
    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = Math.max(0f, Math.min(1f, r / 255f));
        float gf = Math.max(0f, Math.min(1f, g / 255f));
        float bf = Math.max(0f, Math.min(1f, b / 255f));

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        if (delta > 1e-5f) {
            if (max == rf) {
                h = 60f * (((gf - bf) / delta) % 6f);
            } else if (max == gf) {
                h = 60f * (((bf - rf) / delta) + 2f);
            } else {
                h = 60f * (((rf - gf) / delta) + 4f);
            }
            if (h < 0f) h += 360f;
        }
        float s = (max <= 1e-5f) ? 0f : (delta / max);
        return new float[]{h, s, max};
    }

    /**
     * Computes visual harmony [0.0..1.0] between two RGB colors using HSV hue-angle
     * relationships (complementary, triadic, analogous) and value contrast.
     */
    public static float hsvHarmonyScore(int r1, int g1, int b1, int r2, int g2, int b2) {
        float[] hsv1 = rgbToHsv(r1, g1, b1);
        float[] hsv2 = rgbToHsv(r2, g2, b2);

        float valueContrast = Math.abs(hsv1[2] - hsv2[2]);
        boolean eitherNeutral = hsv1[1] < 0.15f || hsv2[1] < 0.15f;

        float hueScore;
        if (eitherNeutral) {
            // Neutral paired with chromatic or contrasting neutral scores well on value separation
            hueScore = 0.55f + Math.min(0.45f, valueContrast * 0.9f);
        } else {
            float diff = Math.abs(hsv1[0] - hsv2[0]);
            if (diff > 180f) diff = 360f - diff;
            // Complementary (~180), triadic (~120), or analogous (<=35)
            float compDist = Math.abs(diff - 180f) / 180f;
            float triadDist = Math.abs(diff - 120f) / 120f;
            float analogDist = diff <= 35f ? (diff / 35f) * 0.25f : 1.0f;
            hueScore = 1.0f - Math.min(compDist, Math.min(triadDist, analogDist));
        }

        return Math.max(0f, Math.min(1f, 0.7f * hueScore + 0.3f * Math.min(1f, valueContrast * 1.6f)));
    }

    public static float scoreHarmony(MaterialPalette palette) {
        if (palette == null) return 0.0f;

        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier roof = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);

        BlockTaxonomyEntry wallEntry = BlockTaxonomyRegistry.get(wall.namespacedId());
        BlockTaxonomyEntry roofEntry = BlockTaxonomyRegistry.get(roof.namespacedId());
        BlockTaxonomyEntry fndEntry = BlockTaxonomyRegistry.get(foundation.namespacedId());

        float score = 0.35f;

        if (wallEntry != null && roofEntry != null) {
            ColorTone wt = wallEntry.primaryTone();
            ColorTone rt = roofEntry.primaryTone();
            float hsvScore = hsvHarmonyScore(wt.r(), wt.g(), wt.b(), rt.r(), rt.g(), rt.b());
            score += 0.40f * hsvScore;

            if (roof.path().contains("stairs")) {
                score += 0.15f;
            }
        }

        if (fndEntry != null) {
            if (fndEntry.substance() == MaterialSubstance.STONE
                || fndEntry.substance() == MaterialSubstance.DEEPSLATE
                || fndEntry.substance() == MaterialSubstance.BRICK) {
                score += 0.10f;
            }
        }

        return Math.max(0.0f, Math.min(1.0f, score));
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
