package com.x64Dynamics.tfb.platform;

import java.nio.file.Path;
import java.util.Objects;

public class DefaultPlatformBridge implements IPlatformBridge {
    private final String loader;
    private final Path configDirectory;
    private final INetworkBridge network;
    private final ILevelAccess levelAccess;

    public DefaultPlatformBridge(String loader, Path configDirectory, INetworkBridge network, ILevelAccess levelAccess) {
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory must not be null");
        this.network = Objects.requireNonNull(network, "network must not be null");
        this.levelAccess = Objects.requireNonNull(levelAccess, "levelAccess must not be null");
    }

    @Override
    public String loader() {
        return loader;
    }

    @Override
    public Path configDirectory() {
        return configDirectory;
    }

    @Override
    public INetworkBridge network() {
        return network;
    }

    @Override
    public ILevelAccess levelAccess() {
        return levelAccess;
    }
}
