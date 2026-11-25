package com.x64Dynamics.tfb.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.brush.BrushRegistry;
import com.x64Dynamics.tfb.brush.IConstructionBrush;
import com.x64Dynamics.tfb.construction.DependencyAwareScheduler;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.editing.WorldConstructionHistory;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
public final class ArchitectCommands {
    private ArchitectCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("tfb")
                .then(Commands.literal("open").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> mc.setScreen(new TerraFormaScreen()));
                    return 1;
                }))
                .then(Commands.literal("mode").executes(ctx -> {
                    ClientController ctrl = ClientController.getInstance();
                    ctrl.toggleArchitectMode();
                    boolean active = ctrl.isArchitectMode();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Architect Mode: " + (active ? "§aACTIVE" : "§cINACTIVE")));
                    return 1;
                }))
                .then(Commands.literal("flight").executes(ctx -> {
                    ClientController ctrl = ClientController.getInstance();
                    ctrl.toggleFlight();
                    boolean active = ctrl.isFlightActive();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Architect Flight: " + (active ? "§aENABLED" : "§cDISABLED")));
                    return 1;
                }))
                .then(Commands.literal("preview").executes(ctx -> {
                    ClientController.getInstance().togglePreview();
                    boolean active = ClientController.getInstance().isPreviewActive();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Preview: " + (active ? "§aEnabled" : "§cDisabled")));
                    return 1;
                }))
                .then(Commands.literal("hud").executes(ctx -> {
                    ClientController ctrl = ClientController.getInstance();
                    ctrl.toggleHud();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] In-World HUD: " + (ctrl.isHudVisible() ? "§aVISIBLE" : "§cMINIMIZED")));
                    return 1;
                }))
                .then(Commands.literal("tool")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        IConstructionBrush brush = BrushRegistry.get(name);
                        if (brush != null) {
                            ClientController.getInstance().model().setActiveBrush(brush);
                            ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Selected tool: §f" + brush.displayName()));
                        } else {
                            ctx.getSource().sendSystemMessage(Component.literal("§c[TFB] Unknown tool: " + name));
                        }
                        return 1;
                    }))
                )
                .then(Commands.literal("style")
                    .then(Commands.argument("preset", StringArgumentType.word()).executes(ctx -> {
                        String presetStr = StringArgumentType.getString(ctx, "preset").toUpperCase();
                        try {
                            StylePreset preset = StylePreset.valueOf(presetStr);
                            ClientController.getInstance().model().setStylePreset(preset);
                            ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Style preset set to: §e" + preset.displayName()));
                        } catch (IllegalArgumentException e) {
                            ctx.getSource().sendSystemMessage(Component.literal("§c[TFB] Unknown style preset: " + presetStr));
                        }
                        return 1;
                    }))
                )
                .then(Commands.literal("snap").executes(ctx -> {
                    ClientController.getInstance().model().cycleGridSnap();
                    int snap = ClientController.getInstance().model().selectionManager().gridSnap();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Grid Snap: §f" + snap + " blocks"));
                    return 1;
                }))
                .then(Commands.literal("build").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null && mc.player != null) {
                        List<PlacementTarget> targets = ClientController.getInstance().model().cachedPreviewTargets();
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null && !targets.isEmpty()) {
                            ServerLevel sl = server.getLevel(mc.level.dimension());
                            if (sl != null) {
                                List<ScheduledBlockPlacement> scheduled = DependencyAwareScheduler.scheduleWithDependencies(targets);
                                server.execute(() -> {
                                    List<WorldConstructionHistory.BlockChange> changes = new ArrayList<>();
                                    for (ScheduledBlockPlacement t : scheduled) {
                                        Vec3i pos = t.pos();
                                        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                                        BlockState prev = sl.getBlockState(bp);
                                        BlockState next = BlockStateParser.parse(t.block());
                                        if (!prev.equals(next) && sl.setBlock(bp, next, 3)) {
                                            changes.add(new WorldConstructionHistory.BlockChange(pos, BlockStateParser.fromBlockState(prev), t.block()));
                                        }
                                    }
                                    if (!changes.isEmpty()) WorldConstructionHistory.getInstance().recordTransaction(changes);
                                });
                            }
                        }
                    }
                    return 1;
                }))
                .then(Commands.literal("undo").executes(ctx -> {
                    if (ClientController.getInstance().model().undo()) {
                        ctx.getSource().sendSystemMessage(Component.literal("§e[TFB] Reverted last design operation."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal("§7[TFB] Nothing to undo."));
                    }
                    return 1;
                }))
                .then(Commands.literal("redo").executes(ctx -> {
                    if (ClientController.getInstance().model().redo()) {
                        ctx.getSource().sendSystemMessage(Component.literal("§e[TFB] Restored undone design operation."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal("§7[TFB] Nothing to redo."));
                    }
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ArchitectWorkspaceModel model = ClientController.getInstance().model();
                    ctx.getSource().sendSystemMessage(Component.literal(
                        "§b[TerraFormaBuilder Info]\n" +
                        "§7Dimensions: §f" + model.width() + "x" + model.height() + "x" + model.depth() + "\n" +
                        "§7Origin: §e(" + model.origin().x() + ", " + model.origin().y() + ", " + model.origin().z() + ")\n" +
                        "§7Total Blocks: §a" + model.totalBlockCount() + "\n" +
                        "§7Style Preset: §e" + model.stylePreset().name()
                    ));
                    return 1;
                }))
        );
    }
}
