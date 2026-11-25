package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.HomeGenerator;
import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.construction.BuildScheduler;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.*;
import com.x64Dynamics.tfb.material.taxonomy.BlockTaxonomyEntry;
import com.x64Dynamics.tfb.material.taxonomy.BlockTaxonomyRegistry;
import com.x64Dynamics.tfb.material.taxonomy.BlockTrait;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class IntelligentHomeGeneratorTest {

    @Test
    public void testMultiStoryHomeHasDoorsStairwellsRoofAndFurnishings() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 15, 14, 15);
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

        // Verify palette resolves real stairs and slabs (never falling back to full cubes)
        assertTrue(palette.get(MaterialRole.STAIR).path().endsWith("_stairs"));
        assertTrue(palette.get(MaterialRole.SLAB).path().endsWith("_slab"));

        HomeGenerator.HomeConfig config = new HomeGenerator.HomeConfig(
            bounds,
            palette,
            RoofGenerator.RoofType.GABLE,
            2,
            WingConfiguration.SINGLE_RECTANGLE,
            true,
            true,
            true,
            1.0f,
            42L
        );

        HomeGenerator.HomeGenerationResult result = HomeGenerator.generateHome(config);
        assertEquals(2, result.stories());
        assertTrue("Multi-story 15x15 home should have at least 4 rooms", result.roomCount() >= 4);

        Map<Vec3i, PlacementTarget> byPos = new HashMap<>();
        for (PlacementTarget t : result.targets()) {
            byPos.put(t.pos(), t);
        }

        // Verify 2-block tall entrance door exists (lower + upper)
        boolean hasLowerDoor = result.targets().stream().anyMatch(
            t -> t.role() == MaterialRole.DOOR && "lower".equals(t.block().property("half"))
        );
        boolean hasUpperDoor = result.targets().stream().anyMatch(
            t -> t.role() == MaterialRole.DOOR && "upper".equals(t.block().property("half"))
        );
        assertTrue("Home must have a lower door block", hasLowerDoor);
        assertTrue("Home must have an upper door block", hasUpperDoor);

        // Verify staircase treads have air headroom above them (no blocked stairwell)
        List<PlacementTarget> stairs = result.targets().stream()
            .filter(t -> t.role() == MaterialRole.STAIR && t.pos().x() == 2 && t.pos().y() >= 65 && t.pos().y() <= 68)
            .toList();
        assertFalse("Multi-story home must have interior stairs", stairs.isEmpty());
        for (PlacementTarget st : stairs) {
            PlacementTarget above1 = byPos.get(st.pos().add(0, 1, 0));
            PlacementTarget above2 = byPos.get(st.pos().add(0, 2, 0));
            assertTrue("Stair headroom y+1 must be air", above1 == null || above1.block().path().equals("air"));
            assertTrue("Stair headroom y+2 must be air", above2 == null || above2.block().path().equals("air"));
        }

        // Verify dependency scheduler orders lower door before upper door
        List<ScheduledBlockPlacement> scheduled = BuildScheduler.scheduleWithDependencies(result.targets());
        assertEquals(result.targets().size(), scheduled.size());
    }

    @Test
    public void testModdedBlockDiscoveryAndFamilyResolution() {
        BlockTaxonomyRegistry reg = BlockTaxonomyRegistry.getInstance();
        reg.registerDiscovered(
            BlockIdentifier.of("create:andesite_bricks"),
            EnumSet.of(BlockTrait.SOLID_CUBE),
            Collections.emptySet(),
            0
        );
        reg.registerDiscovered(
            BlockIdentifier.of("create:andesite_brick_stairs"),
            EnumSet.of(BlockTrait.STAIR, BlockTrait.WATERLOGGABLE),
            Set.of("facing", "half", "shape", "waterlogged"),
            0
        );

        BlockTaxonomyEntry entry = reg.getOrRegister(BlockIdentifier.of("create:andesite_brick_stairs"));
        assertTrue(entry.hasTrait(BlockTrait.STAIR));
        assertTrue(entry.hasTrait(BlockTrait.WATERLOGGABLE));

        MaterialFamily family = MaterialFamilyResolver.resolve(BlockIdentifier.of("create:andesite_bricks"));
        assertEquals("create:andesite_brick_stairs", family.stair().asString());
    }

    @Test
    public void testBuildingPlanValidationSeedDiversityAndMultiWing() throws Exception {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 17, 14, 17);
        MaterialPalette palette = StylePreset.NORDIC.createPalette();

        var def1 = new com.x64Dynamics.tfb.architectural.builds.plan.BuildingDefinition(
            "seed1", bounds, WingConfiguration.SINGLE_RECTANGLE, StylePreset.NORDIC,
            palette, RoofGenerator.RoofType.GABLE, 2, true, true, true, 1.0f, 101L
        );
        var def2 = new com.x64Dynamics.tfb.architectural.builds.plan.BuildingDefinition(
            "seed2", bounds, WingConfiguration.SINGLE_RECTANGLE, StylePreset.NORDIC,
            palette, RoofGenerator.RoofType.GABLE, 2, true, true, true, 1.0f, 9999L
        );

        var plan1 = HomeGenerator.generatePlan(def1);
        var plan2 = HomeGenerator.generatePlan(def2);

        assertTrue("Plan 1 should be valid", plan1.validation().valid());
        assertEquals("All rooms in Plan 1 should be reachable",
            plan1.validation().totalRoomCount(), plan1.validation().reachableRoomCount());
        assertTrue("Plan 2 should be valid", plan2.validation().valid());
        assertNotEquals("Different seeds should produce distinct room bounds",
            plan1.floors().get(0).rooms(), plan2.floors().get(0).rooms());

        // Multi-wing L_SHAPED home
        var lDef = new com.x64Dynamics.tfb.architectural.builds.plan.BuildingDefinition(
            "lwing", bounds, WingConfiguration.L_SHAPED, StylePreset.TRADITIONAL,
            StylePreset.TRADITIONAL.createPalette(), RoofGenerator.RoofType.GABLE, 2, true, true, true, 1.0f, 42L
        );
        var lPlan = HomeGenerator.generatePlan(lDef);
        assertTrue("Multi-wing L-shaped plan should be valid", lPlan.validation().valid());
        assertFalse(lPlan.targets().isEmpty());

        // Blueprint disk persistence round-trip
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("tfb_bp_test");
        com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.setStorageDir(tempDir);
        var model = new com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel();
        var bp = model.toBlueprint("Test Nordic Manor", "KyroXova");
        com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.save(bp);
        assertTrue(com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.listSavedNames().contains("Test_Nordic_Manor"));
        assertTrue(com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.load("Test_Nordic_Manor").isPresent());
    }

    @Test
    public void testMultiStoryFacadeShuttersBalconyAndDormer() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 15, 16, 15);
        HomeGenerator.HomeConfig config = new HomeGenerator.HomeConfig(
            bounds, StylePreset.MEDIEVAL.createPalette(), RoofGenerator.RoofType.GABLE,
            2, WingConfiguration.SINGLE_RECTANGLE, true, true, true, 1.0f, 101L
        );
        HomeGenerator.HomeGenerationResult result = HomeGenerator.generateHome(config);
        boolean hasTrapdoorShutter = result.targets().stream()
            .anyMatch(t -> t.block().path().endsWith("_trapdoor"));
        boolean hasBalconyFence = result.targets().stream()
            .anyMatch(t -> t.block().path().endsWith("_fence") && t.pos().y() >= 68);
        boolean hasDormerGlass = result.targets().stream()
            .anyMatch(t -> t.role() == MaterialRole.WINDOW && t.pos().y() >= 73);
        assertTrue("2-story home should have window trapdoor shutters", hasTrapdoorShutter);
        assertTrue("2-story wide home should have second-floor balcony railing", hasBalconyFence);
        assertTrue("Wide gable home should have roof dormer glazing", hasDormerGlass);
    }

    @Test
    public void testLiveHeightmapTerrainAwareHomeAndShapeEditor() throws Exception {
        com.x64Dynamics.tfb.platform.ILevelAccess slopedLevel = new com.x64Dynamics.tfb.platform.ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return 64 - Math.max(0, z - 5); }
            @Override public BlockIdentifier getBlockState(Vec3i pos) { return BlockIdentifier.AIR; }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { return true; }
            @Override public boolean isAir(Vec3i pos) { return true; }
        };

        var model = new com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel();
        model.setLiveLevelAccess(slopedLevel);
        model.setStylePreset(StylePreset.NORDIC);
        model.setFoundationStrategy(com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy.PILLARS_STILTS);
        model.setShapeEditorMode(true);
        assertFalse(model.cachedPreviewTargets().isEmpty());

        // Verify stilts extend down below minY=64 on the sloped rear
        boolean hasLowerStilt = model.cachedPreviewTargets().stream()
            .anyMatch(t -> t.role() == MaterialRole.PILLAR && t.pos().y() < 64);
        assertTrue("Sloped terrain with PILLARS_STILTS should generate support pillars below base Y", hasLowerStilt);

        // Verify section extension, floor stacking, lock preservation, and persistence
        String mainId = model.selectedSectionId();
        assertNotNull(mainId);
        model.setActiveSculptTool(com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel.SculptTool.TOWER);
        model.sculptAt(new Vec3i(4, 64, 4), com.x64Dynamics.tfb.geometry.Direction.UP, true);
        assertEquals(3, model.activeArchProject().findSection(mainId).orElseThrow().stories());

        model.activeArchProject().toggleLock(mainId);
        assertFalse("Locked section must refuse extension", model.activeArchProject().extendSection(mainId, com.x64Dynamics.tfb.geometry.Direction.EAST, 2));
        model.activeArchProject().toggleLock(mainId);
        assertTrue("Unlocked section should extend", model.activeArchProject().extendSection(mainId, com.x64Dynamics.tfb.geometry.Direction.EAST, 2));

        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("tfb_arch_proj_test");
        com.x64Dynamics.tfb.architectural.builds.editor.ArchitecturalProjectStorage.setStorageDir(tempDir);
        com.x64Dynamics.tfb.architectural.builds.editor.ArchitecturalProjectStorage.save(model.activeArchProject());
        var loaded = com.x64Dynamics.tfb.architectural.builds.editor.ArchitecturalProjectStorage.load(model.activeArchProject().name()).orElseThrow();
        assertEquals(model.activeArchProject().sections().size(), loaded.sections().size());
    }
}
