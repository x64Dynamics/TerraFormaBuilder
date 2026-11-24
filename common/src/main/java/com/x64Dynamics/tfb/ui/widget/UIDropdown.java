package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.List;
import java.util.function.Consumer;

public class UIDropdown<T> extends UIWidget {
    private final String label;
    private final List<T> options;
    private int selectedIndex = 0;
    private boolean open = false;
    private final Consumer<T> selectionAction;

    public UIDropdown(UIBounds bounds, String label, List<T> options, int initialIndex, Consumer<T> selectionAction) {
        super(bounds);
        this.label = label;
        this.options = options;
        this.selectedIndex = Math.max(0, Math.min(options.size() - 1, initialIndex));
        this.selectionAction = selectionAction;
    }

    public T selected() {
        return options.isEmpty() ? null : options.get(selectedIndex);
    }

    public void select(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
            if (selectionAction != null) {
                selectionAction.accept(selected());
            }
        }
    }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        // Header box
        int bgColor = hovered ? 0xFF334155 : 0xFF1E293B;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bgColor);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF64748B);

        String text = label + ": " + (selected() != null ? selected().toString() : "None") + (open ? " [^]" : " [v]");
        graphics.drawText(text, bounds.x() + 4, bounds.y() + (bounds.height() - 8) / 2, 0xFFFFFFFF, false);

        // Dropdown options popup
        if (open) {
            int itemHeight = bounds.height();
            int totalHeight = options.size() * itemHeight;
            int listY = bounds.y() + bounds.height();

            graphics.fill(bounds.x(), listY, bounds.x() + bounds.width(), listY + totalHeight, 0xFF0F172A);
            graphics.drawBorder(bounds.x(), listY, bounds.width(), totalHeight, 0xFF38BDF8);

            for (int i = 0; i < options.size(); i++) {
                int optionY = listY + i * itemHeight;
                boolean optionHovered = mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width()
                    && mouseY >= optionY && mouseY < optionY + itemHeight;

                int optionBg = i == selectedIndex ? 0xFF0369A1 : (optionHovered ? 0xFF1E293B : 0xFF0F172A);
                graphics.fill(bounds.x() + 1, optionY + 1, bounds.x() + bounds.width() - 1, optionY + itemHeight - 1, optionBg);
                graphics.drawText(options.get(i).toString(), bounds.x() + 6, optionY + (itemHeight - 8) / 2, 0xFFE2E8F0, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !enabled) return false;

        // Click on header
        if (bounds.contains(mouseX, mouseY) && button == 0) {
            open = !open;
            return true;
        }

        // Click on options
        if (open && button == 0) {
            int itemHeight = bounds.height();
            int listY = bounds.y() + bounds.height();
            int totalHeight = options.size() * itemHeight;

            if (mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width() && mouseY >= listY && mouseY < listY + totalHeight) {
                int clickedIndex = (mouseY - listY) / itemHeight;
                select(clickedIndex);
                open = false;
                return true;
            } else {
                open = false;
            }
        }
        return false;
    }
}