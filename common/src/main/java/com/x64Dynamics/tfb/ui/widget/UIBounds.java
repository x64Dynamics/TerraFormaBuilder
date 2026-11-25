package com.x64Dynamics.tfb.ui.widget;

public record UIBounds(int x, int y, int width, int height) {
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}