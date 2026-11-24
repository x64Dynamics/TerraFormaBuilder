package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class InteriorFloorPlanner {
    private InteriorFloorPlanner() { }

    public record FloorPlanResult(
        List<InteriorRoom> rooms,
        List<PlacementTarget> partitionWalls
    ) { }

    public static FloorPlanResult planFloor(BoundingBox3i interiorBounds, MaterialPalette palette) {
        List<InteriorRoom> rooms = new ArrayList<>();
        List<PlacementTarget> partitionWalls = new ArrayList<>();

        int minX = interiorBounds.min().x(), maxX = interiorBounds.max().x();
        int minZ = interiorBounds.min().z(), maxZ = interiorBounds.max().z();
        int minY = interiorBounds.min().y(), maxY = interiorBounds.max().y();

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;

        if (spanX < 8 || spanZ < 8) {
            // Small structure: single combined living hall
            rooms.add(new InteriorRoom(interiorBounds, RoomType.LIVING_HALL, new Vec3i((minX + maxX) / 2, minY, minZ)));
            return new FloorPlanResult(rooms, partitionWalls);
        }

        // Subdivide with central hallway along the longer dimension
        boolean splitX = spanX >= spanZ;

        if (splitX) {
            int midX = (minX + maxX) / 2;

            // Partition wall along Z at midX with a doorway at midZ
            for (int z = minZ; z <= maxZ; z++) {
                boolean isDoorway = (z >= (minZ + maxZ) / 2 - 1 && z <= (minZ + maxZ) / 2);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) {
                        continue;
                    }
                    partitionWalls.add(new PlacementTarget(new Vec3i(midX, y, z), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            BoundingBox3i room1Bounds = BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(midX - 1, maxY, maxZ));
            BoundingBox3i room2Bounds = BoundingBox3i.fromCorners(new Vec3i(midX + 1, minY, minZ), new Vec3i(maxX, maxY, maxZ));

            rooms.add(new InteriorRoom(room1Bounds, RoomType.LIVING_HALL, new Vec3i(midX, minY, (minZ + maxZ) / 2)));
            rooms.add(new InteriorRoom(room2Bounds, RoomType.BEDROOM, new Vec3i(midX, minY, (minZ + maxZ) / 2)));
        } else {
            int midZ = (minZ + maxZ) / 2;

            for (int x = minX; x <= maxX; x++) {
                boolean isDoorway = (x >= (minX + maxX) / 2 - 1 && x <= (minX + maxX) / 2);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) {
                        continue;
                    }
                    partitionWalls.add(new PlacementTarget(new Vec3i(x, y, midZ), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            BoundingBox3i room1Bounds = BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, midZ - 1));
            BoundingBox3i room2Bounds = BoundingBox3i.fromCorners(new Vec3i(minX, minY, midZ + 1), new Vec3i(maxX, maxY, maxZ));

            rooms.add(new InteriorRoom(room1Bounds, RoomType.LIVING_HALL, new Vec3i((minX + maxX) / 2, minY, midZ)));
            rooms.add(new InteriorRoom(room2Bounds, RoomType.LIBRARY_STUDY, new Vec3i((minX + maxX) / 2, minY, midZ)));
        }

        return new FloorPlanResult(rooms, partitionWalls);
    }
}
