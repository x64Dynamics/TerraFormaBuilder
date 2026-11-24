package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

public interface ILevelAccess {
    int getSurfaceHeight(int x, int z);
    BlockIdentifier getBlockState(Vec3i pos);
    boolean setBlockState(Vec3i pos, BlockIdentifier state);
    boolean isAir(Vec3i pos);
}
