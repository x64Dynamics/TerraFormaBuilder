package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

public abstract class UIWidget {
    protected UIBounds bounds;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean hovered = false;
    protected String tooltip = null;

    public UIWidget(UIBounds bounds) {
        this.bounds = bounds;
    }

    public UIBounds bounds() { return bounds; }
    public void setBounds(UIBounds bounds) { this.bounds = bounds; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isHovered() { return hovered; }
    public String tooltip() { return tooltip; }
    public void setTooltip(String tooltip) { this.tooltip = tooltip; }

    public void updateHover(int mouseX, int mouseY) {
        this.hovered = visible && bounds.contains(mouseX, mouseY);
    }

    public abstract void render(IGuiGraphicsContext graphics, int mouseX, int mouseY);

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return visible && enabled && bounds.contains(mouseX, mouseY);
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }
}