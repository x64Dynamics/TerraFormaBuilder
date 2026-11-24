package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

public final class TFBClientEvents {
    private TFBClientEvents() {}

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            for (var key : TFBKeyMappings.ALL) {
                ClientRegistry.registerKeyBinding(key);
            }
            OverlayRegistry.registerOverlayTop("tfb_hud", (gui, poseStack, partialTick, width, height) -> {
                TFBInWorldHud.render(poseStack, Minecraft.getInstance());
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
                TFBClientController.getInstance().togglePreview();
                boolean active = TFBClientController.getInstance().isPreviewActive();
                mc.player.displayClientMessage(
                        Component.nullToEmpty("§b[TFB] In-World Preview: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                );
            }

            while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                executeBuild(mc);
            }

            while (TFBKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                TFBClientController.getInstance().toggleFlight();
                boolean active = TFBClientController.getInstance().isFlightActive();
                mc.player.displayClientMessage(
                        Component.nullToEmpty("§b[TFB] Architect Flight: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                );
            }

            while (TFBKeyMappings.SPEED_UP.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(
                        Component.nullToEmpty(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)),
                        true
                );
            }

            while (TFBKeyMappings.SPEED_DOWN.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(-0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(
                        Component.nullToEmpty(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)),
                        true
                );
            }

            if (TFBClientController.getInstance().isArchitectMode() && mc.screen == null) {
                BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                for (int i = 0; i < 9; i++) {
                    if (mc.options.keyHotbarSlots[i].consumeClick()) {
                        hotbar.setSlot(i);
                        BuilderHotbarSlot s = hotbar.getActiveSlotData();
                        if (s != null) {
                            mc.player.displayClientMessage(Component.nullToEmpty("§b[TFB Tool " + (i + 1) + "] §f" + s.name()), true);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGameOverlayEvent.PreLayer event) {
            if (!TFBClientController.getInstance().isArchitectMode()) return;

            var overlay = event.getOverlay();
            if (overlay == ForgeIngameGui.HOTBAR_ELEMENT
                    || overlay == ForgeIngameGui.EXPERIENCE_BAR_ELEMENT
                    || overlay == ForgeIngameGui.ITEM_NAME_ELEMENT) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollEvent event) {
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
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
            Minecraft mc = Minecraft.getInstance();
            ArchitectFlightController.restorePreviousState(mc);
            TFBClientController.getInstance().setArchitectMode(false);
        }

        private static void executeBuild(Minecraft mc) {
            Level level = mc.level;
            if (level == null || mc.player == null) return;

            ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
            List<PlacementTarget> targets = model.cachedPreviewTargets();

            int placed = 0;
            for (PlacementTarget target : targets) {
                Vec3i pos = target.pos();
                BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                BlockState state = BlockStateParser.parse(target.block());
                if (level.setBlock(bp, state, 3)) {
                    placed++;
                }
            }

            mc.player.displayClientMessage(
                    Component.nullToEmpty("§a[TFB] Build Executed: Placed " + placed + " blocks!"),
                    false
            );
        }
    }
}
