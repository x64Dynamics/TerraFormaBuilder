package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.architectural.builds.RoadInfrastructurePlanner;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

import com.x64Dynamics.tfb.architectural.exterior.wall.WallSegmenter;
import com.x64Dynamics.tfb.architectural.interior.floor.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.interior.decor.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.interior.decor.InteriorRoom;
import com.x64Dynamics.tfb.architectural.interior.layouts.BuildingSection;
import com.x64Dynamics.tfb.architectural.interior.layouts.ConnectedBuildingComposer;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public final class ProceduralArchitectureAndInfrastructureTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testRegularPolygonRasterization() {
        Vec3i center = new Vec3i(10, 64, 10);
        // Octagon with radius 5, height 3
        List<Vec3i> octagon = ShapeRasterizer.regularPolygon(center, 5, 8, 3, 0.0, false);
        assertFalse(octagon.isEmpty());
        // Verify center is contained
        assertTrue(octagon.contains(center));
        assertTrue(octagon.contains(center.add(0, 1, 0)));
        assertTrue(octagon.contains(center.add(0, 2, 0)));
        // Verify points outside radius are not contained
        assertFalse(octagon.contains(center.add(10, 0, 0)));
    }

    @Test
    public void testRoundedBoxRasterization() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 10, 4, 10);
        List<Vec3i> rounded = ShapeRasterizer.roundedBox(box, 3, false);
        assertFalse(rounded.isEmpty());
        // Center is contained
        assertTrue(rounded.contains(new Vec3i(5, 64, 5)));
        // Extreme sharp outer corner (0, 64, 0) should be clipped by fillet
        assertFalse(rounded.contains(new Vec3i(0, 64, 0)));
    }

    @Test
    public void testRotatedBoxRasterization() {
        Vec3i origin = new Vec3i(0, 64, 0);
        List<Vec3i> rotated = ShapeRasterizer.rotatedBox(origin, 8, 4, 8, 45.0, false);
        assertFalse(rotated.isEmpty());
        assertTrue(rotated.contains(origin));
    }

    @Test
    public void testSplineRoadGeneration() {
        List<Vec3d> controlPoints = Arrays.asList(
            new Vec3d(0, 64, 0),
            new Vec3d(10, 64, 5),
            new Vec3d(20, 64, 15),
            new Vec3d(30, 64, 20)
        );
        SplinePath path = new SplinePath(SplinePath.SplineType.CATMULL_ROM, controlPoints, false);
        List<SplinePath.PathSample> samples = path.sample(6);
        assertFalse(samples.isEmpty());

        // Normal vectors must be perpendicular to tangents on horizontal plane
        for (SplinePath.PathSample sample : samples) {
            double dot = sample.tangent().x() * sample.normal().x() + sample.tangent().z() * sample.normal().z();
            assertEquals(0.0, dot, 1e-4);
        }

        // Generate full spline road with curbs and sidewalks
        RoadInfrastructurePlanner.RoadProfile profile = RoadInfrastructurePlanner.RoadProfile.defaultUrban();
        List<PlacementTarget> roadTargets = RoadInfrastructurePlanner.generateSplineRoad(path, profile, palette);
        assertFalse(roadTargets.isEmpty());
        // Road surface floor targets must be present
        assertTrue(roadTargets.stream().anyMatch(t -> t.role() == MaterialRole.FLOOR));
    }

    @Test
    public void testRoundaboutIntersectionGeneration() {
        Vec3i center = new Vec3i(50, 64, 50);
        RoadInfrastructurePlanner.RoadProfile profile = RoadInfrastructurePlanner.RoadProfile.defaultUrban();
        List<PlacementTarget> roundabout = RoadInfrastructurePlanner.generateIntersection(
            center,
            RoadInfrastructurePlanner.IntersectionType.ROUNDABOUT,
            profile,
            palette
        );
        assertFalse(roundabout.isEmpty());
        // Central island and roadway must exist
        assertTrue(roundabout.stream().anyMatch(t -> t.pos().equals(center)));
    }

    @Test
    public void testConnectedBuildingComposition() {
        // Main hall: [0, 64, 0] to [14, 72, 10]
        BoundingBox3i mainBox = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 15, 9, 11);
        // Attached side wing touching at Z = 10: [10, 64, 10] to [16, 72, 18]
        BoundingBox3i wingBox = BoundingBox3i.fromOriginAndSize(new Vec3i(8, 64, 10), 7, 9, 8);

        List<BuildingSection> sections = Arrays.asList(
            BuildingSection.primaryRectangle(mainBox, RoofGenerator.RoofType.GABLE),
            BuildingSection.attachedWing("side_wing", wingBox, BuildingSection.SectionShape.RECTANGLE, RoofGenerator.RoofType.GABLE)
        );

        ConnectedBuildingComposer.CompositionResult result = ConnectedBuildingComposer.composeCompound(sections, palette);

        assertFalse(result.targets().isEmpty());
        assertFalse(result.portals().isEmpty());

        // Verify that a connection portal is created at the shared interface
        ConnectedBuildingComposer.ConnectionPortal portal = result.portals().get(0);
        assertNotNull(portal);
        assertEquals(64, portal.centerFloor().y());

        // Portal center air space must be carved open (AIR role)
        Vec3i portalAir = portal.centerFloor().add(0, 1, 0);
        assertTrue(result.targets().stream().anyMatch(t -> t.pos().equals(portalAir) && t.role() == MaterialRole.AIR));
    }

    @Test
    public void testWallSegmentationAndRhythm() {
        // Test rhythm for a 19-block wall (as requested in user prompt)
        WallSegmenter.FacadeSegmentation segmentation = WallSegmenter.calculateRhythm(19, 8, true);

        assertEquals(19, segmentation.wallLength());
        assertFalse(segmentation.bays().isEmpty());
        // Must have corner pillars
        assertTrue(segmentation.pillarOffsets().contains(0));
        assertTrue(segmentation.pillarOffsets().contains(18));

        // Must have a central entrance door bay
        assertTrue(segmentation.bays().stream().anyMatch(b -> b.isCenterBay() && b.type() == WallSegmenter.BayType.ENTRANCE_DOOR));

        // Generate full 3D facade
        List<PlacementTarget> facade = WallSegmenter.composeFacade(
            new Vec3i(0, 64, 0),
            Direction.EAST,
            Direction.SOUTH,
            19,
            8,
            true,
            palette
        );
        assertFalse(facade.isEmpty());
        // Must contain pillars, plinth, and entrance
        assertTrue(facade.stream().anyMatch(t -> t.role() == MaterialRole.PILLAR));
        assertTrue(facade.stream().anyMatch(t -> t.role() == MaterialRole.FOUNDATION));
    }

    @Test
    public void testInteriorQuadrantPlanningAndClearance() {
        BoundingBox3i largeInterior = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 16, 5, 16);
        InteriorFloorPlanner.FloorPlanResult result = InteriorFloorPlanner.planFloor(largeInterior, palette);

        // 4 functional rooms created
        assertEquals(4, result.rooms().size());
        assertFalse(result.partitionWalls().isEmpty());
        assertFalse(result.transitClearanceZones().isEmpty());

        // Furnish each room and verify no furnishings violate transit clearance zones
        for (InteriorRoom room : result.rooms()) {
            List<PlacementTarget> furnishings = InteriorFurnishingGenerator.furnishRoom(
                room,
                palette,
                result.transitClearanceZones(),
                0.8f
            );
            for (PlacementTarget target : furnishings) {
                assertFalse("Furnishing placed inside transit clearance zone: " + target.pos(),
                    result.transitClearanceZones().contains(target.pos()));
            }
        }
    }
}
