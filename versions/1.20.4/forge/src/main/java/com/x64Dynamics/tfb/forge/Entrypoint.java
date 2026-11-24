package com.x64Dynamics.tfb.forge;

import com.x64Dynamics.tfb.TerraFormaBuilder;
import com.x64Dynamics.tfb.platform.RuntimePlatform;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(TerraFormaBuilder.MOD_ID)
public final class Entrypoint {
    public Entrypoint() {
        TerraFormaBuilder.initialize(new RuntimePlatform("forge", FMLPaths.CONFIGDIR.get()));
    }
}
