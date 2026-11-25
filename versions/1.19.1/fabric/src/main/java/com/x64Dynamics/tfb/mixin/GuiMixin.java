package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.ClientEntrypoint;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void tfb$onRenderHud(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ClientEntrypoint.onRenderHud(poseStack, partialTick);
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void tfb$cancelHotbar(float partialTick, PoseStack poseStack, CallbackInfo ci) {
        if (ClientController.getInstance().isArchitectMode()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void tfb$cancelExperienceBar(PoseStack poseStack, int x, CallbackInfo ci) {
        if (ClientController.getInstance().isArchitectMode()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void tfb$cancelSelectedItemName(PoseStack poseStack, CallbackInfo ci) {
        if (ClientController.getInstance().isArchitectMode()) {
            ci.cancel();
        }
    }
}
