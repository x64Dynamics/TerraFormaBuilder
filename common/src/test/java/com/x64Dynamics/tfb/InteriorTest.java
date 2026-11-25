package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

import com.x64Dynamics.tfb.architectural.interior.floor.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.interior.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.interior.InteriorRoom;
import com.x64Dynamics.tfb.architectural.interior.RoomType;
public final class InteriorTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testInteriorFloorPlanningSubdivision() {
        BoundingBox3i interior = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 12, 4, 12);
        InteriorFloorPlanner.FloorPlanResult result = InteriorFloorPlanner.planFloor(interior, palette);

        assertEquals(2, result.rooms().size());
        assertFalse(result.partitionWalls().isEmpty());
    }

    @Test
    public void testFurnishingLivingHall() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 6, 4, 6);
        InteriorRoom room = new InteriorRoom(bounds, RoomType.LIVING_HALL, new Vec3i(3, 0, 0));

        List<PlacementTarget> furniture = InteriorFurnishingGenerator.furnishRoom(room, palette);
        assertFalse(furniture.isEmpty());
        // Verify chandelier lantern is placed
        assertTrue(furniture.stream().anyMatch(t -> t.block().path().contains("lantern")));
    }

    @Test
    public void testFurnishingLibrary() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 6, 4, 6);
        InteriorRoom room = new InteriorRoom(bounds, RoomType.LIBRARY_STUDY, new Vec3i(3, 0, 0));

        List<PlacementTarget> furniture = InteriorFurnishingGenerator.furnishRoom(room, palette);
        assertFalse(furniture.isEmpty());
        assertTrue(furniture.stream().anyMatch(t -> t.block().path().contains("bookshelf")));
    }
}
