package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.architectural.plan.BuildingDefinition;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlanValidator;
import com.x64Dynamics.tfb.architectural.style.ArchitecturalStyleRuleSet;
import com.x64Dynamics.tfb.architectural.style.StyleRuleSetRegistry;
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
 * respecting exact selection bounds, configurable story heights, architectural style rules,
 * dynamic structural pillar spacing, orientation-aware entrances/porches/balconies/dormers,
 * compound-footprint interiors, and a single roof-aligned chimney.
 */
public final class IntelligentHomeGenerator {
    public record HomeConfig(
        BoundingBox3i bounds,
        MaterialPalette palette,
        RoofGenerator.RoofType roofType,
        int requestedStories,
        int storyHeight,
        StylePreset style,
        Direction facing,
        WingConfiguration wingConfig,
        boolean addPorchAndGarden,
        boolean addChimney,
        boolean furnishInterior,
        float furnishingDensity,
        long seed
    ) {
        public HomeConfig(
            BoundingBox3i bounds,
            MaterialPalette palette,
            RoofGenerator.RoofType roofType,
            int requestedStories,
            WingConfiguration wingConfig,
            boolean addPorchAndGarden,
            boolean addChimney,
            boolean furnishInterior,
            float furnishingDensity,
            long seed
        ) {
            this(bounds, palette, roofType, requestedStories, 4, StylePreset.MEDIEVAL, Direction.SOUTH,
                wingConfig, addPorchAndGarden, addChimney, furnishInterior, furnishingDensity, seed);
        }
    }

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
            config.style(),
            config.palette(),
            config.roofType(),
            config.requestedStories(),
            config.storyHeight(),
            config.facing(),
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
        ArchitecturalStyleRuleSet rules = StyleRuleSetRegistry.forPreset(def.style());
        Direction facing = def.facing();

        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        List<BuildingPlan.FloorPlan> floorPlans = new ArrayList<>();
        List<BuildingPlan.StairwellPlan> stairwellPlans = new ArrayList<>();

        // 1. Respect exact selection footprint (never silently expand)
        int width = Math.max(5, bounds.sizeX());
        int depth = Math.max(5, bounds.sizeZ());
        int minX = bounds.min().x();
        int minY = bounds.min().y();
        int minZ = bounds.min().z();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        // 2. Flexible story height & height-budget planner
        int storyHeight = def.storyHeight();
        int estimatedRoofH = (def.roofType() == RoofGenerator.RoofType.FLAT) ? 1 : Math.max(2, Math.min(width, depth) / 2 + 1);
        int stories = def.requestedStories() > 0
            ? def.requestedStories()
            : Math.max(1, (bounds.sizeY() - estimatedRoofH) / storyHeight);

        int wallHeight = stories * storyHeight + 1;
        int wallTopY = minY + wallHeight - 1;
        int roofHeight = (def.roofType() == RoofGenerator.RoofType.FLAT) ? 1 : Math.max(2, Math.min(width, depth) / 2 + 1);
        int overhang = rules.overhang();

        BoundingBox3i roofBounds = BoundingBox3i.fromCorners(
            new Vec3i(minX, wallTopY + 1, minZ),
            new Vec3i(maxX, wallTopY + roofHeight, maxZ)
        );

        BlockIdentifier subFoundation = palette.get(MaterialRole.SUB_FOUNDATION);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier floorBlock = palette.get(MaterialRole.FLOOR);
        BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);

        // Dynamic structural pillar spacing from style rule set
        int pillarSpacing = Math.max(3, Math.min(8, rules.windowSpacing() * 2));

        ParametricFootprint compoundFootprint = null;
        if (def.wingConfiguration() != WingConfiguration.SINGLE_RECTANGLE) {
            BoundingBox3i compoundBounds = BoundingBox3i.fromCorners(
                new Vec3i(minX, minY, minZ),
                new Vec3i(maxX, wallTopY + roofHeight, maxZ)
            );
            compoundFootprint = ParametricFootprint.create(compoundBounds, def.wingConfiguration());
            for (PlacementTarget wt : WingLayoutGenerator.generateWingCompound(compoundFootprint, def.roofType(), palette)) {
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

            // Exterior facades per story with style-driven beams & dynamic structural pillars
            BlockIdentifier beamX = orientLog(palette.get(MaterialRole.BEAM), Axis.X);
            BlockIdentifier beamZ = orientLog(palette.get(MaterialRole.BEAM), Axis.Z);

            for (int s = 0; s < stories; s++) {
                int storyY = minY + 1 + (s * storyHeight);
                boolean isGround = (s == 0);

                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(minX, storyY, maxZ), Direction.EAST, Direction.SOUTH, width, storyHeight, isGround && facing == Direction.SOUTH, palette, rules)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(maxX, storyY, minZ), Direction.WEST, Direction.NORTH, width, storyHeight, isGround && facing == Direction.NORTH, palette, rules)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(maxX, storyY, maxZ), Direction.NORTH, Direction.EAST, depth, storyHeight, isGround && facing == Direction.EAST, palette, rules)) {
                    put(map, t.pos(), t.block(), t.role());
                }
                for (PlacementTarget t : WallSegmentationEngine.composeFacade(
                    new Vec3i(minX, storyY, minZ), Direction.SOUTH, Direction.WEST, depth, storyHeight, isGround && facing == Direction.WEST, palette, rules)) {
                    put(map, t.pos(), t.block(), t.role());
                }

                if (rules.allowCornice() || def.style() != StylePreset.MODERN && def.style() != StylePreset.MINIMALIST) {
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
            }

            // Corner + intermediate structural pillars
            BlockIdentifier pillarY = orientLog(pillarBlock, Axis.Y);
            for (int y = minY; y <= wallTopY; y++) {
                put(map, new Vec3i(minX, y, minZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(maxX, y, minZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(minX, y, maxZ), pillarY, MaterialRole.PILLAR);
                put(map, new Vec3i(maxX, y, maxZ), pillarY, MaterialRole.PILLAR);

                if (rules.allowPilasters() && width >= 11) {
                    for (int x = minX + pillarSpacing; x <= maxX - pillarSpacing; x += pillarSpacing) {
                        put(map, new Vec3i(x, y, minZ), pillarY, MaterialRole.PILLAR);
                        put(map, new Vec3i(x, y, maxZ), pillarY, MaterialRole.PILLAR);
                    }
                }
                if (rules.allowPilasters() && depth >= 11) {
                    for (int z = minZ + pillarSpacing; z <= maxZ - pillarSpacing; z += pillarSpacing) {
                        put(map, new Vec3i(minX, y, z), pillarY, MaterialRole.PILLAR);
                        put(map, new Vec3i(maxX, y, z), pillarY, MaterialRole.PILLAR);
                    }
                }
            }

            Axis ridgeAxis = (width >= depth) ? Axis.X : Axis.Z;
            for (PlacementTarget rt : RoofGenerator.generateRoof(roofBounds, def.roofType(), ridgeAxis, overhang, palette)) {
                put(map, rt.pos(), rt.block(), rt.role());
            }
        }

        // 3. Orientation-aware 2-block-tall Entrance Door
        Vec3i frontDoorPos = computeFrontDoorPos(minX, minY, minZ, maxX, maxZ, facing);
        BlockIdentifier doorBase = palette.get(MaterialRole.DOOR)
            .withProperty("facing", facing.opposite().name().toLowerCase(Locale.ROOT))
            .withProperty("hinge", "left")
            .withProperty("open", "false");
        put(map, frontDoorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
        put(map, frontDoorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
        putAir(map, frontDoorPos.offset(facing.opposite(), 1));
        putAir(map, frontDoorPos.offset(facing.opposite(), 1).add(0, 1, 0));
        putAir(map, frontDoorPos.offset(facing, 1));
        putAir(map, frontDoorPos.offset(facing, 1).add(0, 1, 0));

        // 4. Upper Story Floor Slabs & Openable Stairwells
        Set<Vec3i> globalClearance = new HashSet<>();
        addDoorClearance(frontDoorPos.offset(facing.opposite(), 1), globalClearance);

        if (def.wingConfiguration() == WingConfiguration.SINGLE_RECTANGLE) {
            for (int s = 1; s < stories; s++) {
                int floorY = minY + (s * storyHeight);
                for (int x = minX + 1; x < maxX; x++) {
                    for (int z = minZ + 1; z < maxZ; z++) {
                        put(map, new Vec3i(x, floorY, z), floorBlock, MaterialRole.FLOOR);
                    }
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

        // 5. Single Roof-Aligned Chimney (no duplicate chimney)
        if (def.addChimney() && width >= 5 && depth >= 5) {
            int chimX = (facing == Direction.EAST) ? minX + 1 : maxX - 1;
            int chimZ = (facing == Direction.NORTH) ? maxZ - 1 : minZ + 1;
            int roofPeakY = wallTopY + roofHeight + 1;
            BlockIdentifier chimneyMasonry = palette.get(MaterialRole.FOUNDATION);
            for (int y = minY + 1; y <= roofPeakY; y++) {
                put(map, new Vec3i(chimX, y, chimZ), chimneyMasonry, MaterialRole.FOUNDATION);
            }
            put(map, new Vec3i(chimX, roofPeakY + 1, chimZ), BlockIdentifier.of("minecraft:campfire[lit=true]"), MaterialRole.DECORATION);
        }

        // 6. Seed-Driven BSP Multi-Story Room Planning & Interior Furnishing (compound-footprint aware!)
        List<BoundingBox3i> interiorFootprints = (compoundFootprint != null)
            ? compoundFootprint.wingBoxes()
            : List.of(bounds);

        for (int s = 0; s < stories; s++) {
            int floorY = minY + 1 + (s * storyHeight);
            int ceilingY = floorY + storyHeight - 2;
            if (ceilingY <= floorY) continue;

            List<BuildingPlan.PlannedRoom> storyRooms = new ArrayList<>();
            List<BuildingPlan.RoomConnection> storyConns = new ArrayList<>();

            for (BoundingBox3i wingBox : interiorFootprints) {
                int wMinX = wingBox.min().x() + 1;
                int wMaxX = wingBox.max().x() - 1;
                int wMinZ = wingBox.min().z() + 1;
                int wMaxZ = wingBox.max().z() - 1;
                if (wMinX > wMaxX || wMinZ > wMaxZ) continue;

                BoundingBox3i storyBox = BoundingBox3i.fromCorners(
                    new Vec3i(wMinX, floorY, wMinZ),
                    new Vec3i(wMaxX, ceilingY, wMaxZ)
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
                storyRooms.addAll(plan.plannedRooms());
                storyConns.addAll(plan.connections());
            }

            Vec3i hallLamp = new Vec3i(minX + width / 2, ceilingY, minZ + depth / 2);
            if (!map.containsKey(hallLamp)) {
                put(map, hallLamp, palette.get(MaterialRole.LIGHT_SOURCE).withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);
            }

            floorPlans.add(new BuildingPlan.FloorPlan(s, floorY, ceilingY, storyRooms, storyConns));
        }

        // 6b. Usable Attic Loft / Upper Roof Room when pitched roof is tall (>= 5 blocks)
        if (def.wingConfiguration() == WingConfiguration.SINGLE_RECTANGLE
            && def.roofType() != RoofGenerator.RoofType.FLAT
            && roofHeight >= 5 && width >= 9 && depth >= 9) {
            int atticFloorY = wallTopY;
            int atticInnerY = wallTopY + 1;
            int atticMaxY = wallTopY + roofHeight - 2;
            if (atticMaxY >= atticInnerY + 1) {
                // Attic floor deck (preserving stairwell opening)
                for (int x = minX + 1; x < maxX; x++) {
                    for (int z = minZ + 1; z < maxZ; z++) {
                        Vec3i fp = new Vec3i(x, atticFloorY, z);
                        if (!globalClearance.contains(fp)) {
                            put(map, fp, floorBlock, MaterialRole.FLOOR);
                        }
                    }
                }
                int inset = 2;
                if (minX + inset <= maxX - inset && minZ + inset <= maxZ - inset) {
                    BoundingBox3i atticBox = BoundingBox3i.fromCorners(
                        new Vec3i(minX + inset, atticInnerY, minZ + inset),
                        new Vec3i(maxX - inset, atticMaxY, maxZ - inset)
                    );
                    Vec3i atticDoor = new Vec3i(minX + width / 2, atticInnerY, minZ + inset);
                    InteriorRoom atticRoom = new InteriorRoom(atticBox, RoomType.ATTIC_LOFT, atticDoor);
                    if (def.furnishInterior()) {
                        for (PlacementTarget ft : InteriorFurnishingGenerator.furnishRoom(atticRoom, palette, globalClearance, def.furnishingDensity())) {
                            put(map, ft.pos(), ft.block(), ft.role());
                        }
                    }
                    // Attach attic loft to the top story's FloorPlan so story count stays exact and all rooms remain reachable
                    int topIdx = floorPlans.size() - 1;
                    if (topIdx >= 0) {
                        var topFp = floorPlans.get(topIdx);
                        var updatedRooms = new ArrayList<>(topFp.rooms());
                        updatedRooms.add(new BuildingPlan.PlannedRoom("attic_loft", topIdx, RoomType.ATTIC_LOFT, atticBox, atticDoor));
                        var updatedConns = new ArrayList<>(topFp.connections());
                        if (!topFp.rooms().isEmpty()) {
                            updatedConns.add(new BuildingPlan.RoomConnection(topFp.rooms().get(0).id(), "attic_loft", atticDoor));
                        }
                        floorPlans.set(topIdx, new BuildingPlan.FloorPlan(topFp.floorIndex(), topFp.floorY(), topFp.ceilingY(), updatedRooms, updatedConns));
                    }
                }
            }
        }

        // 7. Orientation-Aware Entrance Porch, Steps & Style-Driven Shutters
        if (def.addPorchAndGarden() && width >= 7 && depth >= 7) {
            Direction cross = facing.rotateY();
            BlockIdentifier stairIn = palette.get(MaterialRole.STAIR)
                .withProperty("facing", facing.opposite().name().toLowerCase(Locale.ROOT))
                .withProperty("half", "bottom");
            BlockIdentifier slab = palette.get(MaterialRole.SLAB).withProperty("type", "bottom");
            BlockIdentifier fence = palette.get(MaterialRole.FENCE);
            BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

            for (int d = -1; d <= 1; d++) {
                Vec3i p1 = new Vec3i(frontDoorPos.x(), minY, frontDoorPos.z()).offset(facing, 1).offset(cross, d);
                Vec3i p2 = new Vec3i(frontDoorPos.x(), minY, frontDoorPos.z()).offset(facing, 2).offset(cross, d);
                put(map, p1, foundation, MaterialRole.FOUNDATION);
                put(map, p2, stairIn, MaterialRole.STAIR);
                put(map, p1.add(0, storyHeight, 0), slab, MaterialRole.SLAB);
                put(map, p2.add(0, storyHeight, 0), slab, MaterialRole.SLAB);
            }
            for (int y = minY + 1; y < minY + storyHeight; y++) {
                Vec3i postL = new Vec3i(frontDoorPos.x(), y, frontDoorPos.z()).offset(facing, 2).offset(cross, -1);
                Vec3i postR = new Vec3i(frontDoorPos.x(), y, frontDoorPos.z()).offset(facing, 2).offset(cross, 1);
                put(map, postL, fence, MaterialRole.FENCE);
                put(map, postR, fence, MaterialRole.FENCE);
            }
            put(map, new Vec3i(frontDoorPos.x(), minY + storyHeight - 1, frontDoorPos.z()).offset(facing, 1),
                lantern.withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);

            if (facing == Direction.SOUTH) {
                for (PlacementTarget lt : com.x64Dynamics.tfb.terrain.ExteriorLandscapingEngine.generateTerrainApproachAndYard(
                        bounds, frontDoorPos.x(), maxZ, heightmap, palette, def.style(), def.seed())) {
                    put(map, lt.pos(), lt.block(), lt.role());
                }
            }

            if (rules.allowShutters()) {
                BlockIdentifier shutter = palette.get(MaterialRole.FRAME)
                    .withProperty("facing", facing.name().toLowerCase(Locale.ROOT))
                    .withProperty("open", "true")
                    .withProperty("half", "bottom");
                for (int off : new int[]{-3, 3}) {
                    Vec3i wallPos = frontDoorPos.offset(cross, off);
                    if (wallPos.x() > minX && wallPos.x() < maxX && wallPos.z() >= minZ && wallPos.z() <= maxZ) {
                        put(map, wallPos.offset(facing, 1).add(0, 1, 0), shutter, MaterialRole.FRAME);
                    }
                }
            }
        }

        // 8. Style-Gated Second-Floor Balcony & Roof Dormer
        if (def.wingConfiguration() == WingConfiguration.SINGLE_RECTANGLE) {
            if (rules.allowBalconies() && stories >= 2 && width >= 11 && depth >= 11) {
                Vec3i balDoorPos = new Vec3i(frontDoorPos.x(), minY + storyHeight + 1, frontDoorPos.z());
                put(map, balDoorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
                put(map, balDoorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
                for (PlacementTarget bt : FacadeOrnamentGenerator.generateBalcony(balDoorPos, facing, 3, 2, palette)) {
                    put(map, bt.pos(), bt.block(), bt.role());
                }
            }
            if (rules.allowDormers() && width >= 13 && depth >= 13 && def.roofType() == RoofGenerator.RoofType.GABLE) {
                Vec3i dormerPos = new Vec3i(frontDoorPos.x(), wallTopY + 2, frontDoorPos.z()).offset(facing.opposite(), 1);
                for (PlacementTarget dt : AdvancedRoofFeatures.generateDormer(dormerPos, facing, 3, 3, palette)) {
                    put(map, dt.pos(), dt.block(), dt.role());
                }
            }
        }

        // 9. WFC Ornament & Variation Pass (connects WfcSolver + ProceduralVariationEngine to live pipeline)
        if (rules.ornamentDensity() >= 0.5f && width >= 11 && depth >= 11) {
            var wfcRes = com.x64Dynamics.tfb.architectural.wfc.WfcSolver.solve(
                3, 1, 3,
                com.x64Dynamics.tfb.architectural.wfc.WfcSolver.defaultArchitecturalGrammar(),
                Map.of("1,0,1", new com.x64Dynamics.tfb.architectural.wfc.WfcSolver.ModuleVariant("interior_hall", 0)),
                def.seed(), 4
            );
            if (wfcRes.success()) {
                for (int gx = 0; gx < 3; gx++) {
                    String mod = wfcRes.grid()[gx][0][0].moduleId();
                    if ("corner_pillar".equals(mod)) {
                        Vec3i accentPos = new Vec3i(minX + (gx * (width - 1)) / 2, minY + 1, minZ).offset(facing.opposite(), 0);
                        map.putIfAbsent(accentPos, new PlacementTarget(accentPos, palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE));
                    }
                }
            }
        }

        // 10. Live Terrain Adaptation
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
            new Vec3i(minX - overhang, minY - 1, minZ - overhang),
            new Vec3i(maxX + overhang, wallTopY + roofHeight + 2, maxZ + overhang + (def.addPorchAndGarden() && facing == Direction.SOUTH ? 6 : 0))
        );
        BuildingPlan.ValidationReport validation = BuildingPlanValidator.validate(def, floorPlans, stairwellPlans, resultTargets);
        return new BuildingPlan(def, floorPlans, stairwellPlans, resultTargets, totalBounds, validation);
    }

    private static Vec3i computeFrontDoorPos(int minX, int minY, int minZ, int maxX, int maxZ, Direction facing) {
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;
        return switch (facing) {
            case NORTH -> new Vec3i(midX, minY + 1, minZ);
            case EAST -> new Vec3i(maxX, minY + 1, midZ);
            case WEST -> new Vec3i(minX, minY + 1, midZ);
            default -> new Vec3i(midX, minY + 1, maxZ);
        };
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
