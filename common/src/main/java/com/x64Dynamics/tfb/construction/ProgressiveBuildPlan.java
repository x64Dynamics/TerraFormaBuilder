package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;

import java.util.*;

public final class ProgressiveBuildPlan {
    private final List<ScheduledBlockPlacement> scheduledPlacements;
    private final Map<String, Integer> materialRequirements;

    public ProgressiveBuildPlan(List<PlacementTarget> targets) {
        List<ScheduledBlockPlacement> placements = new ArrayList<>(targets.size());
        Map<String, Integer> requirements = new LinkedHashMap<>();

        for (PlacementTarget target : targets) {
            TopologicalPhase phase = TopologicalPhase.fromRole(target.role());
            int layerY = target.pos().y();
            placements.add(new ScheduledBlockPlacement(target.pos(), target.block(), phase, layerY));

            if (!target.block().path().equals("air")) {
                String blockKey = target.block().namespace() + ":" + target.block().path();
                requirements.put(blockKey, requirements.getOrDefault(blockKey, 0) + 1);
            }
        }

        // Sort topologically: foundations -> structure -> floors -> walls -> roofs -> details
        Collections.sort(placements);

        this.scheduledPlacements = Collections.unmodifiableList(placements);
        this.materialRequirements = Collections.unmodifiableMap(requirements);
    }

    public List<ScheduledBlockPlacement> scheduledPlacements() {
        return scheduledPlacements;
    }

    public Map<String, Integer> materialRequirements() {
        return materialRequirements;
    }

    public int totalBlocks() {
        return scheduledPlacements.size();
    }
}
