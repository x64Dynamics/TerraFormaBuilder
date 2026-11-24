package com.x64Dynamics.tfb.material;

import java.util.*;

public final class MaterialFamilyResolver {
    private static final Map<String, String> IRREGULAR_STAIRS = new HashMap<>();
    private static final Map<String, String> IRREGULAR_SLABS = new HashMap<>();

    static {
        IRREGULAR_STAIRS.put("bricks", "brick_stairs");
        IRREGULAR_STAIRS.put("stone", "stone_stairs");
        IRREGULAR_STAIRS.put("smooth_stone", "smooth_stone_slab");
        IRREGULAR_SLABS.put("bricks", "brick_slab");
        IRREGULAR_SLABS.put("smooth_stone", "smooth_stone_slab");
    }

    private MaterialFamilyResolver() { }

    public static MaterialFamily resolve(BlockIdentifier base) {
        String ns = base.namespace();
        String path = base.path();

        String stairPath = resolveDerivative(path, "_stairs", IRREGULAR_STAIRS);
        String slabPath = resolveDerivative(path, "_slab", IRREGULAR_SLABS);
        String wallPath = resolveDerivative(path, "_wall", Collections.emptyMap());
        String fencePath = resolveDerivative(path, "_fence", Collections.emptyMap());
        String gatePath = resolveDerivative(path, "_fence_gate", Collections.emptyMap());
        String doorPath = resolveDerivative(path, "_door", Collections.emptyMap());
        String trapdoorPath = resolveDerivative(path, "_trapdoor", Collections.emptyMap());

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

    private static String resolveDerivative(String path, String suffix, Map<String, String> irregulars) {
        if (irregulars.containsKey(path)) {
            return irregulars.get(path);
        }

        // Clean wood prefixes like oak_planks -> oak
        if (path.endsWith("_planks")) {
            String wood = path.substring(0, path.length() - "_planks".length());
            return wood + suffix;
        }

        // Handle block suffixes
        if (path.endsWith("_block")) {
            String core = path.substring(0, path.length() - "_block".length());
            return core + suffix;
        }

        return path + suffix;
    }
}
