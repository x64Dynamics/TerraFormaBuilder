package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.architectural.plan.BuildingDefinition;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlanValidator;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.*;

/**
 * Unified procedural home generator that builds a validated BuildingPlan
 * with multi-floor BSP rooms, connection graphs, real doors, openable stairwells,
 * layered facades, roofs, chimneys, porches, and exterior landscaping.
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
        BoundingBox3i totalBounds,
        BuildingPlan plan
    ) { }

    private IntelligentHomeGenerator() { }

    public static HomeGenerationResult generateHome(HomeConfig config) {
        BuildingDefinition def = new BuildingDefinition(
            "home-" + Long.toHexString(config.seed()),
            config.bounds(),
            config.wingConfig(),
            StylePreset.MEDIEVAL,
            config.palette(),
            config.roofType(),
            config.requestedStories(),
            config.addPorchAndGarden(),
            config.addChimney(),
            config.furnishInterior(),
            config.furnishingDensity(),
            config.seed()
        );
        BuildingPlan plan = generatePlan(def);
        return new HomeGenerationResult(
            plan.targets(),
            plan.floors().size(),
            plan.totalRoomCount(),
            plan.totalBounds(),
            plan
        );
    }

    public static BuildingPlan generatePlan(BuildingDefinition def) {
        return generatePlan(def, null, com.x64Dynamics.tfb.terrain.FoundationStrategy.STEPPED_TERRACE);
    }

    public static BuildingPlan generatePlan(
            BuildingDefinition def,
            com.x64Dynamics.tfb.terrain.HeightmapGrid heightmap,
            com.x64Dynamics.tfb.terrain.FoundationStrategy foundationStrategy) {
        BoundingBox3i bounds = def.footprintBounds();
        MaterialPalette palette = def.palette();
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        List<BuildingPlan.FloorPlan> floorPlans = new ArrayList<>();
        List<BuildingPlan.StairwellPlan> stairwellPlans = new ArrayList<>();

        int width = Math.max(7, bounds.sizeX());
        int depth = Math.max(7, bounds.sizeZ());
        int minX = bounds.min().x();
        int minY = bounds.min().y();
        int minZ = bounds.min().z();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        int storyHeight = 4;
        int stories = def.requestedStories() > 0
            ? Math.min(4, def.requestedStories())
            : Math.max(1, Math.min(3, (bounds.sizeY() - 3) / storyHeight));

        int wallHeight = stories * storyHeight + 1;
        int wallTopY = minY + wallHeight - 1;
        int roofHeight = Math.max(3, Math.min(width, depth) / 2 + 1);

        BoundingBox3i roofBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX, wallTopY + 1, minZ),
            new Vec3i(maxX, wallTopY + roofHeight, maxZ)
        );

        BlockIdentifier subFoundation = palette.get(MaterialRole.SUB_FOUNDATION);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier floorBlock = palette.get(MaterialRole.FLOOR);

        // 1. Shell & Roof (Single Rectangle or Multi-Wing Compound)
        if (def.wingConfiguration() != WingConfiguration.SINGLE_RECTANGLE) {
            BoundingBox3i compoundBounds = BoundingBox3i.fromCorners(
                new Vec3i(minX, minY, minZ),
                new Vec3i(maxX, wallTopY + roofHeight, maxZ)
            );
            ParametricFootprint footprint = ParametricFootprint.create(compoundBounds, def.wingConfiguration());
            for (PlacementTarget wt : WingLayoutGenerator.generateWingCompound(footprint, def.roofType(), palette)) {
                put(map, wt.pos(), wt.block(), wt.role());
            }
        } else {
            // Sub-foundation footing & ground floor slab
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

            // Exterior facades per story with horizontal framing beams & vertical corner pillars
            BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);
            BlockIdentifier beamX = orientLog(palette.get(MaterialRole.BEAM), Axis.X);
            BlockIdentifier beamZ = orientLog(palette.get(MaterialRole.BEAM), Axis.Z);

            for (int s = 0; s < stories; s++) {
                int storyY = minY + 1 + (s * storyHeight);
                boolean isGround = (s == 0);

                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(minX, storyY, maxZ), Direction.EAST, Direction.SOUTH, width, storyHeight, isGround, palette)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(maxX, storyY, minZ), Direction.WEST, Direction.NORTH, width, storyHeight, false, palette)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(maxX, storyY, maxZ), Direction.NORTH, Direction.EAST, depth, storyHeight, false, palette)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(minX, storyY, minZ), Direction.SOUTH, Direction.WEST, depth, storyHeight, false, palette)) {
                    put(map, t.pos(), t.block(), t.role());
                }

                int beamY = storyY + storyHeight - 1;
                for (int x = minX + 1; x < maxX; x++) {
                    put(map, new Vec3i(x, beamY, minZ), beamX, MaterialRole.BEAM);
                    put(map, new Vec3i(x, beamY, maxZ), beamX, MaterialRole.BEAM);
                }
                for (int z = minZ + 1; z < maxZ; z++) {
                    put(map, new Vec3i(minX, beamY, z), beamZ, MaterialRole.BEAM);
                    put(map, new Vec3i(maxX, beamY, z), beamZ, MaterialRole.BEAM);
                }
            }

            BlockIdentifier pillarY = orientLog(pillarBlock, Axis.Y);
            for (int y = minY; y <= wallTopY; y++) {
                put(map, new Vec3i(minX, y, minZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(maxX, y, minZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(minX, y, maxZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(maxX, y, maxZ), pillarY, MaterialRole.PILLAR);
            }

            // Roof with overhangs, filled gable pediments, and ridge cap
            Axis ridgeAxis = (width >= depth) ? Axis.X : Axis.Z;
            for (PlacementTarget rt : RoofGenerator.generateRoof(roofBounds, def.roofType(), ridgeAxis, 1, palette)) {
                put(map, rt.pos(), rt.block(), rt.role());
            }
        }

        // 2. Real 2-block-tall Entrance Door on South facade
        int doorX = minX + width / 2;
        Vec3i frontDoorPos = new Vec3i(doorX, minY + 1, maxZ);
        BlockIdentifier doorBase = palette.get(MaterialRole.DOOR)
            .withProperty("facing", "north")
            .withProperty("hinge", "left")
            .withProperty("open", "false");
        put(map, frontDoorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
        put(map, frontDoorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
        putAir(map, frontDoorPos.add(0, 0, -1));
        putAir(map, frontDoorPos.add(0, 1, -1));
        putAir(map, frontDoorPos.add(0, 0, 1));
        putAir(map, frontDoorPos.add(0, 1, 1));

        // 3. Upper Story Floor Slabs & Openable Stairwells
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

        if (stories > 1 && width >= 8 && depth >= 8) {
            int stairX = minX + 2;
            int stairZStart = minZ + 2;
            BlockIdentifier fence = palette.get(MaterialRole.BALUSTRADE);

            for (int s = 0; s < stories - 1; s++) {
                int baseFloorY = minY + (s * storyHeight);
                int stairBottomY = baseFloorY + 1;
                Vec3i stairBottom = new Vec3i(stairX, stairBottomY, stairZStart);

                List<Vec3i> treadPositions = new ArrayList<>();
                List<Vec3i> headroomPositions = new ArrayList<>();

                List<PlacementTarget> stairs = StaircaseGenerator.generateStraight(
                    stairBottom, Direction.SOUTH, storyHeight, 1, palette
                );
                for (PlacementTarget st : stairs) {
                    put(map, st.pos(), st.block(), st.role());
                    treadPositions.add(st.pos());
                    for (int h = 1; h <= 3; h++) {
                        Vec3i head = st.pos().add(0, h, 0);
                        if (head.y() < wallTopY) {
                            putAir(map, head);
                            globalClearance.add(head);
                            headroomPositions.add(head);
                        }
                    }
                    globalClearance.add(st.pos());
                }

                globalClearance.add(stairBottom.offset(Direction.NORTH, 1));
                globalClearance.add(stairBottom.offset(Direction.NORTH, 1).add(0, 1, 0));
                Vec3i topLanding = stairBottom.offset(Direction.SOUTH, storyHeight).add(0, storyHeight - 1, 0);
                globalClearance.add(topLanding);
                globalClearance.add(topLanding.add(0, 1, 0));

                int upperFloorY = baseFloorY + storyHeight;
                for (int step = 0; step < storyHeight; step++) {
                    Vec3i railPos = new Vec3i(stairX + 1, upperFloorY + 1, stairZStart + step);
                    put(map, railPos, fence, MaterialRole.BALUSTRADE);
                }

                stairwellPlans.add(new BuildingPlan.StairwellPlan(s, s + 1, stairBottom, treadPositions, headroomPositions));
            }
        }

        // 4. Integrated Chimney (from ground floor up through the roof)
        if (def.addChimney()) {
            int chimX = maxX - 1;
            int chimZ = minZ + 2;
            int roofPeakY = wallTopY + Math.min(width, depth) / 2 + 2;
            BlockIdentifier brick = BlockIdentifier.of("minecraft:bricks");
            for (int y = minY + 1; y <= roofPeakY; y++) {
                put(map, new Vec3i(chimX, y, chimZ), brick, MaterialRole.WALL_ACCENT);
            }
            put(map, new Vec3i(chimX, roofPeakY + 1, chimZ), BlockIdentifier.of("minecraft:campfire"), MaterialRole.DECORATION);
        }

        // 5. Seed-Driven BSP Multi-Story Room Planning & Interior Furnishing
        for (int s = 0; s < stories; s++) {
            int floorY = minY + 1 + (s * storyHeight);
            int ceilingY = floorY + storyHeight - 2;
            if (ceilingY <= floorY) continue;

            BoundingBox3i storyBox = BoundingBox3i.fromCorners(
                new Vec3i(minX + 1, floorY, minZ + 1),
                new Vec3i(maxX - 1, ceilingY, maxZ - 1)
            );
            InteriorFloorPlanner.FloorPlanGraphResult plan = InteriorFloorPlanner.planStoryWithGraph(
                storyBox, s, stories, palette, def.seed()
            );
            for (PlacementTarget pw : plan.partitionWalls()) {
                if (!globalClearance.contains(pw.pos())) {
                    put(map, pw.pos(), pw.block(), pw.role());
                }
            }

            Set<Vec3i> roomClearance = new HashSet<>(globalClearance);
            roomClearance.addAll(plan.transitClearanceZones());

            if (def.furnishInterior()) {
                for (InteriorRoom room : plan.rooms()) {
                    for (PlacementTarget ft : InteriorFurnishingGenerator.furnishRoom(
                        room, palette, roomClearance, def.furnishingDensity())) {
                        if (!globalClearance.contains(ft.pos())) {
                            put(map, ft.pos(), ft.block(), ft.role());
                        }
                    }
                }
            }

            // Hallway/stairwell ceiling lantern on every floor for spawn-safe lighting
            Vec3i hallLamp = new Vec3i(minX + width / 2, ceilingY, minZ + depth / 2);
            if (!map.containsKey(hallLamp)) {
                put(map, hallLamp, palette.get(MaterialRole.LIGHT_SOURCE).withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);
            }

            floorPlans.add(new BuildingPlan.FloorPlan(s, floorY, ceilingY, plan.plannedRooms(), plan.connections()));
        }

        // 6. Entrance Porch, Steps & Front Garden Path
        if (def.addPorchAndGarden()) {
            BlockIdentifier stairSouth = palette.get(MaterialRole.STAIR).withProperty("facing", "north").withProperty("half", "bottom");
            BlockIdentifier slab = palette.get(MaterialRole.SLAB).withProperty("type", "bottom");
            BlockIdentifier fence = palette.get(MaterialRole.FENCE);
            BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

            for (int dx = -1; dx <= 1; dx++) {
                put(map, new Vec3i(doorX + dx, minY, maxZ + 1), foundation, MaterialRole.FOUNDATION);
                put(map, new Vec3i(doorX + dx, minY, maxZ + 2), stairSouth, MaterialRole.STAIR);
                put(map, new Vec3i(doorX + dx, minY + 4, maxZ + 1), slab, MaterialRole.SLAB);
                put(map, new Vec3i(doorX + dx, minY + 4, maxZ + 2), slab, MaterialRole.SLAB);
            }
            for (int y = minY + 1; y <= minY + 3; y++) {
                put(map, new Vec3i(doorX - 1, y, maxZ + 2), fence, MaterialRole.FENCE);
                put(map, new Vec3i(doorX + 1, y, maxZ + 2), fence, MaterialRole.FENCE);
            }
            put(map, new Vec3i(doorX, minY + 3, maxZ + 1), lantern.withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);

            for (PlacementTarget lt : com.x64Dynamics.tfb.terrain.ExteriorLandscapingEngine.generateTerrainApproachAndYard(
                    bounds, doorX, maxZ, heightmap, palette, def.style(), def.seed())) {
                put(map, lt.pos(), lt.block(), lt.role());
            }

            for (int wx : new int[]{minX + 2, maxX - 2}) {
                if (Math.abs(wx - doorX) >= 3) {
                    put(map, new Vec3i(wx, minY + 1, maxZ + 1), BlockIdentifier.of("minecraft:grass_block"), MaterialRole.DECORATION);
                    put(map, new Vec3i(wx, minY + 2, maxZ + 1), BlockIdentifier.of("minecraft:poppy"), MaterialRole.DECORATION);
                    // Open trapdoor shutters flanking ground-floor windows
                    BlockIdentifier shutter = palette.get(MaterialRole.FRAME)
                        .withProperty("facing", "south").withProperty("open", "true").withProperty("half", "bottom");
                    if (wx - 1 > minX) put(map, new Vec3i(wx - 1, minY + 2, maxZ + 1), shutter, MaterialRole.FRAME);
                    if (wx + 1 < maxX) put(map, new Vec3i(wx + 1, minY + 2, maxZ + 1), shutter, MaterialRole.FRAME);
                }
            }
        }

        // 7. Second-Floor Balcony & Roof Dormer (on multi-story / wide single-rectangle homes)
        if (def.wingConfiguration() == WingConfiguration.SINGLE_RECTANGLE) {
            if (stories >= 2 && width >= 11) {
                Vec3i balDoorPos = new Vec3i(doorX, minY + storyHeight + 1, maxZ);
                put(map, balDoorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
                put(map, balDoorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
                for (PlacementTarget bt : FacadeOrnamentGenerator.generateBalcony(balDoorPos, Direction.SOUTH, 3, 2, palette)) {
                    put(map, bt.pos(), bt.block(), bt.role());
                }
            }
            if (width >= 13 && def.roofType() == RoofGenerator.RoofType.GABLE) {
                Vec3i dormerPos = new Vec3i(doorX, wallTopY + 2, maxZ - 1);
                for (PlacementTarget dt : AdvancedRoofFeatures.generateDormer(dormerPos, Direction.SOUTH, 3, 3, palette)) {
                    put(map, dt.pos(), dt.block(), dt.role());
                }
            }
            if (def.addChimney() && width >= 9 && depth >= 9) {
                Vec3i chimneyBase = new Vec3i(minX + 2, minY + 1, minZ + 2);
                int chimneyTop = wallTopY + roofHeight + 1;
                BlockIdentifier brick = BlockIdentifier.of("minecraft:bricks");
                for (int y = chimneyBase.y(); y < chimneyTop; y++) {
                    put(map, new Vec3i(chimneyBase.x(), y, chimneyBase.z()), brick, MaterialRole.WALL_PRIMARY);
                }
                put(map, new Vec3i(chimneyBase.x(), chimneyTop, chimneyBase.z()), BlockIdentifier.of("minecraft:campfire[lit=true]"), MaterialRole.DECORATION);
            }
        }

        // 8. Live Terrain Adaptation (foundations, stilts, retaining walls, cut-and-fill)
        if (heightmap != null) {
            com.x64Dynamics.tfb.terrain.FoundationStrategy effectiveStrategy =
                foundationStrategy != null ? foundationStrategy : com.x64Dynamics.tfb.terrain.FoundationStrategy.STEPPED_TERRACE;
            var terrainResult = com.x64Dynamics.tfb.terrain.TerrainAdapter.adapt(bounds, heightmap, effectiveStrategy, palette);
            for (PlacementTarget ft : terrainResult.foundationPlacements()) {
                map.putIfAbsent(ft.pos(), ft);
            }
            for (PlacementTarget fill : terrainResult.fillPlacements()) {
                map.putIfAbsent(fill.pos(), fill);
            }
            for (Vec3i ex : terrainResult.excavations()) {
                map.putIfAbsent(ex, new PlacementTarget(ex, BlockIdentifier.AIR, MaterialRole.AIR));
            }
        }

        List<PlacementTarget> resultTargets = new ArrayList<>(map.values());
        BoundingBox3i totalBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX - 1, minY - 1, minZ - 1),
            new Vec3i(maxX + 1, wallTopY + roofHeight + 2, maxZ + (def.addPorchAndGarden() ? 7 : 1))
        );
        BuildingPlan.ValidationReport validation = BuildingPlanValidator.validate(def, floorPlans, stairwellPlans, resultTargets);
        return new BuildingPlan(def, floorPlans, stairwellPlans, resultTargets, totalBounds, validation);
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
