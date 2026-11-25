package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.platform.DefaultLevelAccess;
import com.x64Dynamics.tfb.platform.DefaultNetworkBridge;
import com.x64Dynamics.tfb.platform.DefaultPlatformBridge;
import com.x64Dynamics.tfb.platform.IPlatformBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.logging.Logger;

@Mod(TerraFormaBuilder.MOD_ID)
public final class Entrypoint {
    private static final Logger LOGGER = Logger.getLogger(TerraFormaBuilder.MOD_ID);

    public Entrypoint() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        String dist = FMLEnvironment.dist != null ? FMLEnvironment.dist.name() : "UNKNOWN";
        LOGGER.info(String.format("[%s] Initializing %s on Forge (Dist: %s)",
                TerraFormaBuilder.MOD_ID, TerraFormaBuilder.MOD_ID, dist));

        IPlatformBridge bridge = new DefaultPlatformBridge("forge", configDir, new DefaultNetworkBridge(), new DefaultLevelAccess());
        TerraFormaBuilder.initializeWithBridge(bridge);
        com.x64Dynamics.tfb.persistence.BlueprintStorage.setStorageDir(configDir.resolve("terraformabuilder").resolve("blueprints"));
        com.x64Dynamics.tfb.city.WorldProjectStorage.setProjectsDir(configDir.resolve("terraformabuilder").resolve("projects"));
        BlockRegistryScanner.scanIfNeeded();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LOGGER.info(String.format("[%s] Initializing client subsystems on Forge (Preview renderer, Workspace UI, Keybindings)",
                    TerraFormaBuilder.MOD_ID));
        }

        LOGGER.info(String.format("[%s] Successfully initialized on Forge platform.", TerraFormaBuilder.MOD_ID));
    }
}
