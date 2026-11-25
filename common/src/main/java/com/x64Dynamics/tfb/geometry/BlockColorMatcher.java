package com.x64Dynamics.tfb.geometry;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;

/**
 * Face-visibility-aware color-to-block matcher with 3D 4x4x4 ordered Bayer dithering
 * (reimplemented in Java from ObjToSchematic's Ditherer and AtlasPalette concepts).
 */
public final class BlockColorMatcher {
    private static final int[] BAYER_4X4X4 = {
        0, 16, 2, 18, 48, 32, 50, 34,
        6, 22, 4, 20, 54, 38, 52, 36,
        24, 40, 26, 42, 8, 56, 10, 58,
        30, 46, 28, 44, 14, 62, 12, 60,
        3, 19, 5, 21, 51, 35, 53, 37,
        1, 17, 7, 23, 49, 33, 55, 39,
        27, 43, 29, 45, 11, 59, 13, 61,
        25, 41, 31, 47, 9, 57, 15, 63
    };

    public record ColorBlockEntry(BlockIdentifier block, int r, int g, int b, boolean translucent, boolean emissive) {}

    private static final List<ColorBlockEntry> PALETTE = List.of(
        new ColorBlockEntry(BlockIdentifier.of("minecraft:stone_bricks"), 122, 121, 122, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:cobblestone"), 127, 127, 127, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:deepslate_bricks"), 70, 70, 71, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:blackstone"), 42, 35, 40, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:oak_planks"), 162, 130, 78, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:spruce_planks"), 114, 84, 48, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:dark_oak_planks"), 66, 43, 20, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:birch_planks"), 192, 175, 121, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:sandstone"), 216, 203, 155, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:terracotta"), 152, 94, 67, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:bricks"), 150, 97, 83, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:white_concrete"), 207, 213, 214, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:gray_concrete"), 54, 57, 61, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:light_gray_concrete"), 125, 125, 115, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:cyan_terracotta"), 86, 91, 91, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:moss_block"), 89, 109, 45, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:oak_leaves"), 74, 114, 42, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:copper_block"), 192, 107, 79, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:oxidized_copper"), 82, 162, 132, false, false),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:sea_lantern"), 172, 199, 190, false, true),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:glowstone"), 171, 131, 84, false, true),
        new ColorBlockEntry(BlockIdentifier.of("minecraft:glass"), 175, 213, 224, true, false)
    );

    private BlockColorMatcher() {}

    public static BlockIdentifier matchBlock(int r, int g, int b, Vec3i pos, float ditherMagnitude, boolean translucent, boolean emissive) {
        if (translucent) {
            return BlockIdentifier.of("minecraft:glass");
        }
        int dr = r, dg = g, db = b;
        if (ditherMagnitude > 0f && pos != null) {
            int ix = Math.floorMod(pos.x(), 4);
            int iy = Math.floorMod(pos.y(), 4);
            int iz = Math.floorMod(pos.z(), 4);
            double thresh = (BAYER_4X4X4[ix + 4 * iy + 16 * iz] / 64.0) - 0.5;
            int offset = (int) Math.round(thresh * ditherMagnitude);
            dr = clamp255(r + offset);
            dg = clamp255(g + offset);
            db = clamp255(b + offset);
        }

        ColorBlockEntry best = PALETTE.get(0);
        double bestDist = Double.MAX_VALUE;
        for (ColorBlockEntry entry : PALETTE) {
            if (entry.translucent() != translucent) continue;
            if (emissive && !entry.emissive()) continue;
            // Perceptual redmean color distance
            double rMean = 0.5 * (dr + entry.r());
            double dR = dr - entry.r();
            double dG = dg - entry.g();
            double dB = db - entry.b();
            double dist = (2.0 + rMean / 256.0) * dR * dR + 4.0 * dG * dG + (2.0 + (255.0 - rMean) / 256.0) * dB * dB;
            if (dist < bestDist) {
                bestDist = dist;
                best = entry;
            }
        }
        return best.block();
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
