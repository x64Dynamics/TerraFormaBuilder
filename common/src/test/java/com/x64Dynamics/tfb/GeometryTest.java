package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.geometry.*;
import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

public final class GeometryTest {
    @Test
    public void testVec3iBasicOperations() {
        Vec3i a = new Vec3i(1, 2, 3);
        Vec3i b = new Vec3i(4, 5, 6);
        assertEquals(new Vec3i(5, 7, 9), a.add(b));
        assertEquals(new Vec3i(-3, -3, -3), a.subtract(b));
        assertEquals(new Vec3i(2, 4, 6), a.multiply(2));
        assertEquals(9, a.manhattanDistance(b));
    }

    @Test
    public void testBoundingBoxOperations() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 10, 5, 10);
        assertEquals(10, box.sizeX());
        assertEquals(5, box.sizeY());
        assertEquals(10, box.sizeZ());
        assertEquals(500L, box.volume());
        assertTrue(box.contains(new Vec3i(5, 2, 5)));
        assertFalse(box.contains(new Vec3i(10, 2, 5)));
    }

    @Test
    public void testRasterizerBox() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 3, 3, 3);
        List<Vec3i> solid = ShapeRasterizer.box(box, false);
        assertEquals(27, solid.size());

        List<Vec3i> hollow = ShapeRasterizer.box(box, true);
        assertEquals(26, hollow.size());
        assertFalse(hollow.contains(new Vec3i(1, 1, 1)));
    }

    @Test
    public void testBresenhamLine() {
        Vec3i start = new Vec3i(0, 0, 0);
        Vec3i end = new Vec3i(3, 0, 0);
        List<Vec3i> line = ShapeRasterizer.bresenhamLine(start, end);
        assertEquals(4, line.size());
        assertEquals(start, line.get(0));
        assertEquals(end, line.get(3));
    }

    @Test
    public void testDirectionRotations() {
        assertEquals(Direction.EAST, Direction.NORTH.rotateY());
        assertEquals(Direction.SOUTH, Direction.EAST.rotateY());
        assertEquals(Direction.WEST, Direction.SOUTH.rotateY());
        assertEquals(Direction.NORTH, Direction.WEST.rotateY());
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
    }
}
