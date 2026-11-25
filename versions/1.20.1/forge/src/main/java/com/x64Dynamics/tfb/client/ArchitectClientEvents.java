package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.ui.controller.ClientController;
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

import com.x64Dynamics.tfb.geometry.Rotation;
public final class ArchitectClientEvents {
    private ArchitectClientEvents() {}

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (var key : ArchitectKeyMappings.ALL) {
                event.register(key);
            }
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("tfb_hud", (gui, guiGraphics, partialTick, width, height) -> {
                ArchitectHudRenderer.render(guiGraphics, Minecraft.getInstance());
            });
        }
    }

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        private static ForgeLevelAccess liveLevelAccess;
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.level != null) {
                if (liveLevelAccess == null) {
                    liveLevelAccess = new ForgeLevelAccess(mc.level);
                    ClientController.getInstance().model().setLiveLevelAccess(liveLevelAccess);
                } else {
                    liveLevelAccess.setLevel(mc.level);
                }
            }

            ArchitectFlightController.tick(mc);

            while (ArchitectKeyMappings.OPEN_WORKSPACE.consumeClick()) {
                if (mc.screen instanceof TerraFormaScreen) {
                    mc.setScreen(null);
                } else if (mc.screen == null) {
                    mc.setScreen(new TerraFormaScreen());
                }
            }

            while (ArchitectKeyMappings.TOGGLE_PREVIEW.consumeClick()) {
                ClientController ctrl = ClientController.getInstance();
                boolean next = !ctrl.isPreviewActive();
                ctrl.setPreviewActive(next);
                mc.player.displayClientMessage(
                        Component.literal("§b[TFB] Live Preview: " + (next ? "§aENABLED" : "§cDISABLED")),
                        true
                );
            }

            while (ArchitectKeyMappings.EXECUTE_BUILD.consumeClick()) {
                executeBuild(mc);
            }

            while (ArchitectKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                ArchitectFlightController.toggleFlight(mc);
            }

            while (ArchitectKeyMappings.SPEED_UP.consumeClick()) {
                ClientController.getInstance().adjustFlightSpeed(0.2f);
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", ClientController.getInstance().getFlightSpeed())),
                        true
                );
            }

            while (ArchitectKeyMappings.SPEED_DOWN.consumeClick()) {
                ClientController.getInstance().adjustFlightSpeed(-0.2f);
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", ClientController.getInstance().getFlightSpeed())),
                        true
                );
            }

            while (ArchitectKeyMappings.ROTATE_TOOL.consumeClick()) {
                ArchitectWorkspaceModel model = ClientController.getInstance().model();
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
            while (ArchitectKeyMappings.UNDO.consumeClick()) {
                if (ClientController.getInstance().model().undo()) {
                    mc.player.displayClientMessage(Component.literal("§e[TFB] Reverted last design operation."), true);
                } else {
                    mc.player.displayClientMessage(Component.literal("§7[TFB] Nothing to undo."), true);
                }
            }

            while (ArchitectKeyMappings.REDO.consumeClick()) {
                if (ClientController.getInstance().model().redo()) {
                    mc.player.displayClientMessage(Component.literal("§e[TFB] Restored undone design operation."), true);
                } else {
                    mc.player.displayClientMessage(Component.literal("§7[TFB] Nothing to redo."), true);
                }
            }
            if (ClientController.getInstance().isArchitectMode() && mc.screen == null) {
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
            if (!ClientController.getInstance().isArchitectMode()) return;
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
            if (!ClientController.getInstance().isArchitectMode()) return;

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
        public static void onMouseButton(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null || mc.level == null) return;
            ClientController controller = ClientController.getInstance();
            if (!controller.isArchitectMode() || event.getAction() != 1) return;

            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                    && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var clicked = bhr.getBlockPos();
                var pos = new com.x64Dynamics.tfb.geometry.Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());
                if (event.getButton() == 0) {
                    controller.model().setPos1(pos);
                    mc.player.displayClientMessage(Component.literal("§a[TFB] POS1 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"), true);
                    event.setCanceled(true);
                } else if (event.getButton() == 1) {
                    controller.model().setPos2(pos);
                    ArchitectWorkspaceModel m = controller.model();
                    mc.player.displayClientMessage(Component.literal("§6[TFB] POS2 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") §8| §b" + m.width() + "x" + m.height() + "x" + m.depth()), true);
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientController.getInstance().setArchitectMode(false);
            ArchitectFlightController.restorePreviousState(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
            ClientController.getInstance().setArchitectMode(false);
            ArchitectFlightController.restorePreviousState(Minecraft.getInstance());
        }

        public static void executeBuild(Minecraft mc) {
            net.minecraft.world.level.Level level = mc.level;
            if (level == null || mc.player == null) return;
            ArchitectWorkspaceModel model = ClientController.getInstance().model();
            java.util.List<com.x64Dynamics.tfb.architectural.PlacementTarget> targets = model.cachedPreviewTargets();
            if (targets.isEmpty()) {
                mc.player.displayClientMessage(Component.literal("§c[TFB] Build failed: No preview blocks available!"), false);
                return;
            }
            java.util.List<com.x64Dynamics.tfb.construction.ScheduledBlockPlacement> scheduled =
                com.x64Dynamics.tfb.construction.BuildScheduler.scheduleWithDependencies(targets);
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    server.execute(() -> {
                        int placed = 0;
                        java.util.List<com.x64Dynamics.tfb.construction.editing.WorldConstructionHistory.BlockChange> changes = new java.util.ArrayList<>();
                        for (com.x64Dynamics.tfb.construction.ScheduledBlockPlacement target : scheduled) {
                            com.x64Dynamics.tfb.geometry.Vec3i pos = target.pos();
                            net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
                            net.minecraft.world.level.block.state.BlockState prevState = serverLevel.getBlockState(bp);
                            net.minecraft.world.level.block.state.BlockState state = BlockStateParser.parse(target.block());
                            if (!prevState.equals(state) && serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                                changes.add(new com.x64Dynamics.tfb.construction.editing.WorldConstructionHistory.BlockChange(
                                    pos, BlockStateParser.fromBlockState(prevState), target.block()
                                ));
                            }
                        }
                        if (!changes.isEmpty()) {
                            com.x64Dynamics.tfb.construction.editing.WorldConstructionHistory.getInstance().recordTransaction(changes);
                        }
                        final int finalPlaced = placed;
                        mc.execute(() -> mc.player.displayClientMessage(
                            Component.literal("§a[TFB] Construction complete! Placed " + finalPlaced + " blocks."), false
                        ));
                    });
                }
            }
        }
    }
}