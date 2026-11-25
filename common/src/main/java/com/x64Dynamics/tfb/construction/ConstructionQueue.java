package com.x64Dynamics.tfb.construction;

import java.util.*;

public final class ConstructionQueue {
    private final UUID sessionId;
    private final List<ScheduledBlockPlacement> pendingPlacements = new ArrayList<>();
    private final int totalBlockCount;
    private int placedCount = 0;
    private boolean paused = false;
    private int blocksPerTick;

    public ConstructionQueue(UUID sessionId, ProgressiveBuildPlan plan, int blocksPerTick) {
        this.sessionId = sessionId;
        this.pendingPlacements.addAll(plan.scheduledPlacements());
        this.totalBlockCount = plan.totalBlocks();
        this.blocksPerTick = Math.max(1, blocksPerTick);
    }

    public UUID sessionId() { return sessionId; }
    public int totalBlockCount() { return totalBlockCount; }
    public int placedCount() { return placedCount; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public int blocksPerTick() { return blocksPerTick; }
    public void setBlocksPerTick(int bpt) { this.blocksPerTick = Math.max(1, bpt); }

    public boolean isComplete() {
        return pendingPlacements.isEmpty();
    }

    public double progressPercentage() {
        if (totalBlockCount == 0) return 100.0;
        return (double) placedCount / (double) totalBlockCount * 100.0;
    }

    public List<ScheduledBlockPlacement> pollNextBatch() {
        if (paused || pendingPlacements.isEmpty()) {
            return Collections.emptyList();
        }

        int count = Math.min(blocksPerTick, pendingPlacements.size());
        List<ScheduledBlockPlacement> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(pendingPlacements.remove(0));
            placedCount++;
        }
        return batch;
    }
}
