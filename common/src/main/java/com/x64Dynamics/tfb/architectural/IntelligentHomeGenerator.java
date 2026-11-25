package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

/**
 * Unified procedural home generator that builds complete, multi-floor, furnished homes
 * with structural framing, layered facades, real doors, openable stairwells, overhangs,
 * integrated chimneys, entrance porches, and exterior landscaping.
 */
public final class IntelligentHomeGenerator {
    public record HomeConfig(
        BoundingBox3i bounds,
        MaterialPalette palette,
        RoofGenerator.RoofType roofType,
        int requestedStories, // 0 = auto
        WingConfiguration wingConfig,
        boolean addPorchAndGarden,
        boolean addChimney,
        boolean furnishInterior,
        float furnishingDensity,
        long seed
    ) { }

    public record HomeGenerationResult(
        List<PlacementTarget> targets,
        int stories,
        int roomCount,
        BoundingBox3i totalBounds
    ) { }

    private IntelligentHomeGenerator() { }

    public static HomeGenerationResult generateHome(HomeConfig config) {
        BoundingBox3i bounds = config.bounds();
        MaterialPalette palette = config.palette();
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();

        int width = Math.max(7, bounds.sizeX());
        int depth = Math.max(7, bounds.sizeZ());
        int minX = bounds.min().x();
        int minY = bounds.min().y();
        int minZ = bounds.min().z();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        // Determine story count (each story = 4 blocks: 1 floor + 3 interior headroom)
        int storyHeight = 4;
        int stories = config.requestedStories() > 0
            ? Math.min(4, config.requestedStories())
            : Math.max(1, Math.min(3, (bounds.sizeY() - 3) / storyHeight));

        int wallHeight = stories * storyHeight + 1;
        int wallTopY = minY + wallHeight - 1;
        int roofHeight = Math.max(3, Math.min(width, depth) / 2 + 1);

        BoundingBox3i wallBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX, minY, minZ),
            new Vec3i(maxX, wallTopY, maxZ)
        );
        BoundingBox3i roofBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX, wallTopY + 1, minZ),
            new Vec3i(maxX, wallTopY + roofHeight, maxZ)
        );

        // 1. Sub-foundation footing & ground floor slab
        BlockIdentifier subFoundation = palette.get(MaterialRole.SUB_FOUNDATION);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier floorBlock = palette.get(MaterialRole.FLOOR);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isPerimeter = (x == minX || x == maxX || z == minZ || z == maxZ);
                if (isPerimeter) {
                    put(map, new Vec3i(x, minY - 1, z), subFoundation, MaterialRole.SUB_FOUNDATION);
                    put(map, new Vec3i(x, minY, z), foundation, MaterialRole.FOUNDATION);
                } else {
                    put(map, new Vec3i(x, minY, z), floorBlock, MaterialRole.FLOOR);
                }
            }
        }

        // 2. Exterior facades per story with horizontal timber/stone beams & real corner pillars
        BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);
        BlockIdentifier beamX = orientLog(palette.get(MaterialRole.BEAM), Axis.X);
        BlockIdentifier beamZ = orientLog(palette.get(MaterialRole.BEAM), Axis.Z);

        for (int s = 0; s < stories; s++) {
            int storyY = minY + 1 + (s * storyHeight);
            int curHeight = storyHeight;
            boolean isGround = (s == 0);

            for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                new Vec3i(minX, storyY, maxZ), Direction.EAST, Direction.SOUTH, width, curHeight, isGround, palette)) {
                put(map, t.pos(), t.block(), t.role());
            }
            for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                new Vec3i(maxX, storyY, minZ), Direction.WEST, Direction.NORTH, width, curHeight, false, palette)) {
                put(map, t.pos(), t.block(), t.role());
            }
            for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                new Vec3i(maxX, storyY, maxZ), Direction.NORTH, Direction.EAST, depth, curHeight, false, palette)) {
                put(map, t.pos(), t.block(), t.role());
            }
            for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                new Vec3i(minX, storyY, minZ), Direction.SOUTH, Direction.WEST, depth, curHeight, false, palette)) {
                put(map, t.pos(), t.block(), t.role());
            }

            // Horizontal framing beam at the top of each story
            int beamY = storyY + curHeight - 1;
            for (int x = minX + 1; x < maxX; x++) {
                put(map, new Vec3i(x, beamY, minZ), beamX, MaterialRole.BEAM);
                put(map, new Vec3i(x, beamY, maxZ), beamX, MaterialRole.BEAM);
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                put(map, new Vec3i(minX, beamY, z), beamZ, MaterialRole.BEAM);
                put(map, new Vec3i(maxX, beamY, z), beamZ, MaterialRole.BEAM);
            }
        }

        // Reinforce all 4 corners with vertical structural pillars from foundation to roof eave
        BlockIdentifier pillarY = orientLog(pillarBlock, Axis.Y);
        for (int y = minY; y <= wallTopY; y++) {
            put(map, new Vec3i(minX, y, minZ), pillarY, MaterialRole.PILLAR);
            put(map, new Vec3i(maxX, y, minZ), pillarY, MaterialRole.PILLAR);
            put(map, new Vec3i(minX, y, maxZ), pillarY, MaterialRole.PILLAR);
            put(map, new Vec3i(maxX, y, maxZ), pillarY, MaterialRole.PILLAR);
        }

        // 3. Place real 2-block-tall Entrance Door on South facade
        int doorX = minX + width / 2;
        Vec3i frontDoorPos = new Vec3i(doorX, minY + 1, maxZ);
        BlockIdentifier doorBase = palette.get(MaterialRole.DOOR)
            .withProperty("facing", "north")
            .withProperty("hinge", "left")
            .withProperty("open", "false");
        put(map, frontDoorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
        put(map, frontDoorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
        // Clear interior & exterior air around entrance door
        putAir(map, frontDoorPos.add(0, 0, -1));
        putAir(map, frontDoorPos.add(0, 1, -1));
        putAir(map, frontDoorPos.add(0, 0, 1));
        putAir(map, frontDoorPos.add(0, 1, 1));

        // 4. Upper Story Floor Slabs & Stairwell Cutouts
        Set<Vec3i> globalClearance = new HashSet<>();
        addDoorClearance(frontDoorPos.add(0, 0, -1), globalClearance);

        for (int s = 1; s < stories; s++) {
            int floorY = minY + (s * storyHeight);
            for (int x = minX + 1; x < maxX; x++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    put(map, new Vec3i(x, floorY, z), floorBlock, MaterialRole.FLOOR);
                }
            }
        }

        // Build vertical staircases connecting stories AND carve headroom through upper floors
        if (stories > 1 && width >= 8 && depth >= 8) {
            int stairX = minX + 2;
            int stairZStart = minZ + 2;
            BlockIdentifier fence = palette.get(MaterialRole.BALUSTRADE);

            for (int s = 0; s < stories - 1; s++) {
                int baseFloorY = minY + (s * storyHeight);
                int stairBottomY = baseFloorY + 1;
                Vec3i stairBottom = new Vec3i(stairX, stairBottomY, stairZStart);

                List<PlacementTarget> stairs = StaircaseGenerator.generateStraight(
                    stairBottom, Direction.SOUTH, storyHeight, 1, palette
                );
                for (PlacementTarget st : stairs) {
                    put(map, st.pos(), st.block(), st.role());
                    // Carve 3 blocks of headroom above each stair step (breaks through floor slab above!)
                    for (int h = 1; h <= 3; h++) {
                        Vec3i head = st.pos().add(0, h, 0);
                        if (head.y() < wallTopY) {
                            putAir(map, head);
                            globalClearance.add(head);
                        }
                    }
                    globalClearance.add(st.pos());
                }

                // Landing clearance at bottom and top of stairs
                globalClearance.add(stairBottom.offset(Direction.NORTH, 1));
                globalClearance.add(stairBottom.offset(Direction.NORTH, 1).add(0, 1, 0));
                Vec3i topLanding = stairBottom.offset(Direction.SOUTH, storyHeight).add(0, storyHeight - 1, 0);
                globalClearance.add(topLanding);
                globalClearance.add(topLanding.add(0, 1, 0));

                // Safety railing along the open side of the upper stairwell
                int upperFloorY = baseFloorY + storyHeight;
                for (int step = 0; step < storyHeight; step++) {
                    Vec3i railPos = new Vec3i(stairX + 1, upperFloorY + 1, stairZStart + step);
                    put(map, railPos, fence, MaterialRole.BALUSTRADE);
                }
            }
        }

        // 5. Roof with overhangs, filled gable pediments, and ridge cap
        Axis ridgeAxis = (width >= depth) ? Axis.X : Axis.Z;
        RoofGenerator.RoofType effectiveRoof = config.roofType() != null ? config.roofType() : RoofGenerator.RoofType.GABLE;
        for (PlacementTarget rt : RoofGenerator.generateRoof(roofBounds, effectiveRoof, ridgeAxis, 1, palette)) {
            put(map, rt.pos(), rt.block(), rt.role());
        }

        // 6. Integrated Chimney (from ground floor up through the roof)
        if (config.addChimney()) {
            int chimX = maxX - 1;
            int chimZ = minZ + 2;
            int roofPeakY = wallTopY + Math.min(width, depth) / 2 + 2;
            BlockIdentifier brick = BlockIdentifier.of("minecraft:bricks");
            for (int y = minY + 1; y <= roofPeakY; y++) {
                put(map, new Vec3i(chimX, y, chimZ), brick, MaterialRole.WALL_ACCENT);
            }
            put(map, new Vec3i(chimX, roofPeakY + 1, chimZ), BlockIdentifier.of("minecraft:campfire"), MaterialRole.DECORATION);
        }

        // 7. Multi-Story Room Planning & Interior Furnishing
        int totalRooms = 0;
        for (int s = 0; s < stories; s++) {
            int floorY = minY + 1 + (s * storyHeight);
            int ceilingY = floorY + storyHeight - 2;
            if (ceilingY <= floorY) continue;

            if (width >= 9 && depth >= 9) {
                BoundingBox3i storyBox = BoundingBox3i.fromCorners(
                    new Vec3i(minX + 1, floorY, minZ + 1),
                    new Vec3i(maxX - 1, ceilingY, maxZ - 1)
                );
                InteriorFloorPlanner.FloorPlanResult plan = InteriorFloorPlanner.planStory(storyBox, s, stories, palette);
                for (PlacementTarget pw : plan.partitionWalls()) {
                    if (!globalClearance.contains(pw.pos())) {
                        put(map, pw.pos(), pw.block(), pw.role());
                    }
                }
                Set<Vec3i> roomClearance = new HashSet<>(globalClearance);
                roomClearance.addAll(plan.transitClearanceZones());

                totalRooms += plan.rooms().size();
                if (config.furnishInterior()) {
                    for (InteriorRoom room : plan.rooms()) {
                        for (PlacementTarget ft : InteriorFurnishingGenerator.furnishRoom(
                            room, palette, roomClearance, config.furnishingDensity())) {
                            if (!globalClearance.contains(ft.pos())) {
                                put(map, ft.pos(), ft.block(), ft.role());
                            }
                        }
                    }
                }
            } else {
                totalRooms += 1;
                if (config.furnishInterior()) {
                    InteriorRoom single = new InteriorRoom(
                        BoundingBox3i.fromCorners(new Vec3i(minX + 1, floorY, minZ + 1), new Vec3i(maxX - 1, ceilingY, maxZ - 1)),
                        s == 0 ? RoomType.LIVING_HALL : RoomType.BEDROOM,
                        frontDoorPos
                    );
                    for (PlacementTarget ft : InteriorFurnishingGenerator.furnishRoom(
                        single, palette, globalClearance, config.furnishingDensity())) {
                        if (!globalClearance.contains(ft.pos())) {
                            put(map, ft.pos(), ft.block(), ft.role());
                        }
                    }
                }
            }
        }

        // 8. Entrance Porch, Steps & Front Garden Path
        if (config.addPorchAndGarden()) {
            BlockIdentifier stairSouth = palette.get(MaterialRole.STAIR).withProperty("facing", "north").withProperty("half", "bottom");
            BlockIdentifier slab = palette.get(MaterialRole.SLAB).withProperty("type", "bottom");
            BlockIdentifier fence = palette.get(MaterialRole.FENCE);
            BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

            // Porch stoop (3 wide x 2 deep in front of door)
            for (int dx = -1; dx <= 1; dx++) {
                Vec3i stoop1 = new Vec3i(doorX + dx, minY, maxZ + 1);
                put(map, stoop1, foundation, MaterialRole.FOUNDATION);
                put(map, new Vec3i(doorX + dx, minY, maxZ + 2), stairSouth, MaterialRole.STAIR);
                // Porch canopy roof at y = minY + 4
                put(map, new Vec3i(doorX + dx, minY + 4, maxZ + 1), slab, MaterialRole.SLAB);
                put(map, new Vec3i(doorX + dx, minY + 4, maxZ + 2), slab, MaterialRole.SLAB);
            }
            // Porch support posts & hanging lantern
            for (int y = minY + 1; y <= minY + 3; y++) {
                put(map, new Vec3i(doorX - 1, y, maxZ + 2), fence, MaterialRole.FENCE);
                put(map, new Vec3i(doorX + 1, y, maxZ + 2), fence, MaterialRole.FENCE);
            }
            put(map, new Vec3i(doorX, minY + 3, maxZ + 1), lantern.withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);

            // Garden walkway leading 5 blocks out from porch steps
            BlockIdentifier dirtPath = BlockIdentifier.of("minecraft:dirt_path");
            BlockIdentifier cobble = BlockIdentifier.of("minecraft:cobblestone");
            for (int pz = maxZ + 3; pz <= maxZ + 7; pz++) {
                put(map, new Vec3i(doorX - 1, minY, pz), (pz % 2 == 0) ? dirtPath : cobble, MaterialRole.FLOOR);
                put(map, new Vec3i(doorX, minY, pz), dirtPath, MaterialRole.FLOOR);
                put(map, new Vec3i(doorX + 1, minY, pz), (pz % 2 == 1) ? dirtPath : cobble, MaterialRole.FLOOR);
            }

            // Window flower planters on front facade
            for (int wx : new int[]{minX + 2, maxX - 2}) {
                if (Math.abs(wx - doorX) >= 3) {
                    put(map, new Vec3i(wx, minY + 1, maxZ + 1), BlockIdentifier.of("minecraft:grass_block"), MaterialRole.DECORATION);
                    put(map, new Vec3i(wx, minY + 2, maxZ + 1), BlockIdentifier.of("minecraft:poppy"), MaterialRole.DECORATION);
                }
            }
        }

        List<PlacementTarget> resultTargets = new ArrayList<>(map.values());
        BoundingBox3i totalBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX - 1, minY - 1, minZ - 1),
            new Vec3i(maxX + 1, wallTopY + roofHeight + 2, maxZ + (config.addPorchAndGarden() ? 7 : 1))
        );
        return new HomeGenerationResult(resultTargets, stories, totalRooms, totalBounds);
    }

    private static BlockIdentifier orientLog(BlockIdentifier id, Axis axis) {
        String p = id.path().toLowerCase(Locale.ROOT);
        if (p.endsWith("_log") || p.endsWith("_wood") || p.endsWith("_stem") || p.endsWith("_hyphae") || p.contains("pillar") || p.contains("basalt")) {
            return id.withProperty("axis", axis.name().toLowerCase(Locale.ROOT));
        }
        return id;
    }

    private static void addDoorClearance(Vec3i pos, Set<Vec3i> clearance) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                clearance.add(pos.add(dx, 0, dz));
                clearance.add(pos.add(dx, 1, dz));
            }
        }
    }

    private static void put(Map<Vec3i, PlacementTarget> map, Vec3i pos, BlockIdentifier block, MaterialRole role) {
        map.put(pos, new PlacementTarget(pos, block, role));
    }

    private static void putAir(Map<Vec3i, PlacementTarget> map, Vec3i pos) {
        map.put(pos, new PlacementTarget(pos, BlockIdentifier.AIR, MaterialRole.AIR));
    }
}
