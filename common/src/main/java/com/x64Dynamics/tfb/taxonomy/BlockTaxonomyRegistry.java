package com.x64Dynamics.tfb.taxonomy;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockTaxonomyRegistry {
    private static final BlockTaxonomyRegistry INSTANCE = new BlockTaxonomyRegistry();

    private final Map<String, BlockTaxonomyEntry> cache = new ConcurrentHashMap<>();
    private final Map<MaterialSubstance, List<BlockTaxonomyEntry>> bySubstance = new EnumMap<>(MaterialSubstance.class);
    private final Map<ColorTone, List<BlockTaxonomyEntry>> byTone = new EnumMap<>(ColorTone.class);

    private BlockTaxonomyRegistry() {
        for (MaterialSubstance s : MaterialSubstance.values()) bySubstance.put(s, new ArrayList<>());
        for (ColorTone t : ColorTone.values()) byTone.put(t, new ArrayList<>());

        // Prepopulate baseline architectural blocks
        seedBaselineBlocks();
    }

    public static BlockTaxonomyRegistry getInstance() {
        return INSTANCE;
    }

    public static BlockTaxonomyEntry get(String id) {
        return getInstance().getOrRegister(BlockIdentifier.of(id));
    }

    public List<BlockTaxonomyEntry> all() {
        return new ArrayList<>(cache.values());
    }

    public synchronized BlockTaxonomyEntry getOrRegister(BlockIdentifier block) {
        String key = block.asString();
        BlockTaxonomyEntry cached = cache.get(key);
        if (cached != null) return cached;

        BlockTaxonomyEntry entry = BlockTaxonomyClassifier.analyze(block);
        return putEntry(key, entry);
    }

    public synchronized BlockTaxonomyEntry registerDiscovered(
        BlockIdentifier block,
        Set<BlockTrait> traits,
        Set<String> propertyNames,
        int luminance
    ) {
        String key = block.asString();
        BlockTaxonomyEntry entry = BlockTaxonomyClassifier.analyze(block, traits, propertyNames, luminance);
        return putEntry(key, entry);
    }

    public boolean contains(String namespacedId) {
        return cache.containsKey(namespacedId);
    }

    private BlockTaxonomyEntry putEntry(String key, BlockTaxonomyEntry entry) {
        BlockTaxonomyEntry existing = cache.put(key, entry);
        if (existing == null) {
            bySubstance.get(entry.substance()).add(entry);
            byTone.get(entry.primaryTone()).add(entry);
        }
        return entry;
    }

    public List<BlockTaxonomyEntry> query(MaterialSubstance substance, BlockTrait requiredTrait) {
        List<BlockTaxonomyEntry> results = new ArrayList<>();
        List<BlockTaxonomyEntry> pool = bySubstance.get(substance);
        if (pool != null) {
            for (BlockTaxonomyEntry e : pool) {
                if (requiredTrait == null || e.hasTrait(requiredTrait)) {
                    results.add(e);
                }
            }
        }
        return results;
    }

    public List<BlockTaxonomyEntry> queryByTone(ColorTone tone, BlockTrait requiredTrait) {
        List<BlockTaxonomyEntry> results = new ArrayList<>();
        List<BlockTaxonomyEntry> pool = byTone.get(tone);
        if (pool != null) {
            for (BlockTaxonomyEntry e : pool) {
                if (requiredTrait == null || e.hasTrait(requiredTrait)) {
                    results.add(e);
                }
            }
        }
        return results;
    }

    public Optional<BlockIdentifier> findMatchingVariant(BlockIdentifier base, BlockTrait desiredTrait) {
        BlockTaxonomyEntry baseEntry = getOrRegister(base);
        List<BlockTaxonomyEntry> candidates = query(baseEntry.substance(), desiredTrait);
        if (candidates.isEmpty()) {
            candidates = queryByTone(baseEntry.primaryTone(), desiredTrait);
        }
        return candidates.stream().findFirst().map(BlockTaxonomyEntry::block);
    }

    private void seedBaselineBlocks() {
        String[] seeds = {
            "minecraft:stone", "minecraft:stone_stairs", "minecraft:stone_slab",
            "minecraft:stone_bricks", "minecraft:stone_brick_stairs", "minecraft:stone_brick_slab", "minecraft:stone_brick_wall",
            "minecraft:cobblestone", "minecraft:cobblestone_stairs", "minecraft:cobblestone_slab", "minecraft:cobblestone_wall",
            "minecraft:deepslate_bricks", "minecraft:deepslate_brick_stairs", "minecraft:deepslate_brick_slab", "minecraft:deepslate_brick_wall",
            "minecraft:oak_planks", "minecraft:oak_stairs", "minecraft:oak_slab", "minecraft:oak_fence", "minecraft:oak_door", "minecraft:oak_log",
            "minecraft:spruce_planks", "minecraft:spruce_stairs", "minecraft:spruce_slab", "minecraft:spruce_fence", "minecraft:spruce_door", "minecraft:spruce_log",
            "minecraft:smooth_quartz", "minecraft:smooth_quartz_stairs", "minecraft:smooth_quartz_slab", "minecraft:quartz_pillar",
            "minecraft:glass_pane", "minecraft:tinted_glass", "minecraft:iron_bars", "minecraft:lantern", "minecraft:torch"
        };
        for (String id : seeds) {
            getOrRegister(BlockIdentifier.of(id));
        }
    }
}
