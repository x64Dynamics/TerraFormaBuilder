package com.x64Dynamics.tfb.architectural.plan;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.interior.RoomType;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.Collections;
import java.util.List;

/**
 * Complete inspectable and validatable architectural plan produced before world construction.
 */
public record BuildingPlan(
    BuildingDefinition definition,
    List<FloorPlan> floors,
    List<StairwellPlan> stairwells,
    List<PlacementTarget> targets,
    BoundingBox3i totalBounds,
    ValidationReport validation
) {
    public record PlannedRoom(
        String id,
        int floorIndex,
        RoomType type,
        BoundingBox3i bounds,
        Vec3i primaryDoor
    ) { }

    public record RoomConnection(
        String fromRoomId,
        String toRoomId,
        Vec3i doorwayPos
    ) { }

    public record FloorPlan(
        int floorIndex,
        int floorY,
        int ceilingY,
        List<PlannedRoom> rooms,
        List<RoomConnection> connections
    ) { }

    public record StairwellPlan(
        int fromFloor,
        int toFloor,
        Vec3i bottomPos,
        List<Vec3i> treadPositions,
        List<Vec3i> headroomPositions
    ) { }

    public record ValidationReport(
        boolean valid,
        int reachableRoomCount,
        int totalRoomCount,
        List<String> warnings,
        List<String> errors
    ) {
        public static ValidationReport ok(int reachable, int total) {
            return new ValidationReport(true, reachable, total, Collections.emptyList(), Collections.emptyList());
        }
    }

    public int totalRoomCount() {
        int count = 0;
        for (FloorPlan f : floors) count += f.rooms().size();
        return count;
    }
}
