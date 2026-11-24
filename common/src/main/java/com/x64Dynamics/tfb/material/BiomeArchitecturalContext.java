package com.x64Dynamics.tfb.material;

public enum BiomeArchitecturalContext {
    TEMPERATE_PLAINS("plains", "minecraft:oak_planks", "minecraft:stone_bricks", StylePreset.RUSTIC),
    TAIGA_COLD("taiga", "minecraft:spruce_planks", "minecraft:cobblestone", StylePreset.MEDIEVAL),
    DESERT_ARID("desert", "minecraft:sandstone", "minecraft:smooth_sandstone", StylePreset.TRADITIONAL),
    SAVANNA_WARM("savanna", "minecraft:acacia_planks", "minecraft:stone", StylePreset.RUSTIC),
    JUNGLE_TROPICAL("jungle", "minecraft:jungle_planks", "minecraft:mossy_stone_bricks", StylePreset.FANTASY),
    SWAMP_HUMID("swamp", "minecraft:mud_bricks", "minecraft:dark_oak_planks", StylePreset.RUSTIC),
    BADLANDS_CANYON("badlands", "minecraft:terracotta", "minecraft:red_sandstone", StylePreset.TRADITIONAL),
    SNOWY_TUNDRA("snowy", "minecraft:spruce_planks", "minecraft:deepslate_bricks", StylePreset.MEDIEVAL),
    NETHER_HELL("nether", "minecraft:blackstone", "minecraft:crimson_planks", StylePreset.INDUSTRIAL),
    END_VOID("end", "minecraft:purpur_block", "minecraft:end_stone_bricks", StylePreset.MINIMALIST);

    private final String id;
    private final String primaryBlockId;
    private final String secondaryBlockId;
    private final StylePreset defaultStyle;

    BiomeArchitecturalContext(String id, String primaryBlockId, String secondaryBlockId, StylePreset defaultStyle) {
        this.id = id;
        this.primaryBlockId = primaryBlockId;
        this.secondaryBlockId = secondaryBlockId;
        this.defaultStyle = defaultStyle;
    }

    public String id() { return id; }
    public String primaryBlockId() { return primaryBlockId; }
    public String secondaryBlockId() { return secondaryBlockId; }
    public StylePreset defaultStyle() { return defaultStyle; }

    public static BiomeArchitecturalContext fromBiomeName(String biomeName) {
        if (biomeName == null) return TEMPERATE_PLAINS;
        String lower = biomeName.toLowerCase();
        if (lower.contains("desert")) return DESERT_ARID;
        if (lower.contains("taiga") || lower.contains("snow")) return TAIGA_COLD;
        if (lower.contains("savanna")) return SAVANNA_WARM;
        if (lower.contains("jungle")) return JUNGLE_TROPICAL;
        if (lower.contains("swamp")) return SWAMP_HUMID;
        if (lower.contains("badlands") || lower.contains("mesa")) return BADLANDS_CANYON;
        if (lower.contains("nether")) return NETHER_HELL;
        if (lower.contains("end")) return END_VOID;
        return TEMPERATE_PLAINS;
    }
}
