package com.x64Dynamics.tfb.platform;

import java.nio.file.Path;

public interface Platform {
    String loader();
    Path configDirectory();
}
