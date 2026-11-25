package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

public class UIProgressBar extends UIWidget {
    private float progress = 0.0f; // 0.0 to 1.0
    private String statusText = "";

    public UIProgressBar(UIBounds bounds) {
        super(bounds);
    }

    public float progress() { return progress; }
    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    public String statusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;

        // Background
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF18181B);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF3F3F46);

        // Progress fill
        int fillWidth = (int) (bounds.width() * progress);
        if (fillWidth > 0) {
            graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + fillWidth - 1, bounds.y() + bounds.height() - 1, 0xFF10B981);
        }

        // Percentage text
        int percent = Math.round(progress * 100);
        String label = (statusText.isEmpty() ? "" : statusText + " - ") + percent + "%";
        graphics.drawCenteredText(label, bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - 8) / 2, 0xFFFFFFFF);
    }
}