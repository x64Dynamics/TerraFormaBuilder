package com.x64Dynamics.tfb.platform;

import java.nio.file.Path;

public interface IPlatformBridge {
    String loader();
    Path configDirectory();
    INetworkBridge network();
    ILevelAccess levelAccess();
}
