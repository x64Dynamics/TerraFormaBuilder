package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArchitectFlightController {
    private static boolean wasInArchitectMode = false;
    private static boolean hadMayFly = false;
    private static boolean hadFlying = false;

    private ArchitectFlightController() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ClientController controller = ClientController.getInstance();
        boolean architectMode = controller.isArchitectMode();
        controller.model().cameraController().setActive(architectMode);
        if (architectMode) controller.model().cameraController().tick(0.05f);
        boolean flightActive = controller.isFlightActive();

        if (architectMode && !wasInArchitectMode) {
            wasInArchitectMode = true;
            hadMayFly = mc.player.getAbilities().mayfly;
            hadFlying = mc.player.getAbilities().flying;
        }

        if (!architectMode && wasInArchitectMode) {
            restorePreviousState(mc);
            return;
        }

        if (!architectMode || !flightActive) {
            return;
        }

        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;
        mc.player.resetFallDistance();

        if (mc.screen != null) {
            mc.player.setDeltaMovement(0, 0, 0);
            return;
        }

        float baseSpeed = 0.50f * controller.getFlightSpeed();
        float vertBaseSpeed = 0.42f * controller.getFlightSpeed();

        float multiplier = 1.0f;
        if (mc.options.keySprint.isDown()) {
            multiplier = 2.5f;
        } else if (mc.hasAltDown()) {
            multiplier = 0.25f;
        }

        float speed = baseSpeed * multiplier;
        float vertSpeed = vertBaseSpeed * multiplier;

        double forward = 0;
        if (mc.options.keyUp.isDown()) forward += 1.0;
        if (mc.options.keyDown.isDown()) forward -= 1.0;

        double strafe = 0;
        if (mc.options.keyLeft.isDown()) strafe += 1.0;
        if (mc.options.keyRight.isDown()) strafe -= 1.0;

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
            moveX = 0;
            moveZ = 0;
        }

        double moveY = 0;
        if (mc.options.keyJump.isDown()) {
            moveY += vertSpeed;
        }
        if (mc.options.keyShift.isDown()) {
            moveY -= vertSpeed;
        }

        mc.player.setDeltaMovement(moveX, moveY, moveZ);
        mc.player.needsSync = true;
    }

    public static void restorePreviousState(Minecraft mc) {
        wasInArchitectMode = false;
        if (mc.player != null) {
            mc.player.getAbilities().mayfly = hadMayFly;
            mc.player.getAbilities().flying = hadFlying;
            if (!hadMayFly) {
                mc.player.onUpdateAbilities();
            }
        }
    }
}
