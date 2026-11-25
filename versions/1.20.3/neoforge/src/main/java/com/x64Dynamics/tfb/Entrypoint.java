package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.client.ArchitectClientEvents;
import com.x64Dynamics.tfb.platform.DefaultLevelAccess;
import com.x64Dynamics.tfb.platform.DefaultNetworkBridge;
import com.x64Dynamics.tfb.platform.DefaultPlatformBridge;
import com.x64Dynamics.tfb.platform.IPlatformBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;
import java.util.logging.Logger;

@Mod(TerraFormaBuilder.MOD_ID)
public final class Entrypoint {
    private static final Logger LOGGER = Logger.getLogger(TerraFormaBuilder.MOD_ID);

    public Entrypoint(IEventBus modBus) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        String dist = FMLEnvironment.dist != null ? FMLEnvironment.dist.name() : "UNKNOWN";
        LOGGER.info(String.format("[%s] Initializing %s on NeoForge (Dist: %s)",
                TerraFormaBuilder.MOD_ID, TerraFormaBuilder.MOD_ID, dist));

        IPlatformBridge bridge = new DefaultPlatformBridge("neoforge", configDir, new DefaultNetworkBridge(), new DefaultLevelAccess());
        TerraFormaBuilder.initializeWithBridge(bridge);
        com.x64Dynamics.tfb.platform.persistence.BlueprintStorage.setStorageDir(configDir.resolve("terraformabuilder").resolve("blueprints"));
        com.x64Dynamics.tfb.architectural.builds.city.WorldProjectStorage.setProjectsDir(configDir.resolve("terraformabuilder").resolve("projects"));
        BlockRegistryScanner.scanIfNeeded();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LOGGER.info(String.format("[%s] Initializing client subsystems on NeoForge (Preview renderer, Workspace UI, Keybindings)",
                    TerraFormaBuilder.MOD_ID));
            modBus.register(ArchitectClientEvents.ModBusEvents.class);
            NeoForge.EVENT_BUS.register(ArchitectClientEvents.ForgeBusEvents.class);
            NeoForge.EVENT_BUS.register(com.x64Dynamics.tfb.client.ArchitectCommands.class);
            NeoForge.EVENT_BUS.register(com.x64Dynamics.tfb.client.ArchitectWorldRenderer.class);
        }

        LOGGER.info(String.format("[%s] Successfully initialized on NeoForge platform.", TerraFormaBuilder.MOD_ID));
    }
}
