package com.x64Dynamics.tfb.architectural.grid;

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

// Dreamer-style 3D modular chunk-grid builder.
// Divides the world into 3D cells (default 5x5x5) where:
// - Placing a module next to an existing one at the same floor automatically connects them
// (opening the shared interior wall and wrapping exterior walls around the merged shape).
// - Placing a module on top of an existing one converts the lower module into a floor section
// with a staircase opening and promotes the roof to the new top-most cell.
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

    // Dreamer-style smart stack/place: if (gx, activeFloor, gz) is already occupied,
    // stacks on top at (gx, highestFloor + 1, gz).
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

    // Compiles all placed 5x5x5 modules into connected PlacementTargets with automatic
    // horizontal wall merging, vertical floor/stair sections, detailed facades, interior
    // furnishings, and stepped overhang roofs on top-most cells.
    public List<PlacementTarget> buildPlacementTargets() {
        if (modules.isEmpty()) return Collections.emptyList();

        Map<Vec3i, PlacementTarget> placed = new LinkedHashMap<>();
        CellCoord3D doorCell = chooseEntranceCell();

        for (ModuleCell cell : modules.values()) {
            buildSingleModule(cell, cell.coord().equals(doorCell), placed);
        }
        MaterialPalette basePal = modules.values().iterator().next().palette();
        return com.x64Dynamics.tfb.architectural.plan.StructuralIntegrityEngine
            .enforce(new ArrayList<>(placed.values()), basePal, null)
            .targets();
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
        BlockIdentifier accentMat = pal.get(MaterialRole.WALL_ACCENT);
        BlockIdentifier trimMat = pal.get(MaterialRole.BEAM);
        BlockIdentifier pillarMat = pal.get(MaterialRole.PILLAR);
        BlockIdentifier windowMat = pal.get(MaterialRole.WINDOW);
        BlockIdentifier slabMat = pal.get(MaterialRole.SLAB);
        BlockIdentifier doorMat = pal.get(MaterialRole.DOOR);
        BlockIdentifier air = BlockIdentifier.of("minecraft:air");

        // 1. Floor slab at y0 (leave stairwell opening if module below connects up)
        int stairX = x0 + 1;
        int stairZ = z0 + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean isStairOpening = hasBelow && (x == stairX && (z == stairZ || z == stairZ + 1));
                boolean isEdgePlinth = (c.gy() == 0 && !hasBelow) && ((x == x0 && !connW) || (x == x1 && !connE) || (z == z0 && !connN) || (z == z1 && !connS));
                BlockIdentifier mat = isStairOpening ? air : (isEdgePlinth ? pal.get(MaterialRole.SUB_FOUNDATION) : floorMat);
                MaterialRole role = isStairOpening ? MaterialRole.AIR : (c.gy() == 0 ? MaterialRole.FOUNDATION : MaterialRole.FLOOR);
                put(out, x, y0, z, mat, role);
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

        // 3. Detailed exterior walls ONLY on non-connected faces
        int midX = (x0 + x1) / 2;
        int midZ = (z0 + z1) / 2;
        int midY = y0 + 2;
        Direction doorPlacedFace = null;

        for (int y = y0 + 1; y <= y1; y++) {
            boolean isBaseCourse = (y == y0 + 1);
            boolean isTopTrim = (y == y1);
            BlockIdentifier faceMat = isTopTrim ? trimMat : (isBaseCourse ? accentMat : wallMat);
            MaterialRole faceRole = isTopTrim ? MaterialRole.BEAM : (isBaseCourse ? MaterialRole.WALL_ACCENT : MaterialRole.WALL_PRIMARY);

            if (!connS) {
                boolean useDoor = isEntranceCell && (doorPlacedFace == null || doorPlacedFace == Direction.SOUTH);
                if (useDoor) doorPlacedFace = Direction.SOUTH;
                buildZFace(x0, x1, z1, y, y0, midX, midY, isTopTrim, connW, connE, useDoor, Direction.SOUTH,
                    pillarMat, faceMat, faceRole, windowMat, slabMat, doorMat, pal, out);
            }
            if (!connN) {
                boolean useDoor = isEntranceCell && (doorPlacedFace == null || doorPlacedFace == Direction.NORTH);
                if (useDoor) doorPlacedFace = Direction.NORTH;
                buildZFace(x0, x1, z0, y, y0, midX, midY, isTopTrim, connW, connE, useDoor, Direction.NORTH,
                    pillarMat, faceMat, faceRole, windowMat, slabMat, doorMat, pal, out);
            }
            if (!connW) {
                boolean useDoor = isEntranceCell && (doorPlacedFace == null || doorPlacedFace == Direction.WEST);
                if (useDoor) doorPlacedFace = Direction.WEST;
                buildXFace(z0, z1, x0, y, y0, midZ, midY, isTopTrim, connN, connS, useDoor, Direction.WEST,
                    pillarMat, faceMat, faceRole, windowMat, slabMat, doorMat, pal, out);
            }
            if (!connE) {
                boolean useDoor = isEntranceCell && (doorPlacedFace == null || doorPlacedFace == Direction.EAST);
                if (useDoor) doorPlacedFace = Direction.EAST;
                buildXFace(z0, z1, x1, y, y0, midZ, midY, isTopTrim, connN, connS, useDoor, Direction.EAST,
                    pillarMat, faceMat, faceRole, windowMat, slabMat, doorMat, pal, out);
            }
        }

        // 4. Vertical connection stairs + interior furnishings
        if (hasAbove) {
            BlockIdentifier stairMat = pal.get(MaterialRole.STAIR).withProperty("facing", "south");
            for (int step = 1; step < cellHeight; step++) {
                int sz = Math.min(z1 - 1, stairZ + step - 1);
                put(out, stairX, y0 + step, sz, stairMat, MaterialRole.STAIR);
            }
            put(out, x1 - 1, y1, z1 - 1, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
        } else {
            // Top-most cell in this column gets the stepped roof + ceiling light
            put(out, midX, y1, midZ, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
            buildModuleRoof(box, pal, cell.roofType(), connN, connS, connW, connE, out);
        }

        furnishModuleInterior(box, c, pal, hasAbove, hasBelow, out);
    }

    private void buildZFace(
        int x0, int x1, int z, int y, int y0, int midX, int midY, boolean isTopTrim,
        boolean connW, boolean connE, boolean useDoor, Direction facing,
        BlockIdentifier pillarMat, BlockIdentifier faceMat, MaterialRole faceRole,
        BlockIdentifier windowMat, BlockIdentifier slabMat, BlockIdentifier doorMat,
        MaterialPalette pal, Map<Vec3i, PlacementTarget> out
    ) {
        String faceName = facing.name().toLowerCase();
        int stepZ = facing == Direction.SOUTH ? z + 1 : z - 1;
        for (int x = x0; x <= x1; x++) {
            boolean corner = (x == x0 && !connW) || (x == x1 && !connE);
            boolean isDoorCol = useDoor && x == midX;
            if (isDoorCol && y == y0 + 1) {
                put(out, x, y, z, doorMat.withProperty("facing", faceName).withProperty("half", "lower"), MaterialRole.DOOR);
                put(out, x, y0, stepZ, slabMat.withProperty("type", "bottom"), MaterialRole.SLAB);
            } else if (isDoorCol && y == y0 + 2) {
                put(out, x, y, z, doorMat.withProperty("facing", faceName).withProperty("half", "upper"), MaterialRole.DOOR);
            } else if (isDoorCol && y == y0 + 3) {
                put(out, x, y, z, pal.get(MaterialRole.BEAM), MaterialRole.BEAM);
                put(out, x, y, stepZ, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
            } else if (!useDoor && !isTopTrim && y == midY && x == midX) {
                put(out, x, y, z, windowMat, MaterialRole.WINDOW);
                put(out, x, y - 1, stepZ, slabMat.withProperty("type", "top"), MaterialRole.SLAB);
            } else {
                put(out, x, y, z, corner ? pillarMat : faceMat, corner ? MaterialRole.PILLAR : faceRole);
            }
        }
    }

    private void buildXFace(
        int z0, int z1, int x, int y, int y0, int midZ, int midY, boolean isTopTrim,
        boolean connN, boolean connS, boolean useDoor, Direction facing,
        BlockIdentifier pillarMat, BlockIdentifier faceMat, MaterialRole faceRole,
        BlockIdentifier windowMat, BlockIdentifier slabMat, BlockIdentifier doorMat,
        MaterialPalette pal, Map<Vec3i, PlacementTarget> out
    ) {
        String faceName = facing.name().toLowerCase();
        int stepX = facing == Direction.EAST ? x + 1 : x - 1;
        for (int z = z0; z <= z1; z++) {
            boolean corner = (z == z0 && !connN) || (z == z1 && !connS);
            boolean isDoorCol = useDoor && z == midZ;
            if (isDoorCol && y == y0 + 1) {
                put(out, x, y, z, doorMat.withProperty("facing", faceName).withProperty("half", "lower"), MaterialRole.DOOR);
                put(out, stepX, y0, z, slabMat.withProperty("type", "bottom"), MaterialRole.SLAB);
            } else if (isDoorCol && y == y0 + 2) {
                put(out, x, y, z, doorMat.withProperty("facing", faceName).withProperty("half", "upper"), MaterialRole.DOOR);
            } else if (isDoorCol && y == y0 + 3) {
                put(out, x, y, z, pal.get(MaterialRole.BEAM), MaterialRole.BEAM);
                put(out, stepX, y, z, pal.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
            } else if (!useDoor && !isTopTrim && y == midY && z == midZ) {
                put(out, x, y, z, windowMat, MaterialRole.WINDOW);
                put(out, stepX, y - 1, z, slabMat.withProperty("type", "top"), MaterialRole.SLAB);
            } else {
                put(out, x, y, z, corner ? pillarMat : faceMat, corner ? MaterialRole.PILLAR : faceRole);
            }
        }
    }

    private void furnishModuleInterior(
        BoundingBox3i box, CellCoord3D c, MaterialPalette pal,
        boolean hasAbove, boolean hasBelow, Map<Vec3i, PlacementTarget> out
    ) {
        int ix = box.max().x() - 1;
        int iz = box.max().z() - 1;
        int iy = box.min().y() + 1;
        // Place cozy interior details away from the stairwell at (x0+1, z0+1)
        if (c.gy() == 0) {
            put(out, ix, iy, iz, BlockIdentifier.of("minecraft:crafting_table"), MaterialRole.DECORATION);
            if (!hasAbove) {
                put(out, ix - 1, iy, iz, BlockIdentifier.of("minecraft:chest"), MaterialRole.DECORATION);
            }
            put(out, box.min().x() + 2, iy, box.min().z() + 2, BlockIdentifier.of("minecraft:red_carpet"), MaterialRole.DECORATION);
        } else {
            put(out, ix, iy, iz, BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION);
            if (!hasAbove && !hasBelow) {
                put(out, ix - 1, iy, iz, BlockIdentifier.of("minecraft:oak_slab").withProperty("type", "top"), MaterialRole.DECORATION);
            }
            put(out, box.min().x() + 2, iy, box.min().z() + 2, BlockIdentifier.of("minecraft:blue_carpet"), MaterialRole.DECORATION);
        }
    }

    private void buildModuleRoof(
        BoundingBox3i box,
        MaterialPalette pal,
        RoofGenerator.RoofType roofType,
        boolean connN, boolean connS, boolean connW, boolean connE,
        Map<Vec3i, PlacementTarget> out
    ) {
        int x0 = box.min().x(), z0 = box.min().z();
        int x1 = box.max().x(), z1 = box.max().z();
        int roofY = box.max().y() + 1;
        BlockIdentifier roofStair = pal.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier ridgeMat = pal.get(MaterialRole.ROOF_RIDGE);
        BlockIdentifier overhangMat = pal.get(MaterialRole.ROOF_OVERHANG);

        if (roofType == RoofGenerator.RoofType.FLAT) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = (!connN && z == z0) || (!connS && z == z1) || (!connW && x == x0) || (!connE && x == x1);
                    put(out, x, roofY, z, edge ? ridgeMat : overhangMat, edge ? MaterialRole.ROOF_RIDGE : MaterialRole.ROOF_OVERHANG);
                }
            }
            return;
        }

        // Stepped roof with 1-block eave overhang on exposed edges
        int ex0 = connW ? x0 : x0 - 1;
        int ex1 = connE ? x1 : x1 + 1;
        int ez0 = connN ? z0 : z0 - 1;
        int ez1 = connS ? z1 : z1 + 1;
        int layers = Math.max(2, (cellSize + 2) / 2);

        for (int layer = 0; layer < layers; layer++) {
            int lx0 = connW ? x0 : ex0 + layer;
            int lx1 = connE ? x1 : ex1 - layer;
            int lz0 = connN ? z0 : ez0 + layer;
            int lz1 = connS ? z1 : ez1 - layer;
            if (lx0 > lx1 || lz0 > lz1) break;
            boolean topLayer = (layer == layers - 1) || (lx1 - lx0 <= 1 && lz1 - lz0 <= 1);
            for (int x = lx0; x <= lx1; x++) {
                for (int z = lz0; z <= lz1; z++) {
                    if (topLayer) {
                        put(out, x, roofY + layer, z, ridgeMat, MaterialRole.ROOF_RIDGE);
                    } else if (!connN && z == lz0) {
                        put(out, x, roofY + layer, z, roofStair.withProperty("facing", "south"), MaterialRole.ROOF_SLOPE);
                    } else if (!connS && z == lz1) {
                        put(out, x, roofY + layer, z, roofStair.withProperty("facing", "north"), MaterialRole.ROOF_SLOPE);
                    } else if (!connW && x == lx0) {
                        put(out, x, roofY + layer, z, roofStair.withProperty("facing", "east"), MaterialRole.ROOF_SLOPE);
                    } else if (!connE && x == lx1) {
                        put(out, x, roofY + layer, z, roofStair.withProperty("facing", "west"), MaterialRole.ROOF_SLOPE);
                    } else {
                        put(out, x, roofY + layer, z, overhangMat, MaterialRole.ROOF_OVERHANG);
                    }
                }
            }
        }
    }

    private static void put(Map<Vec3i, PlacementTarget> out, int x, int y, int z, BlockIdentifier block, MaterialRole role) {
        Vec3i p = new Vec3i(x, y, z);
        out.put(p, new PlacementTarget(p, block, role));
    }
}
