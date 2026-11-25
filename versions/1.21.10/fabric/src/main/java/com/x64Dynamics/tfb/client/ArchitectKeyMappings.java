package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Arrays;

public final class ArchitectKeyMappings {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("terraformabuilder", "general"));

    public static final KeyMapping OPEN_WORKSPACE = new KeyMapping(
            "key.terraformabuilder.workspace",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_PREVIEW = new KeyMapping(
            "key.terraformabuilder.preview",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    );

    public static final KeyMapping EXECUTE_BUILD = new KeyMapping(
            "key.terraformabuilder.build",
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
            "key.terraformabuilder.flight",
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

    public static final KeyMapping[] ALL = {
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
        if (mc.options == null) return;
        try {
            KeyMapping[] current = mc.options.keyMappings;
            KeyMapping[] combined = Arrays.copyOf(current, current.length + ALL.length);
            System.arraycopy(ALL, 0, combined, current.length, ALL.length);

            Field keyMappingsField = null;
            for (Field f : mc.options.getClass().getDeclaredFields()) {
                if (f.getType().isArray() && f.getType().getComponentType() == KeyMapping.class) {
                    keyMappingsField = f;
                    break;
                }
            }
            if (keyMappingsField != null) {
                keyMappingsField.setAccessible(true);
                keyMappingsField.set(mc.options, combined);
            }
        } catch (Throwable t) {
            // Options array fallback
        }
    }

    private ArchitectKeyMappings() {}
}
