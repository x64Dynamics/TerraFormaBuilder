package com.x64Dynamics.tfb.platform;

public final class TFBPlatform {
    private static volatile IPlatformBridge instance;

    private TFBPlatform() { }

    public static void set(IPlatformBridge bridge) {
        instance = bridge;
    }

    public static IPlatformBridge get() {
        return instance;
    }
}
