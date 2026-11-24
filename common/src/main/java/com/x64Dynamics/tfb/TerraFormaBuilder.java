package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.config.ModConfig;
import com.x64Dynamics.tfb.platform.Platform;
import java.io.IOException;
import java.util.logging.Logger;

public final class TerraFormaBuilder {
    public static final String MOD_ID = "terraformabuilder";
    private static final Logger LOG = Logger.getLogger(MOD_ID);

    private TerraFormaBuilder() { }

    public static ModConfig initialize(Platform platform) {
        try {
            ModConfig config = ModConfig.load(platform.configDirectory());
            LOG.info("TerraFormaBuilder initialized on " + platform.loader());
            return config;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load TerraFormaBuilder configuration", exception);
        }
    }
}
