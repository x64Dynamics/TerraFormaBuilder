package com.x64Dynamics.tfb.architectural.builds.grid;

import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.*;

// Analyzes spatial, elevation, orientation, function, and style relationships between
// buildings on the shared grid and computes valid architectural connections.
public final class GridConnectionPlanner {
    public enum ConnectionType {
        SHARED_WALL,
        DOORWAY,
        ARCHWAY,
        CORRIDOR,
        COVERED_PASSAGE,
        ELEVATED_WALKWAY,
        SHARED_COURTYARD,
        DELIBERATE_GAP,
        VERTICAL_STACK
    }

    public enum ConnectionStatus {
        PROPOSED,
        CONFIRMED,
        MANUAL_OVERRIDE,
        PROHIBITED
    }

    public record BuildingConnection(
        String id,
        String fromBuildingId,
        String toBuildingId,
        ConnectionType type,
        ConnectionStatus status,
        Vec3i anchorA,
        Vec3i anchorB,
        String reason
    ) { }

    private final Map<String, ConnectionType> manualOverrides = new HashMap<>();

    public void setManualOverride(String buildingA, String buildingB, ConnectionType type) {
        manualOverrides.put(pairKey(buildingA, buildingB), type);
    }

    public void clearManualOverride(String buildingA, String buildingB) {
        manualOverrides.remove(pairKey(buildingA, buildingB));
    }

    public List<BuildingConnection> analyzeConnections(
        GridManager grid,
        BuildingRegistry registry,
        PlacementValidator.PlacementRules rules
    ) {
        List<GridBuilding> list = new ArrayList<>(registry.allBuildings());
        List<BuildingConnection> result = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                GridBuilding a = list.get(i);
                GridBuilding b = list.get(j);
                BuildingConnection conn = evaluatePair(a, b, grid, rules);
                if (conn != null) {
                    result.add(conn);
                }
            }
        }
        registry.setConnections(result);
        return result;
    }

    private BuildingConnection evaluatePair(
        GridBuilding a,
        GridBuilding b,
        GridManager grid,
        PlacementValidator.PlacementRules rules
    ) {
        String key = pairKey(a.id(), b.id());
        ConnectionType override = manualOverrides.get(key);

        // Check vertical stacking first
        boolean overlapsXZ = a.occupiedCells().stream().anyMatch(c -> b.occupiedCells().contains(c));
        if (overlapsXZ) {
            GridBuilding lower = a.baseY() <= b.baseY() ? a : b;
            GridBuilding upper = a.baseY() <= b.baseY() ? b : a;
            Vec3i anchorA = grid.gridToWorld(lower.gridX() + lower.width() / 2, lower.gridZ() + lower.depth() / 2, lower.baseY() + 1);
            Vec3i anchorB = grid.gridToWorld(upper.gridX() + upper.width() / 2, upper.gridZ() + upper.depth() / 2, upper.baseY() + 1);
            ConnectionType t = override != null ? override : ConnectionType.VERTICAL_STACK;
            return new BuildingConnection(key, lower.id(), upper.id(), t,
                override != null ? ConnectionStatus.MANUAL_OVERRIDE : ConnectionStatus.CONFIRMED,
                anchorA, anchorB, "Vertical stairwell connection between stacked structures");
        }

        // Compute horizontal grid gap
        int gapX = Math.max(0, Math.max(a.gridX() - (b.gridX() + b.width()), b.gridX() - (a.gridX() + a.width())));
        int gapZ = Math.max(0, Math.max(a.gridZ() - (b.gridZ() + b.depth()), b.gridZ() - (a.gridZ() + a.depth())));
        int gap = Math.max(gapX, gapZ);

        if (gap > 10) return null; // Too far to connect automatically

        boolean floorAligned = a.floorElevations().stream().anyMatch(e -> b.floorElevations().contains(e));
        boolean sameStyle = a.style() == b.style();
        int yAlign = floorAligned ? Math.max(a.baseY(), b.baseY()) : Math.min(a.topWallY(), b.topWallY()) - 3;

        Vec3i anchorA = grid.gridToWorld(a.gridX() + a.width() / 2, a.gridZ() + a.depth() / 2, yAlign);
        Vec3i anchorB = grid.gridToWorld(b.gridX() + b.width() / 2, b.gridZ() + b.depth() / 2, yAlign);

        if (override != null) {
            if (override == ConnectionType.SHARED_WALL && !rules.allowSharedWalls()) {
                return new BuildingConnection(key, a.id(), b.id(), override, ConnectionStatus.PROHIBITED, anchorA, anchorB, "Shared walls prohibited by rules");
            }
            return new BuildingConnection(key, a.id(), b.id(), override, ConnectionStatus.MANUAL_OVERRIDE, anchorA, anchorB, "Manual override");
        }

        if (gap == 0) {
            if (floorAligned && a.function() == b.function()) {
                return new BuildingConnection(key, a.id(), b.id(), ConnectionType.DOORWAY, ConnectionStatus.CONFIRMED, anchorA, anchorB, "Touching buildings with aligned floors linked via doorway");
            }
            if (rules.allowSharedWalls()) {
                return new BuildingConnection(key, a.id(), b.id(), ConnectionType.SHARED_WALL, ConnectionStatus.CONFIRMED, anchorA, anchorB, "Adjacent structures share structural party wall");
            }
            return new BuildingConnection(key, a.id(), b.id(), ConnectionType.DELIBERATE_GAP, ConnectionStatus.PROPOSED, anchorA, anchorB, "Shared walls disabled");
        }

        if (gap <= 3) {
            if (a.baseY() != b.baseY() || Math.abs(a.stories() - b.stories()) >= 1) {
                return new BuildingConnection(key, a.id(), b.id(), ConnectionType.ELEVATED_WALKWAY, ConnectionStatus.CONFIRMED, anchorA, anchorB, "Height difference bridged via elevated walkway");
            }
            if (floorAligned && (a.function() == GridBuilding.BuildingFunction.COMMERCIAL || b.function() == GridBuilding.BuildingFunction.COMMERCIAL)) {
                return new BuildingConnection(key, a.id(), b.id(), ConnectionType.COVERED_PASSAGE, ConnectionStatus.CONFIRMED, anchorA, anchorB, "Mixed-use buildings linked via covered passage");
            }
            return new BuildingConnection(key, a.id(), b.id(), ConnectionType.CORRIDOR, ConnectionStatus.CONFIRMED, anchorA, anchorB, "Close structures linked via enclosed corridor");
        }

        if (gap <= 6 && sameStyle) {
            return new BuildingConnection(key, a.id(), b.id(), ConnectionType.ARCHWAY, ConnectionStatus.PROPOSED, anchorA, anchorB, "Matching-style buildings linked via courtyard archway");
        }

        return new BuildingConnection(key, a.id(), b.id(), ConnectionType.DELIBERATE_GAP, ConnectionStatus.PROPOSED, anchorA, anchorB, "Independent structures with architectural separation");
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? (a + "<->" + b) : (b + "<->" + a);
    }
}
