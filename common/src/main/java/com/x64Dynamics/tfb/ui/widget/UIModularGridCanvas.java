package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.architectural.grid.ModularGridSession;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Supplier;

// Interactive 2D top-down canvas for the Dreamer-style 5x5x5 modular chunk grid.
// Supports click-to-place/stack, hold-and-drag connected placement, and right-click removal.
public class UIModularGridCanvas extends UIWidget {
    private static final int GRID_SPAN = 8;

    private final ModularGridSession session;
    private final Supplier<StylePreset> styleSupplier;
    private final Supplier<MaterialPalette> paletteSupplier;
    private final Supplier<RoofGenerator.RoofType> roofSupplier;
    private final Runnable onModified;

    private boolean draggingLeft = false;
    private int lastDragGx = Integer.MIN_VALUE;
    private int lastDragGz = Integer.MIN_VALUE;

    public UIModularGridCanvas(
        UIBounds bounds,
        ModularGridSession session,
        Supplier<StylePreset> styleSupplier,
        Supplier<MaterialPalette> paletteSupplier,
        Supplier<RoofGenerator.RoofType> roofSupplier,
        Runnable onModified
    ) {
        super(bounds);
        this.session = session;
        this.styleSupplier = styleSupplier;
        this.paletteSupplier = paletteSupplier;
        this.roofSupplier = roofSupplier;
        this.onModified = onModified;
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xCC0F172A);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF334155);

        int pxCell = Math.max(8, Math.min(bounds.width() / GRID_SPAN, bounds.height() / GRID_SPAN));
        int ox = bounds.x() + (bounds.width() - pxCell * GRID_SPAN) / 2;
        int oz = bounds.y() + (bounds.height() - pxCell * GRID_SPAN) / 2;
        int half = GRID_SPAN / 2;
        int gy = session.activeFloor();

        for (int ix = 0; ix < GRID_SPAN; ix++) {
            for (int iz = 0; iz < GRID_SPAN; iz++) {
                int gx = ix - half;
                int gz = iz - half;
                int cx = ox + ix * pxCell;
                int cz = oz + iz * pxCell;

                int topFloor = session.highestFloorAt(gx, gz);
                boolean occupiedHere = session.hasModule(gx, gy, gz);
                if (topFloor >= 0) {
                    int fill = occupiedHere ? 0xCC0EA5E9 : 0x6622C55E;
                    graphics.fill(cx + 1, cz + 1, cx + pxCell, cz + pxCell, fill);
                    // Draw connection indicators to adjacent occupied cells on active floor
                    if (occupiedHere) {
                        if (session.hasModule(gx + 1, gy, gz)) {
                            graphics.fill(cx + pxCell - 2, cz + 3, cx + pxCell + 2, cz + pxCell - 3, 0xFF38BDF8);
                        }
                        if (session.hasModule(gx, gy, gz + 1)) {
                            graphics.fill(cx + 3, cz + pxCell - 2, cx + pxCell - 3, cz + pxCell + 2, 0xFF38BDF8);
                        }
                    }
                    graphics.drawCenteredText((topFloor + 1) + "F", cx + pxCell / 2, cz + Math.max(2, (pxCell - 8) / 2), 0xFFF8FAFC);
                } else {
                    graphics.drawBorder(cx, cz, pxCell, pxCell, 0x33475569);
                }

                var sel = session.selectedCell();
                if (sel != null && sel.gx() == gx && sel.gz() == gz) {
                    graphics.drawBorder(cx, cz, pxCell, pxCell, 0xFFFACC15);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !enabled || !bounds.contains(mouseX, mouseY)) return false;
        int[] cell = hitTest(mouseX, mouseY);
        if (cell == null) return false;
        int gx = cell[0], gz = cell[1];

        if (button == 1) {
            if (session.removeTopModule(gx, gz)) {
                onModified.run();
            }
            return true;
        }
        if (button == 0) {
            draggingLeft = true;
            lastDragGx = gx;
            lastDragGz = gz;
            session.stackOrPlace(gx, gz, styleSupplier.get(), paletteSupplier.get(), roofSupplier.get());
            onModified.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (!draggingLeft || button != 0 || !bounds.contains(mouseX, mouseY)) return false;
        int[] cell = hitTest(mouseX, mouseY);
        if (cell == null) return false;
        int gx = cell[0], gz = cell[1];
        if (gx != lastDragGx || gz != lastDragGz) {
            lastDragGx = gx;
            lastDragGz = gz;
            if (!session.hasModule(gx, session.activeFloor(), gz)) {
                session.placeModule(gx, session.activeFloor(), gz, styleSupplier.get(), paletteSupplier.get(), roofSupplier.get());
                onModified.run();
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0 && draggingLeft) {
            draggingLeft = false;
            lastDragGx = Integer.MIN_VALUE;
            lastDragGz = Integer.MIN_VALUE;
            return true;
        }
        return false;
    }

    private int[] hitTest(int mouseX, int mouseY) {
        int pxCell = Math.max(8, Math.min(bounds.width() / GRID_SPAN, bounds.height() / GRID_SPAN));
        int ox = bounds.x() + (bounds.width() - pxCell * GRID_SPAN) / 2;
        int oz = bounds.y() + (bounds.height() - pxCell * GRID_SPAN) / 2;
        int ix = (mouseX - ox) / pxCell;
        int iz = (mouseY - oz) / pxCell;
        if (ix < 0 || ix >= GRID_SPAN || iz < 0 || iz >= GRID_SPAN) return null;
        int half = GRID_SPAN / 2;
        return new int[]{ix - half, iz - half};
    }
}
