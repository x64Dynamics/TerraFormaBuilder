package com.x64Dynamics.tfb.platform;

public final class PlatformBridgeRegistry {
    private static volatile IPlatformBridge instance;

    private PlatformBridgeRegistry() { }

    public static void set(IPlatformBridge bridge) {
        instance = bridge;
    }

    public static IPlatformBridge get() {
        return instance;
    }
}
