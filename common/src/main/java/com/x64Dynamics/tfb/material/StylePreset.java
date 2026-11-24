package com.x64Dynamics.tfb.material;

public enum StylePreset {
    MEDIEVAL("medieval", "Medieval"),
    MODERN("modern", "Modern"),
    RUSTIC("rustic", "Rustic"),
    FANTASY("fantasy", "Fantasy"),
    INDUSTRIAL("industrial", "Industrial"),
    TRADITIONAL("traditional", "Traditional"),
    MINIMALIST("minimalist", "Minimalist");

    private final String id;
    private final String displayName;

    StylePreset(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public MaterialPalette createPalette() {
        MaterialPalette palette = new MaterialPalette(id);
        return switch (this) {
            case MEDIEVAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:stone_bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:spruce_planks")
                .set(MaterialRole.WALL_CORNER, "minecraft:cobblestone")
                .set(MaterialRole.FLOOR, "minecraft:spruce_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_spruce_wood")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:deepslate_tile_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:deepslate_tile_slab")
                .set(MaterialRole.PILLAR, "minecraft:stripped_spruce_log")
                .set(MaterialRole.BEAM, "minecraft:spruce_log")
                .set(MaterialRole.FOUNDATION, "minecraft:cobblestone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:spruce_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:spruce_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case MODERN -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:white_concrete")
                .set(MaterialRole.WALL_ACCENT, "minecraft:gray_concrete")
                .set(MaterialRole.WALL_CORNER, "minecraft:smooth_quartz")
                .set(MaterialRole.FLOOR, "minecraft:birch_planks")
                .set(MaterialRole.CEILING, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.PILLAR, "minecraft:quartz_pillar")
                .set(MaterialRole.BEAM, "minecraft:smooth_quartz")
                .set(MaterialRole.FOUNDATION, "minecraft:polished_andesite")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:iron_door")
                .set(MaterialRole.WINDOW, "minecraft:tinted_glass")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:sea_lantern");

            case RUSTIC -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:oak_planks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:cobblestone")
                .set(MaterialRole.WALL_CORNER, "minecraft:oak_log")
                .set(MaterialRole.FLOOR, "minecraft:oak_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_oak_wood")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:cobblestone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:cobblestone_slab")
                .set(MaterialRole.PILLAR, "minecraft:oak_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_oak_log")
                .set(MaterialRole.FOUNDATION, "minecraft:mossy_cobblestone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:dirt")
                .set(MaterialRole.DOOR, "minecraft:oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:oak_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:torch");

            case FANTASY -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:prismarine_bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:warped_planks")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_prismarine")
                .set(MaterialRole.FLOOR, "minecraft:warped_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_warped_stem")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:waxed_oxidized_cut_copper_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:waxed_oxidized_cut_copper_slab")
                .set(MaterialRole.PILLAR, "minecraft:warped_stem")
                .set(MaterialRole.BEAM, "minecraft:dark_prismarine")
                .set(MaterialRole.FOUNDATION, "minecraft:amethyst_block")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:warped_door")
                .set(MaterialRole.WINDOW, "minecraft:tinted_glass")
                .set(MaterialRole.FENCE, "minecraft:warped_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:sea_lantern");

            case INDUSTRIAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:stone_bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:iron_block")
                .set(MaterialRole.WALL_CORNER, "minecraft:tuff")
                .set(MaterialRole.FLOOR, "minecraft:polished_andesite")
                .set(MaterialRole.CEILING, "minecraft:stone_brick_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:tuff_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:tuff_slab")
                .set(MaterialRole.PILLAR, "minecraft:polished_basalt")
                .set(MaterialRole.BEAM, "minecraft:iron_bars")
                .set(MaterialRole.FOUNDATION, "minecraft:smooth_stone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:bedrock")
                .set(MaterialRole.DOOR, "minecraft:iron_door")
                .set(MaterialRole.WINDOW, "minecraft:iron_bars")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case TRADITIONAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:polished_granite")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_oak_log")
                .set(MaterialRole.FLOOR, "minecraft:dark_oak_planks")
                .set(MaterialRole.CEILING, "minecraft:dark_oak_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:dark_oak_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:dark_oak_slab")
                .set(MaterialRole.PILLAR, "minecraft:dark_oak_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_dark_oak_log")
                .set(MaterialRole.FOUNDATION, "minecraft:stone_bricks")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:dark_oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:dark_oak_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case MINIMALIST -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:smooth_quartz")
                .set(MaterialRole.WALL_ACCENT, "minecraft:polished_blackstone")
                .set(MaterialRole.WALL_CORNER, "minecraft:blackstone")
                .set(MaterialRole.FLOOR, "minecraft:gray_concrete")
                .set(MaterialRole.CEILING, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:polished_blackstone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:polished_blackstone_slab")
                .set(MaterialRole.PILLAR, "minecraft:polished_blackstone_brick_wall")
                .set(MaterialRole.BEAM, "minecraft:smooth_quartz")
                .set(MaterialRole.FOUNDATION, "minecraft:blackstone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:iron_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:end_rod");
        };
    }
}
