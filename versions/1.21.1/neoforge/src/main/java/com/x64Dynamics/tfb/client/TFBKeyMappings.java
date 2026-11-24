package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class TFBKeyMappings {
    public static final String CATEGORY = "key.categories.terraformabuilder";

    public static final KeyMapping OPEN_WORKSPACE = new KeyMapping(
        "key.terraformabuilder.open_workspace",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        CATEGORY
    );

    public static final KeyMapping TOGGLE_PREVIEW = new KeyMapping(
        "key.terraformabuilder.toggle_preview",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,
        CATEGORY
    );

    public static final KeyMapping EXECUTE_BUILD = new KeyMapping(
        "key.terraformabuilder.execute_build",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY
    );

    public static final KeyMapping UNDO = new KeyMapping(
        "key.terraformabuilder.undo",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        CATEGORY
    );

    public static final KeyMapping REDO = new KeyMapping(
        "key.terraformabuilder.redo",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Y,
        CATEGORY
    );

    private TFBKeyMappings() {}
}
