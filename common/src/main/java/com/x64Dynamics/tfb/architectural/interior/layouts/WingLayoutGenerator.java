package com.x64Dynamics.tfb.architectural.interior.layouts;

import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;

import java.util.ArrayList;
import java.util.List;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.architectural.exterior.facades.OpeningGenerator;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public final class WingLayoutGenerator {
    private WingLayoutGenerator() {}

    public static List<PlacementTarget> generateWingCompound(
        ParametricFootprint footprint,
        RoofGenerator.RoofType roofType,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();

        // Use ConnectedBuildingComposer to resolve shared interfaces, seamless floors, and carved connection portals
        ConnectedBuildingComposer.CompositionResult result = ConnectedBuildingComposer.composeCompound(
            footprint.sections(),
            palette
        );
        targets.addAll(result.targets());

        // Carve primary front entrance door on primary section
        if (!footprint.sections().isEmpty()) {
            BuildingSection primary = footprint.sections().get(0);
            if (primary.bounds().sizeX() >= 5 && primary.bounds().sizeZ() >= 5) {
                int midX = (primary.bounds().min().x() + primary.bounds().max().x()) / 2;
                Vec3i doorCenter = new Vec3i(midX, primary.bounds().min().y(), primary.bounds().max().z());
                targets.addAll(OpeningGenerator.carveDoorway(doorCenter, Direction.SOUTH, 1, 2, palette));
            }
        }

        return targets;
    }
}
