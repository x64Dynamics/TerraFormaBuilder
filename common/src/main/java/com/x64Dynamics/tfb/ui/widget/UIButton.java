package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Consumer;

public class UIButton extends UIWidget {
    private String label;
    private final Consumer<UIButton> clickAction;
    private boolean active = false;

    public UIButton(UIBounds bounds, String label, Consumer<UIButton> clickAction) {
        super(bounds);
        this.label = label;
        this.clickAction = clickAction;
    }

    public String label() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        int bgColor;
        if (!enabled) {
            bgColor = 0xFF333333;
        } else if (active) {
            bgColor = 0xFF2A6F97;
        } else if (hovered) {
            bgColor = 0xFF4682B4;
        } else {
            bgColor = 0xFF1E293B;
        }

        int borderColor = enabled ? (hovered ? 0xFF90E0EF : 0xFF64748B) : 0xFF222222;
        int textColor = enabled ? (hovered ? 0xFFFFFFFF : 0xFFE2E8F0) : 0xFF777777;

        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bgColor);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), borderColor);
        graphics.drawCenteredText(label, bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2, textColor);

        if (hovered && tooltip != null && !tooltip.isEmpty()) {
            graphics.drawTooltip(tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            if (clickAction != null) {
                clickAction.accept(this);
            }
            return true;
        }
        return false;
    }
}