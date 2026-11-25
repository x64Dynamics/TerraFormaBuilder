package com.x64Dynamics.tfb.ui.camera;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3d;

/**
 * Pure-Java architectural planning camera controller supporting Isometric (default),
 * Top-Down, Front, Side, Perspective, and Free orbit views with smooth interpolation,
 * zoom, pan, focus targeting, and explicit separation from building orientation.
 */
public final class ArchitectCameraController {
    public enum CameraViewMode {
        ISOMETRIC(45.0f, 35.264f),
        TOP_DOWN(0.0f, 89.0f),
        FRONT(0.0f, 15.0f),
        SIDE(90.0f, 15.0f),
        PERSPECTIVE(30.0f, 25.0f),
        FREE(45.0f, 35.264f);

        private final float defaultYaw;
        private final float defaultPitch;

        CameraViewMode(float defaultYaw, float defaultPitch) {
            this.defaultYaw = defaultYaw;
            this.defaultPitch = defaultPitch;
        }

        public float defaultYaw() { return defaultYaw; }
        public float defaultPitch() { return defaultPitch; }
    }

    private boolean active = false;
    private CameraViewMode viewMode = CameraViewMode.ISOMETRIC;

    private float yaw = CameraViewMode.ISOMETRIC.defaultYaw();
    private float pitch = CameraViewMode.ISOMETRIC.defaultPitch();
    private float targetYaw = yaw;
    private float targetPitch = pitch;

    private double zoomDistance = 24.0;
    private double targetZoomDistance = 24.0;

    private Vec3d focusTarget = new Vec3d(0, 64, 0);
    private Vec3d desiredFocusTarget = new Vec3d(0, 64, 0);

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public CameraViewMode viewMode() { return viewMode; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public double zoomDistance() { return zoomDistance; }
    public Vec3d focusTarget() { return focusTarget; }

    public void setViewMode(CameraViewMode mode) {
        this.viewMode = mode;
        if (mode != CameraViewMode.FREE) {
            this.targetYaw = mode.defaultYaw();
            this.targetPitch = mode.defaultPitch();
        }
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        this.viewMode = CameraViewMode.FREE;
        this.targetYaw = (this.targetYaw + deltaYaw) % 360.0f;
        this.targetPitch = Math.max(-85.0f, Math.min(89.5f, this.targetPitch + deltaPitch));
    }

    public void zoom(double delta) {
        this.targetZoomDistance = Math.max(4.0, Math.min(160.0, this.targetZoomDistance + delta));
    }

    public void pan(double dx, double dz) {
        double rad = Math.toRadians(yaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);
        this.desiredFocusTarget = new Vec3d(
            desiredFocusTarget.x() + rightX * dx + forwardX * dz,
            desiredFocusTarget.y(),
            desiredFocusTarget.z() + rightZ * dx + forwardZ * dz
        );
    }

    public void focusOn(BoundingBox3i box) {
        if (box == null) return;
        double cx = (box.min().x() + box.max().x() + 1) * 0.5;
        double cy = (box.min().y() + box.max().y() + 1) * 0.5;
        double cz = (box.min().z() + box.max().z() + 1) * 0.5;
        this.desiredFocusTarget = new Vec3d(cx, cy, cz);
        int span = Math.max(box.sizeX(), Math.max(box.sizeY(), box.sizeZ()));
        this.targetZoomDistance = Math.max(8.0, Math.min(160.0, span * 1.8));
    }

    /**
     * Advances smooth camera interpolation toward target yaw, pitch, zoom, and focus.
     */
    public void tick(float deltaSeconds) {
        if (!active) return;
        double alpha = Math.min(1.0, Math.max(0.05, deltaSeconds * 10.0));
        this.yaw += (float) ((targetYaw - yaw) * alpha);
        this.pitch += (float) ((targetPitch - pitch) * alpha);
        this.zoomDistance += (targetZoomDistance - zoomDistance) * alpha;
        this.focusTarget = new Vec3d(
            focusTarget.x() + (desiredFocusTarget.x() - focusTarget.x()) * alpha,
            focusTarget.y() + (desiredFocusTarget.y() - focusTarget.y()) * alpha,
            focusTarget.z() + (desiredFocusTarget.z() - focusTarget.z()) * alpha
        );
    }

    public Vec3d computeCameraPosition() {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double horiz = zoomDistance * Math.cos(pitchRad);
        double dy = zoomDistance * Math.sin(pitchRad);
        double dx = -Math.sin(yawRad) * horiz;
        double dz = Math.cos(yawRad) * horiz;
        return new Vec3d(focusTarget.x() + dx, focusTarget.y() + dy, focusTarget.z() + dz);
    }

    /**
     * Returns the cardinal direction the camera is looking toward, kept distinct
     * from any selected building's own facing orientation.
     */
    public Direction viewingCardinal() {
        float norm = ((yaw % 360.0f) + 360.0f) % 360.0f;
        if (norm >= 315 || norm < 45) return Direction.SOUTH;
        if (norm < 135) return Direction.WEST;
        if (norm < 225) return Direction.NORTH;
        return Direction.EAST;
    }
}
