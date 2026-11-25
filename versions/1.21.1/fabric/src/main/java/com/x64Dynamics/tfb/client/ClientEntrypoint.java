package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.logging.Logger;

public final class ClientEntrypoint implements ClientModInitializer {
    private static final Logger LOGGER = Logger.getLogger("terraformabuilder");
    private static boolean keysRegistered = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[terraformabuilder] Initialized TerraFormaBuilder client on Fabric 1.21.1.");
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        if (!keysRegistered && mc.options != null) {
            TFBKeyMappings.registerAll(mc);
            keysRegistered = true;
        }

        ArchitectFlightController.tick(mc);

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

    public static void onRenderHud(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        TFBInWorldHud.render(guiGraphics, mc);
    }

    public static boolean onMouseScroll(double scrollDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return false;
        if (!TFBClientController.getInstance().isArchitectMode()) return false;
        if (scrollDelta == 0) return false;

        BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
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
        return true;
    }

    private static void executeBuild(Minecraft mc) {
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
