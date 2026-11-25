package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.ui.model.VoxelGridEditorModel;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

/**
 * Interactive 2D top-down slice canvas for the VoxelDreamer-style grid editor,
 * featuring onion-skinning of the layer below, symmetry axes, and drag painting.
 */
public class UIVoxelGridCanvas extends UIWidget {
    private final VoxelGridEditorModel grid;
    private final Runnable onModified;

    public UIVoxelGridCanvas(UIBounds bounds, VoxelGridEditorModel grid, Runnable onModified) {
        super(bounds);
        this.grid = grid;
        this.onModified = onModified;
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xCC0F172A);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF334155);

        int gw = grid.width();
        int gd = grid.depth();
        int cellSize = Math.max(4, Math.min(bounds.width() / gw, bounds.height() / gd));
        int gridPxW = cellSize * gw;
        int gridPxH = cellSize * gd;
        int ox = bounds.x() + (bounds.width() - gridPxW) / 2;
        int oz = bounds.y() + (bounds.height() - gridPxH) / 2;
        int y = grid.activeSliceY();

        for (int gx = 0; gx < gw; gx++) {
            for (int gz = 0; gz < gd; gz++) {
                int cx = ox + gx * cellSize;
                int cz = oz + gz * cellSize;
                MaterialRole cur = grid.cellAt(gx, y, gz);
                if (cur != null) {
                    graphics.fill(cx + 1, cz + 1, cx + cellSize, cz + cellSize, colorForRole(cur));
                } else if (y > 0 && grid.cellAt(gx, y - 1, gz) != null) {
                    // Onion-skin ghost of layer below
                    graphics.fill(cx + 1, cz + 1, cx + cellSize, cz + cellSize, 0x3364748B);
                }
                graphics.drawBorder(cx, cz, cellSize, cellSize, 0x22475569);
            }
        }

        if (grid.isMirrorX()) {
            int mx = ox + (gw * cellSize) / 2;
            graphics.fill(mx, oz, mx + 1, oz + gridPxH, 0xAA38BDF8);
        }
        if (grid.isMirrorZ()) {
            int mz = oz + (gd * cellSize) / 2;
            graphics.fill(ox, mz, ox + gridPxW, mz + 1, 0xAA38BDF8);
        }

        if (bounds.contains(mouseX, mouseY)) {
            int hx = (mouseX - ox) / cellSize;
            int hz = (mouseY - oz) / cellSize;
            if (hx >= 0 && hx < gw && hz >= 0 && hz < gd) {
                graphics.drawBorder(ox + hx * cellSize, oz + hz * cellSize, cellSize, cellSize, 0xFFFACC15);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) return false;
        return handleGridPointer(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (!visible || !enabled || !bounds.contains(mouseX, mouseY)) return false;
        return handleGridPointer(mouseX, mouseY, button);
    }

    private boolean handleGridPointer(int mouseX, int mouseY, int button) {
        int gw = grid.width();
        int gd = grid.depth();
        int cellSize = Math.max(4, Math.min(bounds.width() / gw, bounds.height() / gd));
        int ox = bounds.x() + (bounds.width() - cellSize * gw) / 2;
        int oz = bounds.y() + (bounds.height() - cellSize * gd) / 2;
        int gx = (mouseX - ox) / cellSize;
        int gz = (mouseY - oz) / cellSize;
        if (gx < 0 || gx >= gw || gz < 0 || gz >= gd) return false;

        if (button == 1) {
            VoxelGridEditorModel.GridTool prev = grid.activeTool();
            grid.setActiveTool(VoxelGridEditorModel.GridTool.ERASE);
            grid.applyAt(gx, gz);
            grid.setActiveTool(prev);
        } else {
            grid.applyAt(gx, gz);
        }
        if (onModified != null) onModified.run();
        return true;
    }

    private static int colorForRole(MaterialRole role) {
        return switch (role) {
            case WALL_PRIMARY -> 0xFFE2E8F0;
            case WALL_ACCENT, WALL_CORNER -> 0xFF94A3B8;
            case FLOOR, CEILING -> 0xFFD97706;
            case ROOF_SLOPE, ROOF_RIDGE, ROOF_GABLE, ROOF_OVERHANG -> 0xFFB45309;
            case PILLAR, BEAM, ARCH -> 0xFF78350F;
            case FOUNDATION, SUB_FOUNDATION -> 0xFF64748B;
            case WINDOW -> 0xFF38BDF8;
            case DOOR -> 0xFF22C55E;
            case LIGHT_SOURCE -> 0xFFFACC15;
            default -> 0xFFA855F7;
        };
    }
}
