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
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

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
                    if (TFBClientController.getInstance().model().undo()) {
                        mc.player.displayClientMessage(Component.literal("§e[TFB] Reverted last design operation."), true);
                    } else {
                        mc.player.displayClientMessage(Component.literal("§7[TFB] Nothing to undo."), true);
                    }
                }

                while (TFBKeyMappings.REDO.consumeClick()) {
                    if (TFBClientController.getInstance().model().redo()) {
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
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (event.getItemStack().is(Items.STICK) || TFBClientController.getInstance().isArchitectMode()) {
                BlockPos clicked = event.getPos();
                BlockPos target = clicked.above();
                Vec3i origin = new Vec3i(target.getX(), target.getY(), target.getZ());
                TFBClientController.getInstance().setTargetPosition(origin);
                mc.player.displayClientMessage(
                    Component.literal("§b[TFB] Set construction origin to (" + origin.x() + ", " + origin.y() + ", " + origin.z() + ")"),
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
                    Component.nullToEmpty("§c[TFB] Build failed: No preview blocks available! Set POS1/POS2 or configure a brush in Workspace [B]."),
                    false
                );
                return;
            }

            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    server.execute(() -> {
                        int placed = 0;
                        for (PlacementTarget target : targets) {
                            Vec3i pos = target.pos();
                            BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                            BlockState state = BlockStateParser.parse(target.block());
                            if (serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                            }
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
