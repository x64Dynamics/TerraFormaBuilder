package com.x64Dynamics.tfb.ui.rendering;

public interface IGuiGraphicsContext {
    void fill(int x1, int y1, int x2, int y2, int color);
    void drawBorder(int x, int y, int width, int height, int color);
    void drawText(String text, int x, int y, int color, boolean shadow);
    void drawCenteredText(String text, int centerX, int y, int color);
    void drawTooltip(String tooltip, int mouseX, int mouseY);
    void enableScissor(int x, int y, int width, int height);
    void disableScissor();
}