package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.*;

/**
 * Dreamer-style 3D modular chunk-grid builder.
 * Divides the world into 3D cells (default 5x5x5) where:
 * - Placing a module next to an existing one at the same floor automatically connects them
 *   (opening the shared interior wall and wrapping exterior walls around the merged shape).
 * - Placing a module on top of an existing one converts the lower module into a floor section
 *   with a staircase opening and promotes the roof to the new top-most cell.
 */
public final class ModularGridSession {
    public record CellCoord3D(int gx, int gy, int gz) {
        public CellCoord3D offset(int dx, int dy, int dz) {
            return new CellCoord3D(gx + dx, gy + dy, gz + dz);
        }
    }

    public record ModuleCell(
        CellCoord3D coord,
        StylePreset style,
        MaterialPalette palette,
        RoofGenerator.RoofType roofType
    ) { }

    private Vec3i origin;
    private int cellSize;
    private int cellHeight;
    private int activeFloor = 0;
    private CellCoord3D selectedCell = new CellCoord3D(0, 0, 0);

    private final Map<CellCoord3D, ModuleCell> modules = new LinkedHashMap<>();

    public ModularGridSession(Vec3i origin, int cellSize, int cellHeight) {
        this.origin = origin != null ? origin : new Vec3i(0, 64, 0);
        this.cellSize = Math.max(4, Math.min(16, cellSize));
        this.cellHeight = Math.max(4, Math.min(12, cellHeight));
    }

    public Vec3i origin() { return origin; }
    public void setOrigin(Vec3i origin) { this.origin = origin != null ? origin : this.origin; }
    public int cellSize() { return cellSize; }
    public void setCellSize(int cellSize) { this.cellSize = Math.max(4, Math.min(16, cellSize)); }
    public int cellHeight() { return cellHeight; }
    public void setCellHeight(int cellHeight) { this.cellHeight = Math.max(4, Math.min(12, cellHeight)); }
    public int activeFloor() { return activeFloor; }
    public void setActiveFloor(int activeFloor) { this.activeFloor = Math.max(0, Math.min(16, activeFloor)); }
    public CellCoord3D selectedCell() { return selectedCell; }
    public void setSelectedCell(CellCoord3D cell) { this.selectedCell = cell; }

    public Map<CellCoord3D, ModuleCell> modules() {
        return Collections.unmodifiableMap(modules);
    }

    public boolean hasModule(int gx, int gy, int gz) {
        return modules.containsKey(new CellCoord3D(gx, gy, gz));
    }

    public int highestFloorAt(int gx, int gz) {
        int maxGy = -1;
        for (CellCoord3D c : modules.keySet()) {
            if (c.gx() == gx && c.gz() == gz && c.gy() > maxGy) {
                maxGy = c.gy();
            }
        }
        return maxGy;
    }

    public ModuleCell placeModule(int gx, int gy, int gz, StylePreset style, MaterialPalette palette, RoofGenerator.RoofType roofType) {
        CellCoord3D coord = new CellCoord3D(gx, Math.max(0, gy), gz);
        StylePreset st = style != null ? style : StylePreset.MEDIEVAL;
        MaterialPalette pal = palette != null ? palette : st.createPalette();
        RoofGenerator.RoofType rt = roofType != null ? roofType : RoofGenerator.RoofType.GABLE;
        ModuleCell cell = new ModuleCell(coord, st, pal, rt);
        modules.put(coord, cell);
        selectedCell = coord;
        return cell;
    }

    /**
     * Dreamer-style smart stack/place: if (gx, activeFloor, gz) is already occupied,
     * stacks on top at (gx, highestFloor + 1, gz).
     */
    public ModuleCell stackOrPlace(int gx, int gz, StylePreset style, MaterialPalette palette, RoofGenerator.RoofType roofType) {
        int gy = hasModule(gx, activeFloor, gz) ? highestFloorAt(gx, gz) + 1 : activeFloor;
        return placeModule(gx, gy, gz, style, palette, roofType);
    }

    public boolean removeModule(int gx, int gy, int gz) {
        return modules.remove(new CellCoord3D(gx, gy, gz)) != null;
    }

    public boolean removeTopModule(int gx, int gz) {
        int top = highestFloorAt(gx, gz);
        if (top < 0) return false;
        return removeModule(gx, top, gz);
    }

    public void clear() {
        modules.clear();
    }

    public CellCoord3D worldToCell(Vec3i worldPos) {
        int gx = Math.floorDiv(worldPos.x() - origin.x(), cellSize);
        int gy = Math.max(0, Math.floorDiv(worldPos.y() - origin.y(), cellHeight));
        int gz = Math.floorDiv(worldPos.z() - origin.z(), cellSize);
        return new CellCoord3D(gx, gy, gz);
    }

    public BoundingBox3i cellToWorldBounds(CellCoord3D c) {
        int x0 = origin.x() + c.gx() * cellSize;
        int y0 = origin.y() + c.gy() * cellHeight;
        int z0 = origin.z() + c.gz() * cellSize;
        return new BoundingBox3i(
            new Vec3i(x0, y0, z0),
            new Vec3i(x0 + cellSize - 1, y0 + cellHeight - 1, z0 + cellSize - 1)
        );
    }

    /**
     * Compiles all placed 5x5x5 modules into connected PlacementTargets with automatic
     * horizontal wall merging, vertical floor/stair sections, and top-layer roofs.
     */
    public List<PlacementTarget> buildPlacementTargets() {
        if (modules.isEmpty()) return Collections.emptyList();

        Map<Vec3i, PlacementTarget> placed = new LinkedHashMap<>();
        CellCoord3D doorCell = chooseEntranceCell();

        for (ModuleCell cell : modules.values()) {
            buildSingleModule(cell, cell.coord().equals(doorCell), placed);
        }
        return new ArrayList<>(placed.values());
    }

    private CellCoord3D chooseEntranceCell() {
        CellCoord3D best = null;
        for (CellCoord3D c : modules.keySet()) {
            if (c.gy() != 0) continue;
            if (!hasModule(c.gx(), 0, c.gz() + 1) || !hasModule(c.gx(), 0, c.gz() - 1)
                || !hasModule(c.gx() - 1, 0, c.gz()) || !hasModule(c.gx() + 1, 0, c.gz())) {
                if (best == null || c.gz() > best.gz() || (c.gz() == best.gz() && c.gx() < best.gx())) {
                    best = c;
                }
            }
        }
        return best;
    }

    private void buildSingleModule(ModuleCell cell, boolean isEntranceCell, Map<Vec3i, PlacementTarget> out) {
        CellCoord3D c = cell.coord();
        MaterialPalette pal = cell.palette();
        BoundingBox3i box = cellToWorldBounds(c);
        int x0 = box.min().x(), y0 = box.min().y(), z0 = box.min().z();
        int x1 = box.max().x(), y1 = box.max().y(), z1 = box.max().z();

        boolean connN = hasModule(c.gx(), c.gy(), c.gz() - 1);
        boolean connS = hasModule(c.gx(), c.gy(), c.gz() + 1);
        boolean connW = hasModule(c.gx() - 1, c.gy(), c.gz());
        boolean connE = hasModule(c.gx() + 1, c.gy(), c.gz());
        boolean hasAbove = hasModule(c.gx(), c.gy() + 1, c.gz());
        boolean hasBelow = c.gy() > 0 && hasModule(c.gx(), c.gy() - 1, c.gz());

        BlockIdentifier floorMat = (c.gy() == 0 && !hasBelow) ? pal.get(MaterialRole.FOUNDATION) : pal.get(MaterialRole.FLOOR);
        BlockIdentifier wallMat = pal.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier trimMat = pal.get(MaterialRole.BEAM);
        BlockIdentifier pillarMat = pal.get(MaterialRole.PILLAR);
        BlockIdentifier windowMat = pal.get(MaterialRole.WINDOW);
        BlockIdentifier air = BlockIdentifier.of("minecraft:air");

        // 1. Floor slab at y0 (leave stairwell opening if module below connects up)
        int stairX = x0 + 1;
        int stairZ = z0 + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean isStairOpening = hasBelow && (x == stairX && (z == stairZ || z == stairZ + 1));
                put(out, x, y0, z, isStairOpening ? air : floorMat,
                    isStairOpening ? MaterialRole.AIR : (c.gy() == 0 ? MaterialRole.FOUNDATION : MaterialRole.FLOOR));
            }
        }

        // 2. Interior air volume (y0 + 1 .. y1)
        for (int y = y0 + 1; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    put(out, x, y, z, air, MaterialRole.AIR);
                }
            }
        }

        // 3. Exterior walls ONLY on non-connected faces
        int midX = (x0 + x1) / 2;
        int midZ = (z0 + z1) / 2;
        int midY = y0 + 2;
        boolean placedDoor = false;

        for (int y = y0 + 1; y <= y1; y++) {
            boolean isTopTrim = (y == y1);
            BlockIdentifier faceMat = isTopTrim ? trimMat : wallMat;
            MaterialRole faceRole = isTopTrim ? MaterialRole.BEAM : MaterialRole.WALL_PRIMARY;

            if (!connN) {
                for (int x = x0; x <= x1; x++) {
                    boolean corner = (x == x0 && !connW) || (x == x1 && !connE);
                    boolean win = !isTopTrim && y == midY && x == midX;
                    boolean door = isEntranceCell && !placedDoor && (x == midX) && (y == y0 + 1 || y == y0 + 2);
                    if (door) {
                        put(out, x, y, z0, air, MaterialRole.DOOR);
                    } else {
                        put(out, x, y, z0, corner ? pillarMat : (win ? windowMat : faceMat),
                            corner ? MaterialRole.PILLAR : (win ? MaterialRole.WINDOW : faceRole));
                    }
                }
                if (isEntranceCell && !placedDoor) placedDoor = true;
            }
            if (!connS) {
                for (int x = x0; x <= x1; x++) {
                    boolean corner = (x == x0 && !connW) || (x == x1 && !connE);
                    boolean win = !isTopTrim && y == midY && x == midX;
                    boolean door = isEntranceCell && !placedDoor && (x == midX) && (y == y0 + 1 || y == y0 + 2);
                    if (door) {
                        put(out, x, y, z1, air, MaterialRole.DOOR);
                    } else {
                        put(out, x, y, z1, corner ? pillarMat : (win ? windowMat : faceMat),
                            corner ? MaterialRole.PILLAR : (win ? MaterialRole.WINDOW : faceRole));
                    }
                }
                if (isEntranceCell && !placedDoor) placedDoor = true;
            }
            if (!connW) {
                for (int z = z0; z <= z1; z++) {
                    boolean corner = (z == z0 && !connN) || (z == z1 && !connS);
                    boolean win = !isTopTrim && y == midY && z == midZ;
                    put(out, x0, y, z, corner ? pillarMat : (win ? windowMat : faceMat),
                        corner ? MaterialRole.PILLAR : (win ? MaterialRole.WINDOW : faceRole));
                }
            }
            if (!connE) {
                for (int z = z0; z <= z1; z++) {
                    boolean corner = (z == z0 && !connN) || (z == z1 && !connS);
                    boolean win = !isTopTrim && y == midY && z == midZ;
                    put(out, x1, y, z, corner ? pillarMat : (win ? windowMat : faceMat),
                        corner ? MaterialRole.PILLAR : (win ? MaterialRole.WINDOW : faceRole));
                }
            }
        }

        // 4. Vertical connection stairs if a module is stacked on top of this one
        if (hasAbove) {
            BlockIdentifier stairMat = pal.get(MaterialRole.STAIR).withProperty("facing", "south");
            for (int step = 1; step < cellHeight; step++) {
                int sz = Math.min(z1 - 1, stairZ + step - 1);
                put(out, stairX, y0 + step, sz, stairMat, MaterialRole.STAIR);
            }
            // Corner lantern for interior lighting
            put(out, x1 - 1, y1, z1 - 1, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
        } else {
            // 5. Top-most cell in this column gets the roof + ceiling light
            put(out, midX, y1, midZ, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
            buildModuleRoof(box, c, pal, cell.roofType(), connN, connS, connW, connE, out);
        }
    }

    private void buildModuleRoof(
        BoundingBox3i box,
        CellCoord3D c,
        MaterialPalette pal,
        RoofGenerator.RoofType roofType,
        boolean connN, boolean connS, boolean connW, boolean connE,
        Map<Vec3i, PlacementTarget> out
    ) {
        int x0 = box.min().x(), z0 = box.min().z();
        int x1 = box.max().x(), z1 = box.max().z();
        int roofY = box.max().y() + 1;
        BlockIdentifier roofMat = pal.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier ridgeMat = pal.get(MaterialRole.ROOF_RIDGE);

        if (roofType == RoofGenerator.RoofType.FLAT) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = (!connN && z == z0) || (!connS && z == z1) || (!connW && x == x0) || (!connE && x == x1);
                    put(out, x, roofY, z, edge ? ridgeMat : roofMat, edge ? MaterialRole.ROOF_RIDGE : MaterialRole.ROOF_SLOPE);
                }
            }
            return;
        }

        // Pitched layered roof respecting connected edges so adjacent roofs merge cleanly
        int layers = Math.max(2, (cellSize + 1) / 2);
        for (int layer = 0; layer < layers; layer++) {
            int lx0 = connW ? x0 : x0 + layer;
            int lx1 = connE ? x1 : x1 - layer;
            int lz0 = connN ? z0 : z0 + layer;
            int lz1 = connS ? z1 : z1 - layer;
            if (lx0 > lx1 || lz0 > lz1) break;
            boolean topLayer = (layer == layers - 1) || (lx1 - lx0 <= 1 && lz1 - lz0 <= 1);
            BlockIdentifier mat = topLayer ? ridgeMat : roofMat;
            MaterialRole role = topLayer ? MaterialRole.ROOF_RIDGE : MaterialRole.ROOF_SLOPE;
            for (int x = lx0; x <= lx1; x++) {
                for (int z = lz0; z <= lz1; z++) {
                    put(out, x, roofY + layer, z, mat, role);
                }
            }
        }
    }

    private static void put(Map<Vec3i, PlacementTarget> out, int x, int y, int z, BlockIdentifier block, MaterialRole role) {
        Vec3i p = new Vec3i(x, y, z);
        out.put(p, new PlacementTarget(p, block, role));
    }
}
