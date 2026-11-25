package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.facade.OpeningGenerator;
import com.x64Dynamics.tfb.architectural.facade.WallSegmenter;
import com.x64Dynamics.tfb.architectural.interior.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.interior.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.interior.InteriorRoom;
import com.x64Dynamics.tfb.architectural.interior.RoomType;
import com.x64Dynamics.tfb.architectural.layout.StaircaseGenerator;
import com.x64Dynamics.tfb.architectural.roof.AdvancedRoofFeatures;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.grid.*;
import com.x64Dynamics.tfb.material.*;
import com.x64Dynamics.tfb.network.C2SConstructionPacket;
import com.x64Dynamics.tfb.network.ServerBuildPermissionValidator;
import com.x64Dynamics.tfb.network.ServerTickBuildExecutor;
import com.x64Dynamics.tfb.persistence.SchematicExporter;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.platform.IPlayerAccess;
import com.x64Dynamics.tfb.settlement.ProceduralSettlementGenerator;
import com.x64Dynamics.tfb.terrain.ExteriorLandscaper;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.terrain.TerrainAdapter;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class RegressionAndCleanupTest {

    @Test
    public void testCubicBezierFourControlPointsSingleSegment() {
        SplinePath bezier = new SplinePath(
            SplinePath.SplineType.CUBIC_BEZIER,
            List.of(new Vec3d(0, 0, 0), new Vec3d(0, 0, 5), new Vec3d(10, 0, 5), new Vec3d(10, 0, 10)),
            false
        );
        var samples = bezier.sample(5);
        assertEquals(6, samples.size());
        assertEquals(10.0, samples.get(samples.size() - 1).position().x(), 1e-4);
    }

    @Test
    public void testDoorwayExactEvenWidthAndWindowGuard() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        var doorTargets = OpeningGenerator.carveDoorway(new Vec3i(5, 64, 5), Direction.SOUTH, 2, 2, palette);
        long doorBlocks = doorTargets.stream().filter(t -> t.role() == MaterialRole.DOOR).count();
        assertEquals(4, doorBlocks); // 2 wide x 2 tall

        var badWindows = OpeningGenerator.generateWindows(
            BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 10, 5, 1), 0, 0, 2, 1, palette
        );
        assertTrue(badWindows.isEmpty());
    }

    @Test
    public void testConicalRoofOn1x1BoundsAndPaletteChimney() {
        MaterialPalette palette = StylePreset.MINIMALIST.createPalette();
        palette.set(MaterialRole.FOUNDATION, "minecraft:sandstone");
        palette.set(MaterialRole.SLAB, "minecraft:sandstone_slab");

        var cone = RoofGenerator.generateRoof(
            BoundingBox3i.fromOriginAndSize(new Vec3i(0, 70, 0), 1, 1, 1),
            RoofGenerator.RoofType.CONICAL, Axis.X, 0, palette
        );
        assertEquals(1, cone.size());

        var chimney = AdvancedRoofFeatures.generateChimney(new Vec3i(0, 70, 0), 3, palette);
        assertTrue(chimney.stream().anyMatch(t -> t.block().path().equals("sandstone")));
        assertFalse(chimney.stream().anyMatch(t -> t.block().path().equals("bricks")));
    }

    @Test
    public void testInteriorBedSkipsBothHalvesWhenDoorwayOverlapsFoot() {
        MaterialPalette palette = StylePreset.RUSTIC.createPalette();
        InteriorRoom bedroom = new InteriorRoom(
            BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 5, 4, 5),
            RoomType.BEDROOM,
            new Vec3i(0, 64, 1)
        );
        Set<Vec3i> clearance = Set.of(new Vec3i(0, 64, 1));
        var furnished = InteriorFurnishingGenerator.furnishRoom(bedroom, palette, clearance, 1.0f);
        assertFalse(furnished.stream().anyMatch(t -> t.block().path().endsWith("_bed")));
    }

    @Test
    public void testSmallHouseInteriorNotAllHallways() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        var plan = InteriorFloorPlanner.planStoryWithGraph(
            BoundingBox3i.fromOriginAndSize(new Vec3i(1, 64, 1), 9, 4, 9),
            0, 1, palette, 123L
        );
        assertTrue(plan.rooms().size() >= 2);
        assertTrue(plan.rooms().stream().anyMatch(r -> r.type() != RoomType.LIVING_HALL && r.type() != RoomType.HALLWAY));
    }

    @Test
    public void testStaircaseAscendingFacing() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        var stairs = StaircaseGenerator.generateStraight(Vec3i.ZERO, Direction.NORTH, 3, 1, palette);
        assertTrue(stairs.stream().allMatch(t -> "north".equals(t.block().property("facing"))));
    }

    @Test
    public void testExteriorLandscaperRespectsNorthEntrance() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 9, 6, 9);
        var yard = ExteriorLandscaper.generateTerrainApproachAndYard(
            box, 4, 0, null, palette, StylePreset.MEDIEVAL, 99L
        );
        assertTrue(yard.stream().anyMatch(t -> t.role() == MaterialRole.FLOOR && t.pos().z() <= -3));
    }

    @Test
    public void testSettlementNorthAndWestPlotsClearCentralPlaza() {
        MaterialPalette palette = StylePreset.RUSTIC.createPalette();
        var plan = ProceduralSettlementGenerator.generateSettlement(new Vec3i(0, 64, 0), 32, 8, palette);
        BoundingBox3i plaza = BoundingBox3i.fromCorners(new Vec3i(-3, 60, -3), new Vec3i(3, 80, 3));
        for (var plot : plan.buildingPlots()) {
            assertFalse("Plot " + plot.bounds() + " overlapped plaza", plot.bounds().intersects(plaza));
        }
    }

    @Test
    public void testGridRotateRollsBackOutOfBounds() {
        GridManager grid = new GridManager(new Vec3i(0, 64, 0), 1, 10, 10, -64, 320);
        StructurePlacementController ctrl = new StructurePlacementController(
            grid, new BuildingRegistry(),
            new PlacementValidator(PlacementValidator.PlacementRules.defaults()),
            new ComplexConnectionPlanner()
        );
        GridBuilding edge2 = new GridBuilding(
            "edge2", 0, 5, 64, 8, 5, 1, 4,
            GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH,
            StylePreset.MEDIEVAL, StylePreset.MEDIEVAL.createPalette(),
            RoofGenerator.RoofType.GABLE, GridBuilding.BuildingFunction.RESIDENTIAL, false, 2L
        );
        assertTrue(ctrl.placeBuilding(edge2).valid());
        assertFalse(ctrl.rotateBuilding("edge2"));
        assertEquals(Direction.SOUTH, edge2.facing());
        assertEquals(8, edge2.width());
    }

    @Test
    public void testTerrainAdapterNullHeightmapAndSchematicEmptyJson() throws IOException {
        var res = TerrainAdapter.adapt(
            BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 5, 5, 5),
            null, FoundationStrategy.SOLID_SUB_BASEMENT, StylePreset.MEDIEVAL.createPalette()
        );
        assertTrue(res.foundationPlacements().isEmpty());

        Path tmp = Files.createTempFile("tfb-empty", ".json");
        try {
            Files.writeString(tmp, "");
            assertThrows(IOException.class, () -> SchematicExporter.loadJson(tmp));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testServerExecutorDoesNotConsumeResourceWhenSetBlockFails() {
        UUID pid = UUID.randomUUID();
        int[] stoneCount = {1};
        IPlayerAccess player = new IPlayerAccess() {
            @Override public UUID getUuid() { return pid; }
            @Override public String getTeamId() { return "builders"; }
            @Override public boolean isCreative() { return false; }
            @Override public boolean hasResource(BlockIdentifier b, int count) { return stoneCount[0] >= count; }
            @Override public boolean consumeResource(BlockIdentifier b, int count) {
                if (stoneCount[0] >= count) { stoneCount[0] -= count; return true; }
                return false;
            }
        };
        ILevelAccess failingLevel = new ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return 64; }
            @Override public BlockIdentifier getBlockState(Vec3i pos) { return BlockIdentifier.AIR; }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { return false; }
            @Override public boolean isAir(Vec3i pos) { return true; }
        };

        var exec = new ServerTickBuildExecutor(ServerBuildPermissionValidator.defaultOverworld(), 10);
        var pkt = new C2SConstructionPacket(
            UUID.randomUUID(), pid, new Vec3i(0, 64, 0),
            List.of(new PlacementTarget(new Vec3i(0, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.FOUNDATION)),
            true
        );
        assertTrue(exec.submit(player, pkt).accepted());
        exec.tick(failingLevel, null, null);
        assertEquals(1, stoneCount[0]);
    }

    @Test
    public void testWallSegmenterUsesValidSlabTypeProperty() {
        var targets = WallSegmenter.composeFacade(
            new Vec3i(0, 64, 0), Direction.EAST, Direction.SOUTH,
            9, 8, false, StylePreset.MEDIEVAL.createPalette()
        );
        assertFalse(targets.stream().anyMatch(t -> t.block().property("half") != null && t.role() == MaterialRole.SLAB));
    }
}
