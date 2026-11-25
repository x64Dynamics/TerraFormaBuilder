package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
        LOGGER.info("[terraformabuilder] Initialized TerraFormaBuilder client on Fabric 1.19.4.");
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

            while (TFBKeyMappings.TOGGLE_PREVIEW.consumeClick()) {
                TFBClientController.getInstance().togglePreview();
                boolean active = TFBClientController.getInstance().isPreviewActive();
                mc.player.displayClientMessage(
                    Component.literal("§b[TFB] 3D Preview: " + (active ? "§aEnabled" : "§cDisabled")),
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

            while (TFBKeyMappings.SPEED_UP.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)), true);
            }

            while (TFBKeyMappings.SPEED_DOWN.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(-0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.displayClientMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)), true);
            }

            if (TFBClientController.getInstance().isArchitectMode()) {
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
    }

    public static boolean onMouseScroll(double yOffset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return false;
        if (!TFBClientController.getInstance().isArchitectMode()) return false;

        if (yOffset != 0) {
            BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
            if (Screen.hasShiftDown()) {
                if (yOffset > 0) {
                    hotbar.prevPage();
                } else {
                    hotbar.nextPage();
                }
            } else {
                if (yOffset > 0) {
                    hotbar.prevSlot();
                } else {
                    hotbar.nextSlot();
                }
            }
            return true;
        }
        return false;
    }

    public static void onRenderHud(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.screen == null) {
            TFBInWorldHud.render(poseStack, mc);
        }
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
