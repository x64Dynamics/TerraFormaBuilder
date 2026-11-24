package com.x64Dynamics.tfb.neoforge;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.platform.RuntimePlatform;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(TerraFormaBuilder.MOD_ID)
public final class Entrypoint {
    public Entrypoint() {
        TerraFormaBuilder.initialize(new RuntimePlatform("neoforge", FMLPaths.CONFIGDIR.get()));
    }
}
