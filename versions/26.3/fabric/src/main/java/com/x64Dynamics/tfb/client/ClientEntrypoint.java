package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel;
import com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarSlot;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.logging.Logger;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.geometry.Rotation;
public final class ClientEntrypoint implements ClientModInitializer {
    private static final Logger LOGGER = Logger.getLogger("terraformabuilder");
    private static boolean keysRegistered = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[terraformabuilder] Initialized TerraFormaBuilder client on Fabric 1.21.4.");
    }

    private static FabricLevelAccess liveLevelAccess;
    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (liveLevelAccess == null) {
            liveLevelAccess = new FabricLevelAccess(mc.level);
            ClientController.getInstance().model().setLiveLevelAccess(liveLevelAccess);
        } else {
            liveLevelAccess.setLevel(mc.level);
        }

        if (!keysRegistered && mc.options != null) {
            ArchitectKeyMappings.registerAll(mc);
            keysRegistered = true;
        }

        ArchitectFlightController.tick(mc);

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

    public static void onRenderHud(GuiGraphicsExtractor guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ArchitectHudRenderer.render(guiGraphics, mc);
    }

    public static boolean onMouseScroll(double scrollDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null) return false;
        if (!ClientController.getInstance().isArchitectMode()) return false;
        if (scrollDelta == 0) return false;

        BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
        if (Minecraft.getInstance().hasShiftDown()) {
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
                        com.x64Dynamics.tfb.construction.DependencyAwareScheduler.scheduleWithDependencies(targets);
                    server.execute(() -> {
                        int placed = 0;
                        java.util.List<com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange> changes = new java.util.ArrayList<>();
                        for (com.x64Dynamics.tfb.construction.ScheduledBlockPlacement target : scheduled) {
                            Vec3i pos = target.pos();
                            BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                            BlockState prevState = serverLevel.getBlockState(bp);
                            BlockState state = BlockStateParser.parse(target.block());
                            if (!prevState.equals(state) && serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                                changes.add(new com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange(
                                    pos, BlockStateParser.fromBlockState(prevState), target.block()
                                ));
                            }
                        }
                        if (!changes.isEmpty()) {
                            com.x64Dynamics.tfb.editing.WorldConstructionHistory.getInstance().recordTransaction(changes);
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
