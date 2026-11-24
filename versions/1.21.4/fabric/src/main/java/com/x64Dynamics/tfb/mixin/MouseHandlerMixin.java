package com.x64Dynamics.tfb.mixin;

import com.x64Dynamics.tfb.client.ClientEntrypoint;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void tfb$onMouseScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (ClientEntrypoint.onMouseScroll(yOffset)) {
            ci.cancel();
        }
    }
}
