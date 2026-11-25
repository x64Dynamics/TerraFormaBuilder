package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyRegistry;
import com.x64Dynamics.tfb.taxonomy.BlockTrait;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Scans Minecraft's BuiltInRegistries.BLOCK at runtime to classify all vanilla and modded blocks
 * by actual Block subclass, StateDefinition properties, and light emission.
 */
public final class BlockRegistryScanner {
    private static boolean scanned = false;

    private BlockRegistryScanner() { }

    public static synchronized void scanIfNeeded() {
        if (scanned) return;
        scanned = true;

        BlockTaxonomyRegistry registry = BlockTaxonomyRegistry.getInstance();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                continue;
            }

            EnumSet<BlockTrait> traits = EnumSet.noneOf(BlockTrait.class);
            if (block instanceof StairBlock) traits.add(BlockTrait.STAIR);
            else if (block instanceof SlabBlock) traits.add(BlockTrait.SLAB);
            else if (block instanceof WallBlock) traits.add(BlockTrait.WALL);
            else if (block instanceof FenceGateBlock) traits.add(BlockTrait.FENCE_GATE);
            else if (block instanceof FenceBlock) traits.add(BlockTrait.FENCE);
            else if (block instanceof DoorBlock) traits.add(BlockTrait.DOOR);
            else if (block instanceof TrapDoorBlock) traits.add(BlockTrait.TRAPDOOR);
            else if (block instanceof RotatedPillarBlock) traits.add(BlockTrait.PILLAR);
            else if (block instanceof IronBarsBlock || block instanceof StainedGlassPaneBlock) traits.add(BlockTrait.GLASS_PANE);
            else if (block instanceof LanternBlock) traits.add(BlockTrait.LANTERN);
            else if (block instanceof TorchBlock) traits.add(BlockTrait.TORCH);
            else if (block instanceof CarpetBlock) traits.add(BlockTrait.CARPET);
            else if (block instanceof LiquidBlock || block instanceof BaseFireBlock) continue;
            else traits.add(BlockTrait.SOLID_CUBE);

            Set<String> propNames = new HashSet<>();
            for (Property<?> prop : block.getStateDefinition().getProperties()) {
                propNames.add(prop.getName());
            }

            BlockState defaultState = block.defaultBlockState();
            int luminance = defaultState.getLightEmission();
            if (luminance > 0) {
                traits.add(BlockTrait.LIGHT_EMITTING);
            }

            registry.registerDiscovered(BlockIdentifier.of(key.toString()), traits, propNames, luminance);
        }
    }
}
