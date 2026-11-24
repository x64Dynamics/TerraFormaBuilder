package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Consumer;

public class UITextField extends UIWidget {
    private String text;
    private final String placeholder;
    private boolean focused = false;
    private final Consumer<String> textChangeAction;

    public UITextField(UIBounds bounds, String initialText, String placeholder, Consumer<String> textChangeAction) {
        super(bounds);
        this.text = initialText != null ? initialText : "";
        this.placeholder = placeholder != null ? placeholder : "";
        this.textChangeAction = textChangeAction;
    }

    public String text() { return text; }

    public void setText(String text) {
        this.text = text != null ? text : "";
        if (textChangeAction != null) {
            textChangeAction.accept(this.text);
        }
    }

    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        int bg = focused ? 0xFF0F172A : 0xFF1E293B;
        int border = focused ? 0xFF38BDF8 : 0xFF475569;

        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bg);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);

        if (text.isEmpty() && !focused) {
            graphics.drawText(placeholder, bounds.x() + 4, bounds.y() + (bounds.height() - 8) / 2, 0xFF64748B, false);
        } else {
            String display = text + (focused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            graphics.drawText(display, bounds.x() + 4, bounds.y() + (bounds.height() - 8) / 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            focused = true;
            return true;
        } else {
            focused = false;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focused && !Character.isISOControl(codePoint)) {
            setText(text + codePoint);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused) {
            // 259 is GLFW Backspace
            if (keyCode == 259 && !text.isEmpty()) {
                setText(text.substring(0, text.length() - 1));
                return true;
            }
        }
        return false;
    }
}