package com.x64Dynamics.tfb.material;

import java.util.*;

public final class MaterialFamilyResolver {
    private static final Map<String, String> IRREGULAR_STAIRS = new HashMap<>();
    private static final Map<String, String> IRREGULAR_SLABS = new HashMap<>();

    static {
        IRREGULAR_STAIRS.put("bricks", "brick_stairs");
        IRREGULAR_STAIRS.put("stone", "stone_stairs");
        IRREGULAR_STAIRS.put("stone_bricks", "stone_brick_stairs");
        IRREGULAR_STAIRS.put("deepslate_bricks", "deepslate_brick_stairs");
        IRREGULAR_STAIRS.put("deepslate_tiles", "deepslate_tile_stairs");
        IRREGULAR_STAIRS.put("polished_blackstone_bricks", "polished_blackstone_brick_stairs");
        IRREGULAR_STAIRS.put("nether_bricks", "nether_brick_stairs");
        IRREGULAR_STAIRS.put("red_nether_bricks", "red_nether_brick_stairs");
        IRREGULAR_STAIRS.put("prismarine_bricks", "prismarine_brick_stairs");
        IRREGULAR_STAIRS.put("mud_bricks", "mud_brick_stairs");
        IRREGULAR_STAIRS.put("end_stone_bricks", "end_stone_brick_stairs");

        IRREGULAR_SLABS.put("bricks", "brick_slab");
        IRREGULAR_SLABS.put("smooth_stone", "smooth_stone_slab");
        IRREGULAR_SLABS.put("stone_bricks", "stone_brick_slab");
        IRREGULAR_SLABS.put("deepslate_bricks", "deepslate_brick_slab");
        IRREGULAR_SLABS.put("deepslate_tiles", "deepslate_tile_slab");
        IRREGULAR_SLABS.put("polished_blackstone_bricks", "polished_blackstone_brick_slab");
        IRREGULAR_SLABS.put("nether_bricks", "nether_brick_slab");
        IRREGULAR_SLABS.put("red_nether_bricks", "red_nether_brick_slab");
        IRREGULAR_SLABS.put("prismarine_bricks", "prismarine_brick_slab");
        IRREGULAR_SLABS.put("mud_bricks", "mud_brick_slab");
        IRREGULAR_SLABS.put("end_stone_bricks", "end_stone_brick_slab");
    }

    private MaterialFamilyResolver() { }

    public static MaterialFamily resolve(BlockIdentifier base) {
        String ns = base.namespace();
        String path = base.path();

        String stairPath = resolveDerivative(ns, path, "_stairs", IRREGULAR_STAIRS, path);
        String slabPath = resolveDerivative(ns, path, "_slab", IRREGULAR_SLABS, path);
        String wallPath = resolveDerivative(ns, path, "_wall", Collections.emptyMap(), path);
        String fencePath = resolveDerivative(ns, path, "_fence", Collections.emptyMap(), path);
        String gatePath = resolveDerivative(ns, path, "_fence_gate", Collections.emptyMap(), path);
        String doorPath = resolveDerivative(ns, path, "_door", Collections.emptyMap(), "oak_door");
        String trapdoorPath = resolveDerivative(ns, path, "_trapdoor", Collections.emptyMap(), "oak_trapdoor");

        return new MaterialFamily(
            base,
            new BlockIdentifier(ns, stairPath, Collections.emptyMap()),
            new BlockIdentifier(ns, slabPath, Collections.emptyMap()),
            new BlockIdentifier(ns, wallPath, Collections.emptyMap()),
            new BlockIdentifier(ns, fencePath, Collections.emptyMap()),
            new BlockIdentifier(ns, gatePath, Collections.emptyMap()),
            new BlockIdentifier(ns, doorPath, Collections.emptyMap()),
            new BlockIdentifier(ns, trapdoorPath, Collections.emptyMap())
        );
    }

    private static String resolveDerivative(String ns, String path, String suffix, Map<String, String> irregulars, String fallbackPath) {
        if (irregulars.containsKey(path)) {
            return irregulars.get(path);
        }

        com.x64Dynamics.tfb.material.taxonomy.BlockTaxonomyRegistry reg = com.x64Dynamics.tfb.material.taxonomy.BlockTaxonomyRegistry.getInstance();

        // Clean wood prefixes like oak_planks -> oak
        if (path.endsWith("_planks")) {
            String wood = path.substring(0, path.length() - "_planks".length());
            String candidate = wood + suffix;
            if (!reg.contains(ns + ":" + candidate) && reg.contains("minecraft:" + candidate)) {
                return candidate;
            }
            return candidate;
        }

        // Handle block suffixes (e.g., copper_block -> copper_stairs, quartz_block -> quartz_stairs)
        if (path.endsWith("_block")) {
            String core = path.substring(0, path.length() - "_block".length());
            String candidate = core + suffix;
            if (reg.contains(ns + ":" + candidate)) {
                return candidate;
            }
        }

        // Handle plural bricks/tiles in modded or vanilla blocks (e.g., foo_bricks -> foo_brick_stairs)
        if (path.endsWith("bricks")) {
            String singular = path.substring(0, path.length() - 1) + suffix;
            if (reg.contains(ns + ":" + singular)) {
                return singular;
            }
        }
        if (path.endsWith("tiles")) {
            String singular = path.substring(0, path.length() - 1) + suffix;
            if (reg.contains(ns + ":" + singular)) {
                return singular;
            }
        }

        String direct = path + suffix;
        if (reg.all().size() > 40 && !reg.contains(ns + ":" + direct)) {
            return fallbackPath;
        }
        return direct;
    }
}
