package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates batched, hierarchy-aware 3D grid geometry (major/minor lines,
 * cell state highlights, building footprint bounds, and connection vectors)
 * for efficient rendering inside Minecraft without per-cell line duplication.
 */
public final class GridRendererModel {
    public record GridLine(Vec3i start, Vec3i end, int argbColor, float width) { }
    public record CellHighlight(int gx, int gz, int y, GridManager.CellState state, int argbColor) { }

    public record RenderBatch(
        List<GridLine> lines,
        List<CellHighlight> highlights
    ) { }

    private GridRendererModel() { }

    public static RenderBatch buildBatch(
        GridManager grid,
        BuildingRegistry registry,
        GridManager.GridCoord selectedCell,
        String selectedBuildingId,
        int planeY
    ) {
        List<GridLine> lines = new ArrayList<>();
        List<CellHighlight> highlights = new ArrayList<>();

        int cs = grid.cellSize();
        int gw = grid.gridWidth();
        int gd = grid.gridDepth();
        Vec3i org = grid.origin();
        int maxX = org.x() + gw * cs;
        int maxZ = org.z() + gd * cs;

        // Batched full-span grid lines + vertical chunk-slice pillars (major every 4 cells)
        int sliceTopY = planeY + 10;
        for (int gx = 0; gx <= gw; gx++) {
            boolean major = (gx % 4 == 0);
            int color = major ? 0xCC38BDF8 : 0x4464748B;
            float w = major ? 2.0f : 1.0f;
            int wx = org.x() + gx * cs;
            lines.add(new GridLine(new Vec3i(wx, planeY, org.z()), new Vec3i(wx, planeY, maxZ), color, w));
            if (major) {
                lines.add(new GridLine(new Vec3i(wx, planeY + 5, org.z()), new Vec3i(wx, planeY + 5, maxZ), 0x4438BDF8, 1.0f));
                for (int gz = 0; gz <= gd; gz += 4) {
                    int wz = org.z() + gz * cs;
                    lines.add(new GridLine(new Vec3i(wx, planeY, wz), new Vec3i(wx, sliceTopY, wz), 0x8838BDF8, 1.5f));
                }
            }
        }
        for (int gz = 0; gz <= gd; gz++) {
            boolean major = (gz % 4 == 0);
            int color = major ? 0xCC38BDF8 : 0x4464748B;
            float w = major ? 2.0f : 1.0f;
            int wz = org.z() + gz * cs;
            lines.add(new GridLine(new Vec3i(org.x(), planeY, wz), new Vec3i(maxX, planeY, wz), color, w));
            if (major) {
                lines.add(new GridLine(new Vec3i(org.x(), planeY + 5, wz), new Vec3i(maxX, planeY + 5, wz), 0x4438BDF8, 1.0f));
            }
        }

        // Only emit highlights for non-AVAILABLE cells or selected cell
        for (int gx = 0; gx < gw; gx++) {
            for (int gz = 0; gz < gd; gz++) {
                var state = grid.getCellState(gx, gz);
                if (state != GridManager.CellState.AVAILABLE) {
                    highlights.add(new CellHighlight(gx, gz, planeY, state, colorForState(state)));
                }
            }
        }
        if (selectedCell != null && grid.inBounds(selectedCell.gx(), selectedCell.gz())) {
            highlights.add(new CellHighlight(selectedCell.gx(), selectedCell.gz(), planeY, GridManager.CellState.RESERVED, 0xCCFACC15));
        }

        // Building footprint bounding frames & connection lines
        for (GridBuilding b : registry.allBuildings()) {
            boolean isSel = b.id().equals(selectedBuildingId);
            int color = isSel ? 0xFFFACC15 : 0xFF22C55E;
            var box = grid.toWorldBounds(b);
            int x0 = box.min().x(), z0 = box.min().z();
            int x1 = box.max().x() + 1, z1 = box.max().z() + 1;
            int yTop = box.min().y() + b.height();
            lines.add(new GridLine(new Vec3i(x0, yTop, z0), new Vec3i(x1, yTop, z0), color, 2.5f));
            lines.add(new GridLine(new Vec3i(x1, yTop, z0), new Vec3i(x1, yTop, z1), color, 2.5f));
            lines.add(new GridLine(new Vec3i(x1, yTop, z1), new Vec3i(x0, yTop, z1), color, 2.5f));
            lines.add(new GridLine(new Vec3i(x0, yTop, z1), new Vec3i(x0, yTop, z0), color, 2.5f));
        }

        for (var conn : registry.connections()) {
            int cColor = switch (conn.status()) {
                case CONFIRMED, MANUAL_OVERRIDE -> 0xFF34D399;
                case PROPOSED -> 0xFFF59E0B;
                case PROHIBITED -> 0xFFEF4444;
            };
            lines.add(new GridLine(conn.anchorA(), conn.anchorB(), cColor, 2.5f));
        }

        return new RenderBatch(lines, highlights);
    }

    private static int colorForState(GridManager.CellState state) {
        return switch (state) {
            case OCCUPIED -> 0x6622C55E;
            case BLOCKED -> 0x88EF4444;
            case RESERVED -> 0x66F59E0B;
            case CONNECTED -> 0x6638BDF8;
            case AVAILABLE -> 0x00000000;
        };
    }
}
