package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class Phase2ArchitectureTest {

    @Test
    public void testDogLegAndBifurcatedImperialStaircases() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

        List<PlacementTarget> dogLeg = StaircaseGenerator.generateDogLeg(Vec3i.ZERO, Direction.NORTH, 8, 2, palette);
        assertFalse(dogLeg.isEmpty());
        assertTrue(dogLeg.stream().anyMatch(t -> t.role() == MaterialRole.FLOOR));
        assertTrue(dogLeg.stream().anyMatch(t -> t.role() == MaterialRole.BALUSTRADE));
        assertEquals(7, dogLeg.stream().filter(t -> t.role() == MaterialRole.STAIR).mapToInt(t -> t.pos().y()).max().orElse(-1));

        List<PlacementTarget> imperial = StaircaseGenerator.generateBifurcatedImperial(Vec3i.ZERO, Direction.SOUTH, 8, 4, palette);
        assertFalse(imperial.isEmpty());
        assertTrue(imperial.stream().filter(t -> t.role() == MaterialRole.STAIR).count() >= 16);
        assertTrue(imperial.stream().anyMatch(t -> t.role() == MaterialRole.FLOOR));
    }

    @Test
    public void testMultiSegmentPolygonalAndCircularTowersWithBattlements() {
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

        var octConfig = new TowerGenerator.TowerConfig(TowerGenerator.TowerShape.OCTAGONAL, 5, 14, 3, true, true, true);
        List<PlacementTarget> octTower = TowerGenerator.generateTower(Vec3i.ZERO, octConfig, palette);
        assertTrue(octTower.stream().anyMatch(t -> t.role() == MaterialRole.FOUNDATION));
        assertTrue(octTower.stream().anyMatch(t -> t.role() == MaterialRole.BEAM));
        assertTrue(octTower.stream().anyMatch(t -> t.role() == MaterialRole.WINDOW));
        assertTrue(octTower.stream().anyMatch(t -> t.role() == MaterialRole.SLAB));
        assertTrue(octTower.stream().anyMatch(t -> t.role() == MaterialRole.LIGHT_SOURCE));

        List<PlacementTarget> curtain = TowerGenerator.generateConnectedBattlementWall(new Vec3i(0, 0, 0), new Vec3i(12, 0, 0), 6, palette);
        assertTrue(curtain.stream().anyMatch(t -> t.pos().y() == 8 && t.role() == MaterialRole.SLAB));
    }

    @Test
    public void testUrbanIntersectionCrosswalksAndTrafficLights() {
        MaterialPalette palette = StylePreset.MODERN.createPalette();
        List<PlacementTarget> fourWay = RoadInfrastructureEngine.generateIntersection(
            Vec3i.ZERO, RoadInfrastructureEngine.IntersectionType.FOUR_WAY, RoadInfrastructureEngine.RoadProfile.defaultUrban(), palette
        );
        assertTrue("Urban intersection should include white concrete crosswalks",
            fourWay.stream().anyMatch(t -> t.block().asString().contains("white_concrete")));
        assertTrue("Urban intersection should include traffic light signals",
            fourWay.stream().anyMatch(t -> t.block().asString().contains("red_concrete")));
    }

    @Test
    public void testFlutedPilastersPedimentAndRhythmicWindows() {
        MaterialPalette palette = StylePreset.CLASSICAL.createPalette();
        BoundingBox3i wall = BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 15, 6, 1);

        List<PlacementTarget> array = FacadeOrnamentGenerator.generateRhythmicWindowArray(wall, Direction.SOUTH, 4, 2, palette);
        assertTrue(array.stream().anyMatch(t -> t.role() == MaterialRole.PILLAR));
        assertTrue(array.stream().anyMatch(t -> t.role() == MaterialRole.WINDOW));
        assertTrue(array.stream().anyMatch(t -> t.role() == MaterialRole.BEAM));

        List<PlacementTarget> pediment = FacadeOrnamentGenerator.generatePedimentCornice(new Vec3i(7, 6, 0), Direction.SOUTH, 5, palette);
        assertTrue(pediment.stream().anyMatch(t -> t.role() == MaterialRole.ROOF_RIDGE));
        assertTrue(pediment.stream().anyMatch(t -> t.role() == MaterialRole.ROOF_SLOPE));
    }
}
