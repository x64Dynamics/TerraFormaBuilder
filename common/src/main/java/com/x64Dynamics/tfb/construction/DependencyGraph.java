package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;

public final class DependencyGraph {
    private final Map<Vec3i, ScheduledBlockPlacement> nodes = new HashMap<>();
    private final Map<Vec3i, List<Vec3i>> dependencies = new HashMap<>();

    public void addBlock(ScheduledBlockPlacement block) {
        nodes.put(block.pos(), block);
        dependencies.put(block.pos(), new ArrayList<>());
    }

    public void addDependency(Vec3i dependent, Vec3i requirement) {
        if (nodes.containsKey(dependent) && nodes.containsKey(requirement)) {
            dependencies.get(dependent).add(requirement);
        }
    }

    public List<ScheduledBlockPlacement> topologicalSort() {
        List<ScheduledBlockPlacement> sorted = new ArrayList<>(nodes.size());
        Set<Vec3i> visited = new HashSet<>();
        Set<Vec3i> recursionStack = new HashSet<>();

        // Sort starting nodes by layer and phase to preserve aesthetic progressive order
        List<ScheduledBlockPlacement> initialOrder = new ArrayList<>(nodes.values());
        Collections.sort(initialOrder);

        for (ScheduledBlockPlacement node : initialOrder) {
            if (!visited.contains(node.pos())) {
                dfs(node.pos(), visited, recursionStack, sorted);
            }
        }
        return sorted;
    }

    private void dfs(Vec3i pos, Set<Vec3i> visited, Set<Vec3i> recursionStack, List<ScheduledBlockPlacement> result) {
        visited.add(pos);
        recursionStack.add(pos);

        List<Vec3i> reqs = dependencies.get(pos);
        if (reqs != null) {
            for (Vec3i req : reqs) {
                if (!visited.contains(req)) {
                    dfs(req, visited, recursionStack, result);
                }
            }
        }

        recursionStack.remove(pos);
        result.add(nodes.get(pos));
    }
}
