package com.x64Dynamics.tfb.architectural.interior.layouts;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;

import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
public record BuildingSection(
    String id,
    BoundingBox3i bounds,
    SectionShape shape,
    RoofGenerator.RoofType roofType,
    int floorCount,
    boolean isPrimary
) {
    public enum SectionShape {
        RECTANGLE,
        ROUNDED_RECTANGLE,
        CYLINDER,
        OCTAGON,
        HEXAGON,
        ROTATED_45
    }

    public static BuildingSection primaryRectangle(BoundingBox3i bounds, RoofGenerator.RoofType roofType) {
        return new BuildingSection("primary", bounds, SectionShape.RECTANGLE, roofType, 1, true);
    }

    public static BuildingSection attachedWing(String id, BoundingBox3i bounds, SectionShape shape, RoofGenerator.RoofType roofType) {
        return new BuildingSection(id, bounds, shape, roofType, 1, false);
    }
}
