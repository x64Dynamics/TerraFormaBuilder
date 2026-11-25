package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.config.ModConfig;
import com.x64Dynamics.tfb.persistence.BlueprintManager;
import com.x64Dynamics.tfb.platform.IPlatformBridge;
import com.x64Dynamics.tfb.platform.Platform;
import com.x64Dynamics.tfb.platform.PlatformBridgeRegistry;

import java.io.IOException;
import java.util.logging.Logger;

public final class TerraFormaBuilder {
    public static final String MOD_ID = "terraformabuilder";
    private static final Logger LOG = Logger.getLogger(MOD_ID);

    private static volatile ModConfig config;
    private static volatile BlueprintManager blueprintManager;

    private TerraFormaBuilder() { }

    public static ModConfig initialize(Platform platform) {
        try {
            config = ModConfig.load(platform.configDirectory());
            blueprintManager = new BlueprintManager(platform.configDirectory());
            LOG.info("TerraFormaBuilder initialized on " + platform.loader());
            return config;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load TerraFormaBuilder configuration", exception);
        }
    }

    public static void initializeWithBridge(IPlatformBridge bridge) {
        PlatformBridgeRegistry.set(bridge);
        try {
            config = ModConfig.load(bridge.configDirectory());
            blueprintManager = new BlueprintManager(bridge.configDirectory());
            LOG.info("TerraFormaBuilder fully initialized with bridge on " + bridge.loader());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load TerraFormaBuilder configuration", exception);
        }
    }

    public static ModConfig config() {
        return config;
    }

    public static BlueprintManager blueprintManager() {
        return blueprintManager;
    }
}
