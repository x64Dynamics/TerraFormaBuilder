package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.BlockStateNormalizer;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.ui.model.VoxelGridEditorModel;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class InteriorAndGridEditorTest {

    @Test
    public void testBlockStateNormalizer() {
        BlockIdentifier slab = BlockStateNormalizer.normalize(BlockIdentifier.of("minecraft:oak_slab[half=top]"), Direction.NORTH);
        assertEquals("top", slab.property("type"));
        assertNull(slab.property("half"));
        assertEquals("false", slab.property("waterlogged"));

        BlockIdentifier stair = BlockStateNormalizer.normalize(BlockIdentifier.of("minecraft:stone_brick_stairs"), Direction.EAST);
        assertEquals("east", stair.property("facing"));
        assertEquals("bottom", stair.property("half"));
        assertEquals("south", BlockStateNormalizer.rotateClockwise(stair).property("facing"));
    }

    @Test
    public void testOverhauledInteriorsAcrossAllRoomTypes() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();
        BoundingBox3i roomBox = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 7, 5, 7);
        for (RoomType rt : RoomType.values()) {
            InteriorRoom room = new InteriorRoom(roomBox, rt, new Vec3i(3, 64, 0));
            List<PlacementTarget> targets = InteriorFurnishingGenerator.furnishRoom(room, palette, Collections.emptySet(), 1.0f);
            assertFalse("Room " + rt + " should have furnishings", targets.isEmpty());
        }
    }

    @Test
    public void testCoveredWalkwayAndCourtyardCloister() {
        MaterialPalette palette = StylePreset.CLASSICAL.createPalette();
        List<PlacementTarget> bridge = ConnectedBuildingComposer.generateCoveredWalkway(
            new Vec3i(0, 70, 0), new Vec3i(10, 70, 0), 3, 3, palette
        );
        assertTrue(bridge.stream().anyMatch(t -> t.role() == MaterialRole.FLOOR));
        assertTrue(bridge.stream().anyMatch(t -> t.role() == MaterialRole.ROOF_RIDGE));
        assertTrue(bridge.stream().anyMatch(t -> t.role() == MaterialRole.LIGHT_SOURCE));

        List<PlacementTarget> cloister = ConnectedBuildingComposer.generateCourtyardCloister(
            BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 12, 5, 12), 3, palette
        );
        assertTrue(cloister.size() > 100);
    }

    @Test
    public void testVoxelGridEditorSliceMirrorFloodFillAndUndo() {
        VoxelGridEditorModel grid = new VoxelGridEditorModel(8, 8, 8);
        grid.setMirrorX(true);
        grid.setActiveRole(MaterialRole.WALL_PRIMARY);
        grid.applyAt(1, 2);
        assertEquals(MaterialRole.WALL_PRIMARY, grid.cellAt(1, 0, 2));
        assertEquals(MaterialRole.WALL_PRIMARY, grid.cellAt(6, 0, 2));

        assertTrue(grid.undo());
        assertEquals(0, grid.voxelCount());
        assertTrue(grid.redo());
        assertEquals(2, grid.voxelCount());

        grid.setActiveTool(VoxelGridEditorModel.GridTool.EXTRUDE_UP);
        grid.applyAt(0, 0);
        assertEquals(MaterialRole.WALL_PRIMARY, grid.cellAt(0, 7, 0));

        List<PlacementTarget> estate = ConnectedBuildingComposer.generateConnectedEstate(Vec3i.ZERO, StylePreset.MEDIEVAL.createPalette());
        assertTrue("Connected estate should have >= 400 blocks", estate.size() >= 400);

        grid.clearAll();
        grid.setActiveSliceY(0);
        grid.stampHollowRoom(1);
        assertEquals(MaterialRole.FOUNDATION, grid.cellAt(1, 0, 1));
        assertEquals(MaterialRole.FLOOR, grid.cellAt(3, 0, 3));

        grid.fillRect(2, 2, 4, 4);
        var asset = grid.toAuthoredAsset("test_grid", "Test Grid", StylePreset.MEDIEVAL.createPalette());
        assertTrue("AuthoredAsset from grid should have blocks", asset.blocks().size() >= 16);
    }
}
