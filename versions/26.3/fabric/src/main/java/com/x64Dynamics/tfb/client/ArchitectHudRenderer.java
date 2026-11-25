package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArchitectHudRenderer {
    private ArchitectHudRenderer() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        ClientController controller = ClientController.getInstance();
        if (!controller.isArchitectMode() || !controller.isHudVisible()) return;
        if (mc.gui.screen() != null || mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        ArchitectWorkspaceModel workspaceModel = controller.model();
        BuilderHotbarModel hotbarModel = BuilderHotbarModel.getInstance();

        // 1. Top-Left Architectural Studio Banner
        if (controller.isStudioBannerVisible()) {
            int bannerX = 8;
            int bannerY = 8;
            int bannerWidth = Math.min(420, screenWidth - 16);
            int bannerHeight = 54;

            g.fill(bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight, 0xD80B132B);
            drawBorder(g, bannerX, bannerY, bannerWidth, bannerHeight, 0xFF1E293B);

            String pageName = BuilderHotbarModel.PAGE_NAMES[hotbarModel.getActivePage()];
            String title = "§b§lTERRAFORMABUILDER §8| §fArchitect Mode §a● ACTIVE §8| §ePg " + (hotbarModel.getActivePage() + 1) + "/4: " + pageName;
            g.text(mc.font, title, bannerX + 6, bannerY + 5, 0xFFFFFFFF, true);

            BuilderHotbarSlot activeSlot = hotbarModel.getActiveSlotData();
            String toolName = activeSlot != null ? activeSlot.name() : "None";
            Vec3i origin = workspaceModel.origin();
            String row1 = "§7Tool: §f" + toolName + " §8| §7Size: §b" + workspaceModel.width() + "x" + workspaceModel.height() + "x" + workspaceModel.depth() +
                          " §8| §7Pos: §e(" + origin.x() + "," + origin.y() + "," + origin.z() + ")" +
                          " §8| §7Rot: §6" + workspaceModel.rotation().name();
            g.text(mc.font, row1, bannerX + 6, bannerY + 18, 0xFFFFFFFF, false);

            String flightStatus = controller.isFlightActive() ? "§aON" : "§cOFF";
            String previewStatus = controller.isPreviewActive() ? "§aON" : "§cOFF";
            String row2 = "§7Flight: " + flightStatus + " §8| §7Speed: §e" + String.format("%.1fx", controller.getFlightSpeed()) +
                          " §8| §7Snap: §f" + workspaceModel.selectionManager().gridSnap() +
                          " §8| §7Style: §d" + workspaceModel.stylePreset().displayName() +
                          " §8| §7Blocks: §a" + workspaceModel.totalBlockCount() +
                          " §8| §7[B] Workspace";
            g.text(mc.font, row2, bannerX + 6, bannerY + 29, 0xFFFFFFFF, false);

            String hint = "§6▶ §f" + hotbarModel.getContextualHint();
            g.text(mc.font, hint, bannerX + 6, bannerY + 41, 0xFFFFFFFF, false);
        }

        if (workspaceModel.isShapeEditorMode() && controller.isStudioBannerVisible()) {
            String sid = workspaceModel.selectedSectionId() != null ? workspaceModel.selectedSectionId() : "none";
            String tool = workspaceModel.activeSculptTool().name();
            String secInfo = "§aSculpt: §e" + tool + " §8| §7Section: §b" + sid + " §8| §7Sections: §f" + workspaceModel.activeArchProject().sections().size();
            g.text(mc.font, secInfo, 14, 66, 0xFFFFFFFF, false);
        }

        // 2. Bottom Dedicated Builder Hotbar
        if (controller.isBuilderHotbarVisible()) {
            int hotbarWidth = 202;
            int hotbarX = (screenWidth - hotbarWidth) / 2;
            int hotbarY = screenHeight - 26;
            int pageBarY = hotbarY - 14;

            // Page Indicator Tabs
            g.fill(hotbarX, pageBarY, hotbarX + hotbarWidth, hotbarY - 2, 0xD00F172A);
            drawBorder(g, hotbarX, pageBarY, hotbarWidth, hotbarY - 2 - pageBarY, 0xFF1E293B);

            String[] shortPageNames = { "1: Arch", "2: Infra", "3: Edit", "4: Decor" };
            int tabW = hotbarWidth / 4;
            for (int p = 0; p < 4; p++) {
                int tx = hotbarX + p * tabW;
                if (p == hotbarModel.getActivePage()) {
                    g.fill(tx, pageBarY, tx + tabW, hotbarY - 2, 0x4438BDF8);
                    g.centeredText(mc.font, "§b" + shortPageNames[p], tx + tabW / 2, pageBarY + 2, 0xFF38BDF8);
                } else {
                    g.centeredText(mc.font, "§7" + shortPageNames[p], tx + tabW / 2, pageBarY + 2, 0xFF64748B);
                }
            }

            // Hotbar Dock Container
            g.fill(hotbarX, hotbarY, hotbarX + hotbarWidth, screenHeight - 2, 0xEE0B132B);
            drawBorder(g, hotbarX, hotbarY, hotbarWidth, screenHeight - 2 - hotbarY, 0xFF1E293B);

            // 9 Hotbar Slots
            for (int i = 0; i < 9; i++) {
                int slotX = hotbarX + 3 + i * 22;
                int slotY = hotbarY + 3;
                BuilderHotbarSlot slot = hotbarModel.getSlot(hotbarModel.getActivePage(), i);
                boolean isSelected = (i == hotbarModel.getActiveSlot());

                if (isSelected) {
                    g.fill(slotX, slotY, slotX + 20, slotY + 20, 0x5538BDF8);
                    drawBorder(g, slotX, slotY, 20, 20, 0xFF38BDF8);
                    g.text(mc.font, String.valueOf(i + 1), slotX + 2, slotY + 1, 0xFFF59E0B, true);
                } else {
                    g.fill(slotX, slotY, slotX + 20, slotY + 20, 0x880F172A);
                    drawBorder(g, slotX, slotY, 20, 20, 0xFF334155);
                    g.text(mc.font, String.valueOf(i + 1), slotX + 2, slotY + 1, 0xFF64748B, true);
                }

                if (slot != null) {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(slot.iconItemKey()));
                    if (item != null && item != Items.AIR) {
                        g.item(new ItemStack(item), slotX + 2, slotY + 2);
                    } else {
                        g.centeredText(mc.font, slot.code(), slotX + 10, slotY + 8, slot.accentColor());
                    }
                }
            }

            // Floating Tooltip Pill over Active Slot
            BuilderHotbarSlot activeSlot = hotbarModel.getActiveSlotData();
            if (activeSlot != null && controller.isTooltipsVisible()) {
                Item activeItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(activeSlot.iconItemKey()));
                String tip = activeSlot.name() + " (" + (hotbarModel.getActiveSlot() + 1) + ")";
                int textWidth = mc.font.width(tip);
                int tipWidth = textWidth + 28;
                int tipX = Math.max(hotbarX, Math.min(screenWidth - tipWidth - 4, hotbarX + 3 + hotbarModel.getActiveSlot() * 22 - (tipWidth - 20) / 2));
                int tipY = pageBarY - 18;

                g.fill(tipX, tipY, tipX + tipWidth, tipY + 16, 0xEE0F172A);
                drawBorder(g, tipX, tipY, tipWidth, 16, 0xFF38BDF8);
                if (activeItem != null && activeItem != Items.AIR) {
                    g.item(new ItemStack(activeItem), tipX + 2, tipY);
                }
                g.text(mc.font, tip, tipX + 20, tipY + 4, 0xFFFFFFFF, false);
            }
        }
    }

    private static void drawBorder(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height, color);
        g.fill(x + width - 1, y, x + width, y + height, color);
    }
}
