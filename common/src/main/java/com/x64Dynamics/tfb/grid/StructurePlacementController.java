package com.x64Dynamics.tfb.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates interactive building placement, movement, rotation, resizing, deletion,
 * and connection overrides on the shared GridManager and BuildingRegistry.
 */
public final class StructurePlacementController {
    private final GridManager grid;
    private final BuildingRegistry registry;
    private final PlacementRuleEngine ruleEngine;
    private final ConnectionEngine connectionEngine;

    private String selectedBuildingId;
    private GridManager.GridCoord selectedCell;
    private String lastValidationMessage = "Ready";

    public StructurePlacementController(
        GridManager grid,
        BuildingRegistry registry,
        PlacementRuleEngine ruleEngine,
        ConnectionEngine connectionEngine
    ) {
        this.grid = grid;
        this.registry = registry;
        this.ruleEngine = ruleEngine;
        this.connectionEngine = connectionEngine;
    }

    public GridManager grid() { return grid; }
    public BuildingRegistry registry() { return registry; }
    public PlacementRuleEngine ruleEngine() { return ruleEngine; }
    public ConnectionEngine connectionEngine() { return connectionEngine; }
    public String selectedBuildingId() { return selectedBuildingId; }
    public void selectBuilding(String id) { this.selectedBuildingId = id; }
    public GridManager.GridCoord selectedCell() { return selectedCell; }
    public void selectCell(GridManager.GridCoord cell) { this.selectedCell = cell; }
    public String lastValidationMessage() { return lastValidationMessage; }

    public PlacementRuleEngine.ValidationResult placeBuilding(GridBuilding building) {
        var res = ruleEngine.validatePlacement(building, grid, registry);
        if (!res.valid()) {
            this.lastValidationMessage = String.join("; ", res.errors());
            return res;
        }
        registry.register(building);
        rebuildOccupancyAndConnections();
        this.selectedBuildingId = building.id();
        this.lastValidationMessage = res.warnings().isEmpty()
            ? "Placed '" + building.id() + "'"
            : "Placed '" + building.id() + "' (" + String.join("; ", res.warnings()) + ")";
        return res;
    }

    public PlacementRuleEngine.ValidationResult moveBuilding(String id, int newGridX, int newGridZ, int newBaseY) {
        GridBuilding b = registry.get(id).orElse(null);
        if (b == null) {
            return PlacementRuleEngine.ValidationResult.fail(List.of("Unknown building: " + id), List.of());
        }
        int oldX = b.gridX(), oldZ = b.gridZ(), oldY = b.baseY();
        b.setPosition(newGridX, newGridZ, newBaseY);
        var res = ruleEngine.validatePlacement(b, grid, registry);
        if (!res.valid()) {
            b.setPosition(oldX, oldZ, oldY);
            this.lastValidationMessage = String.join("; ", res.errors());
            return res;
        }
        rebuildOccupancyAndConnections();
        this.lastValidationMessage = "Moved '" + id + "'";
        return res;
    }

    public boolean rotateBuilding(String id) {
        GridBuilding b = registry.get(id).orElse(null);
        if (b == null) return false;
        b.rotateClockwise();
        rebuildOccupancyAndConnections();
        this.lastValidationMessage = "Rotated '" + id + "' to " + b.facing();
        return true;
    }

    public PlacementRuleEngine.ValidationResult resizeBuilding(String id, int newWidth, int newDepth) {
        GridBuilding b = registry.get(id).orElse(null);
        if (b == null) {
            return PlacementRuleEngine.ValidationResult.fail(List.of("Unknown building: " + id), List.of());
        }
        int oldW = b.width(), oldD = b.depth();
        b.setSize(newWidth, newDepth);
        var res = ruleEngine.validatePlacement(b, grid, registry);
        if (!res.valid()) {
            b.setSize(oldW, oldD);
            this.lastValidationMessage = String.join("; ", res.errors());
            return res;
        }
        rebuildOccupancyAndConnections();
        this.lastValidationMessage = "Resized '" + id + "' to " + b.width() + "x" + b.depth();
        return res;
    }

    public boolean deleteBuilding(String id) {
        boolean removed = registry.remove(id);
        if (removed) {
            if (id.equals(selectedBuildingId)) selectedBuildingId = null;
            rebuildOccupancyAndConnections();
            this.lastValidationMessage = "Deleted '" + id + "'";
        }
        return removed;
    }

    public void overrideConnection(String buildingA, String buildingB, ConnectionEngine.ConnectionType type) {
        connectionEngine.setManualOverride(buildingA, buildingB, type);
        rebuildOccupancyAndConnections();
    }

    public GridComplexGenerator.ComplexResult generateComplex() {
        return GridComplexGenerator.generate(grid, registry, connectionEngine, ruleEngine.rules());
    }

    public List<String> rebuildOccupancyAndConnections() {
        grid.clearOccupancy();
        for (GridBuilding b : registry.allBuildings()) {
            grid.markBuildingOccupied(b);
        }
        var conns = connectionEngine.analyzeConnections(grid, registry, ruleEngine.rules());
        List<String> summary = new ArrayList<>();
        for (var c : conns) {
            summary.add(c.id() + ": " + c.type() + " (" + c.status() + ")");
        }
        return summary;
    }
}
