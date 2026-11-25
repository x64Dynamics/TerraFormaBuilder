package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.BlockStateNormalizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

        Identifier loc = Identifier.fromNamespaceAndPath(id.namespace(), id.path());
        Block block = BuiltInRegistries.BLOCK.getValue(loc);
        if (block == null || (block == Blocks.AIR && !id.path().equals("air"))) {
            return Blocks.STONE.defaultBlockState();
        }

        BlockState state = block.defaultBlockState();
        BlockIdentifier norm = BlockStateNormalizer.normalize(id, com.x64Dynamics.tfb.geometry.Direction.SOUTH);
        for (Map.Entry<String, String> entry : norm.properties().entrySet()) {
            Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
            if (property != null) {
                Optional<?> val = property.getValue(entry.getValue());
                if (val.isPresent()) {
                    state = state.setValue((Property) property, (Comparable) val.get());
                }
            }
        }
        return state;
    }

    public static BlockIdentifier fromBlockState(BlockState state) {
        if (state == null) return BlockIdentifier.of("minecraft:air");
        Identifier loc = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockIdentifier id = BlockIdentifier.of(loc.toString());
        for (Property<?> prop : state.getProperties()) {
            id = id.withProperty(prop.getName(), state.getValue(prop).toString().toLowerCase());
        }
        return id;
    }
}
