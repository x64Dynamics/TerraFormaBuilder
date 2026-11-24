package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Consumer;

public class UISlider extends UIWidget {
    private final String prefix;
    private final int min;
    private final int max;
    private int value;
    private boolean dragging = false;
    private final Consumer<Integer> valueChangeAction;

    public UISlider(UIBounds bounds, String prefix, int min, int max, int initialValue, Consumer<Integer> valueChangeAction) {
        super(bounds);
        this.prefix = prefix;
        this.min = min;
        this.max = Math.max(min, max);
        this.value = Math.max(min, Math.min(max, initialValue));
        this.valueChangeAction = valueChangeAction;
    }

    public int value() { return value; }

    public void setValue(int value) {
        int clamped = Math.max(min, Math.min(max, value));
        if (clamped != this.value) {
            this.value = clamped;
            if (valueChangeAction != null) {
                valueChangeAction.accept(this.value);
            }
        }
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        // Background track
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF1E293B);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF475569);

        // Filled track
        float ratio = (float) (value - min) / (max - min > 0 ? max - min : 1);
        int fillWidth = (int) (bounds.width() * ratio);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + fillWidth, bounds.y() + bounds.height(), 0xFF0284C7);

        // Handle
        int handleX = bounds.x() + fillWidth - 3;
        graphics.fill(Math.max(bounds.x(), handleX), bounds.y(), Math.min(bounds.x() + bounds.width(), handleX + 6), bounds.y() + bounds.height(), 0xFF38BDF8);

        // Label
        String label = prefix + ": " + value;
        graphics.drawCenteredText(label, bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2, 0xFFFFFFFF);

        if (hovered && tooltip != null) {
            graphics.drawTooltip(tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            dragging = true;
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    private void updateValueFromMouse(int mouseX) {
        float ratio = (float) (mouseX - bounds.x()) / bounds.width();
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        setValue(Math.round(min + ratio * (max - min)));
    }
}