package com.x64Dynamics.tfb.architectural.builds.grid;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.*;

// Shared project-level 3D architectural grid managing coordinates, snapping,
// cell dimensions, footprint rasterization, and occupancy states.
public final class GridManager {
    public enum CellState {
        AVAILABLE,
        OCCUPIED,
        BLOCKED,
        RESERVED,
        CONNECTED
    }

    public record GridCoord(int gx, int gz) {
        public GridCoord offset(int dx, int dz) {
            return new GridCoord(gx + dx, gz + dz);
        }
    }

    private Vec3i origin;
    private int cellSize;
    private int gridWidth;
    private int gridDepth;
    private int minElevation;
    private int maxElevation;

    private final Map<GridCoord, CellState> explicitStates = new HashMap<>();
    private final Map<GridCoord, Set<String>> cellOwners = new HashMap<>();

    public GridManager(Vec3i origin, int cellSize, int gridWidth, int gridDepth, int minElevation, int maxElevation) {
        this.origin = origin != null ? origin : new Vec3i(0, 64, 0);
        this.cellSize = Math.max(1, Math.min(16, cellSize));
        this.gridWidth = Math.max(8, gridWidth);
        this.gridDepth = Math.max(8, gridDepth);
        this.minElevation = minElevation;
        this.maxElevation = Math.max(minElevation + 16, maxElevation);
    }

    public static GridManager defaultProjectGrid(Vec3i origin) {
        return new GridManager(origin, 1, 64, 64, -64, 319);
    }

    public Vec3i origin() { return origin; }
    public void setOrigin(Vec3i origin) { this.origin = origin; }
    public int cellSize() { return cellSize; }
    public void setCellSize(int cellSize) { this.cellSize = Math.max(1, Math.min(16, cellSize)); }
    public int gridWidth() { return gridWidth; }
    public int gridDepth() { return gridDepth; }
    public void resizeGrid(int width, int depth) {
        this.gridWidth = Math.max(8, width);
        this.gridDepth = Math.max(8, depth);
    }
    public int minElevation() { return minElevation; }
    public int maxElevation() { return maxElevation; }

    public GridCoord worldToGrid(Vec3i worldPos) {
        int gx = Math.floorDiv(worldPos.x() - origin.x(), cellSize);
        int gz = Math.floorDiv(worldPos.z() - origin.z(), cellSize);
        return new GridCoord(gx, gz);
    }

    public Vec3i gridToWorld(int gx, int gz, int y) {
        return new Vec3i(origin.x() + gx * cellSize, y, origin.z() + gz * cellSize);
    }

    public Vec3i snapToGrid(Vec3i worldPos) {
        GridCoord c = worldToGrid(worldPos);
        return gridToWorld(c.gx(), c.gz(), Math.max(minElevation, Math.min(maxElevation, worldPos.y())));
    }

    public boolean inBounds(int gx, int gz) {
        return gx >= 0 && gx < gridWidth && gz >= 0 && gz < gridDepth;
    }

    public void setCellState(int gx, int gz, CellState state) {
        GridCoord c = new GridCoord(gx, gz);
        if (state == CellState.AVAILABLE) explicitStates.remove(c);
        else explicitStates.put(c, state);
    }

    public CellState getCellState(int gx, int gz) {
        GridCoord c = new GridCoord(gx, gz);
        CellState explicit = explicitStates.get(c);
        if (explicit == CellState.BLOCKED || explicit == CellState.RESERVED || explicit == CellState.CONNECTED) {
            return explicit;
        }
        Set<String> owners = cellOwners.get(c);
        if (owners != null && !owners.isEmpty()) {
            return CellState.OCCUPIED;
        }
        return explicit != null ? explicit : CellState.AVAILABLE;
    }

    public Set<String> ownersAt(int gx, int gz) {
        return cellOwners.getOrDefault(new GridCoord(gx, gz), Collections.emptySet());
    }

    public void clearOccupancy() {
        cellOwners.clear();
        explicitStates.entrySet().removeIf(e -> e.getValue() == CellState.OCCUPIED || e.getValue() == CellState.CONNECTED);
    }

    public void markBuildingOccupied(GridBuilding building) {
        for (GridCoord c : building.occupiedCells()) {
            cellOwners.computeIfAbsent(c, k -> new LinkedHashSet<>()).add(building.id());
        }
    }

    public void markConnectedCells(Collection<GridCoord> cells) {
        for (GridCoord c : cells) {
            if (inBounds(c.gx(), c.gz()) && getCellState(c.gx(), c.gz()) == CellState.AVAILABLE) {
                explicitStates.put(c, CellState.CONNECTED);
            }
        }
    }

    public BoundingBox3i toWorldBounds(GridBuilding b) {
        Vec3i min = gridToWorld(b.gridX(), b.gridZ(), b.baseY());
        int wx = b.width() * cellSize;
        int wz = b.depth() * cellSize;
        return BoundingBox3i.fromOriginAndSize(min, wx, b.height(), wz);
    }
}
