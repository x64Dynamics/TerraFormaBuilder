package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.IntelligentHomeGenerator;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import static org.junit.Assert.*;

public class HomeGeneratorOverhaulTest {

    @Test
    public void testRespectsExact5x5SelectionWithoutExpanding() {
        var bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 5, 8, 5);
        var cfg = new IntelligentHomeGenerator.HomeConfig(
            bounds, StylePreset.MEDIEVAL.createPalette(), RoofGenerator.RoofType.GABLE,
            1, 4, StylePreset.MEDIEVAL, Direction.SOUTH, WingConfiguration.SINGLE_RECTANGLE,
            false, true, true, 1.0f, 101L
        );
        var res = IntelligentHomeGenerator.generateHome(cfg);
        assertEquals(1, res.stories());
        for (PlacementTarget t : res.targets()) {
            if (t.role() == MaterialRole.WALL_PRIMARY || t.role() == MaterialRole.FOUNDATION) {
                assertTrue("5x5 selection must not expand X to 7x7", t.pos().x() >= 0 && t.pos().x() <= 4);
                assertTrue("5x5 selection must not expand Z to 7x7", t.pos().z() >= 0 && t.pos().z() <= 4);
            }
        }
    }

    @Test
    public void testSizes9x9_15x15_31x31AndConfigurableStoryHeight() {
        for (int size : new int[]{9, 15, 31}) {
            var bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), size, 24, size);
            var cfg = new IntelligentHomeGenerator.HomeConfig(
                bounds, StylePreset.TRADITIONAL.createPalette(), RoofGenerator.RoofType.HIP,
                2, 5, StylePreset.TRADITIONAL, Direction.SOUTH, WingConfiguration.SINGLE_RECTANGLE,
                true, true, true, 1.0f, 202L + size
            );
            var res = IntelligentHomeGenerator.generateHome(cfg);
            assertEquals(2, res.stories());
            assertTrue(res.plan().validation().valid());
            assertEquals("Single chimney should have exactly 1 rooftop campfire",
                1, res.targets().stream().filter(t -> t.block().path().equals("campfire") && t.pos().y() >= 74).count());
        }
    }

    @Test
    public void testOrientationAwareEntranceAndModernStyleRules() {
        var bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 13, 14, 13);
        var northModern = new IntelligentHomeGenerator.HomeConfig(
            bounds, StylePreset.MODERN.createPalette(), RoofGenerator.RoofType.FLAT,
            2, 4, StylePreset.MODERN, Direction.NORTH, WingConfiguration.SINGLE_RECTANGLE,
            true, false, true, 1.0f, 303L
        );
        var res = IntelligentHomeGenerator.generateHome(northModern);
        assertTrue("Door should be placed on NORTH facade (z=0)",
            res.targets().stream().anyMatch(t -> t.role() == MaterialRole.DOOR && t.pos().z() == 0));
        assertEquals("Modern flat roof has 0 overhang",
            0, res.targets().stream().filter(t -> t.pos().x() == -1 || t.pos().x() == 13).count());
    }

    @Test
    public void testMultiWingCompoundFootprintInteriors() {
        var bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 15, 14, 15);
        for (WingConfiguration wing : new WingConfiguration[]{WingConfiguration.L_SHAPED, WingConfiguration.U_SHAPED}) {
            var cfg = new IntelligentHomeGenerator.HomeConfig(
                bounds, StylePreset.FANTASY.createPalette(), RoofGenerator.RoofType.MANSARD,
                2, 4, StylePreset.FANTASY, Direction.EAST, wing,
                false, true, true, 1.0f, 404L
            );
            var res = IntelligentHomeGenerator.generateHome(cfg);
            assertTrue("Compound " + wing + " should generate rooms", res.roomCount() >= 2);
            assertTrue(res.targets().stream().anyMatch(t -> t.role() == MaterialRole.DOOR && t.pos().x() == 14));
        }
    }
}
