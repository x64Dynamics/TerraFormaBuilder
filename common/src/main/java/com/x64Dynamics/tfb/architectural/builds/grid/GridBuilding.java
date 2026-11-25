package com.x64Dynamics.tfb.architectural.builds.grid;

import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.*;

// Independent architectural building object registered within the shared project grid.
public final class GridBuilding {
    public enum FootprintShape {
        RECTANGLE,
        SQUARE,
        L_SHAPED,
        U_SHAPED,
        CUSTOM
    }

    public enum BuildingFunction {
        RESIDENTIAL,
        COMMERCIAL,
        CIVIC,
        TOWER,
        COURTYARD,
        STORAGE
    }

    private final String id;
    private int gridX;
    private int gridZ;
    private int baseY;
    private int width;
    private int depth;
    private int stories;
    private int storyHeight;
    private FootprintShape footprint;
    private Direction facing;
    private StylePreset style;
    private MaterialPalette palette;
    private RoofGenerator.RoofType roofType;
    private BuildingFunction function;
    private boolean allowExpansion;
    private long seed;
    private final Set<GridManager.GridCoord> customCells = new LinkedHashSet<>();

    public GridBuilding(
        String id,
        int gridX,
        int gridZ,
        int baseY,
        int width,
        int depth,
        int stories,
        int storyHeight,
        FootprintShape footprint,
        Direction facing,
        StylePreset style,
        MaterialPalette palette,
        RoofGenerator.RoofType roofType,
        BuildingFunction function,
        boolean allowExpansion,
        long seed
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.baseY = baseY;
        this.width = Math.max(3, width);
        this.depth = footprint == FootprintShape.SQUARE ? this.width : Math.max(3, depth);
        this.stories = Math.max(1, stories);
        this.storyHeight = Math.max(3, Math.min(8, storyHeight));
        this.footprint = footprint != null ? footprint : FootprintShape.RECTANGLE;
        this.facing = (facing != null && facing != Direction.UP && facing != Direction.DOWN) ? facing : Direction.SOUTH;
        this.style = style != null ? style : StylePreset.MEDIEVAL;
        this.palette = palette != null ? palette : this.style.createPalette();
        this.roofType = roofType != null ? roofType : RoofGenerator.RoofType.GABLE;
        this.function = function != null ? function : BuildingFunction.RESIDENTIAL;
        this.allowExpansion = allowExpansion;
        this.seed = seed;
    }

    public String id() { return id; }
    public int gridX() { return gridX; }
    public int gridZ() { return gridZ; }
    public int baseY() { return baseY; }
    public int width() { return width; }
    public int depth() { return depth; }
    public int stories() { return stories; }
    public int storyHeight() { return storyHeight; }
    public int height() { return stories * storyHeight + (roofType == RoofGenerator.RoofType.FLAT ? 2 : Math.max(3, Math.min(width, depth) / 2 + 2)); }
    public FootprintShape footprint() { return footprint; }
    public Direction facing() { return facing; }
    public StylePreset style() { return style; }
    public MaterialPalette palette() { return palette; }
    public RoofGenerator.RoofType roofType() { return roofType; }
    public BuildingFunction function() { return function; }
    public boolean allowExpansion() { return allowExpansion; }
    public long seed() { return seed; }

    public void setPosition(int gridX, int gridZ, int baseY) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.baseY = baseY;
    }

    public void setSize(int width, int depth) {
        this.width = Math.max(3, width);
        this.depth = this.footprint == FootprintShape.SQUARE ? this.width : Math.max(3, depth);
    }

    public void setStories(int stories) { this.stories = Math.max(1, stories); }
    public void setStoryHeight(int storyHeight) { this.storyHeight = Math.max(3, Math.min(8, storyHeight)); }
    public void setFacing(Direction facing) {
        if (facing != null && facing != Direction.UP && facing != Direction.DOWN) this.facing = facing;
    }
    public void rotateClockwise() {
        this.facing = this.facing.rotateY();
        int tmp = this.width;
        this.width = this.depth;
        this.depth = tmp;
    }
    public void setStyle(StylePreset style) {
        this.style = style;
        this.palette = style.createPalette();
    }
    public void setRoofType(RoofGenerator.RoofType roofType) { this.roofType = roofType; }
    public void setFunction(BuildingFunction function) { this.function = function; }
    public void setAllowExpansion(boolean allowExpansion) { this.allowExpansion = allowExpansion; }
    public void setCustomCells(Collection<GridManager.GridCoord> cells) {
        customCells.clear();
        customCells.addAll(cells);
    }

    public List<Integer> floorElevations() {
        List<Integer> list = new ArrayList<>(stories);
        for (int s = 0; s < stories; s++) {
            list.add(baseY + s * storyHeight);
        }
        return list;
    }

    public int topWallY() {
        return baseY + stories * storyHeight;
    }

    public Set<GridManager.GridCoord> occupiedCells() {
        Set<GridManager.GridCoord> cells = new LinkedHashSet<>();
        if (footprint == FootprintShape.CUSTOM && !customCells.isEmpty()) {
            for (var c : customCells) {
                cells.add(new GridManager.GridCoord(gridX + c.gx(), gridZ + c.gz()));
            }
            return cells;
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (footprint == FootprintShape.L_SHAPED && dx >= width / 2 && dz >= depth / 2) {
                    continue;
                }
                if (footprint == FootprintShape.U_SHAPED && dx >= width / 3 && dx < (width * 2) / 3 && dz >= depth / 2) {
                    continue;
                }
                cells.add(new GridManager.GridCoord(gridX + dx, gridZ + dz));
            }
        }
        return cells;
    }
}
