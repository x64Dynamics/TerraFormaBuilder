package com.x64Dynamics.tfb.mixin;

import com.x64Dynamics.tfb.client.ClientEntrypoint;
import com.x64Dynamics.tfb.ui.TFBClientController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tfb$onRenderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientEntrypoint.onRenderHud(guiGraphics, 0f);
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void tfb$cancelHotbar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (TFBClientController.getInstance().isArchitectMode()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void tfb$cancelSelectedItemName(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        if (TFBClientController.getInstance().isArchitectMode()) {
            ci.cancel();
        }
    }
}
