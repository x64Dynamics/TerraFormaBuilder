package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Consumer;

public class UIToggleButton extends UIWidget {
    private final String label;
    private boolean state;
    private final Consumer<Boolean> toggleAction;

    public UIToggleButton(UIBounds bounds, String label, boolean initialState, Consumer<Boolean> toggleAction) {
        super(bounds);
        this.label = label;
        this.state = initialState;
        this.toggleAction = toggleAction;
    }

    public boolean state() { return state; }

    public void setState(boolean state) {
        if (this.state != state) {
            this.state = state;
            if (toggleAction != null) {
                toggleAction.accept(this.state);
            }
        }
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        int bgColor = state ? 0xFF065F46 : 0xFF3F3F46;
        int borderColor = state ? 0xFF10B981 : 0xFF71717A;
        int indicatorColor = state ? 0xFF34D399 : 0xFFA1A1AA;

        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bgColor);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), borderColor);

        String text = label + ": " + (state ? "ON" : "OFF");
        graphics.drawText(text, bounds.x() + 4, bounds.y() + (bounds.height() - 8) / 2, 0xFFFFFFFF, false);

        int indSize = bounds.height() - 6;
        int indX = bounds.x() + bounds.width() - indSize - 3;
        int indY = bounds.y() + 3;
        graphics.fill(indX, indY, indX + indSize, indY + indSize, indicatorColor);

        if (hovered && tooltip != null) {
            graphics.drawTooltip(tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            setState(!state);
            return true;
        }
        return false;
    }
}