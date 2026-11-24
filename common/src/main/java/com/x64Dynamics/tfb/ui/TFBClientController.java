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

    private TFBClientController() {}

    public static TFBClientController getInstance() {
        return INSTANCE;
    }

    public ArchitectWorkspaceModel model() { return model; }
    public ArchitectWorkspaceScreen screen() { return screen; }

    public boolean isPreviewActive() { return previewActive; }
    public void setPreviewActive(boolean active) { this.previewActive = active; }
    public void togglePreview() { this.previewActive = !this.previewActive; }

    public SelectionHandle activeHoveredHandle() { return activeHoveredHandle; }
    public void setActiveHoveredHandle(SelectionHandle handle) { this.activeHoveredHandle = handle; }

    public void setTargetPosition(Vec3i pos) {
        model.setOrigin(pos);
    }
}