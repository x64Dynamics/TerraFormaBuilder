package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.brush.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public final class BrushTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testRegistryHasAllBrushes() {
        assertNotNull(BrushRegistry.get("room"));
        assertNotNull(BrushRegistry.get("house"));
        assertNotNull(BrushRegistry.get("tower"));
        assertNotNull(BrushRegistry.get("wall"));
        assertNotNull(BrushRegistry.get("bridge"));
        assertNotNull(BrushRegistry.get("road"));
        assertNotNull(BrushRegistry.get("tunnel"));
        assertTrue(BrushRegistry.all().size() >= 26);
    }

    @Test
    public void testHouseBrushGeneration() {
        IConstructionBrush house = BrushRegistry.get("house");
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 9, 8, 9);
        BrushContext ctx = new BrushContext(box, Collections.emptyList(), palette, null, null, null);

        List<PlacementTarget> targets = house.generate(ctx);
        assertFalse(targets.isEmpty());
    }

    @Test
    public void testRoadBrushGeneration() {
        IConstructionBrush road = BrushRegistry.get("road");
        List<Vec3i> path = List.of(new Vec3i(0, 64, 0), new Vec3i(10, 64, 5), new Vec3i(20, 64, 10));
        BrushContext ctx = new BrushContext(BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 20, 1, 10), path, palette, null, null, null);

        List<PlacementTarget> targets = road.generate(ctx);
        assertFalse(targets.isEmpty());
    }
}
