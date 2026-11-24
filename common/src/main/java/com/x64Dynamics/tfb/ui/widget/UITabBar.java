package com.x64Dynamics.tfb.ui.widget;

import com.x64Dynamics.tfb.ui.model.WorkspaceTab;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;

import java.util.function.Consumer;

public class UITabBar extends UIWidget {
    private final WorkspaceTab[] tabs = WorkspaceTab.values();
    private WorkspaceTab activeTab = WorkspaceTab.ARCHITECTURE;
    private final Consumer<WorkspaceTab> tabSelectAction;

    public UITabBar(UIBounds bounds, Consumer<WorkspaceTab> tabSelectAction) {
        super(bounds);
        this.tabSelectAction = tabSelectAction;
    }

    public WorkspaceTab activeTab() { return activeTab; }

    public void setActiveTab(WorkspaceTab tab) {
        if (tab != null && tab != this.activeTab) {
            this.activeTab = tab;
            if (tabSelectAction != null) {
                tabSelectAction.accept(this.activeTab);
            }
        }
    }

    @Override
    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        if (!visible) return;
        updateHover(mouseX, mouseY);

        int tabWidth = bounds.width() / tabs.length;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF0F172A);
        graphics.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF334155);

        for (int i = 0; i < tabs.length; i++) {
            WorkspaceTab tab = tabs[i];
            int tx = bounds.x() + i * tabWidth;
            int tw = (i == tabs.length - 1) ? (bounds.x() + bounds.width() - tx) : tabWidth;
            boolean isCurrent = tab == activeTab;
            boolean tabHovered = mouseX >= tx && mouseX < tx + tw && mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height();

            int bg = isCurrent ? 0xFF0284C7 : (tabHovered ? 0xFF1E293B : 0xFF0F172A);
            graphics.fill(tx + 1, bounds.y() + 1, tx + tw - 1, bounds.y() + bounds.height() - 1, bg);
            graphics.drawCenteredText(tab.title(), tx + tw / 2, bounds.y() + (bounds.height() - 8) / 2, isCurrent ? 0xFFFFFFFF : 0xFF94A3B8);

            if (tabHovered) {
                graphics.drawTooltip(tab.description(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button) && button == 0) {
            int tabWidth = bounds.width() / tabs.length;
            int index = (mouseX - bounds.x()) / tabWidth;
            if (index >= 0 && index < tabs.length) {
                setActiveTab(tabs[index]);
                return true;
            }
        }
        return false;
    }
}