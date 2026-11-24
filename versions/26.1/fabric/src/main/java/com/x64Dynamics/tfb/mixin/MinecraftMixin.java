package com.x64Dynamics.tfb.mixin;

import com.x64Dynamics.tfb.client.ClientEntrypoint;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tfb$onClientTick(CallbackInfo ci) {
        ClientEntrypoint.onClientTick((Minecraft) (Object) this);
    }
}
