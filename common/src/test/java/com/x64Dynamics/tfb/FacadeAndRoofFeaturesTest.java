package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

import com.x64Dynamics.tfb.architectural.exterior.facades.FacadeOrnamentGenerator;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofFeatures;
public final class FacadeAndRoofFeaturesTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testPilasterGeneration() {
        BoundingBox3i wall = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 10, 5, 1);
        List<PlacementTarget> pilasters = FacadeOrnamentGenerator.generatePilasters(wall, Direction.NORTH, 3, palette);
        assertFalse(pilasters.isEmpty());
    }

    @Test
    public void testBalconyGeneration() {
        Vec3i doorCenter = new Vec3i(5, 5, 0);
        List<PlacementTarget> balcony = FacadeOrnamentGenerator.generateBalcony(doorCenter, Direction.SOUTH, 3, 2, palette);
        assertFalse(balcony.isEmpty());
    }

    @Test
    public void testChimneyGeneration() {
        Vec3i roofPt = new Vec3i(3, 10, 3);
        List<PlacementTarget> chimney = RoofFeatures.generateChimney(roofPt, 4, palette);
        assertFalse(chimney.isEmpty());
        // Verify campfire top is placed
        assertTrue(chimney.stream().anyMatch(t -> t.block().path().equals("campfire")));
    }
}
