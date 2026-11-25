package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArchitectFlightController {
    private static boolean wasInArchitectMode = false;
    private static boolean wasGridMouseDetached = false;
    private static boolean hadMayFly = false;
    private static boolean hadFlying = false;

    private ArchitectFlightController() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ClientController controller = ClientController.getInstance();
        boolean architectMode = controller.isArchitectMode();
        controller.model().cameraController().setActive(architectMode);
        if (architectMode) controller.model().cameraController().tick(0.05f);
        boolean detachMouse = architectMode && controller.isGridMouseDetached() && mc.screen == null;
        if (detachMouse && mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        } else if (!detachMouse && wasGridMouseDetached && mc.screen == null && !mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.grabMouse();
        }
        wasGridMouseDetached = detachMouse;
        boolean flightActive = controller.isFlightActive();

        // Mode transition: entered Architect Mode
        if (architectMode && !wasInArchitectMode) {
            wasInArchitectMode = true;
            hadMayFly = mc.player.getAbilities().mayfly;
            hadFlying = mc.player.getAbilities().flying;
        }

        // Mode transition: exited Architect Mode
        if (!architectMode && wasInArchitectMode) {
            restorePreviousState(mc);
            return;
        }

        if (!architectMode || !flightActive) {
            // Mode is inactive or flight is paused
            return;
        }

        // Enable flying abilities and prevent fall damage
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;
        mc.player.resetFallDistance();

        // When a screen is open (e.g. Architect Workspace, chat), lock velocity so builder hovers stably
        if (mc.screen != null) {
            mc.player.setDeltaMovement(0, 0, 0);
            return;
        }

        // Active in-world flight navigation
        float baseSpeed = 0.50f * controller.getFlightSpeed();
        float vertBaseSpeed = 0.42f * controller.getFlightSpeed();

        // Speed multipliers: Ctrl / Sprint = 2.5x fast transit, Alt = 0.25x precision alignment
        float multiplier = 1.0f;
        if (mc.options.keySprint.isDown()) {
            multiplier = 2.5f;
        } else if (Screen.hasAltDown()) {
            multiplier = 0.25f;
        }

        float speed = baseSpeed * multiplier;
        float vertSpeed = vertBaseSpeed * multiplier;

        // Read movement inputs
        double forward = 0;
        if (mc.options.keyUp.isDown()) forward += 1.0;
        if (mc.options.keyDown.isDown()) forward -= 1.0;

        double strafe = 0;
        if (mc.options.keyLeft.isDown()) strafe += 1.0;
        if (mc.options.keyRight.isDown()) strafe -= 1.0;

        // Directional vectors from camera yaw
        float yaw = mc.player.getYRot();
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);

        double moveX = (forward * fx) + (strafe * rx);
        double moveZ = (forward * fz) + (strafe * rz);
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0.001) {
            moveX = (moveX / len) * speed;
            moveZ = (moveZ / len) * speed;
        } else {
            // Instant horizontal stop (no vanilla momentum drift)
            moveX = 0;
            moveZ = 0;
        }

        // Vertical ascending & descending
        double moveY = 0;
        if (mc.options.keyJump.isDown()) {
            moveY += vertSpeed;
        }
        if (mc.options.keyShift.isDown()) {
            moveY -= vertSpeed;
        }

        mc.player.setDeltaMovement(moveX, moveY, moveZ);
        mc.player.hasImpulse = true;
    }

    public static void restorePreviousState(Minecraft mc) {
        wasInArchitectMode = false;
        if (mc.player != null) {
            mc.player.getAbilities().mayfly = hadMayFly;
            mc.player.getAbilities().flying = hadFlying;
            mc.player.resetFallDistance();
            mc.player.displayClientMessage(
                Component.literal("§e[TFB] Architect Mode: §cDisabled §7(Restored standard movement)."),
                true
            );
        }
    }
}
