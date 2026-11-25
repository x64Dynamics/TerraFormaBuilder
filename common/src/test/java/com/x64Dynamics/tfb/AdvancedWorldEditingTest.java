package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.interior.facades.LayeredFacadeComposer;
import com.x64Dynamics.tfb.architectural.interior.layouts.ParametricFootprint;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.architectural.interior.RoomType;
import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.architectural.interior.layouts.WingLayoutGenerator;
import com.x64Dynamics.tfb.architectural.style.ArchitecturalStyleRuleSet;
import com.x64Dynamics.tfb.architectural.style.ProceduralVariator;
import com.x64Dynamics.tfb.architectural.style.StyleRuleSetRegistry;
import com.x64Dynamics.tfb.construction.editing.ArchitecturalComponentType;
import com.x64Dynamics.tfb.construction.editing.ClipboardManager;
import com.x64Dynamics.tfb.construction.editing.SelectableComponent;
import com.x64Dynamics.tfb.construction.editing.SubstructureRegenerator;
import com.x64Dynamics.tfb.construction.editing.SelectionManager;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.construction.preview.PreviewFilterMode;
import com.x64Dynamics.tfb.construction.preview.PreviewVoxel;
import com.x64Dynamics.tfb.construction.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.material.taxonomy.BlockTaxonomyEntry;
import com.x64Dynamics.tfb.material.taxonomy.BlockTrait;
import com.x64Dynamics.tfb.material.taxonomy.ColorTone;
import com.x64Dynamics.tfb.material.taxonomy.MaterialSubstance;
import com.x64Dynamics.tfb.material.taxonomy.PaletteHarmonizer;
import com.x64Dynamics.tfb.material.taxonomy.SearchableTaxonomyBrowser;
import com.x64Dynamics.tfb.architectural.terrain.LandscapeAdapter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AdvancedWorldEditingTest {

    @Test
    public void testWorldSelectionManager() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(0, 64, 0), new Vec3i(15, 73, 15));
        SelectionManager manager = new SelectionManager(box);

        assertEquals(SelectionManager.SelectionMode.ENTIRE_BUILDING, manager.mode());
        manager.setMode(SelectionManager.SelectionMode.COMPONENT_LEVEL);
        assertEquals(SelectionManager.SelectionMode.COMPONENT_LEVEL, manager.mode());

        // Grid snapping
        manager.setGridSnap(4);
        assertEquals(4, manager.gridSnap());

        // Resize east face by 8
        manager.resizePrimaryBox(Direction.EAST, 8);
        assertEquals(24, manager.primaryBox().sizeX());

        // Translation
        manager.translatePrimaryBox(new Vec3i(4, 0, 4));
        assertEquals(4, manager.primaryBox().min().x());

        // Component selection
        SelectableComponent comp = new SelectableComponent("roof-1", ArchitecturalComponentType.ROOF, box, Collections.emptyList());
        manager.setComponents(Collections.singletonList(comp));
        assertEquals(1, manager.allComponents().size());

        manager.selectComponent(comp, false);
        assertTrue(comp.isSelected());
        assertEquals(1, manager.selectedComponents().size());

        manager.clearSelection();
        assertFalse(comp.isSelected());
        assertEquals(0, manager.selectedComponents().size());
    }

    @Test
    public void testClipboardManager() {
        ClipboardManager clipboard = ClipboardManager.getInstance();
        List<PlacementTarget> targets = new ArrayList<>();
        targets.add(new PlacementTarget(new Vec3i(10, 64, 10), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY));
        targets.add(new PlacementTarget(new Vec3i(10, 65, 10), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY));

        clipboard.copy(targets, new Vec3i(10, 64, 10));
        assertTrue(clipboard.hasContent());
        assertNotNull(clipboard.currentEntry());
        assertEquals(1, clipboard.currentEntry().width());
        assertEquals(2, clipboard.currentEntry().height());

        List<PlacementTarget> pasted = clipboard.paste(new Vec3i(20, 70, 20), Rotation.NONE, null);
        assertEquals(2, pasted.size());
        assertEquals(new Vec3i(20, 70, 20), pasted.get(0).pos());
        assertEquals(new Vec3i(20, 71, 20), pasted.get(1).pos());

        List<PlacementTarget> dup = ClipboardManager.duplicate(targets, new Vec3i(5, 0, 5));
        assertEquals(new Vec3i(15, 64, 15), dup.get(0).pos());
    }

    @Test
    public void testSubstructureRegenerator() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        BoundingBox3i box = new BoundingBox3i(new Vec3i(0, 64, 0), new Vec3i(10, 74, 10));

        List<PlacementTarget> targets = new ArrayList<>();
        targets.add(new PlacementTarget(new Vec3i(0, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY));
        targets.add(new PlacementTarget(new Vec3i(0, 70, 0), BlockIdentifier.of("minecraft:oak_stairs"), MaterialRole.ROOF_SLOPE));

        List<PlacementTarget> newRoof = SubstructureRegenerator.regenerateRoof(targets, box, RoofGenerator.RoofType.GABLE, Axis.X, 1, palette);
        assertNotNull(newRoof);
        assertTrue(newRoof.size() > targets.size());
    }

    @Test
    public void testWorldPreviewBuffer() {
        WorldPreviewBuffer buffer = new WorldPreviewBuffer();
        List<PlacementTarget> targets = new ArrayList<>();
        targets.add(new PlacementTarget(new Vec3i(0, 64, 0), BlockIdentifier.of("minecraft:cobblestone"), MaterialRole.FOUNDATION));
        targets.add(new PlacementTarget(new Vec3i(0, 65, 0), BlockIdentifier.of("minecraft:stone_bricks"), MaterialRole.WALL_PRIMARY));
        targets.add(new PlacementTarget(new Vec3i(0, 70, 0), BlockIdentifier.of("minecraft:oak_stairs"), MaterialRole.ROOF_SLOPE));

        buffer.updateFromTargets(targets, null);
        assertEquals(3, buffer.totalVoxelCount());

        buffer.setFilterMode(PreviewFilterMode.FOUNDATIONS_ONLY);
        List<PreviewVoxel> foundations = buffer.getFilteredVoxels();
        assertEquals(1, foundations.size());
        assertEquals(MaterialRole.FOUNDATION, foundations.get(0).role());

        buffer.setFilterMode(PreviewFilterMode.WALLS_OPENINGS);
        List<PreviewVoxel> walls = buffer.getFilteredVoxels();
        assertEquals(1, walls.size());
        assertEquals(MaterialRole.WALL_PRIMARY, walls.get(0).role());

        buffer.setFilterMode(PreviewFilterMode.ALL);
        assertEquals(3, buffer.getFilteredVoxels().size());
    }

    @Test
    public void testParametricFootprintAndWings() {
        BoundingBox3i mainBox = new BoundingBox3i(new Vec3i(0, 64, 0), new Vec3i(15, 73, 15));

        ParametricFootprint lFootprint = ParametricFootprint.create(mainBox, WingConfiguration.L_SHAPED);
        assertEquals(WingConfiguration.L_SHAPED, lFootprint.configuration());
        assertEquals(2, lFootprint.wingBoxes().size());

        ParametricFootprint uFootprint = ParametricFootprint.create(mainBox, WingConfiguration.U_SHAPED);
        assertEquals(3, uFootprint.wingBoxes().size());

        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        List<PlacementTarget> wingPlacements = WingLayoutGenerator.generateWingCompound(lFootprint, RoofGenerator.RoofType.GABLE, palette);
        assertNotNull(wingPlacements);
        assertTrue(wingPlacements.size() > 50);
    }

    @Test
    public void testLayeredFacadeComposer() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(0, 64, 0), new Vec3i(10, 72, 10));
        MaterialPalette palette = StylePreset.TRADITIONAL.createPalette();

        List<PlacementTarget> cornice = LayeredFacadeComposer.generateCorniceTrim(box, palette);
        assertNotNull(cornice);
        assertTrue(cornice.size() > 0);

        List<PlacementTarget> portico = LayeredFacadeComposer.generateEntrancePortico(new Vec3i(5, 64, 10), Direction.SOUTH, palette);
        assertNotNull(portico);
        assertTrue(portico.size() > 0);
    }

    @Test
    public void testArchitecturalStyleRulesAndVariation() {
        ArchitecturalStyleRuleSet medieval = StyleRuleSetRegistry.forPreset(StylePreset.MEDIEVAL);
        assertEquals(RoofGenerator.RoofType.GABLE, medieval.preferredRoofType());
        assertTrue(medieval.allowPilasters());
        assertTrue(medieval.allowShutters());

        ArchitecturalStyleRuleSet modern = StyleRuleSetRegistry.forPreset(StylePreset.MODERN);
        assertEquals(RoofGenerator.RoofType.FLAT, modern.preferredRoofType());
        assertFalse(modern.allowPilasters());

        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        List<PlacementTarget> wall = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            wall.add(new PlacementTarget(new Vec3i(i, 64, 0), BlockIdentifier.of("minecraft:stone_bricks"), MaterialRole.WALL_PRIMARY));
        }

        List<PlacementTarget> varied = ProceduralVariator.applyFacadeVariation(wall, palette, 12345L, 0.5f);
        assertEquals(wall.size(), varied.size());
    }

    @Test
    public void testSearchableTaxonomyAndHarmony() {
        List<BlockTaxonomyEntry> stoneMatches = SearchableTaxonomyBrowser.search("stone", MaterialSubstance.STONE, null, null);
        assertNotNull(stoneMatches);
        assertTrue(stoneMatches.size() > 0);

        MaterialPalette medievalPalette = StylePreset.MEDIEVAL.createPalette();
        float score = PaletteHarmonizer.scoreHarmony(medievalPalette);
        assertTrue(score >= 0.5f);

        MaterialPalette generated = PaletteHarmonizer.completePalette("minecraft:deepslate_bricks");
        assertNotNull(generated);
        assertEquals("minecraft:deepslate_bricks", generated.get(MaterialRole.WALL_PRIMARY).namespacedId());
    }

    @Test
    public void testLandscapeAdapter() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(0, 64, 0), new Vec3i(9, 64, 9));
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

        List<PlacementTarget> courtyard = LandscapeAdapter.generateCourtyard(box, palette);
        assertNotNull(courtyard);
        assertTrue(courtyard.size() > 0);

        List<PlacementTarget> retaining = LandscapeAdapter.generateRetainingWall(box, 3, palette);
        assertNotNull(retaining);
        assertTrue(retaining.size() > 0);
    }
}