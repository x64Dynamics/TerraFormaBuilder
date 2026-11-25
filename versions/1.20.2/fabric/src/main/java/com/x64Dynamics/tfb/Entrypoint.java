package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.platform.DefaultLevelAccess;
import com.x64Dynamics.tfb.platform.DefaultNetworkBridge;
import com.x64Dynamics.tfb.platform.DefaultPlatformBridge;
import com.x64Dynamics.tfb.platform.IPlatformBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class Entrypoint implements ModInitializer {
    private static final Logger LOGGER = Logger.getLogger(TerraFormaBuilder.MOD_ID);

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        Path configDir = loader.getConfigDir();
        String environment = loader.getEnvironmentType().name();
        LOGGER.info(String.format("[%s] Initializing %s on Fabric (Env: %s)",
                TerraFormaBuilder.MOD_ID, TerraFormaBuilder.MOD_ID, environment));

        IPlatformBridge bridge = new DefaultPlatformBridge("fabric", configDir, new DefaultNetworkBridge(), new DefaultLevelAccess());
        TerraFormaBuilder.initializeWithBridge(bridge);
        com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.setStorageDir(configDir.resolve("terraformabuilder").resolve("blueprints"));
        com.x64Dynamics.tfb.architectural.builds.city.WorldProjectStorage.setProjectsDir(configDir.resolve("terraformabuilder").resolve("projects"));
        BlockRegistryScanner.scanIfNeeded();
        LOGGER.info(String.format("[%s] Successfully initialized on Fabric platform.", TerraFormaBuilder.MOD_ID));
    }
}
