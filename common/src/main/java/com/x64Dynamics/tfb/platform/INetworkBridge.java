package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.platform.persistence.ParametricBlueprint;

import java.util.UUID;

public interface INetworkBridge {
    void sendBlueprintSyncToClient(UUID targetPlayerId, ParametricBlueprint blueprint);
    void sendBuildProgressToClient(UUID targetPlayerId, UUID sessionId, int total, int placed, boolean isPaused);
    void spawnFlyingBlockParticle(UUID targetPlayerId, Vec3d origin, Vec3i destination, BlockIdentifier block);
}
