package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

import com.x64Dynamics.tfb.geometry.Rotation;
public final class ArchitectClientEvents {
    private ArchitectClientEvents() {}

    @EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.registerCategory(ArchitectKeyMappings.CATEGORY);
            event.registerCategory(ArchitectKeyMappings.CATEGORY);
            event.registerCategory(ArchitectKeyMappings.CATEGORY);
            for (var key : ArchitectKeyMappings.ALL) {
                event.register(key);
            }
        }
    }

    @EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
    public static class ForgeEvents {
        private static NeoForgeLevelAccess liveLevelAccess;
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.level != null) {
                if (liveLevelAccess == null) {
                    liveLevelAccess = new NeoForgeLevelAccess(mc.level);
                    ClientController.getInstance().model().setLiveLevelAccess(liveLevelAccess);
                } else {
                    liveLevelAccess.setLevel(mc.level);
                }
            }

            ArchitectFlightController.tick(mc);
            ArchitectWorldRenderer.renderGizmos();

            if (mc.gui.screen() == null) {
                while (ArchitectKeyMappings.OPEN_WORKSPACE.consumeClick()) {
                    mc.gui.setScreen(new TerraFormaScreen());
                }

                while (ArchitectKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                    ClientController.getInstance().toggleFlight();
                    boolean active = ClientController.getInstance().isFlightActive();
                    mc.player.sendOverlayMessage(Component.literal("§b[TFB] Architect Flight: " + (active ? "§aEnabled" : "§cDisabled")));
                }

                while (ArchitectKeyMappings.SPEED_UP.consumeClick()) {
                    ClientController.getInstance().adjustFlightSpeed(0.2f);
                    mc.player.sendOverlayMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", ClientController.getInstance().getFlightSpeed())));
                }

                while (ArchitectKeyMappings.SPEED_DOWN.consumeClick()) {
                    ClientController.getInstance().adjustFlightSpeed(-0.2f);
                    mc.player.sendOverlayMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", ClientController.getInstance().getFlightSpeed())));
                }

                while (ArchitectKeyMappings.TOGGLE_PREVIEW.consumeClick()) {
                    ClientController.getInstance().togglePreview();
                    boolean active = ClientController.getInstance().isPreviewActive();
                    mc.player.sendOverlayMessage(Component.literal("§b[TFB] In-World Preview: " + (active ? "§aEnabled" : "§cDisabled")));
                }

                while (ArchitectKeyMappings.EXECUTE_BUILD.consumeClick()) {
                    executeBuild(mc);
                }

                while (ArchitectKeyMappings.UNDO.consumeClick()) {
                    if (ClientController.getInstance().model().undo()) {
                        mc.player.sendOverlayMessage(Component.literal("§e[TFB] Reverted last design operation."));
                    } else {
                        mc.player.sendOverlayMessage(Component.literal("§7[TFB] Nothing to undo."));
                    }
                }

                while (ArchitectKeyMappings.REDO.consumeClick()) {
                    if (ClientController.getInstance().model().redo()) {
                        mc.player.sendOverlayMessage(Component.literal("§e[TFB] Restored undone design operation."));
                    } else {
                        mc.player.sendOverlayMessage(Component.literal("§7[TFB] Nothing to redo."));
                    }
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
                    mc.player.sendOverlayMessage(Component.literal("§b[TFB] Rotation: §e" + next.name()));
                }

                // Builder Hotbar number keys 1–9
                if (ClientController.getInstance().isArchitectMode()) {
                    for (int i = 0; i < 9; i++) {
                        if (mc.options.keyHotbarSlots[i].consumeClick()) {
                            BuilderHotbarModel.getInstance().setSlot(i);
                            BuilderHotbarSlot s = BuilderHotbarModel.getInstance().getActiveSlotData();
                            if (s != null) {
                                mc.player.sendOverlayMessage(Component.literal("§b[TFB Tool " + (i + 1) + "] §f" + s.name()));
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            ClientController controller = ClientController.getInstance();
            if (!controller.isArchitectMode()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() == null && mc.player != null) {
                double scroll = event.getScrollDeltaY();
                if (scroll != 0) {
                    BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                    if (mc.hasShiftDown()) {
                        if (scroll > 0) {
                            hotbar.prevPage();
                        } else {
                            hotbar.nextPage();
                        }
                    } else {
                        if (scroll > 0) {
                            hotbar.prevSlot();
                        } else {
                            hotbar.nextSlot();
                        }
                    }
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            if (!ClientController.getInstance().isArchitectMode()) return;

            Identifier layerName = event.getName();
            if (VanillaGuiLayers.HOTBAR.equals(layerName)
                    || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(layerName)
                    || VanillaGuiLayers.SELECTED_ITEM_NAME.equals(layerName)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderGuiPost(RenderGuiEvent.Post event) {
            ArchitectHudRenderer.render(event.getGuiGraphics(), Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onMouseButton(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() != null || mc.player == null || mc.level == null) return;
            ClientController controller = ClientController.getInstance();
            if (!controller.isArchitectMode() || event.getAction() != 1) return;

            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                    && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var clicked = bhr.getBlockPos();
                var pos = new com.x64Dynamics.tfb.geometry.Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());
                if (event.getButton() == 0) {
                    controller.model().setPos1(pos);
                    mc.player.sendOverlayMessage(Component.literal("§a[TFB] POS1 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"));
                    event.setCanceled(true);
                } else if (event.getButton() == 1) {
                    controller.model().setPos2(pos);
                    ArchitectWorkspaceModel m = controller.model();
                    mc.player.sendOverlayMessage(Component.literal("§6[TFB] POS2 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") §8| §b" + m.width() + "x" + m.height() + "x" + m.depth()));
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
            Level level = mc.level;
            if (level == null || mc.player == null) return;

            ArchitectWorkspaceModel model = ClientController.getInstance().model();
            List<PlacementTarget> targets = model.cachedPreviewTargets();
            if (targets.isEmpty()) {
                mc.player.sendSystemMessage(Component.nullToEmpty("§c[TFB] Build failed: No preview blocks available! Set POS1/POS2 or configure a brush in Workspace [B]."));
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
                            mc.player.sendSystemMessage(Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + finalPlaced + " blocks with server lighting."));
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

            mc.player.sendSystemMessage(Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + placed + " client blocks."));
        }
    }
}
