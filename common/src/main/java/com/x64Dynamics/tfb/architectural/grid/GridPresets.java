package com.x64Dynamics.tfb.architectural.grid;

import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.material.StylePreset;

// Curated multi-building connected complex presets demonstrating shared walls,
// doorways, elevated walkways, courtyards, and architectural variation.
public final class GridPresets {
    public enum PresetKind {
        MANOR_ESTATE("Manor Estate"),
        VILLAGE_SQUARE("Village Square"),
        COMMERCIAL_ROW("Commercial Row");

        private final String displayName;
        PresetKind(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    private GridPresets() { }

    public static void populate(PresetKind kind, StructurePlacementController ctrl, int baseY, StylePreset style, long seed) {
        for (var b : new java.util.ArrayList<>(ctrl.registry().allBuildings())) {
            ctrl.deleteBuilding(b.id());
        }
        switch (kind) {
            case MANOR_ESTATE -> {
                ctrl.placeBuilding(new GridBuilding("manor_main", 4, 4, baseY, 11, 9, 2, 5,
                    GridBuilding.FootprintShape.L_SHAPED, Direction.SOUTH, style, null, RoofGenerator.RoofType.GABLE,
                    GridBuilding.BuildingFunction.RESIDENTIAL, false, seed));
                ctrl.placeBuilding(new GridBuilding("manor_east", 15, 4, baseY, 7, 9, 2, 5,
                    GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH, style, null, RoofGenerator.RoofType.GABLE,
                    GridBuilding.BuildingFunction.RESIDENTIAL, false, seed + 1));
                ctrl.placeBuilding(new GridBuilding("manor_tower", 24, 5, baseY, 7, 7, 3, 5,
                    GridBuilding.FootprintShape.SQUARE, Direction.WEST, style, null, RoofGenerator.RoofType.GABLE,
                    GridBuilding.BuildingFunction.TOWER, false, seed + 2));
                ctrl.placeBuilding(new GridBuilding("manor_courtyard", 4, 15, baseY, 11, 9, 1, 5,
                    GridBuilding.FootprintShape.SQUARE, Direction.NORTH, style, null, RoofGenerator.RoofType.FLAT,
                    GridBuilding.BuildingFunction.COURTYARD, false, seed + 3));
            }
            case VILLAGE_SQUARE -> {
                ctrl.placeBuilding(new GridBuilding("town_hall", 10, 2, baseY, 11, 9, 2, 5,
                    GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH, style, null, RoofGenerator.RoofType.HIP,
                    GridBuilding.BuildingFunction.CIVIC, false, seed));
                ctrl.placeBuilding(new GridBuilding("bakery", 2, 13, baseY, 7, 7, 1, 5,
                    GridBuilding.FootprintShape.SQUARE, Direction.EAST, style, null, RoofGenerator.RoofType.GABLE,
                    GridBuilding.BuildingFunction.COMMERCIAL, false, seed + 1));
                ctrl.placeBuilding(new GridBuilding("square_plaza", 11, 13, baseY, 9, 9, 1, 5,
                    GridBuilding.FootprintShape.SQUARE, Direction.SOUTH, style, null, RoofGenerator.RoofType.FLAT,
                    GridBuilding.BuildingFunction.COURTYARD, false, seed + 2));
                ctrl.placeBuilding(new GridBuilding("tavern", 22, 13, baseY, 9, 9, 2, 5,
                    GridBuilding.FootprintShape.U_SHAPED, Direction.WEST, style, null, RoofGenerator.RoofType.MANSARD,
                    GridBuilding.BuildingFunction.COMMERCIAL, false, seed + 3));
            }
            case COMMERCIAL_ROW -> {
                ctrl.placeBuilding(new GridBuilding("shop_1", 2, 6, baseY, 7, 9, 2, 5,
                    GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH, style, null, RoofGenerator.RoofType.GABLE,
                    GridBuilding.BuildingFunction.COMMERCIAL, false, seed));
                ctrl.placeBuilding(new GridBuilding("shop_2", 9, 6, baseY, 7, 9, 2, 5,
                    GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH, style, null, RoofGenerator.RoofType.HIP,
                    GridBuilding.BuildingFunction.COMMERCIAL, false, seed + 1));
                ctrl.placeBuilding(new GridBuilding("warehouse", 18, 6, baseY, 9, 9, 2, 5,
                    GridBuilding.FootprintShape.RECTANGLE, Direction.SOUTH, style, null, RoofGenerator.RoofType.FLAT,
                    GridBuilding.BuildingFunction.STORAGE, false, seed + 2));
            }
        }
    }
}
