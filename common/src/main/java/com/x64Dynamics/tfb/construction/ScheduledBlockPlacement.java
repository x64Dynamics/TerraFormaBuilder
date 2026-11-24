package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

public record ScheduledBlockPlacement(
    Vec3i pos,
    BlockIdentifier block,
    TopologicalPhase phase,
    int layerY
) implements Comparable<ScheduledBlockPlacement> {

    @Override
    public int compareTo(ScheduledBlockPlacement o) {
        // First order by phase (Foundations before Walls before Roofs)
        if (phase != o.phase) {
            return Integer.compare(phase.ordinal(), o.phase.ordinal());
        }
        // Then by vertical layer Y (bottom-up construction)
        if (layerY != o.layerY) {
            return Integer.compare(layerY, o.layerY);
        }
        // Then by position coordinate
        return pos.compareTo(o.pos);
    }
}
