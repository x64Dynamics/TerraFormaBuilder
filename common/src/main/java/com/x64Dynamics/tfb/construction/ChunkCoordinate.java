package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.geometry.Vec3i;

public record ChunkCoordinate(int chunkX, int chunkZ) {
    public static ChunkCoordinate fromBlock(Vec3i pos) {
        return new ChunkCoordinate(pos.x() >> 4, pos.z() >> 4);
    }
}
