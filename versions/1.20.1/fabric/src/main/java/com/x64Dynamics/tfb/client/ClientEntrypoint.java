package com.x64Dynamics.tfb.client;

import net.fabricmc.api.ClientModInitializer;
import java.util.logging.Logger;

public final class ClientEntrypoint implements ClientModInitializer {
    private static final Logger LOGGER = Logger.getLogger("terraformabuilder");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[terraformabuilder] Initialized TerraFormaBuilder client on Fabric.");
    }
}
