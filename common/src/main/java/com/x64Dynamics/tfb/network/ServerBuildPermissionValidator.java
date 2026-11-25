package com.x64Dynamics.tfb.network;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.platform.IPlayerAccess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side permission, world-bounds, anti-spam, and survival resource validator
 * for incoming C2S construction requests.
 */
public final class ServerBuildPermissionValidator {
    public record ValidationResult(boolean allowed, String reason, Map<BlockIdentifier, Integer> requiredResources) {
        public static ValidationResult ok(Map<BlockIdentifier, Integer> resources) {
            return new ValidationResult(true, "OK", resources);
        }
        public static ValidationResult deny(String reason) {
            return new ValidationResult(false, reason, Map.of());
        }
    }

    private final int maxBlocksPerRequest;
    private final int minBuildY;
    private final int maxBuildY;

    public ServerBuildPermissionValidator(int maxBlocksPerRequest, int minBuildY, int maxBuildY) {
        this.maxBlocksPerRequest = Math.max(1, maxBlocksPerRequest);
        this.minBuildY = minBuildY;
        this.maxBuildY = maxBuildY;
    }

    public static ServerBuildPermissionValidator defaultOverworld() {
        return new ServerBuildPermissionValidator(65_536, -64, 319);
    }

    public ValidationResult validate(IPlayerAccess player, C2SConstructionPacket packet) {
        if (player == null || packet == null) {
            return ValidationResult.deny("Missing player or packet");
        }
        if (!player.getUuid().equals(packet.playerId())) {
            return ValidationResult.deny("Player UUID mismatch");
        }
        List<PlacementTarget> targets = packet.targets();
        if (targets.isEmpty()) {
            return ValidationResult.deny("Empty construction packet");
        }
        if (targets.size() > maxBlocksPerRequest) {
            return ValidationResult.deny("Exceeds max blocks per request (" + maxBlocksPerRequest + ")");
        }

        Map<BlockIdentifier, Integer> counts = new HashMap<>();
        for (PlacementTarget t : targets) {
            int y = t.pos().y();
            if (y < minBuildY || y > maxBuildY) {
                return ValidationResult.deny("Block Y=" + y + " outside build limits [" + minBuildY + ", " + maxBuildY + "]");
            }
            if (!t.block().isAir()) {
                counts.merge(BlockIdentifier.of(t.block().namespacedId()), 1, Integer::sum);
            }
        }

        if (!player.isCreative() && packet.validateResources()) {
            for (var entry : counts.entrySet()) {
                if (!player.hasResource(entry.getKey(), entry.getValue())) {
                    return ValidationResult.deny("Insufficient survival resource: " + entry.getKey().namespacedId() + " x" + entry.getValue());
                }
            }
        }

        return ValidationResult.ok(counts);
    }
}
