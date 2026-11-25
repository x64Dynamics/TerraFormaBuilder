package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.construction.editing.ConstructionTransforms;
import com.x64Dynamics.tfb.construction.editing.DesignHistorySnapshot;
import com.x64Dynamics.tfb.construction.editing.DesignHistoryStack;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.architectural.terrain.FoundationStrategy;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public final class EditingTest {
    @Test
    public void testRotationWithFacingProperties() {
        PlacementTarget target = new PlacementTarget(
            new Vec3i(5, 0, 0),
            BlockIdentifier.of("minecraft:oak_stairs[facing=north]"),
            MaterialRole.ROOF_SLOPE
        );

        List<PlacementTarget> rotated = ConstructionTransforms.rotate(List.of(target), Rotation.CLOCKWISE_90, new Vec3i(0, 0, 0));
        assertEquals(1, rotated.size());
        assertEquals(new Vec3i(0, 0, 5), rotated.get(0).pos());
        assertEquals("east", rotated.get(0).block().property("facing"));
    }

    @Test
    public void testMirroring() {
        PlacementTarget target = new PlacementTarget(
            new Vec3i(2, 5, 3),
            BlockIdentifier.of("minecraft:stone"),
            MaterialRole.WALL_PRIMARY
        );

        List<PlacementTarget> mirrored = ConstructionTransforms.mirror(List.of(target), Axis.X, 0);
        assertEquals(new Vec3i(-2, 5, 3), mirrored.get(0).pos());
    }

    @Test
    public void testMaterialReplacement() {
        PlacementTarget target = new PlacementTarget(
            new Vec3i(0, 0, 0),
            BlockIdentifier.of("minecraft:stone_bricks"),
            MaterialRole.WALL_PRIMARY
        );

        List<PlacementTarget> replaced = ConstructionTransforms.replaceMaterial(
            List.of(target), "stone_bricks", BlockIdentifier.of("minecraft:deepslate_bricks")
        );
        assertEquals("deepslate_bricks", replaced.get(0).block().path());
    }

    @Test
    public void testDesignHistoryUndoRedo() {
        DesignHistoryStack stack = new DesignHistoryStack(10);
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 5, 5, 5);

        DesignHistorySnapshot s1 = new DesignHistorySnapshot("house", box, "medieval", FoundationStrategy.PILLARS_STILTS, null);
        DesignHistorySnapshot s2 = new DesignHistorySnapshot("tower", box, "medieval", FoundationStrategy.PILLARS_STILTS, null);

        stack.push(s1);
        assertFalse(stack.canUndo());

        stack.push(s2);
        assertTrue(stack.canUndo());

        Optional<DesignHistorySnapshot> undone = stack.undo();
        assertTrue(undone.isPresent());
        assertEquals("house", undone.get().brushId());
        assertTrue(stack.canRedo());

        Optional<DesignHistorySnapshot> redone = stack.redo();
        assertTrue(redone.isPresent());
        assertEquals("tower", redone.get().brushId());
    }
}
