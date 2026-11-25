package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

import com.x64Dynamics.tfb.architectural.exterior.wall.WallGenerator;
import com.x64Dynamics.tfb.architectural.interior.floor.FloorGenerator;
import com.x64Dynamics.tfb.architectural.interior.layouts.StaircaseGenerator;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public final class ArchitecturalTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testWallGeneration() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 5, 4, 5);
        List<PlacementTarget> walls = WallGenerator.generateBoxWalls(box, 1, true, palette);
        assertFalse(walls.isEmpty());
        // Verify hollow interior (center block at level 1 is not in wall)
        assertFalse(walls.stream().anyMatch(t -> t.pos().equals(new Vec3i(2, 1, 2))));
    }

    @Test
    public void testFloorGeneration() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 4, 1, 4);
        List<PlacementTarget> floor = FloorGenerator.generateFloor(box, 0, true, palette);
        assertEquals(16, floor.size());
    }

    @Test
    public void testRoofGableGeneration() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 10, 0), 7, 1, 7);
        List<PlacementTarget> roof = RoofGenerator.generateRoof(box, RoofGenerator.RoofType.GABLE, Axis.X, 0, palette);
        assertFalse(roof.isEmpty());
    }

    @Test
    public void testStaircaseStraight() {
        Vec3i start = new Vec3i(0, 0, 0);
        List<PlacementTarget> stairs = StaircaseGenerator.generateStraight(start, Direction.NORTH, 5, 2, palette);
        assertEquals(10, stairs.size());
    }
}
