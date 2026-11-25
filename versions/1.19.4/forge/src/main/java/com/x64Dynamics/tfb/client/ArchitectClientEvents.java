package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.controller.ClientController;
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
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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
            event.registerAboveAll("tfb_hud", (gui, poseStack, partialTick, width, height) -> {
                ArchitectHudRenderer.render(poseStack, Minecraft.getInstance());
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
                ClientController.getInstance().togglePreview();
                boolean active = ClientController.getInstance().isPreviewActive();
                mc.player.displayClientMessage(
                        Component.literal("§b[TFB] In-World Preview: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                );
            }

            while (ArchitectKeyMappings.EXECUTE_BUILD.consumeClick()) {
                executeBuild(mc);
            }

            while (ArchitectKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                ClientController.getInstance().toggleFlight();
                boolean active = ClientController.getInstance().isFlightActive();
                mc.player.displayClientMessage(
                        Component.literal("§b[TFB] Architect Flight: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                );
            }

            while (ArchitectKeyMappings.SPEED_UP.consumeClick()) {
                ClientController.getInstance().adjustFlightSpeed(0.2f);
                float spd = ClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)),
                        true
                );
            }

            while (ArchitectKeyMappings.SPEED_DOWN.consumeClick()) {
                ClientController.getInstance().adjustFlightSpeed(-0.2f);
                float spd = ClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(
                        Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)),
                        true
                );
            }

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
                BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                for (int i = 0; i < 9; i++) {
                    if (mc.options.keyHotbarSlots[i].consumeClick()) {
                        hotbar.setSlot(i);
                        BuilderHotbarSlot s = hotbar.getActiveSlotData();
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
            Minecraft mc = Minecraft.getInstance();
            ArchitectFlightController.restorePreviousState(mc);
            ClientController.getInstance().setArchitectMode(false);
        }

        private static void executeBuild(Minecraft mc) {
            Level level = mc.level;
            if (level == null || mc.player == null) return;

            ArchitectWorkspaceModel model = ClientController.getInstance().model();
            List<PlacementTarget> targets = model.cachedPreviewTargets();
            if (targets.isEmpty()) {
                mc.player.displayClientMessage(
                    Component.nullToEmpty("§c[TFB] Build failed: No preview blocks available! Set POS1/POS2 or configure a brush in Workspace [B]."),
                    false
                );
                return;
            }

            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    List<com.x64Dynamics.tfb.construction.ScheduledBlockPlacement> scheduled =
                        com.x64Dynamics.tfb.construction.BuildScheduler.scheduleWithDependencies(targets);
                    server.execute(() -> {
                        int placed = 0;
                        java.util.List<com.x64Dynamics.tfb.construction.editing.WorldConstructionHistory.BlockChange> changes = new java.util.ArrayList<>();
                        for (com.x64Dynamics.tfb.construction.ScheduledBlockPlacement target : scheduled) {
                            Vec3i pos = target.pos();
                            BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                            BlockState prevState = serverLevel.getBlockState(bp);
                            BlockState state = BlockStateParser.parse(target.block());
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
                        serverLevel.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        mc.execute(() -> {
                            mc.player.displayClientMessage(
                                Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + finalPlaced + " blocks with server lighting."),
                                false
                            );
                        });
                    });
                    return;
                }
            }

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
                Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + placed + " client blocks."),
                false
            );
        }
    }
}
