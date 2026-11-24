package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

public final class BlockStateParser {
    private BlockStateParser() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState parse(BlockIdentifier id) {
        if (id == null) return Blocks.AIR.defaultBlockState();

        ResourceLocation loc = new ResourceLocation(id.namespace(), id.path());
        Block block = Registry.BLOCK.get(loc);
        if (block == null || block == Blocks.AIR && !id.path().equals("air")) {
            return Blocks.STONE.defaultBlockState();
        }

        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> entry : id.properties().entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue();

            Property<?> property = block.getStateDefinition().getProperty(propName);
            if (property != null) {
                Optional<?> val = property.getValue(propValue);
                if (val.isPresent()) {
                    state = state.setValue((Property) property, (Comparable) val.get());
                }
            }
        }
        return state;
    }

    public static BlockIdentifier fromBlockState(BlockState state) {
        if (state == null) return BlockIdentifier.of("minecraft:air");
        ResourceLocation loc = Registry.BLOCK.getKey(state.getBlock());
        BlockIdentifier id = BlockIdentifier.of(loc.toString());
        for (Property<?> prop : state.getProperties()) {
            id = id.withProperty(prop.getName(), state.getValue(prop).toString().toLowerCase());
        }
        return id;
    }
}
