package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.grid.*;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.BiConsumer;

/**
 * Interactive 2D top-down architectural site-plan canvas rendering the project grid,
 * occupied/blocked/connected cells, building footprints with facing indicators,
 * and connection graph lines. Clicking selects a cell or building.
 */
public class UIGridComplexCanvas extends UIWidget {
    private final StructurePlacementController controller;
    private final BiConsumer<Integer, Integer> onCellClicked;

    public UIGridComplexCanvas(
        UIBounds bounds,
        StructurePlacementController controller,
        BiConsumer<Integer, Integer> onCellClicked
    ) {
        super(bounds);
        this.controller = controller;
        this.onCellClicked = onCellClicked;
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xCC0B1120);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF334155);

        int viewCells = 32;
        int cellPx = Math.max(3, Math.min(bounds.width() / viewCells, bounds.height() / viewCells));
        int ox = bounds.x() + (bounds.width() - cellPx * viewCells) / 2;
        int oz = bounds.y() + (bounds.height() - cellPx * viewCells) / 2;

        GridManager grid = controller.grid();

        // Draw grid cells (major lines every 8)
        for (int gx = 0; gx < viewCells; gx++) {
            for (int gz = 0; gz < viewCells; gz++) {
                int cx = ox + gx * cellPx;
                int cz = oz + gz * cellPx;
                var state = grid.getCellState(gx, gz);
                if (state != GridManager.CellState.AVAILABLE) {
                    int fill = switch (state) {
                        case OCCUPIED -> 0x6622C55E;
                        case BLOCKED -> 0x88EF4444;
                        case RESERVED -> 0x66F59E0B;
                        case CONNECTED -> 0x6638BDF8;
                        default -> 0;
                    };
                    graphics.fill(cx + 1, cz + 1, cx + cellPx, cz + cellPx, fill);
                }
                int border = (gx % 8 == 0 || gz % 8 == 0) ? 0x5538BDF8 : 0x22475569;
                graphics.drawBorder(cx, cz, cellPx, cellPx, border);
            }
        }

        // Draw building outlines + facing arrow
        for (GridBuilding b : controller.registry().allBuildings()) {
            boolean sel = b.id().equals(controller.selectedBuildingId());
            int bx = ox + b.gridX() * cellPx;
            int bz = oz + b.gridZ() * cellPx;
            int bw = b.width() * cellPx;
            int bd = b.depth() * cellPx;
            graphics.drawBorder(bx, bz, bw, bd, sel ? 0xFFFACC15 : 0xFF34D399);

            // Facing indicator dot on front edge
            int fx = bx + bw / 2;
            int fz = bz + bd / 2;
            if (b.facing() == Direction.NORTH) fz = bz;
            else if (b.facing() == Direction.SOUTH) fz = bz + bd - 2;
            else if (b.facing() == Direction.WEST) fx = bx;
            else if (b.facing() == Direction.EAST) fx = bx + bw - 2;
            graphics.fill(fx - 1, fz - 1, fx + 2, fz + 2, 0xFF38BDF8);
        }

        // Draw selected cell highlight
        var sc = controller.selectedCell();
        if (sc != null && sc.gx() >= 0 && sc.gx() < viewCells && sc.gz() >= 0 && sc.gz() < viewCells) {
            graphics.drawBorder(ox + sc.gx() * cellPx, oz + sc.gz() * cellPx, cellPx, cellPx, 0xFFFACC15);
        }

        // Hover highlight
        if (bounds.contains(mouseX, mouseY)) {
            int hx = (mouseX - ox) / cellPx;
            int hz = (mouseY - oz) / cellPx;
            if (hx >= 0 && hx < viewCells && hz >= 0 && hz < viewCells) {
                graphics.drawBorder(ox + hx * cellPx, oz + hz * cellPx, cellPx, cellPx, 0xAAFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) return false;
        int viewCells = 32;
        int cellPx = Math.max(3, Math.min(bounds.width() / viewCells, bounds.height() / viewCells));
        int ox = bounds.x() + (bounds.width() - cellPx * viewCells) / 2;
        int oz = bounds.y() + (bounds.height() - cellPx * viewCells) / 2;
        int gx = (mouseX - ox) / cellPx;
        int gz = (mouseY - oz) / cellPx;
        if (gx < 0 || gx >= viewCells || gz < 0 || gz >= viewCells) return false;

        controller.selectCell(new GridManager.GridCoord(gx, gz));
        var owners = controller.grid().ownersAt(gx, gz);
        if (!owners.isEmpty()) {
            controller.selectBuilding(owners.iterator().next());
        }
        if (onCellClicked != null) onCellClicked.accept(gx, gz);
        return true;
    }
}
