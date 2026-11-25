package com.x64Dynamics.tfb.architectural.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Validates grid building placements, movements, rotations, resizes, and connections
// against configurable architectural constraints before accepting them.
public final class PlacementValidator {
    public record PlacementRules(
        int minGapBetweenIndependent,
        boolean allowSharedWalls,
        boolean allowOverlap,
        int minCorridorWidth,
        int minDoorWidth,
        int maxBuildingHeight,
        boolean requireFloorAlignmentForDoorway,
        boolean requireStyleCompatibility,
        boolean allowStackedStructures
    ) {
        public static PlacementRules defaults() {
            return new PlacementRules(0, true, false, 3, 1, 64, true, false, true);
        }
    }

    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        public static ValidationResult ok(List<String> warnings) {
            return new ValidationResult(true, List.of(), warnings);
        }
        public static ValidationResult fail(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, errors, warnings);
        }
    }

    private PlacementRules rules;

    public PlacementValidator(PlacementRules rules) {
        this.rules = rules != null ? rules : PlacementRules.defaults();
    }

    public PlacementRules rules() { return rules; }
    public void setRules(PlacementRules rules) { this.rules = rules; }

    public ValidationResult validatePlacement(GridBuilding candidate, GridManager grid, BuildingRegistry registry) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (candidate.width() < 5 || candidate.depth() < 5) {
            errors.add("Selected area (" + candidate.width() + "x" + candidate.depth() + ") is too small; minimum 5x5 required.");
        }
        if (candidate.height() > rules.maxBuildingHeight()) {
            errors.add("Building height " + candidate.height() + " exceeds maxBuildingHeight (" + rules.maxBuildingHeight() + ").");
        }
        if (candidate.baseY() < grid.minElevation() || candidate.baseY() + candidate.height() > grid.maxElevation()) {
            errors.add("Building elevation [" + candidate.baseY() + ".." + (candidate.baseY() + candidate.height()) + "] is outside grid limits.");
        }

        Set<GridManager.GridCoord> candCells = candidate.occupiedCells();
        for (var c : candCells) {
            if (!grid.inBounds(c.gx(), c.gz())) {
                errors.add("Building footprint extends outside grid bounds at (" + c.gx() + ", " + c.gz() + ").");
                break;
            }
            var state = grid.getCellState(c.gx(), c.gz());
            if (state == GridManager.CellState.BLOCKED || state == GridManager.CellState.RESERVED) {
                errors.add("Cell (" + c.gx() + ", " + c.gz() + ") is " + state.name() + ".");
                break;
            }
        }

        for (GridBuilding other : registry.allBuildings()) {
            if (other.id().equals(candidate.id())) continue;

            boolean horizontalOverlap = false;
            for (var c : candCells) {
                if (other.occupiedCells().contains(c)) {
                    horizontalOverlap = true;
                    break;
                }
            }

            if (horizontalOverlap) {
                boolean verticalOverlap = candidate.baseY() < other.baseY() + other.height()
                    && other.baseY() < candidate.baseY() + candidate.height();
                if (verticalOverlap && !rules.allowOverlap()) {
                    errors.add("Footprint overlaps existing building '" + other.id() + "'.");
                } else if (!verticalOverlap && !rules.allowStackedStructures()) {
                    errors.add("Stacked placement over '" + other.id() + "' is disabled by placement rules.");
                } else if (!verticalOverlap && candidate.baseY() < other.topWallY()) {
                    warnings.add("Stacked building '" + candidate.id() + "' rests on sloped roof of '" + other.id() + "'; flat roof recommended.");
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.ok(warnings) : ValidationResult.fail(errors, warnings);
    }
}
