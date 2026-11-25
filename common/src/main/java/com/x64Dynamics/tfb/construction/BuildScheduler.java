package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;

public final class BuildScheduler {
    private BuildScheduler() { }

    public static List<ScheduledBlockPlacement> scheduleWithDependencies(List<PlacementTarget> targets) {
        DependencyGraph graph = new DependencyGraph();
        Map<Vec3i, BlockIdentifier> blockMap = new HashMap<>();

        for (PlacementTarget t : targets) {
            TopologicalPhase phase = TopologicalPhase.fromRole(t.role());
            ScheduledBlockPlacement sbp = new ScheduledBlockPlacement(t.pos(), t.block(), phase, t.pos().y());
            graph.addBlock(sbp);
            blockMap.put(t.pos(), t.block());
        }

        // Establish physical block support dependencies
        for (Map.Entry<Vec3i, BlockIdentifier> entry : blockMap.entrySet()) {
            Vec3i pos = entry.getKey();
            BlockIdentifier block = entry.getValue();
            String path = block.path();

            // 1. Blocks requiring support underneath (floor/ground)
            if (path.contains("door") || path.contains("rail") || path.contains("carpet") ||
                path.contains("pressure_plate") || path.contains("flower") || path.contains("campfire") ||
                path.contains("candle") || path.contains("_bed") ||
                (path.contains("lantern") && !"true".equalsIgnoreCase(block.property("hanging"))) ||
                (path.contains("torch") && !path.contains("wall_torch"))) {

                Vec3i supportBelow = pos.add(0, -1, 0);
                if (blockMap.containsKey(supportBelow)) {
                    graph.addDependency(pos, supportBelow);
                }
            }

            // 2. Door upper half depends on door lower half
            if (path.contains("door") && "upper".equalsIgnoreCase(block.property("half"))) {
                Vec3i lowerHalf = pos.add(0, -1, 0);
                if (blockMap.containsKey(lowerHalf)) {
                    graph.addDependency(pos, lowerHalf);
                }
            }

            // 3. Wall torches depend on the wall block behind them
            if (path.contains("wall_torch")) {
                String facing = block.property("facing");
                if (facing != null) {
                    try {
                        Direction dir = Direction.valueOf(facing.toUpperCase(Locale.ROOT));
                        Vec3i attachedWall = pos.offset(dir.opposite());
                        if (blockMap.containsKey(attachedWall)) {
                            graph.addDependency(pos, attachedWall);
                        }
                    } catch (IllegalArgumentException ignored) { }
                }
            }

            // 4. Hanging lanterns and chains depend on ceiling block above
            if ((path.contains("lantern") && "true".equalsIgnoreCase(block.property("hanging"))) || path.contains("chain")) {
                Vec3i supportAbove = pos.add(0, 1, 0);
                if (blockMap.containsKey(supportAbove)) {
                    graph.addDependency(pos, supportAbove);
                }
            }
        }

        return graph.topologicalSort();
    }

    public static Map<ChunkCoordinate, List<ScheduledBlockPlacement>> groupByChunk(List<ScheduledBlockPlacement> placements) {
        Map<ChunkCoordinate, List<ScheduledBlockPlacement>> grouped = new LinkedHashMap<>();
        for (ScheduledBlockPlacement p : placements) {
            ChunkCoordinate chunk = ChunkCoordinate.fromBlock(p.pos());
            grouped.computeIfAbsent(chunk, k -> new ArrayList<>()).add(p);
        }
        return grouped;
    }
}
