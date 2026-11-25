package com.x64Dynamics.tfb.material;

public enum StylePreset {
    MEDIEVAL("medieval", "Medieval"),
    MODERN("modern", "Modern"),
    RUSTIC("rustic", "Rustic"),
    FANTASY("fantasy", "Fantasy"),
    INDUSTRIAL("industrial", "Industrial"),
    TRADITIONAL("traditional", "Traditional"),
    MINIMALIST("minimalist", "Minimalist"),
    NORDIC("nordic", "Nordic"),
    JAPANESE("japanese", "Japanese"),
    VICTORIAN("victorian", "Victorian"),
    GOTHIC("gothic", "Gothic"),
    CLASSICAL("classical", "Classical");

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
                .set(MaterialRole.ROOF_GABLE, "minecraft:spruce_planks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:spruce_stairs")
                .set(MaterialRole.PILLAR, "minecraft:spruce_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_spruce_log")
                .set(MaterialRole.ARCH, "minecraft:stone_brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:spruce_stairs")
                .set(MaterialRole.SLAB, "minecraft:spruce_slab")
                .set(MaterialRole.FRAME, "minecraft:spruce_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:cobblestone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:cobbled_deepslate")
                .set(MaterialRole.DOOR, "minecraft:spruce_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:spruce_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:spruce_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case MODERN -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:white_concrete")
                .set(MaterialRole.WALL_ACCENT, "minecraft:gray_concrete")
                .set(MaterialRole.WALL_CORNER, "minecraft:smooth_quartz")
                .set(MaterialRole.FLOOR, "minecraft:birch_planks")
                .set(MaterialRole.CEILING, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:white_concrete")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.PILLAR, "minecraft:quartz_pillar")
                .set(MaterialRole.BEAM, "minecraft:smooth_quartz")
                .set(MaterialRole.ARCH, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.STAIR, "minecraft:birch_stairs")
                .set(MaterialRole.SLAB, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.FRAME, "minecraft:dark_oak_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:polished_andesite")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:birch_door")
                .set(MaterialRole.WINDOW, "minecraft:light_gray_stained_glass_pane")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.BALUSTRADE, "minecraft:glass_pane")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:sea_lantern");

            case RUSTIC -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:oak_planks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:cobblestone")
                .set(MaterialRole.WALL_CORNER, "minecraft:oak_log")
                .set(MaterialRole.FLOOR, "minecraft:oak_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_oak_wood")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:cobblestone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:cobblestone_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:oak_planks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:oak_stairs")
                .set(MaterialRole.PILLAR, "minecraft:oak_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_oak_log")
                .set(MaterialRole.ARCH, "minecraft:oak_stairs")
                .set(MaterialRole.STAIR, "minecraft:oak_stairs")
                .set(MaterialRole.SLAB, "minecraft:oak_slab")
                .set(MaterialRole.FRAME, "minecraft:oak_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:mossy_cobblestone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:cobblestone")
                .set(MaterialRole.DOOR, "minecraft:oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:oak_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:oak_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case FANTASY -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:prismarine_bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:warped_planks")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_prismarine")
                .set(MaterialRole.FLOOR, "minecraft:warped_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_warped_stem")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:waxed_oxidized_cut_copper_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:waxed_oxidized_cut_copper_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:warped_planks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:warped_stairs")
                .set(MaterialRole.PILLAR, "minecraft:warped_stem")
                .set(MaterialRole.BEAM, "minecraft:dark_prismarine")
                .set(MaterialRole.ARCH, "minecraft:prismarine_brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:warped_stairs")
                .set(MaterialRole.SLAB, "minecraft:warped_slab")
                .set(MaterialRole.FRAME, "minecraft:warped_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:dark_prismarine")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:warped_door")
                .set(MaterialRole.WINDOW, "minecraft:cyan_stained_glass_pane")
                .set(MaterialRole.FENCE, "minecraft:warped_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:warped_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:sea_lantern");

            case INDUSTRIAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:stone_bricks")
                .set(MaterialRole.WALL_CORNER, "minecraft:deepslate_bricks")
                .set(MaterialRole.FLOOR, "minecraft:polished_andesite")
                .set(MaterialRole.CEILING, "minecraft:stone_brick_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:deepslate_brick_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:deepslate_brick_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:bricks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:stone_brick_stairs")
                .set(MaterialRole.PILLAR, "minecraft:polished_basalt")
                .set(MaterialRole.BEAM, "minecraft:deepslate_bricks")
                .set(MaterialRole.ARCH, "minecraft:brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:stone_brick_stairs")
                .set(MaterialRole.SLAB, "minecraft:stone_brick_slab")
                .set(MaterialRole.FRAME, "minecraft:iron_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:smooth_stone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:cobbled_deepslate")
                .set(MaterialRole.DOOR, "minecraft:spruce_door")
                .set(MaterialRole.WINDOW, "minecraft:iron_bars")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.BALUSTRADE, "minecraft:iron_bars")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case TRADITIONAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:white_terracotta")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_oak_log")
                .set(MaterialRole.FLOOR, "minecraft:dark_oak_planks")
                .set(MaterialRole.CEILING, "minecraft:dark_oak_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:dark_oak_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:dark_oak_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:white_terracotta")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:dark_oak_stairs")
                .set(MaterialRole.PILLAR, "minecraft:dark_oak_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_dark_oak_log")
                .set(MaterialRole.ARCH, "minecraft:brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:dark_oak_stairs")
                .set(MaterialRole.SLAB, "minecraft:dark_oak_slab")
                .set(MaterialRole.FRAME, "minecraft:dark_oak_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:stone_bricks")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:dark_oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:dark_oak_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:dark_oak_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case MINIMALIST -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:smooth_quartz")
                .set(MaterialRole.WALL_ACCENT, "minecraft:polished_blackstone")
                .set(MaterialRole.WALL_CORNER, "minecraft:polished_basalt")
                .set(MaterialRole.FLOOR, "minecraft:oak_planks")
                .set(MaterialRole.CEILING, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:polished_blackstone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:polished_blackstone_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:smooth_quartz")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.PILLAR, "minecraft:polished_basalt")
                .set(MaterialRole.BEAM, "minecraft:smooth_quartz")
                .set(MaterialRole.ARCH, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.STAIR, "minecraft:oak_stairs")
                .set(MaterialRole.SLAB, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.FRAME, "minecraft:birch_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:polished_blackstone")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.BALUSTRADE, "minecraft:glass_pane")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case NORDIC -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:spruce_planks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:cobblestone")
                .set(MaterialRole.WALL_CORNER, "minecraft:spruce_log")
                .set(MaterialRole.FLOOR, "minecraft:spruce_planks")
                .set(MaterialRole.CEILING, "minecraft:stripped_spruce_wood")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:dark_oak_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:dark_oak_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:spruce_planks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:spruce_stairs")
                .set(MaterialRole.PILLAR, "minecraft:spruce_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_spruce_log")
                .set(MaterialRole.ARCH, "minecraft:spruce_stairs")
                .set(MaterialRole.STAIR, "minecraft:spruce_stairs")
                .set(MaterialRole.SLAB, "minecraft:spruce_slab")
                .set(MaterialRole.FRAME, "minecraft:spruce_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:cobbled_deepslate")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:spruce_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:spruce_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:spruce_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case JAPANESE -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:calcite")
                .set(MaterialRole.WALL_ACCENT, "minecraft:cherry_planks")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_oak_log")
                .set(MaterialRole.FLOOR, "minecraft:bamboo_planks")
                .set(MaterialRole.CEILING, "minecraft:spruce_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:deepslate_brick_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:deepslate_brick_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:calcite")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:dark_oak_stairs")
                .set(MaterialRole.PILLAR, "minecraft:dark_oak_log")
                .set(MaterialRole.BEAM, "minecraft:stripped_dark_oak_log")
                .set(MaterialRole.ARCH, "minecraft:dark_oak_stairs")
                .set(MaterialRole.STAIR, "minecraft:bamboo_stairs")
                .set(MaterialRole.SLAB, "minecraft:bamboo_slab")
                .set(MaterialRole.FRAME, "minecraft:birch_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:stone_bricks")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:cobblestone")
                .set(MaterialRole.DOOR, "minecraft:cherry_door")
                .set(MaterialRole.WINDOW, "minecraft:white_stained_glass_pane")
                .set(MaterialRole.FENCE, "minecraft:bamboo_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:bamboo_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case VICTORIAN -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:terracotta")
                .set(MaterialRole.WALL_ACCENT, "minecraft:oak_planks")
                .set(MaterialRole.WALL_CORNER, "minecraft:dark_oak_log")
                .set(MaterialRole.FLOOR, "minecraft:dark_oak_planks")
                .set(MaterialRole.CEILING, "minecraft:birch_planks")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:polished_blackstone_brick_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:polished_blackstone_brick_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:oak_planks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:dark_oak_stairs")
                .set(MaterialRole.PILLAR, "minecraft:stripped_dark_oak_log")
                .set(MaterialRole.BEAM, "minecraft:dark_oak_log")
                .set(MaterialRole.ARCH, "minecraft:brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:dark_oak_stairs")
                .set(MaterialRole.SLAB, "minecraft:dark_oak_slab")
                .set(MaterialRole.FRAME, "minecraft:dark_oak_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:stone_bricks")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:dark_oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:dark_oak_fence")
                .set(MaterialRole.BALUSTRADE, "minecraft:dark_oak_fence")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");

            case GOTHIC -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:deepslate_bricks")
                .set(MaterialRole.WALL_ACCENT, "minecraft:stone_bricks")
                .set(MaterialRole.WALL_CORNER, "minecraft:polished_deepslate")
                .set(MaterialRole.FLOOR, "minecraft:dark_oak_planks")
                .set(MaterialRole.CEILING, "minecraft:deepslate_tile_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:polished_blackstone_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:polished_blackstone_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:deepslate_bricks")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:deepslate_brick_stairs")
                .set(MaterialRole.PILLAR, "minecraft:polished_basalt")
                .set(MaterialRole.BEAM, "minecraft:polished_deepslate")
                .set(MaterialRole.ARCH, "minecraft:deepslate_brick_stairs")
                .set(MaterialRole.STAIR, "minecraft:dark_oak_stairs")
                .set(MaterialRole.SLAB, "minecraft:deepslate_brick_slab")
                .set(MaterialRole.FRAME, "minecraft:spruce_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:cobbled_deepslate")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:deepslate")
                .set(MaterialRole.DOOR, "minecraft:spruce_door")
                .set(MaterialRole.WINDOW, "minecraft:purple_stained_glass_pane")
                .set(MaterialRole.FENCE, "minecraft:iron_bars")
                .set(MaterialRole.BALUSTRADE, "minecraft:stone_brick_wall")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:soul_lantern");

            case CLASSICAL -> palette
                .set(MaterialRole.WALL_PRIMARY, "minecraft:smooth_sandstone")
                .set(MaterialRole.WALL_ACCENT, "minecraft:calcite")
                .set(MaterialRole.WALL_CORNER, "minecraft:quartz_pillar")
                .set(MaterialRole.FLOOR, "minecraft:birch_planks")
                .set(MaterialRole.CEILING, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.ROOF_SLOPE, "minecraft:brick_stairs")
                .set(MaterialRole.ROOF_RIDGE, "minecraft:brick_slab")
                .set(MaterialRole.ROOF_GABLE, "minecraft:calcite")
                .set(MaterialRole.ROOF_OVERHANG, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.PILLAR, "minecraft:quartz_pillar")
                .set(MaterialRole.BEAM, "minecraft:smooth_quartz")
                .set(MaterialRole.ARCH, "minecraft:smooth_quartz_stairs")
                .set(MaterialRole.STAIR, "minecraft:birch_stairs")
                .set(MaterialRole.SLAB, "minecraft:smooth_quartz_slab")
                .set(MaterialRole.FRAME, "minecraft:oak_trapdoor")
                .set(MaterialRole.FOUNDATION, "minecraft:stone_bricks")
                .set(MaterialRole.SUB_FOUNDATION, "minecraft:stone")
                .set(MaterialRole.DOOR, "minecraft:oak_door")
                .set(MaterialRole.WINDOW, "minecraft:glass_pane")
                .set(MaterialRole.FENCE, "minecraft:quartz_slab")
                .set(MaterialRole.BALUSTRADE, "minecraft:stone_brick_wall")
                .set(MaterialRole.LIGHT_SOURCE, "minecraft:lantern");
        };
    }
}
