package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.persistence.ParametricBlueprint;

import java.util.UUID;
import java.util.logging.Logger;

import com.x64Dynamics.tfb.TerraFormaBuilder;
public class DefaultNetworkBridge implements INetworkBridge {
    private static final Logger LOGGER = Logger.getLogger("TerraFormaBuilder.Network");

    @Override
    public void sendBlueprintSyncToClient(UUID targetPlayerId, ParametricBlueprint blueprint) {
        if (blueprint != null) {
            String name = blueprint.metadata() != null ? blueprint.metadata().name() : "unnamed";
            String brush = blueprint.brushId() != null ? blueprint.brushId() : "unknown";
            LOGGER.fine(String.format("Blueprint sync sent to %s: %s (brush: %s)", targetPlayerId, name, brush));
        }
    }

    @Override
    public void sendBuildProgressToClient(UUID targetPlayerId, UUID sessionId, int total, int placed, boolean isPaused) {
        LOGGER.fine(String.format("Build progress to %s: session %s - %d/%d (paused: %b)",
                targetPlayerId, sessionId, placed, total, isPaused));
    }

    @Override
    public void spawnFlyingBlockParticle(UUID targetPlayerId, Vec3d origin, Vec3i destination, BlockIdentifier block) {
        // Visual particle effect hook
    }
}
