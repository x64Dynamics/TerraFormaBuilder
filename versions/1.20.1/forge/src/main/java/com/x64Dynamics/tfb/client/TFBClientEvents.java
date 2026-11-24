package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class TFBClientEvents {
    private TFBClientEvents() {}

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (var key : TFBKeyMappings.ALL) {
                event.register(key);
            }
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("tfb_hud", (gui, guiGraphics, partialTick, width, height) -> {
                TFBInWorldHud.render(guiGraphics, Minecraft.getInstance());
            });
        }
    }

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ArchitectFlightController.tick(mc);

            while (TFBKeyMappings.OPEN_WORKSPACE.consumeClick()) {
                if (mc.screen instanceof TerraFormaScreen) {
                    mc.setScreen(null);
                } else if (mc.screen == null) {
                    mc.setScreen(new TerraFormaScreen());
                }
            }

            while (TFBKeyMappings.TOGGLE_PREVIEW.consumeClick()) {
                TFBClientController ctrl = TFBClientController.getInstance();
                boolean next = !ctrl.isPreviewActive();
                ctrl.setPreviewActive(next);
                mc.player.displayClientMessage(
                        Component.literal("§b[TFB] Live Preview: " + (next ? "§aENABLED" : "§cDISABLED")),
                        true
                );
            }

            while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                mc.player.displayClientMessage(
                        Component.literal("§a[TFB] Build execution started..."),
                        true
                );
            }

            while (TFBKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                ArchitectFlightController.toggleFlight(mc);
            }

            while (TFBKeyMappings.SPEED_UP.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(0.2f);
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", TFBClientController.getInstance().getFlightSpeed())),
                        true
                );
            }

            while (TFBKeyMappings.SPEED_DOWN.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(-0.2f);
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", TFBClientController.getInstance().getFlightSpeed())),
                        true
                );
            }

            while (TFBKeyMappings.ROTATE_TOOL.consumeClick()) {
                ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
                com.x64Dynamics.tfb.geometry.Rotation next = switch (model.rotation()) {
                    case NONE -> com.x64Dynamics.tfb.geometry.Rotation.CLOCKWISE_90;
                    case CLOCKWISE_90 -> com.x64Dynamics.tfb.geometry.Rotation.CLOCKWISE_180;
                    case CLOCKWISE_180 -> com.x64Dynamics.tfb.geometry.Rotation.COUNTERCLOCKWISE_90;
                    case COUNTERCLOCKWISE_90 -> com.x64Dynamics.tfb.geometry.Rotation.NONE;
                };
                model.setRotation(next);
                mc.player.displayClientMessage(
                        Component.literal("§b[TFB] Rotation: §e" + next.name()),
                        true
                );
            }

            // Builder Hotbar number keys 1–9
            if (TFBClientController.getInstance().isArchitectMode() && mc.screen == null) {
                for (int i = 0; i < 9; i++) {
                    if (mc.options.keyHotbarSlots[i].consumeClick()) {
                        BuilderHotbarModel.getInstance().setSlot(i);
                        BuilderHotbarSlot s = BuilderHotbarModel.getInstance().getActiveSlotData();
                        if (s != null) {
                            mc.player.displayClientMessage(Component.literal("§b[TFB Tool " + (i + 1) + "] §f" + s.name()), true);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
            if (!TFBClientController.getInstance().isArchitectMode()) return;
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())
                    || event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                    || event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;
            if (!TFBClientController.getInstance().isArchitectMode()) return;

            double scrollDelta = event.getScrollDelta();
            if (scrollDelta != 0) {
                BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                if (Screen.hasShiftDown()) {
                    if (scrollDelta > 0) {
                        hotbar.prevPage();
                    } else {
                        hotbar.nextPage();
                    }
                } else {
                    if (scrollDelta > 0) {
                        hotbar.prevSlot();
                    } else {
                        hotbar.nextSlot();
                    }
                }
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            TFBClientController.getInstance().setArchitectMode(false);
            ArchitectFlightController.restorePreviousState(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
            TFBClientController.getInstance().setArchitectMode(false);
            ArchitectFlightController.restorePreviousState(Minecraft.getInstance());
        }
    }
}
