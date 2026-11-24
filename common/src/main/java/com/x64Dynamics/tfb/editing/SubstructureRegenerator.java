package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.architectural.AdvancedRoofFeatures;
import com.x64Dynamics.tfb.architectural.FacadeOrnamentGenerator;
import com.x64Dynamics.tfb.architectural.FloorGenerator;
import com.x64Dynamics.tfb.architectural.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.InteriorRoom;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WallGenerator;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;

public final class SubstructureRegenerator {
    private SubstructureRegenerator() {}

    public static List<PlacementTarget> regenerateRoof(
        List<PlacementTarget> existingTargets,
        BoundingBox3i roofBounds,
        RoofGenerator.RoofType roofType,
        Axis ridgeAxis,
        int overhang,
        MaterialPalette palette
    ) {
        List<PlacementTarget> result = new ArrayList<>();
        // Retain non-roof blocks
        for (PlacementTarget target : existingTargets) {
            MaterialRole role = target.role();
            if (role != MaterialRole.ROOF_SLOPE && role != MaterialRole.ROOF_RIDGE && role != MaterialRole.ROOF_GABLE && role != MaterialRole.ROOF_OVERHANG) {
                result.add(target);
            }
        }
        // Generate new roof
        result.addAll(RoofGenerator.generateRoof(roofBounds, roofType, ridgeAxis, overhang, palette));
        return result;
    }

    public static List<PlacementTarget> regenerateWalls(
        List<PlacementTarget> existingTargets,
        BoundingBox3i wallBounds,
        int thickness,
        boolean crenellated,
        MaterialPalette palette
    ) {
        List<PlacementTarget> result = new ArrayList<>();
        for (PlacementTarget target : existingTargets) {
            MaterialRole role = target.role();
            if (role != MaterialRole.WALL_PRIMARY && role != MaterialRole.WALL_ACCENT && role != MaterialRole.WALL_CORNER) {
                result.add(target);
            }
        }
        result.addAll(WallGenerator.generateBoxWalls(wallBounds, thickness, crenellated, palette));
        return result;
    }

    public static List<PlacementTarget> regenerateInteriors(
        List<PlacementTarget> existingTargets,
        BoundingBox3i interiorBounds,
        boolean furnish,
        MaterialPalette palette
    ) {
        List<PlacementTarget> result = new ArrayList<>();
        for (PlacementTarget target : existingTargets) {
            if (target.role() != MaterialRole.DECORATION && target.role() != MaterialRole.LIGHT_SOURCE) {
                result.add(target);
            }
        }
        InteriorFloorPlanner.FloorPlanResult plan = InteriorFloorPlanner.planFloor(interiorBounds, palette);
        result.addAll(plan.partitionWalls());
        if (furnish) {
            for (InteriorRoom room : plan.rooms()) {
                result.addAll(InteriorFurnishingGenerator.furnishRoom(room, palette));
            }
        }
        return result;
    }
}