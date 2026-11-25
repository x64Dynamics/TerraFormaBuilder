package com.x64Dynamics.tfb.architectural.plan;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.*;

/**
 * Validates structural dimensions, room connectivity graph, stairwell headroom, and placement integrity.
 */
public final class BuildingPlanValidator {
    private BuildingPlanValidator() { }

    public static BuildingPlan.ValidationReport validate(
        BuildingDefinition def,
        List<BuildingPlan.FloorPlan> floors,
        List<BuildingPlan.StairwellPlan> stairwells,
        List<PlacementTarget> targets
    ) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        BoundingBox3i box = def.footprintBounds();
        if (box.sizeX() < 5 || box.sizeZ() < 5) {
            errors.add("Footprint too small (" + box.sizeX() + "x" + box.sizeZ() + "); minimum 5x5 required.");
        }
        if (floors.isEmpty()) {
            errors.add("Building plan contains no floors.");
        }
        if (targets.isEmpty()) {
            errors.add("Building plan produced zero block placements.");
        }

        // Map placements for stairwell headroom check
        Map<Vec3i, PlacementTarget> byPos = new HashMap<>(targets.size());
        for (PlacementTarget t : targets) {
            if (t.pos() == null || t.block() == null) {
                errors.add("Null placement position or block detected.");
                continue;
            }
            byPos.put(t.pos(), t);
        }

        for (BuildingPlan.StairwellPlan sw : stairwells) {
            for (Vec3i head : sw.headroomPositions()) {
                PlacementTarget atHead = byPos.get(head);
                if (atHead != null && !atHead.block().path().equals("air")) {
                    warnings.add("Stairwell headroom partially obstructed at " + head + " by " + atHead.block().asString());
                }
            }
        }

        // BFS room connectivity graph across floors via RoomConnections and StairwellPlans
        Map<String, Set<String>> adj = new HashMap<>();
        List<BuildingPlan.PlannedRoom> allRooms = new ArrayList<>();
        for (BuildingPlan.FloorPlan fp : floors) {
            for (BuildingPlan.PlannedRoom r : fp.rooms()) {
                allRooms.add(r);
                adj.putIfAbsent(r.id(), new HashSet<>());
            }
            for (BuildingPlan.RoomConnection c : fp.connections()) {
                adj.computeIfAbsent(c.fromRoomId(), k -> new HashSet<>()).add(c.toRoomId());
                adj.computeIfAbsent(c.toRoomId(), k -> new HashSet<>()).add(c.fromRoomId());
            }
        }

        // Link floors via stairwells (connect any room on fromFloor to any room on toFloor)
        for (BuildingPlan.StairwellPlan sw : stairwells) {
            if (sw.fromFloor() < floors.size() && sw.toFloor() < floors.size()) {
                List<BuildingPlan.PlannedRoom> fromRooms = floors.get(sw.fromFloor()).rooms();
                List<BuildingPlan.PlannedRoom> toRooms = floors.get(sw.toFloor()).rooms();
                if (!fromRooms.isEmpty() && !toRooms.isEmpty()) {
                    String a = fromRooms.get(0).id();
                    String b = toRooms.get(0).id();
                    adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        if (!allRooms.isEmpty()) {
            Queue<String> queue = new ArrayDeque<>();
            String start = allRooms.get(0).id();
            visited.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                for (String next : adj.getOrDefault(cur, Collections.emptySet())) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }

        if (visited.size() < allRooms.size()) {
            warnings.add("Only " + visited.size() + "/" + allRooms.size() + " rooms are connected to the entry graph.");
        }

        return new BuildingPlan.ValidationReport(errors.isEmpty(), visited.size(), allRooms.size(), warnings, errors);
    }
}
