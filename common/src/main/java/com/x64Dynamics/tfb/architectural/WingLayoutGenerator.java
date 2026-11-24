package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;

public final class WingLayoutGenerator {
    private WingLayoutGenerator() {}

    public static List<PlacementTarget> generateWingCompound(
        ParametricFootprint footprint,
        RoofGenerator.RoofType roofType,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();

        for (BoundingBox3i wingBox : footprint.wingBoxes()) {
            int roofHeight = Math.max(3, wingBox.sizeY() / 3);
            int wallHeight = wingBox.sizeY() - roofHeight;

            BoundingBox3i wallBounds = BoundingBox3i.fromOriginAndSize(wingBox.min(), wingBox.sizeX(), wallHeight, wingBox.sizeZ());
            BoundingBox3i roofBounds = BoundingBox3i.fromOriginAndSize(wingBox.min().add(0, wallHeight, 0), wingBox.sizeX(), roofHeight, wingBox.sizeZ());

            // 1. Floors
            targets.addAll(FloorGenerator.generateFloor(wallBounds, wallBounds.min().y(), true, palette));

            // 2. Walls
            targets.addAll(WallGenerator.generateBoxWalls(wallBounds, 1, false, palette));

            // 3. Roof
            Axis ridge = (wingBox.sizeX() >= wingBox.sizeZ()) ? Axis.X : Axis.Z;
            targets.addAll(RoofGenerator.generateRoof(roofBounds, roofType, ridge, 1, palette));

            // 4. Windows & Doors
            if (wingBox.sizeX() >= 7 && wingBox.sizeZ() >= 7) {
                Vec3i doorCenter = new Vec3i((wingBox.min().x() + wingBox.max().x()) / 2, wingBox.min().y(), wingBox.min().z());
                targets.addAll(OpeningGenerator.carveDoorway(doorCenter, Direction.SOUTH, 1, 2, palette));
            }
        }

        return targets;
    }
}