package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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
        }
    }

    @EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            // When no GUI is open, process gameplay hotkeys
            if (mc.screen == null) {
                while (TFBKeyMappings.OPEN_WORKSPACE.consumeClick()) {
                    mc.setScreen(new TerraFormaScreen());
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
            }
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // If holding a stick or architect mode is active, set foundation origin
            if (event.getItemStack().is(Items.STICK)) {
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

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            TFBClientController controller = TFBClientController.getInstance();
            if (!controller.isPreviewActive()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;

            GuiGraphics g = event.getGuiGraphics();
            ArchitectWorkspaceModel model = controller.model();

            String hudText = "§b[TFB] §f" + model.width() + "x" + model.height() + "x" + model.depth() +
                             " | §a" + model.totalBlockCount() + " blocks §8| §7[B] Workspace §8| §7[G] Build";
            g.drawString(mc.font, hudText, 8, 8, 0xFFFFFFFF, true);
        }

        public static void executeBuild(Minecraft mc) {
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
                Component.literal("§a[TFB] Construction complete! Successfully placed " + placed + " blocks."),
                false
            );
        }
    }
}
