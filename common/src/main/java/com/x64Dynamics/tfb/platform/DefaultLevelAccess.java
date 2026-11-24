package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultLevelAccess implements ILevelAccess {
    private final ConcurrentMap<Vec3i, BlockIdentifier> blocks = new ConcurrentHashMap<>();
    private final BlockIdentifier airBlock = BlockIdentifier.AIR;

    @Override
    public int getSurfaceHeight(int x, int z) {
        int highest = 64;
        for (Vec3i pos : blocks.keySet()) {
            if (pos.x() == x && pos.z() == z && pos.y() > highest) {
                if (!isAir(pos)) {
                    highest = pos.y();
                }
            }
        }
        return highest;
    }

    @Override
    public BlockIdentifier getBlockState(Vec3i pos) {
        return blocks.getOrDefault(pos, airBlock);
    }

    @Override
    public boolean setBlockState(Vec3i pos, BlockIdentifier state) {
        if (state == null || state.equals(airBlock)) {
            blocks.remove(pos);
        } else {
            blocks.put(pos, state);
        }
        return true;
    }

    @Override
    public boolean isAir(Vec3i pos) {
        BlockIdentifier state = getBlockState(pos);
        return state == null || state.equals(airBlock) || state.namespacedId().equals("minecraft:air");
    }
}
