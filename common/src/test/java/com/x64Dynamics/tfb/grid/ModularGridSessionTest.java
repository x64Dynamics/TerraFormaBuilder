package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ModularGridSessionTest {
    @Test
    public void single5x5x5ModuleGeneratesWallsFloorDoorAndRoof() {
        ModularGridSession session = new ModularGridSession(new Vec3i(0, 64, 0), 5, 5);
        session.placeModule(0, 0, 0, StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.GABLE);

        List<PlacementTarget> targets = session.buildPlacementTargets();
        assertFalse(targets.isEmpty());
        assertTrue(targets.stream().anyMatch(t -> t.role() == MaterialRole.FOUNDATION));
        assertTrue(targets.stream().anyMatch(t -> t.role() == MaterialRole.WALL_PRIMARY));
        assertTrue(targets.stream().anyMatch(t -> t.role() == MaterialRole.DOOR));
        assertTrue(targets.stream().anyMatch(t -> t.role() == MaterialRole.ROOF_RIDGE || t.role() == MaterialRole.ROOF_SLOPE));
    }

    @Test
    public void adjacentHorizontalModulesConnectAndOpenSharedWall() {
        ModularGridSession session = new ModularGridSession(new Vec3i(0, 64, 0), 5, 5);
        session.placeModule(0, 0, 0, StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.FLAT);
        session.placeModule(1, 0, 0, StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.FLAT);

        Map<Vec3i, PlacementTarget> byPos = session.buildPlacementTargets().stream()
            .collect(Collectors.toMap(PlacementTarget::pos, t -> t, (a, b) -> b));

        // Shared boundary at x = 4 (east wall of cell 0) and x = 5 (west wall of cell 1) must be open interior air
        for (int z = 1; z <= 3; z++) {
            assertEquals(MaterialRole.AIR, byPos.get(new Vec3i(4, 65, z)).role());
            assertEquals(MaterialRole.AIR, byPos.get(new Vec3i(5, 65, z)).role());
        }
    }

    @Test
    public void stackingModuleOnTopConvertsLowerIntoFloorSectionAndMovesRoofUp() {
        ModularGridSession session = new ModularGridSession(new Vec3i(0, 64, 0), 5, 5);
        session.placeModule(0, 0, 0, StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.FLAT);
        session.stackOrPlace(0, 0, StylePreset.MEDIEVAL, null, RoofGenerator.RoofType.FLAT);

        assertEquals(1, session.highestFloorAt(0, 0));

        List<PlacementTarget> targets = session.buildPlacementTargets();
        // Lower module had roof at y = 69 (64 + 5); after stacking, y = 69 must be FLOOR/AIR, not ROOF_SLOPE/ROOF_RIDGE
        assertTrue(targets.stream().noneMatch(t -> t.pos().y() == 69 && (t.role() == MaterialRole.ROOF_SLOPE || t.role() == MaterialRole.ROOF_RIDGE)));
        assertTrue(targets.stream().anyMatch(t -> t.pos().y() == 69 && t.role() == MaterialRole.FLOOR));
        assertTrue(targets.stream().anyMatch(t -> t.role() == MaterialRole.STAIR));
        // New top module at gy=1 (y=69..73) gets the roof at y = 74
        assertTrue(targets.stream().anyMatch(t -> t.pos().y() == 74 && (t.role() == MaterialRole.ROOF_SLOPE || t.role() == MaterialRole.ROOF_RIDGE)));

        // Removing the top floor restores the roof on gy=0
        assertTrue(session.removeTopModule(0, 0));
        List<PlacementTarget> afterRemove = session.buildPlacementTargets();
        assertTrue(afterRemove.stream().anyMatch(t -> t.pos().y() == 69 && (t.role() == MaterialRole.ROOF_SLOPE || t.role() == MaterialRole.ROOF_RIDGE)));
    }

    @Test
    public void inWorldGridModePlacesAndStacksViaWorldClicks() {
        com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel model = new com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel();
        model.setOrigin(new Vec3i(0, 64, 0));
        model.setInWorldGridMode(true);
        assertEquals(1, model.modularGrid().modules().size());
        assertFalse(model.cachedPreviewTargets().isEmpty());

        // Clicking on same 5x5 cell stacks floor 2 on top
        model.setPos1(new Vec3i(2, 64, 2));
        assertEquals(2, model.modularGrid().modules().size());
        assertEquals(1, model.modularGrid().highestFloorAt(0, 0));

        // Right-click removes top floor
        model.setPos2(new Vec3i(2, 64, 2));
        assertEquals(1, model.modularGrid().modules().size());
        assertEquals(0, model.modularGrid().highestFloorAt(0, 0));
    }
}
