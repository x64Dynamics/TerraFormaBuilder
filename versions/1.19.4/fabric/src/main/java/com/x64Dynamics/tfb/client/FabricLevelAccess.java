package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class FabricLevelAccess implements ILevelAccess {
    private Level level;

    public FabricLevelAccess(Level level) {
        this.level = level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public int getSurfaceHeight(int x, int z) {
        if (level == null) return 64;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    @Override
    public BlockIdentifier getBlockState(Vec3i pos) {
        if (level == null) return BlockIdentifier.of("minecraft:air");
        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
        return BlockStateParser.fromBlockState(level.getBlockState(bp));
    }

    @Override
    public boolean setBlockState(Vec3i pos, BlockIdentifier state) {
        if (level == null) return false;
        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
        BlockState bs = BlockStateParser.parse(state);
        return level.setBlock(bp, bs, 3);
    }

    @Override
    public boolean isAir(Vec3i pos) {
        if (level == null) return true;
        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
        return level.isEmptyBlock(bp);
    }
}
