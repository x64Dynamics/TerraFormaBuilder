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
        List<PlacementTarget> partitionWalls,
        Set<Vec3i> transitClearanceZones
    ) { }

    public static FloorPlanResult planFloor(BoundingBox3i interiorBounds, MaterialPalette palette) {
        List<InteriorRoom> rooms = new ArrayList<>();
        List<PlacementTarget> partitionWalls = new ArrayList<>();
        Set<Vec3i> transitZones = new HashSet<>();

        int minX = interiorBounds.min().x(), maxX = interiorBounds.max().x();
        int minZ = interiorBounds.min().z(), maxZ = interiorBounds.max().z();
        int minY = interiorBounds.min().y(), maxY = interiorBounds.max().y();

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;

        if (spanX < 8 || spanZ < 8) {
            // Small structure: single combined living hall
            Vec3i doorPos = new Vec3i((minX + maxX) / 2, minY, minZ);
            rooms.add(new InteriorRoom(interiorBounds, RoomType.LIVING_HALL, doorPos));
            addClearance(doorPos, transitZones);
            return new FloorPlanResult(rooms, partitionWalls, transitZones);
        }

        if (spanX >= 14 && spanZ >= 14) {
            // Large structure: 4-quadrant layout with central hallway cross
            int midX = (minX + maxX) / 2;
            int midZ = (minZ + maxZ) / 2;

            // X-partition wall
            for (int z = minZ; z <= maxZ; z++) {
                boolean isDoorway = (Math.abs(z - (minZ + midZ) / 2) <= 1) || (Math.abs(z - (midZ + maxZ) / 2) <= 1);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(midX, y, z), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            // Z-partition wall
            for (int x = minX; x <= maxX; x++) {
                boolean isDoorway = (Math.abs(x - (minX + midX) / 2) <= 1) || (Math.abs(x - (midX + maxX) / 2) <= 1);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(x, y, midZ), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            // 4 distinct functional quadrants
            BoundingBox3i q1 = BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(midX - 1, maxY, midZ - 1));
            BoundingBox3i q2 = BoundingBox3i.fromCorners(new Vec3i(midX + 1, minY, minZ), new Vec3i(maxX, maxY, midZ - 1));
            BoundingBox3i q3 = BoundingBox3i.fromCorners(new Vec3i(minX, minY, midZ + 1), new Vec3i(midX - 1, maxY, maxZ));
            BoundingBox3i q4 = BoundingBox3i.fromCorners(new Vec3i(midX + 1, minY, midZ + 1), new Vec3i(maxX, maxY, maxZ));

            Vec3i d1 = new Vec3i(midX, minY, (minZ + midZ) / 2);
            Vec3i d2 = new Vec3i(midX, minY, (minZ + midZ) / 2);
            Vec3i d3 = new Vec3i(midX, minY, (midZ + maxZ) / 2);
            Vec3i d4 = new Vec3i(midX, minY, (midZ + maxZ) / 2);

            rooms.add(new InteriorRoom(q1, RoomType.LIVING_HALL, d1));
            rooms.add(new InteriorRoom(q2, RoomType.KITCHEN_HEARTH, d2));
            rooms.add(new InteriorRoom(q3, RoomType.BEDROOM, d3));
            rooms.add(new InteriorRoom(q4, RoomType.LIBRARY_STUDY, d4));

            addClearance(d1, transitZones);
            addClearance(d2, transitZones);
            addClearance(d3, transitZones);
            addClearance(d4, transitZones);

            return new FloorPlanResult(rooms, partitionWalls, transitZones);
        }

        // Medium structure: 2-room split along the longer dimension
        boolean splitX = spanX >= spanZ;
        if (splitX) {
            int midX = (minX + maxX) / 2;
            int midZ = (minZ + maxZ) / 2;

            for (int z = minZ; z <= maxZ; z++) {
                boolean isDoorway = Math.abs(z - midZ) <= 1;
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(midX, y, z), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            BoundingBox3i r1 = BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(midX - 1, maxY, maxZ));
            BoundingBox3i r2 = BoundingBox3i.fromCorners(new Vec3i(midX + 1, minY, minZ), new Vec3i(maxX, maxY, maxZ));
            Vec3i doorPos = new Vec3i(midX, minY, midZ);

            rooms.add(new InteriorRoom(r1, RoomType.LIVING_HALL, doorPos));
            rooms.add(new InteriorRoom(r2, RoomType.BEDROOM, doorPos));
            addClearance(doorPos, transitZones);
        } else {
            int midX = (minX + maxX) / 2;
            int midZ = (minZ + maxZ) / 2;

            for (int x = minX; x <= maxX; x++) {
                boolean isDoorway = Math.abs(x - midX) <= 1;
                for (int y = minY; y <= maxY; y++) {
                    if (isDoorway && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(x, y, midZ), palette.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY));
                }
            }

            BoundingBox3i r1 = BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, midZ - 1));
            BoundingBox3i r2 = BoundingBox3i.fromCorners(new Vec3i(minX, minY, midZ + 1), new Vec3i(maxX, maxY, maxZ));
            Vec3i doorPos = new Vec3i(midX, minY, midZ);

            rooms.add(new InteriorRoom(r1, RoomType.LIVING_HALL, doorPos));
            rooms.add(new InteriorRoom(r2, RoomType.DINING_HALL, doorPos));
            addClearance(doorPos, transitZones);
        }

        return new FloorPlanResult(rooms, partitionWalls, transitZones);
    }

    private static void addClearance(Vec3i doorPos, Set<Vec3i> transitZones) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                transitZones.add(doorPos.add(dx, 0, dz));
                transitZones.add(doorPos.add(dx, 1, dz));
            }
        }
    }
}
