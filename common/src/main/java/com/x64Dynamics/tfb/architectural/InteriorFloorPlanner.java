package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
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
        return planStory(interiorBounds, 0, 1, palette);
    }

    public static FloorPlanResult planStory(BoundingBox3i interiorBounds, int storyIndex, int totalStories, MaterialPalette palette) {
        List<InteriorRoom> rooms = new ArrayList<>();
        List<PlacementTarget> partitionWalls = new ArrayList<>();
        Set<Vec3i> transitZones = new HashSet<>();

        int minX = interiorBounds.min().x(), maxX = interiorBounds.max().x();
        int minZ = interiorBounds.min().z(), maxZ = interiorBounds.max().z();
        int minY = interiorBounds.min().y(), maxY = interiorBounds.max().y();
        int height = maxY - minY + 1;

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;

        // Vertical circulation: staircase to next floor if not top floor
        if (storyIndex < totalStories - 1 && spanX >= 7 && spanZ >= 7 && height >= 3) {
            Vec3i stairBase = new Vec3i(minX + 1, minY, minZ + 1);
            partitionWalls.addAll(StaircaseGenerator.generateStraight(stairBase, Direction.SOUTH, height, 1, palette));
            for (int s = 0; s < height; s++) {
                transitZones.add(stairBase.offset(Direction.SOUTH, s).add(0, s, 0));
                transitZones.add(stairBase.offset(Direction.SOUTH, s).add(0, s + 1, 0));
            }
            transitZones.add(stairBase.offset(Direction.SOUTH, height));
        }

        // Room selection based on story index
        RoomType r1Type, r2Type, r3Type, r4Type;
        if (storyIndex == 0) {
            // Ground floor: Public & utility spaces
            r1Type = RoomType.LIVING_HALL;
            r2Type = RoomType.KITCHEN_HEARTH;
            r3Type = RoomType.DINING_HALL;
            r4Type = RoomType.WORKSHOP_SMITHY;
        } else if (storyIndex == 1) {
            // Second floor: Private living & study
            r1Type = RoomType.BEDROOM;
            r2Type = RoomType.LIBRARY_STUDY;
            r3Type = RoomType.BATHROOM;
            r4Type = RoomType.BEDROOM;
        } else {
            // Upper / attic floors: Specialty & storage
            r1Type = RoomType.ALCHEMICAL_LAB;
            r2Type = RoomType.ARMORY;
            r3Type = RoomType.STORAGE_PANTRY;
            r4Type = RoomType.LIBRARY_STUDY;
        }

        if (spanX < 8 || spanZ < 8) {
            // Small structure: single room
            Vec3i doorPos = new Vec3i((minX + maxX) / 2, minY, minZ);
            rooms.add(new InteriorRoom(interiorBounds, r1Type, doorPos));
            addClearance(doorPos, transitZones);
            return new FloorPlanResult(rooms, partitionWalls, transitZones);
        }

        if (spanX >= 13 && spanZ >= 13) {
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

            rooms.add(new InteriorRoom(q1, r1Type, d1));
            rooms.add(new InteriorRoom(q2, r2Type, d2));
            rooms.add(new InteriorRoom(q3, r3Type, d3));
            rooms.add(new InteriorRoom(q4, r4Type, d4));

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

            rooms.add(new InteriorRoom(r1, r1Type, doorPos));
            rooms.add(new InteriorRoom(r2, r2Type, doorPos));
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

            rooms.add(new InteriorRoom(r1, r1Type, doorPos));
            rooms.add(new InteriorRoom(r2, r2Type, doorPos));
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
