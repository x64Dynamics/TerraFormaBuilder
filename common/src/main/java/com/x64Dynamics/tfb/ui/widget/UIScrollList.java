package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UIScrollList<T> extends UIWidget {
    private List<T> items;
    private final int itemHeight;
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private final BiConsumer<T, Integer> itemSelectAction;
    private Consumer<T> doubleClickAction;
    private java.util.function.Function<T, String> labelProvider = Object::toString;

    public UIScrollList(UIBounds bounds, int itemHeight, List<T> items, BiConsumer<T, Integer> itemSelectAction) {
        super(bounds);
        this.itemHeight = itemHeight;
        this.items = items;
        this.itemSelectAction = itemSelectAction;
    }

    public void setLabelProvider(java.util.function.Function<T, String> provider) {
        this.labelProvider = provider != null ? provider : Object::toString;
    }

    public void setItems(List<T> items) {
        this.items = items;
        this.scrollOffset = 0;
        this.selectedIndex = -1;
    }

    public T selectedItem() {
        return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex) : null;
    }

    public int selectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int index) { this.selectedIndex = index; }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        // Background
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF0F172A);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF334155);

        graphics.enableScissor(bounds.x(), bounds.y(), bounds.width(), bounds.height());

        int visibleCount = bounds.height() / itemHeight + 2;
        int startIndex = Math.max(0, scrollOffset / itemHeight);
        int endIndex = Math.min(items.size(), startIndex + visibleCount);

        for (int i = startIndex; i < endIndex; i++) {
            T item = items.get(i);
            int itemY = bounds.y() + (i * itemHeight) - scrollOffset;

            boolean isSelected = i == selectedIndex;
            boolean itemHovered = mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width() - 8
                && mouseY >= itemY && mouseY < itemY + itemHeight;

            int bg = isSelected ? 0xFF0284C7 : (itemHovered ? 0xFF1E293B : 0xFF0F172A);
            graphics.fill(bounds.x() + 1, itemY, bounds.x() + bounds.width() - 9, itemY + itemHeight, bg);

            int textColor = isSelected ? 0xFFFFFFFF : (itemHovered ? 0xFFE2E8F0 : 0xFF94A3B8);
            String label = labelProvider != null ? labelProvider.apply(item) : String.valueOf(item);
            graphics.drawText(label, bounds.x() + 4, itemY + (itemHeight - 8) / 2, textColor, false);
        }

        graphics.disableScissor();

        // Scrollbar
        int totalContentHeight = items.size() * itemHeight;
        if (totalContentHeight > bounds.height()) {
            int barWidth = 6;
            int barX = bounds.x() + bounds.width() - barWidth - 1;
            float viewRatio = (float) bounds.height() / totalContentHeight;
            int thumbHeight = Math.max(12, (int) (bounds.height() * viewRatio));
            float scrollRatio = (float) scrollOffset / (totalContentHeight - bounds.height());
            int thumbY = bounds.y() + (int) ((bounds.height() - thumbHeight) * scrollRatio);

            graphics.fill(barX, bounds.y(), barX + barWidth, bounds.y() + bounds.height(), 0xFF1E293B);
            graphics.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xFF64748B);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            int clickedY = mouseY - bounds.y() + scrollOffset;
            int index = clickedY / itemHeight;
            if (index >= 0 && index < items.size()) {
                this.selectedIndex = index;
                if (itemSelectAction != null) {
                    itemSelectAction.accept(items.get(index), index);
                }
                return true;
            }
        }
        return false;
    }

    public void scroll(double amount) {
        int totalHeight = items.size() * itemHeight;
        int maxScroll = Math.max(0, totalHeight - bounds.height());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - amount * itemHeight));
    }
}