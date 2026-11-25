package com.x64Dynamics.tfb.ui.model;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.geometry.VoxelVolume;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

// Interactive layer-by-layer 3D voxel grid editor model (inspired by VoxelDreamer)
// supporting slice navigation, paint/erase/flood-fill/extrude/eyedropper tools,
// X/Z symmetry mirroring, undo/redo, and live 3D preview export.
public final class VoxelGridEditorModel {
    public enum GridTool {
        PAINT("Paint"),
        ERASE("Erase"),
        FLOOD_FILL("Fill"),
        EXTRUDE_UP("Extrude"),
        EYEDROPPER("Pick");

        private final String label;
        GridTool(String label) { this.label = label; }
        public String label() { return label; }
    }

    private final int width;
    private final int height;
    private final int depth;
    private int activeSliceY = 0;
    private GridTool activeTool = GridTool.PAINT;
    private MaterialRole activeRole = MaterialRole.WALL_PRIMARY;
    private boolean mirrorX = false;
    private boolean mirrorZ = false;

    private final Map<Vec3i, MaterialRole> cells = new LinkedHashMap<>();
    private final Deque<Map<Vec3i, MaterialRole>> undoStack = new ArrayDeque<>();
    private final Deque<Map<Vec3i, MaterialRole>> redoStack = new ArrayDeque<>();

    public VoxelGridEditorModel(int width, int height, int depth) {
        this.width = Math.max(4, width);
        this.height = Math.max(4, height);
        this.depth = Math.max(4, depth);
    }

    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int activeSliceY() { return activeSliceY; }
    public void setActiveSliceY(int y) { this.activeSliceY = Math.max(0, Math.min(height - 1, y)); }
    public GridTool activeTool() { return activeTool; }
    public void setActiveTool(GridTool tool) { this.activeTool = tool; }
    public MaterialRole activeRole() { return activeRole; }
    public void setActiveRole(MaterialRole role) { this.activeRole = role; }
    public boolean isMirrorX() { return mirrorX; }
    public void setMirrorX(boolean mirrorX) { this.mirrorX = mirrorX; }
    public boolean isMirrorZ() { return mirrorZ; }
    public void setMirrorZ(boolean mirrorZ) { this.mirrorZ = mirrorZ; }
    public int voxelCount() { return cells.size(); }

    public MaterialRole cellAt(int x, int y, int z) {
        return cells.get(new Vec3i(x, y, z));
    }

    public void pushUndoSnapshot() {
        undoStack.push(new LinkedHashMap<>(cells));
        redoStack.clear();
        while (undoStack.size() > 32) undoStack.removeLast();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        redoStack.push(new LinkedHashMap<>(cells));
        cells.clear();
        cells.putAll(undoStack.pop());
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        undoStack.push(new LinkedHashMap<>(cells));
        cells.clear();
        cells.putAll(redoStack.pop());
        return true;
    }

    public void applyAt(int x, int z) {
        if (x < 0 || x >= width || z < 0 || z >= depth) return;
        if (activeTool == GridTool.EYEDROPPER) {
            MaterialRole picked = cellAt(x, activeSliceY, z);
            if (picked != null) activeRole = picked;
            return;
        }
        pushUndoSnapshot();
        switch (activeTool) {
            case PAINT -> setWithMirror(x, activeSliceY, z, activeRole);
            case ERASE -> setWithMirror(x, activeSliceY, z, null);
            case FLOOD_FILL -> floodFillSlice(x, z, activeSliceY, activeRole);
            case EXTRUDE_UP -> {
                for (int y = activeSliceY; y < height; y++) {
                    setWithMirror(x, y, z, activeRole);
                }
            }
            case EYEDROPPER -> {}
        }
    }

    private void setWithMirror(int x, int y, int z, MaterialRole role) {
        setSingle(x, y, z, role);
        if (mirrorX) setSingle(width - 1 - x, y, z, role);
        if (mirrorZ) setSingle(x, y, depth - 1 - z, role);
        if (mirrorX && mirrorZ) setSingle(width - 1 - x, y, depth - 1 - z, role);
    }

    private void setSingle(int x, int y, int z, MaterialRole role) {
        Vec3i key = new Vec3i(x, y, z);
        if (role == null) cells.remove(key);
        else cells.put(key, role);
    }

    private void floodFillSlice(int startX, int startZ, int y, MaterialRole replacement) {
        MaterialRole target = cellAt(startX, y, startZ);
        if (Objects.equals(target, replacement)) return;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startZ});
        boolean[][] visited = new boolean[width][depth];
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cz = cur[1];
            if (cx < 0 || cx >= width || cz < 0 || cz >= depth || visited[cx][cz]) continue;
            if (!Objects.equals(cellAt(cx, y, cz), target)) continue;
            visited[cx][cz] = true;
            setWithMirror(cx, y, cz, replacement);
            queue.add(new int[]{cx + 1, cz});
            queue.add(new int[]{cx - 1, cz});
            queue.add(new int[]{cx, cz + 1});
            queue.add(new int[]{cx, cz - 1});
        }
    }

    public void copySliceUp() {
        if (activeSliceY + 1 >= height) return;
        pushUndoSnapshot();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                MaterialRole r = cellAt(x, activeSliceY, z);
                if (r != null) setSingle(x, activeSliceY + 1, z, r);
            }
        }
        activeSliceY++;
    }

    public void clearAll() {
        if (cells.isEmpty()) return;
        pushUndoSnapshot();
        cells.clear();
    }

    public void stampHollowRoom(int margin) {
        int m = Math.max(0, Math.min(Math.min(width, depth) / 2 - 2, margin));
        pushUndoSnapshot();
        for (int x = m; x < width - m; x++) {
            for (int z = m; z < depth - m; z++) {
                boolean isEdge = (x == m || x == width - 1 - m || z == m || z == depth - 1 - m);
                if (activeSliceY == 0) {
                    setSingle(x, 0, z, isEdge ? MaterialRole.FOUNDATION : MaterialRole.FLOOR);
                } else if (isEdge) {
                    setSingle(x, activeSliceY, z, activeRole);
                }
            }
        }
    }

    public List<PlacementTarget> toPlacementTargets(Vec3i origin, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>(cells.size());
        for (var e : cells.entrySet()) {
            MaterialRole role = e.getValue();
            BlockIdentifier block = palette.get(role);
            targets.add(new PlacementTarget(origin.add(e.getKey()), block, role));
        }
        return targets;
    }

    public VoxelVolume toVoxelVolume(Vec3i origin, MaterialPalette palette) {
        VoxelVolume vol = new VoxelVolume();
        for (PlacementTarget t : toPlacementTargets(origin, palette)) {
            vol.set(t.pos(), t.block());
        }
        return vol;
    }

    public void fillRect(int x1, int z1, int x2, int z2) {
        int minX = Math.max(0, Math.min(x1, x2));
        int maxX = Math.min(width - 1, Math.max(x1, x2));
        int minZ = Math.max(0, Math.min(z1, z2));
        int maxZ = Math.min(depth - 1, Math.max(z1, z2));
        pushUndoSnapshot();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setWithMirror(x, activeSliceY, z, activeRole);
            }
        }
    }

    public com.x64Dynamics.tfb.architectural.assets.AuthoredAsset toAuthoredAsset(String id, String name, MaterialPalette palette) {
        List<com.x64Dynamics.tfb.architectural.assets.AuthoredAsset.RelativeBlock> blocks = new ArrayList<>();
        for (var e : cells.entrySet()) {
            Vec3i p = e.getKey();
            blocks.add(new com.x64Dynamics.tfb.architectural.assets.AuthoredAsset.RelativeBlock(
                p.x(), p.y(), p.z(), palette.get(e.getValue()).asString(), e.getValue().name()
            ));
        }
        return new com.x64Dynamics.tfb.architectural.assets.AuthoredAsset(
            id, name, com.x64Dynamics.tfb.architectural.assets.AuthoredAsset.AssetCategory.BUILDING,
            width, height, depth, 0,
            com.x64Dynamics.tfb.geometry.Direction.SOUTH, true, blocks
        );
    }

    public void loadAuthoredAsset(com.x64Dynamics.tfb.architectural.assets.AuthoredAsset asset) {
        if (asset == null) return;
        pushUndoSnapshot();
        cells.clear();
        for (var rb : asset.blocks()) {
            if (rb.rx() >= 0 && rb.rx() < width && rb.ry() >= 0 && rb.ry() < height && rb.rz() >= 0 && rb.rz() < depth) {
                MaterialRole role = MaterialRole.WALL_PRIMARY;
                try {
                    if (rb.roleName() != null && !rb.roleName().isBlank()) role = MaterialRole.valueOf(rb.roleName());
                } catch (IllegalArgumentException ignored) {}
                setSingle(rb.rx(), rb.ry(), rb.rz(), role);
            }
        }
    }
}
