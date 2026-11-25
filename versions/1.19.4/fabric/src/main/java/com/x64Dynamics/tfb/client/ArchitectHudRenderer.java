package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArchitectHudRenderer {
    private ArchitectHudRenderer() {}

    public static void render(PoseStack ps, Minecraft mc) {
        ClientController controller = ClientController.getInstance();
        if (!controller.isArchitectMode() || !controller.isHudVisible()) return;
        if (mc.screen != null || mc.player == null) return;

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

            GuiComponent.fill(ps, bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight, 0xD80B132B);
            drawBorder(ps, bannerX, bannerY, bannerWidth, bannerHeight, 0xFF1E293B);

            String pageName = BuilderHotbarModel.PAGE_NAMES[hotbarModel.getActivePage()];
            String title = "§b§lTERRAFORMABUILDER §8| §fArchitect Mode §a● ACTIVE §8| §ePg " + (hotbarModel.getActivePage() + 1) + "/4: " + pageName;
            mc.font.drawShadow(ps, title, bannerX + 6, bannerY + 5, 0xFFFFFFFF);

            BuilderHotbarSlot activeSlot = hotbarModel.getActiveSlotData();
            String toolName = activeSlot != null ? activeSlot.name() : "None";
            Vec3i origin = workspaceModel.origin();
            String row1 = "§7Tool: §f" + toolName + " §8| §7Size: §b" + workspaceModel.width() + "x" + workspaceModel.height() + "x" + workspaceModel.depth() +
                          " §8| §7Pos: §e(" + origin.x() + "," + origin.y() + "," + origin.z() + ")" +
                          " §8| §7Rot: §6" + workspaceModel.rotation().name();
            mc.font.draw(ps, row1, bannerX + 6, bannerY + 18, 0xFFFFFFFF);

            String flightStatus = controller.isFlightActive() ? "§aON" : "§cOFF";
            String previewStatus = controller.isPreviewActive() ? "§aON" : "§cOFF";
            String row2 = "§7Flight: " + flightStatus + " §8| §7Speed: §e" + String.format("%.1fx", controller.getFlightSpeed()) +
                          " §8| §7Snap: §f" + workspaceModel.selectionManager().gridSnap() +
                          " §8| §7Style: §d" + workspaceModel.stylePreset().displayName() +
                          " §8| §7Blocks: §a" + workspaceModel.totalBlockCount() +
                          " §8| §7[B] Workspace";
            mc.font.draw(ps, row2, bannerX + 6, bannerY + 29, 0xFFFFFFFF);

            String hint = "§6▶ §f" + hotbarModel.getContextualHint();
            mc.font.draw(ps, hint, bannerX + 6, bannerY + 41, 0xFFFFFFFF);
        }

        // 2. Bottom Dedicated Builder Hotbar
        if (controller.isBuilderHotbarVisible()) {
            int hotbarWidth = 202;
            int hotbarX = (screenWidth - hotbarWidth) / 2;
            int hotbarY = screenHeight - 26;
            int pageBarY = hotbarY - 14;

            // Page Selector / Category Tabs indicator
            int activePage = hotbarModel.getActivePage();
            for (int p = 0; p < 4; p++) {
                int tabX = hotbarX + (p * 50);
                int tabW = 48;
                boolean isCurrentTab = (p == activePage);
                int tabColor = isCurrentTab ? 0xD838BDF8 : 0xA00F172A;
                GuiComponent.fill(ps, tabX, pageBarY, tabX + tabW, pageBarY + 12, tabColor);
                drawBorder(ps, tabX, pageBarY, tabW, 12, isCurrentTab ? 0xFF0284C7 : 0xFF1E293B);

                String tabTitle = "§f" + (p + 1) + ". " + (isCurrentTab ? "§e" : "§7") + BuilderHotbarModel.PAGE_NAMES[p];
                mc.font.draw(ps, tabTitle, tabX + 3, pageBarY + 2, 0xFFFFFFFF);
            }

            // Main 9-slot Hotbar Container
            GuiComponent.fill(ps, hotbarX, hotbarY, hotbarX + hotbarWidth, hotbarY + 24, 0xD8090D1A);
            drawBorder(ps, hotbarX, hotbarY, hotbarWidth, 24, 0xFF334155);

            int activeSlot = hotbarModel.getActiveSlot();

            for (int i = 0; i < 9; i++) {
                int slotX = hotbarX + 3 + (i * 22);
                int slotY = hotbarY + 3;
                boolean isSelected = (i == activeSlot);

                if (isSelected) {
                    GuiComponent.fill(ps, slotX, slotY, slotX + 20, slotY + 20, 0x5538BDF8);
                    drawBorder(ps, slotX, slotY, 20, 20, 0xFF38BDF8);
                    mc.font.draw(ps, String.valueOf(i + 1), slotX + 2, slotY + 1, 0xFFF59E0B);
                } else {
                    GuiComponent.fill(ps, slotX, slotY, slotX + 20, slotY + 20, 0x880F172A);
                    drawBorder(ps, slotX, slotY, 20, 20, 0xFF334155);
                    mc.font.draw(ps, String.valueOf(i + 1), slotX + 2, slotY + 1, 0xFF64748B);
                }

                BuilderHotbarSlot slot = hotbarModel.getSlot(hotbarModel.getActivePage(), i);
                if (slot != null) {
                    ItemStack iconStack = resolveItem(slot.iconItemKey());
                    mc.getItemRenderer().renderAndDecorateItem(ps, iconStack, slotX + 2, slotY + 2);
                }
            }

            BuilderHotbarSlot activeSlotData = hotbarModel.getActiveSlotData();
            if (activeSlotData != null && controller.isTooltipsVisible()) {
                String tip = activeSlotData.name() + " (" + (activeSlot + 1) + ")";
                int textWidth = mc.font.width(tip);
                int tipWidth = textWidth + 28;
                int tipX = Math.max(hotbarX, Math.min(screenWidth - tipWidth - 4, hotbarX + 3 + activeSlot * 22 - (tipWidth - 20) / 2));
                int tipY = pageBarY - 18;

                GuiComponent.fill(ps, tipX, tipY, tipX + tipWidth, tipY + 16, 0xEE0F172A);
                drawBorder(ps, tipX, tipY, tipWidth, 16, 0xFF38BDF8);
                ItemStack iconStack = resolveItem(activeSlotData.iconItemKey());
                mc.getItemRenderer().renderAndDecorateItem(ps, iconStack, tipX + 2, tipY);
                mc.font.draw(ps, tip, tipX + 20, tipY + 4, 0xFFFFFFFF);
            }
        }
    }

    private static ItemStack resolveItem(String itemKey) {
        if (itemKey == null || itemKey.isEmpty()) {
            return new ItemStack(Items.STONE);
        }
        ResourceLocation loc = new ResourceLocation(itemKey);
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.STONE);
        }
        return new ItemStack(item);
    }

    private static void drawBorder(PoseStack ps, int x, int y, int width, int height, int color) {
        GuiComponent.fill(ps, x, y, x + width, y + 1, color);
        GuiComponent.fill(ps, x, y + height - 1, x + width, y + height, color);
        GuiComponent.fill(ps, x, y + 1, x + 1, y + height, color);
        GuiComponent.fill(ps, x + width - 1, y, x + width, y + height, color);
    }
}
