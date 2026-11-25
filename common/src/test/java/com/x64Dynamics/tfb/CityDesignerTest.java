package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.city.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
public class CityDesignerTest {

    @Test
    public void testProceduralCityPlanningDistrictsRoadsAndPublicSpaces() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 56, 24, 56);
        WorldProject project = new WorldProject(
            "test-town", "Oakridge", "KyroXova", bounds,
            SettlementPattern.MEDIEVAL_TOWN, StylePreset.MEDIEVAL, 12345L
        );

        CityPlanner.planCity(project);

        assertEquals(4, project.districts().size());
        assertFalse("Road network must have arterial segments", project.roadNetwork().segments().isEmpty());
        assertFalse("Road network must have resolved placements", project.roadNetwork().resolvedTargets().isEmpty());
        assertTrue("City should include market plaza and park", project.publicSpaces().size() >= 2);
        assertTrue("City should have multiple subdivided parcels", project.totalParcelCount() >= 4);

        for (CityDistrict d : project.districts()) {
            for (CityParcel p : d.parcels()) {
                assertNotNull("Every parcel must have a generated BuildingPlan", p.buildingPlan());
                assertTrue("Every parcel BuildingPlan must be valid", p.buildingPlan().validation().valid());
            }
        }

        List<PlacementTarget> allTargets = project.resolveAllTargets();
        assertTrue("Resolved city must contain substantial block geometry", allTargets.size() > 1500);
    }

    @Test
    public void testSelectiveParcelRegenerationAndLockPreservation() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 48, 24, 48);
        WorldProject project = new WorldProject(
            "lock-test", "Riverbend", "KyroXova", bounds,
            SettlementPattern.PLANNED_GRID, StylePreset.MODERN, 777L
        );
        CityPlanner.planCity(project);

        CityParcel firstParcel = project.districts().stream()
            .flatMap(d -> d.parcels().stream())
            .findFirst()
            .orElseThrow();

        long originalSeed = firstParcel.seed();
        firstParcel.setLocked(true);

        // Locked parcel must refuse regeneration
        assertFalse(CityPlanner.regenerateParcel(project, firstParcel.id(), 9999L));
        assertEquals(originalSeed, firstParcel.seed());

        // Unlock and regenerate
        firstParcel.setLocked(false);
        assertTrue(CityPlanner.regenerateParcel(project, firstParcel.id(), 9999L));
        assertEquals(9999L, firstParcel.seed());
    }

    @Test
    public void testWorldProjectStorageAndWorkspaceCityMode() throws Exception {
        Path tempDir = Files.createTempDirectory("tfb_city_proj_test");
        WorldProjectStorage.setProjectsDir(tempDir);

        ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
        model.createNewCity("Harborview", SettlementPattern.ORGANIC_VILLAGE, 48, 48);
        assertTrue(model.isCityMode());
        assertNotNull(model.activeCityProject());
        assertTrue(model.totalBlockCount() > 1000);

        WorldProjectStorage.save(model.activeCityProject());
        assertTrue(WorldProjectStorage.listSavedProjects().contains("Harborview"));

        WorldProject loaded = WorldProjectStorage.load("Harborview").orElseThrow();
        assertEquals("Harborview", loaded.name());
        assertEquals(SettlementPattern.ORGANIC_VILLAGE, loaded.pattern());
        assertEquals(model.activeCityProject().totalParcelCount(), loaded.totalParcelCount());
    }

    @Test
    public void testWaterfrontDockAndBuildScopeSelection() {
        BoundingBox3i dockBox = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 10, 4, 8);
        PublicSpace dock = new PublicSpace("pub_dock", "Harbor Pier", PublicSpace.PublicSpaceType.WATERFRONT_DOCK, dockBox);
        List<PlacementTarget> dockTargets = PublicSpaceGenerator.generate(dock, StylePreset.MEDIEVAL.createPalette(), 42L);
        assertTrue(dockTargets.stream().anyMatch(t -> t.block().path().equals("spruce_planks")));
        assertTrue(dockTargets.stream().anyMatch(t -> t.block().path().equals("barrel")));

        ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
        model.createNewCity("ScopeCity", SettlementPattern.PLANNED_GRID, 48, 48);
        int fullCount = model.totalBlockCount();

        var firstDistrict = model.activeCityProject().districts().get(0);
        var firstParcel = firstDistrict.parcels().get(0);
        model.setSelectedDistrictId(firstDistrict.id());
        model.setSelectedParcelId(firstParcel.id());

        model.setBuildScope(ArchitectWorkspaceModel.BuildScope.SELECTED_PARCEL);
        int parcelCount = model.totalBlockCount();
        assertTrue("Selected parcel scope should be smaller than full city", parcelCount > 0 && parcelCount < fullCount);

        model.setBuildScope(ArchitectWorkspaceModel.BuildScope.SELECTED_DISTRICT);
        int districtCount = model.totalBlockCount();
        assertTrue("Selected district scope should be >= parcel and <= full city", districtCount >= parcelCount && districtCount <= fullCount);
    }

    @Test
    public void testTerrainGradedCityPlanningWithHeightmap() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 48, 24, 48);
        com.x64Dynamics.tfb.terrain.HeightmapGrid grid = new com.x64Dynamics.tfb.terrain.HeightmapGrid(bounds);
        for (int z = 0; z < 48; z++) {
            for (int x = 0; x < 48; x++) {
                grid.setElevation(x, z, 64 + (x / 12));
            }
        }
        WorldProject project = new WorldProject(
            "hillside-town", "Terrace", "KyroXova", bounds,
            SettlementPattern.MEDIEVAL_TOWN, StylePreset.MEDIEVAL, 555L
        );
        CityPlanner.planCity(project, grid);
        assertFalse(project.districts().isEmpty());
        int minParcelY = project.districts().stream()
            .flatMap(d -> d.parcels().stream())
            .mapToInt(p -> p.bounds().min().y())
            .min().orElse(64);
        int maxParcelY = project.districts().stream()
            .flatMap(d -> d.parcels().stream())
            .mapToInt(p -> p.bounds().min().y())
            .max().orElse(64);
        assertTrue("Sloped heightmap should place parcels at varying base elevations", maxParcelY > minParcelY);
    }
}
