package com.x64Dynamics.tfb.client;

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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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
    }

    @Mod.EventBusSubscriber(modid = TerraFormaBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onCustomizeChat(CustomizeGuiOverlayEvent.Chat event) {
            TFBInWorldHud.render(event.getGuiGraphics(), Minecraft.getInstance());
        }
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {

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
                mc.player.sendOverlayMessage(Component.literal("§b[TFB] In-World Preview: " + (active ? "§aEnabled" : "§cDisabled")));
            }

            while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                executeBuild(mc);
            }

            while (TFBKeyMappings.TOGGLE_FLIGHT.consumeClick()) {
                TFBClientController.getInstance().toggleFlight();
                boolean active = TFBClientController.getInstance().isFlightActive();
                mc.player.sendOverlayMessage(Component.literal("§b[TFB] Architect Flight: " + (active ? "§aEnabled" : "§cDisabled")));
            }

            while (TFBKeyMappings.SPEED_UP.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.sendOverlayMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)));
            }

            while (TFBKeyMappings.SPEED_DOWN.consumeClick()) {
                TFBClientController.getInstance().adjustFlightSpeed(-0.2f);
                float spd = TFBClientController.getInstance().getFlightSpeed();
                mc.player.sendOverlayMessage(Component.literal(String.format("§b[TFB] Flight Speed: §e%.1fx", spd)));
            }

            if (TFBClientController.getInstance().isArchitectMode() && mc.screen == null) {
                BuilderHotbarModel hotbar = BuilderHotbarModel.getInstance();
                for (int i = 0; i < 9; i++) {
                    if (mc.options.keyHotbarSlots[i].consumeClick()) {
                        hotbar.setSlot(i);
                        BuilderHotbarSlot s = hotbar.getActiveSlotData();
                        if (s != null) {
                            mc.player.sendOverlayMessage(Component.literal("§b[TFB Tool " + (i + 1) + "] §f" + s.name()));
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static boolean onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return false;
            if (!TFBClientController.getInstance().isArchitectMode()) return false;

            double scrollDelta = event.getDeltaY();
            if (scrollDelta != 0) {
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
            return false;
        }

        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft mc = Minecraft.getInstance();
            ArchitectFlightController.restorePreviousState(mc);
            TFBClientController.getInstance().setArchitectMode(false);
        }

        public static void executeBuild(Minecraft mc) {
            Level level = mc.level;
            if (level == null || mc.player == null) return;

            ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
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
}
