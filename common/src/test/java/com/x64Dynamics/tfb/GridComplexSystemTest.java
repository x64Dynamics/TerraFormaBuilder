package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.grid.*;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.ui.camera.ArchitectCameraController;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GridComplexSystemTest {

    @Test
    public void testGridSnappingOccupancyAndCellStates() {
        GridManager grid = GridManager.defaultProjectGrid(new Vec3i(100, 64, 200));
        grid.setCellSize(2);
        Vec3i snapped = grid.snapToGrid(new Vec3i(105, 70, 207));
        assertEquals(new Vec3i(104, 70, 206), snapped);

        grid.setCellState(5, 5, GridManager.CellState.BLOCKED);
        assertEquals(GridManager.CellState.BLOCKED, grid.getCellState(5, 5));
    }

    @Test
    public void testPlacementRulesOverlapHeightAndExact5x5Footprint() {
        GridManager grid = GridManager.defaultProjectGrid(new Vec3i(0, 64, 0));
        BuildingRegistry registry = new BuildingRegistry();
        PlacementRuleEngine ruleEngine = new PlacementRuleEngine(PlacementRuleEngine.PlacementRules.defaults());
        ConnectionEngine connEngine = new ConnectionEngine();
        StructurePlacementController ctrl = new StructurePlacementController(grid, registry, ruleEngine, connEngine);

        GridBuilding b1 = new GridBuilding(
            "cottage_5x5", 2, 2, 64, 5, 5, 1, 4,
            GridBuilding.FootprintShape.SQUARE, Direction.SOUTH,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.GABLE,
            GridBuilding.BuildingFunction.RESIDENTIAL, false, 11L
        );
        assertTrue(ctrl.placeBuilding(b1).valid());

        // Overlapping building must be rejected
        GridBuilding overlap = new GridBuilding(
            "overlap", 4, 4, 64, 6, 6, 1, 4,
            GridBuilding.FootprintShape.RECTANGLE, Direction.NORTH,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.GABLE,
            GridBuilding.BuildingFunction.RESIDENTIAL, false, 22L
        );
        var overlapRes = ctrl.placeBuilding(overlap);
        assertFalse(overlapRes.valid());
        assertTrue(overlapRes.errors().get(0).contains("overlaps"));

        // Rotation & move
        assertTrue(ctrl.rotateBuilding("cottage_5x5"));
        assertEquals(Direction.WEST, b1.facing());
        assertTrue(ctrl.moveBuilding("cottage_5x5", 3, 3, 64).valid());
    }

    @Test
    public void testConnectedComplexGenerationAndVariation() {
        GridManager grid = GridManager.defaultProjectGrid(new Vec3i(0, 64, 0));
        BuildingRegistry registry = new BuildingRegistry();
        PlacementRuleEngine ruleEngine = new PlacementRuleEngine(PlacementRuleEngine.PlacementRules.defaults());
        ConnectionEngine connEngine = new ConnectionEngine();
        StructurePlacementController ctrl = new StructurePlacementController(grid, registry, ruleEngine, connEngine);

        // Side-by-side touching residential buildings -> DOORWAY
        GridBuilding mainHouse = new GridBuilding(
            "main", 2, 2, 64, 9, 9, 2, 4,
            GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.GABLE,
            GridBuilding.BuildingFunction.RESIDENTIAL, false, 100L
        );
        GridBuilding sideHouse = new GridBuilding(
            "side", 11, 2, 64, 7, 9, 2, 4,
            GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.HIP,
            GridBuilding.BuildingFunction.RESIDENTIAL, false, 200L
        );
        // Nearby tower with 2-cell gap and different height -> ELEVATED_WALKWAY
        GridBuilding tower = new GridBuilding(
            "tower", 20, 2, 64, 7, 7, 3, 4,
            GridBuilding.FootprintShape.SQUARE, Direction.WEST,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.GABLE,
            GridBuilding.BuildingFunction.TOWER, false, 300L
        );
        // Stacked upper penthouse over mainHouse (flat roof) -> VERTICAL_STACK
        GridBuilding stacked = new GridBuilding(
            "upper", 2, 2, 78, 7, 7, 1, 4,
            GridBuilding.FootprintShape.SQUARE, Direction.SOUTH,
            StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.FLAT,
            GridBuilding.BuildingFunction.RESIDENTIAL, false, 400L
        );

        assertTrue(ctrl.placeBuilding(mainHouse).valid());
        assertTrue(ctrl.placeBuilding(sideHouse).valid());
        assertTrue(ctrl.placeBuilding(tower).valid());
        assertTrue(ctrl.placeBuilding(stacked).valid());

        var complex = ctrl.generateComplex();
        assertTrue("Complex should generate >= 500 blocks", complex.targets().size() >= 500);
        assertTrue(complex.connections().stream().anyMatch(c -> c.type() == ConnectionEngine.ConnectionType.DOORWAY));
        assertTrue(complex.connections().stream().anyMatch(c -> c.type() == ConnectionEngine.ConnectionType.ELEVATED_WALKWAY));
        assertTrue(complex.connections().stream().anyMatch(c -> c.type() == ConnectionEngine.ConnectionType.VERTICAL_STACK));

        // Batched grid renderer output
        var batch = GridRendererModel.buildBatch(grid, registry, new GridManager.GridCoord(2, 2), "main", 64);
        assertFalse(batch.lines().isEmpty());
        assertFalse(batch.highlights().isEmpty());
    }

    @Test
    public void testCameraControllerAndProjectPersistence() throws Exception {
        ArchitectCameraController cam = new ArchitectCameraController();
        assertEquals(ArchitectCameraController.CameraViewMode.ISOMETRIC, cam.viewMode());
        cam.setActive(true);
        cam.setViewMode(ArchitectCameraController.CameraViewMode.TOP_DOWN);
        cam.tick(0.5f);
        assertTrue(cam.pitch() > 60.0f);

        cam.focusOn(BoundingBox3i.fromOriginAndSize(new Vec3i(10, 64, 10), 12, 8, 12));
        cam.tick(1.0f);
        assertEquals(16.0, cam.focusTarget().x(), 0.5);

        Path tmp = Files.createTempDirectory("tfb_grid_proj");
        GridProjectManager.setStorageDir(tmp);
        GridManager grid = GridManager.defaultProjectGrid(Vec3i.ZERO);
        BuildingRegistry reg = new BuildingRegistry();
        StructurePlacementController ctrl = new StructurePlacementController(grid, reg, new PlacementRuleEngine(PlacementRuleEngine.PlacementRules.defaults()), new ConnectionEngine());
        ctrl.placeBuilding(new GridBuilding(
            "hall", 1, 1, 64, 9, 9, 1, 4,
            GridBuilding.FootprintShape.L_SHAPED, Direction.EAST,
            StylePreset.TRADITIONAL, null, RoofGenerator.RoofType.HIP,
            GridBuilding.BuildingFunction.CIVIC, false, 77L
        ));
        GridProjectManager.save("estate_alpha", grid, reg);
        var loaded = GridProjectManager.load("estate_alpha").orElseThrow();
        assertEquals(1, loaded.registry().allBuildings().size());
        assertEquals(Direction.EAST, loaded.registry().get("hall").orElseThrow().facing());

        // Courtyard generation test
        loaded.placeBuilding(new GridBuilding(
            "plaza", 11, 1, 64, 9, 9, 1, 4,
            GridBuilding.FootprintShape.SQUARE, Direction.SOUTH,
            StylePreset.TRADITIONAL, null, RoofGenerator.RoofType.FLAT,
            GridBuilding.BuildingFunction.COURTYARD, false, 88L
        ));
        var res = loaded.generateComplex();
        assertTrue(res.targets().size() > 200);

        for (GridComplexPresets.PresetKind kind : GridComplexPresets.PresetKind.values()) {
            GridComplexPresets.populate(kind, loaded, 64, StylePreset.MEDIEVAL, 99L);
            var presetRes = loaded.generateComplex();
            assertTrue("Preset " + kind + " should generate >= 300 blocks", presetRes.targets().size() >= 300);
            assertFalse("Preset " + kind + " should form connections", presetRes.connections().isEmpty());
        }
    }
}
