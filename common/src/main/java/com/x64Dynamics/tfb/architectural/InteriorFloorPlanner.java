package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
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

    public record FloorPlanGraphResult(
        List<InteriorRoom> rooms,
        List<BuildingPlan.PlannedRoom> plannedRooms,
        List<BuildingPlan.RoomConnection> connections,
        List<PlacementTarget> partitionWalls,
        Set<Vec3i> transitClearanceZones
    ) { }

    public static FloorPlanResult planFloor(BoundingBox3i interiorBounds, MaterialPalette palette) {
        return planStory(interiorBounds, 0, 1, palette);
    }

    public static FloorPlanResult planStory(BoundingBox3i interiorBounds, int storyIndex, int totalStories, MaterialPalette palette) {
        FloorPlanGraphResult graph = planStoryWithGraph(interiorBounds, storyIndex, totalStories, palette, 42L);
        return new FloorPlanResult(graph.rooms(), graph.partitionWalls(), graph.transitClearanceZones());
    }

    /**
     * Generates a seed-driven weighted BSP floor plan with shared partition walls,
     * connected doorways forming a spanning tree, and story-appropriate room assignments.
     */
    public static FloorPlanGraphResult planStoryWithGraph(
        BoundingBox3i interiorBounds,
        int storyIndex,
        int totalStories,
        MaterialPalette palette,
        long seed
    ) {
        List<InteriorRoom> rooms = new ArrayList<>();
        List<BuildingPlan.PlannedRoom> plannedRooms = new ArrayList<>();
        List<BuildingPlan.RoomConnection> connections = new ArrayList<>();
        List<PlacementTarget> partitionWalls = new ArrayList<>();
        Set<Vec3i> transitZones = new HashSet<>();

        int minX = interiorBounds.min().x(), maxX = interiorBounds.max().x();
        int minZ = interiorBounds.min().z(), maxZ = interiorBounds.max().z();
        int minY = interiorBounds.min().y(), maxY = interiorBounds.max().y();
        int height = maxY - minY + 1;

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;

        // Vertical circulation: reserve stair clearance if multi-story
        if (storyIndex < totalStories - 1 && spanX >= 7 && spanZ >= 7 && height >= 3) {
            Vec3i stairBase = new Vec3i(minX + 1, minY, minZ + 1);
            for (int s = 0; s < height; s++) {
                transitZones.add(stairBase.offset(Direction.SOUTH, s).add(0, s, 0));
                transitZones.add(stairBase.offset(Direction.SOUTH, s).add(0, s + 1, 0));
            }
            transitZones.add(stairBase.offset(Direction.SOUTH, height));
        }

        Random rng = new Random(seed ^ (storyIndex * 0x9E3779B97F4A7C15L));

        // Perform weighted BSP subdivision (min room span 5 blocks, plus 1 shared wall)
        int maxDepth = (spanX >= 13 && spanZ >= 13) ? 3 : 1;
        List<BoundingBox3i> leafBoxes = new ArrayList<>();
        List<Vec3i> roomDoors = new ArrayList<>();
        BspNode root = subdivide(interiorBounds, rng, 0, maxDepth);
        collectLeavesAndWalls(root, palette, partitionWalls, connections, transitZones, leafBoxes, roomDoors, storyIndex);

        // Assign story-appropriate RoomTypes with seed variation
        List<RoomType> rolePool = getStoryRolePool(storyIndex, rng);
        for (int i = 0; i < leafBoxes.size(); i++) {
            BoundingBox3i b = leafBoxes.get(i);
            Vec3i door = i < roomDoors.size() ? roomDoors.get(i) : new Vec3i((b.min().x() + b.max().x()) / 2, minY, b.min().z());
            RoomType type = rolePool.get(i % rolePool.size());
            if (storyIndex == 0 && i == 0) {
                type = RoomType.LIVING_HALL;
            }
            rooms.add(new InteriorRoom(b, type, door));
            plannedRooms.add(new BuildingPlan.PlannedRoom("f" + storyIndex + "_r" + i, storyIndex, type, b, door));
            addClearance(door, transitZones);
        }

        return new FloorPlanGraphResult(rooms, plannedRooms, connections, partitionWalls, transitZones);
    }

    private static final class BspNode {
        final BoundingBox3i bounds;
        BspNode left;
        BspNode right;
        boolean splitAlongX;
        int splitCoord;
        String leafId;

        BspNode(BoundingBox3i bounds) {
            this.bounds = bounds;
        }
    }

    private static BspNode subdivide(BoundingBox3i box, Random rng, int depth, int maxDepth) {
        BspNode node = new BspNode(box);
        int spanX = box.sizeX();
        int spanZ = box.sizeZ();
        int minRoom = 5; // minimum walkable room width/depth

        boolean canSplitX = spanX >= (minRoom * 2 + 1);
        boolean canSplitZ = spanZ >= (minRoom * 2 + 1);
        if (depth >= maxDepth || (!canSplitX && !canSplitZ)) {
            return node;
        }

        boolean splitX;
        if (canSplitX && canSplitZ) {
            if (spanX > spanZ + 2) splitX = true;
            else if (spanZ > spanX + 2) splitX = false;
            else splitX = rng.nextBoolean();
        } else {
            splitX = canSplitX;
        }

        node.splitAlongX = splitX;
        if (splitX) {
            int minSplit = box.min().x() + minRoom;
            int maxSplit = box.max().x() - minRoom;
            int mid = minSplit + (maxSplit > minSplit ? rng.nextInt(maxSplit - minSplit + 1) : 0);
            node.splitCoord = mid;
            BoundingBox3i leftBox = BoundingBox3i.fromCorners(box.min(), new Vec3i(mid - 1, box.max().y(), box.max().z()));
            BoundingBox3i rightBox = BoundingBox3i.fromCorners(new Vec3i(mid + 1, box.min().y(), box.min().z()), box.max());
            node.left = subdivide(leftBox, rng, depth + 1, maxDepth);
            node.right = subdivide(rightBox, rng, depth + 1, maxDepth);
        } else {
            int minSplit = box.min().z() + minRoom;
            int maxSplit = box.max().z() - minRoom;
            int mid = minSplit + (maxSplit > minSplit ? rng.nextInt(maxSplit - minSplit + 1) : 0);
            node.splitCoord = mid;
            BoundingBox3i leftBox = BoundingBox3i.fromCorners(box.min(), new Vec3i(box.max().x(), box.max().y(), mid - 1));
            BoundingBox3i rightBox = BoundingBox3i.fromCorners(new Vec3i(box.min().x(), box.min().y(), mid + 1), box.max());
            node.left = subdivide(leftBox, rng, depth + 1, maxDepth);
            node.right = subdivide(rightBox, rng, depth + 1, maxDepth);
        }
        return node;
    }

    private static String collectLeavesAndWalls(
        BspNode node,
        MaterialPalette palette,
        List<PlacementTarget> partitionWalls,
        List<BuildingPlan.RoomConnection> connections,
        Set<Vec3i> transitZones,
        List<BoundingBox3i> leaves,
        List<Vec3i> doors,
        int storyIndex
    ) {
        if (node.left == null || node.right == null) {
            int idx = leaves.size();
            node.leafId = "f" + storyIndex + "_r" + idx;
            leaves.add(node.bounds);
            doors.add(new Vec3i(
                (node.bounds.min().x() + node.bounds.max().x()) / 2,
                node.bounds.min().y(),
                node.bounds.min().z()
            ));
            return node.leafId;
        }

        String leftRepId = collectLeavesAndWalls(node.left, palette, partitionWalls, connections, transitZones, leaves, doors, storyIndex);
        String rightRepId = collectLeavesAndWalls(node.right, palette, partitionWalls, connections, transitZones, leaves, doors, storyIndex);

        int minY = node.bounds.min().y();
        int maxY = node.bounds.max().y();
        BlockIdentifier wallBlock = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier doorBlock = palette.get(MaterialRole.DOOR);

        Vec3i doorwayPos;
        if (node.splitAlongX) {
            int midX = node.splitCoord;
            int minZ = node.bounds.min().z();
            int maxZ = node.bounds.max().z();
            int doorZ = (minZ + maxZ) / 2;
            doorwayPos = new Vec3i(midX, minY, doorZ);

            for (int z = minZ; z <= maxZ; z++) {
                boolean isDoor = (z == doorZ);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoor && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(midX, y, z), wallBlock, MaterialRole.WALL_PRIMARY));
                }
            }
            if (maxY - minY >= 1) {
                BlockIdentifier oriented = doorBlock.withProperty("facing", "east").withProperty("hinge", "left").withProperty("open", "false");
                partitionWalls.add(new PlacementTarget(doorwayPos, oriented.withProperty("half", "lower"), MaterialRole.DOOR));
                partitionWalls.add(new PlacementTarget(doorwayPos.add(0, 1, 0), oriented.withProperty("half", "upper"), MaterialRole.DOOR));
            }
        } else {
            int midZ = node.splitCoord;
            int minX = node.bounds.min().x();
            int maxX = node.bounds.max().x();
            int doorX = (minX + maxX) / 2;
            doorwayPos = new Vec3i(doorX, minY, midZ);

            for (int x = minX; x <= maxX; x++) {
                boolean isDoor = (x == doorX);
                for (int y = minY; y <= maxY; y++) {
                    if (isDoor && y < minY + 2) continue;
                    partitionWalls.add(new PlacementTarget(new Vec3i(x, y, midZ), wallBlock, MaterialRole.WALL_PRIMARY));
                }
            }
            if (maxY - minY >= 1) {
                BlockIdentifier oriented = doorBlock.withProperty("facing", "south").withProperty("hinge", "left").withProperty("open", "false");
                partitionWalls.add(new PlacementTarget(doorwayPos, oriented.withProperty("half", "lower"), MaterialRole.DOOR));
                partitionWalls.add(new PlacementTarget(doorwayPos.add(0, 1, 0), oriented.withProperty("half", "upper"), MaterialRole.DOOR));
            }
        }

        addClearance(doorwayPos, transitZones);
        connections.add(new BuildingPlan.RoomConnection(leftRepId, rightRepId, doorwayPos));
        return leftRepId;
    }

    private static List<RoomType> getStoryRolePool(int storyIndex, Random rng) {
        List<RoomType> pool;
        if (storyIndex == 0) {
            pool = new ArrayList<>(Arrays.asList(
                RoomType.LIVING_HALL, RoomType.KITCHEN_HEARTH, RoomType.DINING_HALL,
                RoomType.WORKSHOP_SMITHY, RoomType.STORAGE_PANTRY
            ));
        } else if (storyIndex == 1) {
            pool = new ArrayList<>(Arrays.asList(
                RoomType.BEDROOM, RoomType.LIBRARY_STUDY, RoomType.BATHROOM,
                RoomType.BEDROOM, RoomType.ALCHEMICAL_LAB
            ));
        } else {
            pool = new ArrayList<>(Arrays.asList(
                RoomType.ALCHEMICAL_LAB, RoomType.ARMORY, RoomType.STORAGE_PANTRY,
                RoomType.LIBRARY_STUDY, RoomType.BEDROOM
            ));
        }
        Collections.shuffle(pool, rng);
        return pool;
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
