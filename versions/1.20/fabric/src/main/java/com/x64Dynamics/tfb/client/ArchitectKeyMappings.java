package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ArchitectKeyMappings {
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

    public static final KeyMapping TOGGLE_FLIGHT = new KeyMapping(
        "key.terraformabuilder.toggle_flight",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        CATEGORY
    );

    public static final KeyMapping SPEED_UP = new KeyMapping(
        "key.terraformabuilder.speed_up",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_BRACKET,
        CATEGORY
    );

    public static final KeyMapping SPEED_DOWN = new KeyMapping(
        "key.terraformabuilder.speed_down",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_BRACKET,
        CATEGORY
    );

    public static final KeyMapping ROTATE_TOOL = new KeyMapping(
        "key.terraformabuilder.rotate_tool",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    public static final KeyMapping[] ALL = new KeyMapping[] {
        OPEN_WORKSPACE,
        TOGGLE_PREVIEW,
        EXECUTE_BUILD,
        UNDO,
        REDO,
        TOGGLE_FLIGHT,
        SPEED_UP,
        SPEED_DOWN,
        ROTATE_TOOL
    };

    public static void registerAll(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        try {
            java.lang.reflect.Field field = net.minecraft.client.Options.class.getDeclaredField("keyMappings");
            field.setAccessible(true);
            KeyMapping[] current = (KeyMapping[]) field.get(mc.options);
            KeyMapping[] updated = new KeyMapping[current.length + ALL.length];
            System.arraycopy(current, 0, updated, 0, current.length);
            System.arraycopy(ALL, 0, updated, current.length, ALL.length);
            field.set(mc.options, updated);
        } catch (Exception ignored) {}
    }

    private ArchitectKeyMappings() {}
}
