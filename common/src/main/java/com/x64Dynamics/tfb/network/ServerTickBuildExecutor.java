package com.x64Dynamics.tfb.network;

import com.x64Dynamics.tfb.construction.DependencyAwareScheduler;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.editing.WorldConstructionHistory;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.platform.INetworkBridge;
import com.x64Dynamics.tfb.platform.IPlayerAccess;

import java.util.*;

/**
 * Progressive tick-budgeted server block placement engine with anti-cheat pacing,
 * survival resource consumption, and progress sync.
 */
public final class ServerTickBuildExecutor {
    public static final class ActiveJob {
        private final UUID sessionId;
        private final IPlayerAccess player;
        private final List<ScheduledBlockPlacement> orderedTargets;
        private int cursor = 0;

        ActiveJob(UUID sessionId, IPlayerAccess player, List<ScheduledBlockPlacement> orderedTargets) {
            this.sessionId = sessionId;
            this.player = player;
            this.orderedTargets = orderedTargets;
        }

        public UUID sessionId() { return sessionId; }
        public int total() { return orderedTargets.size(); }
        public int placed() { return cursor; }
        public boolean isComplete() { return cursor >= orderedTargets.size(); }
    }

    private final ServerBuildPermissionValidator validator;
    private final int blocksPerTickBudget;
    private final Map<UUID, ActiveJob> jobs = new LinkedHashMap<>();

    public ServerTickBuildExecutor(ServerBuildPermissionValidator validator, int blocksPerTickBudget) {
        this.validator = validator;
        this.blocksPerTickBudget = Math.max(1, blocksPerTickBudget);
    }

    public S2CConstructionAckPacket submit(IPlayerAccess player, C2SConstructionPacket packet) {
        var validation = validator.validate(player, packet);
        if (!validation.allowed()) {
            return new S2CConstructionAckPacket(packet.sessionId(), false, 0, packet.targets().size(), validation.reason());
        }
        List<ScheduledBlockPlacement> ordered = DependencyAwareScheduler.scheduleWithDependencies(packet.targets());
        jobs.put(packet.sessionId(), new ActiveJob(packet.sessionId(), player, ordered));
        return new S2CConstructionAckPacket(packet.sessionId(), true, 0, ordered.size(), "Queued " + ordered.size() + " blocks");
    }

    public int tick(ILevelAccess level, INetworkBridge network, WorldConstructionHistory history) {
        int totalPlacedThisTick = 0;
        Iterator<Map.Entry<UUID, ActiveJob>> it = jobs.entrySet().iterator();
        while (it.hasNext() && totalPlacedThisTick < blocksPerTickBudget) {
            ActiveJob job = it.next().getValue();
            int remainingBudget = blocksPerTickBudget - totalPlacedThisTick;
            List<WorldConstructionHistory.BlockChange> changes = new ArrayList<>();

            while (!job.isComplete() && remainingBudget > 0) {
                ScheduledBlockPlacement target = job.orderedTargets.get(job.cursor);
                if (!job.player.isCreative() && !target.block().isAir()) {
                    BlockIdentifier baseBlock = BlockIdentifier.of(target.block().namespacedId());
                    if (!job.player.consumeResource(baseBlock, 1)) {
                        break;
                    }
                }
                BlockIdentifier previous = level.getBlockState(target.pos());
                if (level.setBlockState(target.pos(), target.block())) {
                    changes.add(new WorldConstructionHistory.BlockChange(target.pos(), previous, target.block()));
                }
                job.cursor++;
                remainingBudget--;
                totalPlacedThisTick++;
            }

            if (!changes.isEmpty() && history != null) {
                history.recordTransaction(changes);
            }
            if (network != null) {
                network.sendBuildProgressToClient(job.player.getUuid(), job.sessionId(), job.total(), job.placed(), false);
            }
            if (job.isComplete()) {
                it.remove();
            }
        }
        return totalPlacedThisTick;
    }

    public int activeJobCount() { return jobs.size(); }
}
