package com.x64Dynamics.tfb.ui;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.interaction.SelectionHandle;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;

public final class TFBClientController {
    private static final TFBClientController INSTANCE = new TFBClientController();

    private final ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
    private final ArchitectWorkspaceScreen screen = new ArchitectWorkspaceScreen(model);
    private boolean previewActive = true;
    private SelectionHandle activeHoveredHandle = null;

    // Architect Mode & Flight Lifecycle
    private boolean architectMode = false;
    private boolean flightActive = true;
    private float flightSpeed = 1.0f;
    private boolean noclip = false;

    private TFBClientController() {}

    public static TFBClientController getInstance() {
        return INSTANCE;
    }

    public ArchitectWorkspaceModel model() { return model; }
    public ArchitectWorkspaceScreen screen() { return screen; }

    public boolean isPreviewActive() { return previewActive; }
    public void setPreviewActive(boolean active) { this.previewActive = active; }
    public void togglePreview() { this.previewActive = !this.previewActive; }

    public boolean isArchitectMode() { return architectMode; }
    public void setArchitectMode(boolean active) { this.architectMode = active; }
    public void toggleArchitectMode() { this.architectMode = !this.architectMode; }

    public boolean isFlightActive() { return flightActive; }
    public void setFlightActive(boolean active) { this.flightActive = active; }
    public void toggleFlight() { this.flightActive = !this.flightActive; }

    public float getFlightSpeed() { return flightSpeed; }
    public void setFlightSpeed(float speed) {
        this.flightSpeed = Math.max(0.2f, Math.min(4.0f, speed));
    }
    public void adjustFlightSpeed(float delta) {
        setFlightSpeed(this.flightSpeed + delta);
    }

    public boolean isNoclip() { return noclip; }
    public void setNoclip(boolean noclip) { this.noclip = noclip; }

    public SelectionHandle activeHoveredHandle() { return activeHoveredHandle; }
    public void setActiveHoveredHandle(SelectionHandle handle) { this.activeHoveredHandle = handle; }

    public void setTargetPosition(Vec3i pos) {
        model.setOrigin(pos);
    }
}