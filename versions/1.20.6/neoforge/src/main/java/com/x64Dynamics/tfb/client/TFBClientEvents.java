package com.x64Dynamics.tfb.client;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class TFBClientEvents {
    private TFBClientEvents() {}

    @EventBusSubscriber(modid = "terraformabuilder", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TFBKeyMappings.OPEN_WORKSPACE);
            event.register(TFBKeyMappings.TOGGLE_PREVIEW);
            event.register(TFBKeyMappings.EXECUTE_BUILD);
            event.register(TFBKeyMappings.UNDO);
            event.register(TFBKeyMappings.REDO);
            event.register(TFBKeyMappings.TOGGLE_FLIGHT);
            event.register(TFBKeyMappings.SPEED_UP);
            event.register(TFBKeyMappings.SPEED_DOWN);
            event.register(TFBKeyMappings.ROTATE_TOOL);
        }
    }

    @EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
    public static class GameEvents {
        @SubscribeEvent
        private static NeoForgeLevelAccess liveLevelAccess;
    public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
        if (liveLevelAccess == null) {
            liveLevelAccess = new NeoForgeLevelAccess(mc.level);
            TFBClientController.getInstance().model().setLiveLevelAccess(liveLevelAccess);
        } else {
            liveLevelAccess.setLevel(mc.level);
        }

            // Tick active flight physics and navigation
            ArchitectFlightController.tick(mc);

            // Process hotkeys when in-world (no screen open)
            if (mc.screen == null) {
                while (TFBKeyMappings.OPEN_WORKSPACE.consumeClick()) {
                    mc.setScreen(new TerraFormaScreen());
                }

                while (TFBKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                    TFBClientController.getInstance().toggleFlight();
                    boolean active = TFBClientController.getInstance().isFlightActive();
                    mc.player.displayClientMessage(
                        Component.literal("§b[TFB] Architect Flight: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                    );
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

                while (TFBKeyMappings.TOGGLE_PREVIEW.consumeClick()) {
                    TFBClientController.getInstance().togglePreview();
                    boolean active = TFBClientController.getInstance().isPreviewActive();
                    mc.player.displayClientMessage(
                        Component.literal("§b[TFB] In-World Preview: " + (active ? "§aEnabled" : "§cDisabled")),
                        true
                    );
                }

                while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                    executeBuild(mc);
                }

                while (TFBKeyMappings.UNDO.consumeClick()) {
                    if (executeWorldUndo(mc)) {
                        // World undo handled asynchronously on server
                    } else if (TFBClientController.getInstance().model().undo()) {
                        mc.player.displayClientMessage(Component.literal("§e[TFB] Reverted last design operation."), true);
                    } else {
                        mc.player.displayClientMessage(Component.literal("§7[TFB] Nothing to undo."), true);
                    }
                }

                while (TFBKeyMappings.REDO.consumeClick()) {
                    if (executeWorldRedo(mc)) {
                        // World redo handled asynchronously on server
                    } else if (TFBClientController.getInstance().model().redo()) {
                        mc.player.displayClientMessage(Component.literal("§e[TFB] Restored undone design operation."), true);
                    } else {
                        mc.player.displayClientMessage(Component.literal("§7[TFB] Nothing to redo."), true);
                    }
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
                if (TFBClientController.getInstance().isArchitectMode()) {
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
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            TFBClientController controller = TFBClientController.getInstance();
            if (!controller.isArchitectMode()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null) {
                double scroll = event.getScrollDeltaY();
                if (scroll != 0) {
                    BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                    if (Screen.hasShiftDown()) {
                        // Shift + scroll cycles category pages
                        if (scroll > 0) hotbar.nextPage();
                        else hotbar.prevPage();
                        String page = BuilderHotbarModel.PAGE_NAMES[hotbar.getActivePage()];
                        mc.player.displayClientMessage(Component.literal("§e[TFB Category] §f" + page), true);
                    } else {
                        // Standard scroll cycles tool slots
                        if (scroll > 0) hotbar.prevSlot();
                        else hotbar.nextSlot();
                        BuilderHotbarSlot s = hotbar.getActiveSlotData();
                        if (s != null) {
                            mc.player.displayClientMessage(Component.literal("§b[TFB Tool] §f" + s.name()), true);
                        }
                    }
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            // Replace vanilla survival hotbar and exp bar with dedicated TFB Builder Hotbar
            if (TFBClientController.getInstance().isArchitectMode()) {
                ResourceLocation name = event.getName();
                if (name.equals(VanillaGuiLayers.HOTBAR) ||
                    name.equals(VanillaGuiLayers.EXPERIENCE_BAR) ||
                    name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            TFBInWorldHud.render(event.getGuiGraphics(), mc);
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft mc = Minecraft.getInstance();
            ArchitectFlightController.restorePreviousState(mc);
            TFBClientController.getInstance().setArchitectMode(false);
        }

        @SubscribeEvent
        public static void onClientRespawn(ClientPlayerNetworkEvent.Clone event) {
            Minecraft mc = Minecraft.getInstance();
            ArchitectFlightController.restorePreviousState(mc);
            TFBClientController.getInstance().setArchitectMode(false);
        }

        @SubscribeEvent
        public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null || mc.level == null) return;

            TFBClientController controller = TFBClientController.getInstance();
            if (!controller.isArchitectMode()) return;

            // Only act on button press (action 1 = GLFW_PRESS)
            if (event.getAction() == 1) {
                if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos clicked = bhr.getBlockPos();
                    Vec3i pos = new Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());

                    if (event.getButton() == 0) { // Left click -> POS1
                        controller.model().setPos1(pos);
                        mc.player.displayClientMessage(
                            Component.literal("§a[TFB] POS1 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"),
                            true
                        );
                        event.setCanceled(true);
                    } else if (event.getButton() == 1) { // Right click -> POS2
                        controller.model().setPos2(pos);
                        ArchitectWorkspaceModel m = controller.model();
                        mc.player.displayClientMessage(
                            Component.literal("§6[TFB] POS2 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") §8| §b" +
                                m.width() + "x" + m.height() + "x" + m.depth() + " (" + ((long) m.width() * m.height() * m.depth()) + " blocks)"),
                            true
                        );
                        event.setCanceled(true);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            TFBClientController controller = TFBClientController.getInstance();
            if (controller.isArchitectMode()) {
                BlockPos clicked = event.getPos();
                Vec3i pos = new Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());
                controller.model().setPos1(pos);
                mc.player.displayClientMessage(
                    Component.literal("§a[TFB] POS1 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"),
                    true
                );
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            TFBClientController controller = TFBClientController.getInstance();
            if (controller.isArchitectMode()) {
                BlockPos clicked = event.getPos();
                Vec3i pos = new Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());
                controller.model().setPos2(pos);
                ArchitectWorkspaceModel m = controller.model();
                mc.player.displayClientMessage(
                    Component.literal("§6[TFB] POS2 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") §8| §b" +
                        m.width() + "x" + m.height() + "x" + m.depth() + " (" + ((long) m.width() * m.height() * m.depth()) + " blocks)"),
                    true
                );
                event.setCanceled(true);
            }
        }

        public static void executeBuild(Minecraft mc) {
            Level level = mc.level;
            if (level == null || mc.player == null) return;

            ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
            List<PlacementTarget> targets = model.cachedPreviewTargets();

            if (targets.isEmpty()) {
                mc.player.displayClientMessage(
                    Component.literal("§c[TFB] Build failed: No preview blocks available! Set POS1/POS2 or configure a brush in Workspace [B]."),
                    false
                );
                return;
            }

            List<com.x64Dynamics.tfb.construction.ScheduledBlockPlacement> scheduled =
                com.x64Dynamics.tfb.construction.DependencyAwareScheduler.scheduleWithDependencies(targets);

            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    server.execute(() -> {
                        int placed = 0;
                        int confirmed = 0;
                        List<com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange> changes = new java.util.ArrayList<>();
                        for (com.x64Dynamics.tfb.construction.ScheduledBlockPlacement target : scheduled) {
                            Vec3i pos = target.pos();
                            BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                            BlockState prevState = serverLevel.getBlockState(bp);
                            BlockState state = BlockStateParser.parse(target.block());
                            if (!prevState.equals(state) && serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                                confirmed++;
                                changes.add(new com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange(
                                    pos, BlockStateParser.fromBlockState(prevState), target.block()
                                ));
                            } else if (serverLevel.getBlockState(bp).equals(state)) {
                                confirmed++;
                            }
                        }
                        if (!changes.isEmpty()) {
                            com.x64Dynamics.tfb.editing.WorldConstructionHistory.getInstance().recordTransaction(changes);
                        }
                        final int finalPlaced = placed;
                        final int finalConfirmed = confirmed;
                        serverLevel.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        mc.execute(() -> {
                            String msg = finalPlaced > 0
                                ? "§a[TFB] Construction complete! Placed " + finalPlaced + " blocks (" + finalConfirmed + " verified)."
                                : "§e[TFB] Construction verified: All " + finalConfirmed + " blocks are already constructed.";
                            mc.player.displayClientMessage(
                                Component.literal(msg),
                                false
                            );
                        });
                    });
                    return;
                }
            }

            // Fallback for multiplayer / dedicated server environments
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
                Component.literal("§a[TFB] Construction complete! Successfully placed " + placed + " client blocks."),
                false
            );
        }

        public static boolean executeWorldUndo(Minecraft mc) {
            var history = com.x64Dynamics.tfb.editing.WorldConstructionHistory.getInstance();
            if (!history.canUndo() || mc.level == null || mc.player == null) return false;
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) return false;
            net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(mc.level.dimension());
            if (serverLevel == null) return false;

            List<com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange> tx = history.popUndo();
            server.execute(() -> {
                for (int i = tx.size() - 1; i >= 0; i--) {
                    var ch = tx.get(i);
                    BlockPos bp = new BlockPos(ch.pos().x(), ch.pos().y(), ch.pos().z());
                    serverLevel.setBlock(bp, BlockStateParser.parse(ch.oldBlock()), 3);
                }
                mc.execute(() -> mc.player.displayClientMessage(
                    Component.literal("§e[TFB] Undid world construction (" + tx.size() + " blocks restored)."),
                    true
                ));
            });
            return true;
        }

        public static boolean executeWorldRedo(Minecraft mc) {
            var history = com.x64Dynamics.tfb.editing.WorldConstructionHistory.getInstance();
            if (!history.canRedo() || mc.level == null || mc.player == null) return false;
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) return false;
            net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(mc.level.dimension());
            if (serverLevel == null) return false;

            List<com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange> tx = history.popRedo();
            server.execute(() -> {
                for (var ch : tx) {
                    BlockPos bp = new BlockPos(ch.pos().x(), ch.pos().y(), ch.pos().z());
                    serverLevel.setBlock(bp, BlockStateParser.parse(ch.newBlock()), 3);
                }
                mc.execute(() -> mc.player.displayClientMessage(
                    Component.literal("§e[TFB] Redid world construction (" + tx.size() + " blocks placed)."),
                    true
                ));
            });
            return true;
        }
    }
}
