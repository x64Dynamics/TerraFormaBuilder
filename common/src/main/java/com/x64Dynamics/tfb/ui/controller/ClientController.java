package com.x64Dynamics.tfb.ui.controller;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.ui.interaction.SelectionHandle;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;

public final class ClientController {
    private static final ClientController INSTANCE = new ClientController();

    private final ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
    private final ArchitectWorkspaceScreen screen = new ArchitectWorkspaceScreen(model);
    private boolean previewActive = true;
    private SelectionHandle activeHoveredHandle = null;

    // Architect Mode & Flight Lifecycle
    private boolean architectMode = false;
    private boolean flightActive = true;
    private float flightSpeed = 1.0f;
    private boolean noclip = false;

    // HUD Customization & Visibility
    private boolean hudVisible = true;
    private boolean studioBannerVisible = true;
    private boolean builderHotbarVisible = true;
    private boolean tooltipsVisible = true;

    private ClientController() {}

    public static ClientController getInstance() {
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

    public boolean isHudVisible() { return hudVisible; }
    public void setHudVisible(boolean v) { this.hudVisible = v; }
    public void toggleHud() { this.hudVisible = !this.hudVisible; }

    public boolean isStudioBannerVisible() { return studioBannerVisible; }
    public void setStudioBannerVisible(boolean v) { this.studioBannerVisible = v; }
    public void toggleStudioBanner() { this.studioBannerVisible = !this.studioBannerVisible; }

    public boolean isBuilderHotbarVisible() { return builderHotbarVisible; }
    public void setBuilderHotbarVisible(boolean v) { this.builderHotbarVisible = v; }
    public void toggleBuilderHotbar() { this.builderHotbarVisible = !this.builderHotbarVisible; }

    public boolean isTooltipsVisible() { return tooltipsVisible; }
    public void setTooltipsVisible(boolean v) { this.tooltipsVisible = v; }
    public void toggleTooltips() { this.tooltipsVisible = !this.tooltipsVisible; }

    public SelectionHandle activeHoveredHandle() { return activeHoveredHandle; }
    public void setActiveHoveredHandle(SelectionHandle handle) { this.activeHoveredHandle = handle; }

    public void setTargetPosition(Vec3i pos) {
        model.setOrigin(pos);
    }
}