package com.x64Dynamics.tfb.client;

import com.mojang.brigadier.CommandDispatcher;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
public final class TFBCommands {
    private TFBCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("tfb")
                .then(Commands.literal("open").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.tell(() -> mc.setScreen(new TerraFormaScreen()));
                    return 1;
                }))
                .then(Commands.literal("preview").executes(ctx -> {
                    TFBClientController.getInstance().togglePreview();
                    boolean active = TFBClientController.getInstance().isPreviewActive();
                    ctx.getSource().sendSystemMessage(Component.literal("§b[TFB] Preview: " + (active ? "§aEnabled" : "§cDisabled")));
                    return 1;
                }))
                .then(Commands.literal("build").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    TFBClientEvents.GameEvents.executeBuild(mc);
                    return 1;
                }))
                .then(Commands.literal("undo").executes(ctx -> {
                    if (TFBClientController.getInstance().model().undo()) {
                        ctx.getSource().sendSystemMessage(Component.literal("§e[TFB] Reverted last design operation."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal("§7[TFB] Nothing to undo."));
                    }
                    return 1;
                }))
                .then(Commands.literal("redo").executes(ctx -> {
                    if (TFBClientController.getInstance().model().redo()) {
                        ctx.getSource().sendSystemMessage(Component.literal("§e[TFB] Restored undone design operation."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal("§7[TFB] Nothing to redo."));
                    }
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
                    ctx.getSource().sendSystemMessage(Component.literal(
                        "§b[TerraFormaBuilder Info]\n" +
                        "§7Dimensions: §f" + model.width() + "x" + model.height() + "x" + model.depth() + "\n" +
                        "§7Total Blocks: §a" + model.totalBlockCount() + "\n" +
                        "§7Style Preset: §e" + model.stylePreset().name() + " (Harmony: " + Math.round(model.paletteHarmonyScore() * 100) + "%)\n" +
                        "§7Wing Layout: §d" + model.wingConfiguration().displayName() + "\n" +
                        "§7Roof Style: §6" + model.roofShape().name()
                    ));
                    return 1;
                }))
        );
    }
}
