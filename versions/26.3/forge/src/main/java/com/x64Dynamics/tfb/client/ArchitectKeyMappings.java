package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ArchitectKeyMappings {
    public static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath("terraformabuilder", "general"));

    public static final KeyMapping OPEN_WORKSPACE = new KeyMapping(
        "key.terraformabuilder.open_workspace", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY);
    public static final KeyMapping TOGGLE_PREVIEW = new KeyMapping(
        "key.terraformabuilder.toggle_preview", InputConstants.Type.KEYBOARD, InputConstants.KEY_P, CATEGORY);
    public static final KeyMapping EXECUTE_BUILD = new KeyMapping(
        "key.terraformabuilder.execute_build", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping UNDO = new KeyMapping(
        "key.terraformabuilder.undo", InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, CATEGORY);
    public static final KeyMapping REDO = new KeyMapping(
        "key.terraformabuilder.redo", InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, CATEGORY);
    public static final KeyMapping TOGGLE_FLIGHT = new KeyMapping(
        "key.terraformabuilder.toggle_flight", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping SPEED_UP = new KeyMapping(
        "key.terraformabuilder.speed_up", InputConstants.Type.KEYBOARD, InputConstants.KEY_RBRACKET, CATEGORY);
    public static final KeyMapping SPEED_DOWN = new KeyMapping(
        "key.terraformabuilder.speed_down", InputConstants.Type.KEYBOARD, InputConstants.KEY_LBRACKET, CATEGORY);
    public static final KeyMapping ROTATE_TOOL = new KeyMapping(
        "key.terraformabuilder.rotate_tool", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);

    public static final KeyMapping[] ALL = {
        OPEN_WORKSPACE, TOGGLE_PREVIEW, EXECUTE_BUILD, UNDO, REDO,
        TOGGLE_FLIGHT, SPEED_UP, SPEED_DOWN, ROTATE_TOOL
    };

    private ArchitectKeyMappings() {}
}
