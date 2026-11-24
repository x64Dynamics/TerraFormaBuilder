package com.x64Dynamics.tfb.fabric;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.platform.RuntimePlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class Entrypoint implements ModInitializer {
    public void onInitialize() {
        TerraFormaBuilder.initialize(new RuntimePlatform("fabric", FabricLoader.getInstance().getConfigDir()));
    }
}
