package com.x64Dynamics.tfb.construction.preview;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.ui.interaction.SelectionHandle;
import com.x64Dynamics.tfb.ui.rendering.IWorldRenderContext;

import java.util.List;

public class PreviewRenderer {
    private final WorldPreviewBuffer buffer;
    private boolean showBoundingBox = true;
    private boolean showHandles = true;
    private boolean showVoxels = true;

    public PreviewRenderer(WorldPreviewBuffer buffer) {
        this.buffer = buffer;
    }

    public WorldPreviewBuffer buffer() { return buffer; }

    public boolean isShowBoundingBox() { return showBoundingBox; }
    public void setShowBoundingBox(boolean show) { this.showBoundingBox = show; }

    public boolean isShowHandles() { return showHandles; }
    public void setShowHandles(boolean show) { this.showHandles = show; }

    public boolean isShowVoxels() { return showVoxels; }
    public void setShowVoxels(boolean show) { this.showVoxels = show; }

    public void render(IWorldRenderContext context, SelectionHandle hoveredHandle) {
        if (context == null || buffer == null) return;

        BoundingBox3i bounds = buffer.cachedBounds();
        if (bounds == null) return;

        // 1. Draw Bounding Box Wireframe
        if (showBoundingBox) {
            boolean hasConflicts = buffer.invalidVoxelCount() > 0;
            float r = hasConflicts ? 0.95f : 0.22f;
            float g = hasConflicts ? 0.25f : 0.74f;
            float b = hasConflicts ? 0.25f : 0.97f;
            context.renderBoundingBox(bounds, r, g, b, 0.9f, 2.0f);
        }

        // 2. Draw Translucent Preview Voxels
        if (showVoxels) {
            List<PreviewVoxel> voxels = buffer.getFilteredVoxels();
            for (PreviewVoxel voxel : voxels) {
                context.renderVoxelPreview(voxel.pos(), voxel.block(), voxel.alpha(), voxel.invalidPlacement());
            }
        }

        // 3. Draw In-World Interactive Handles
        if (showHandles) {
            SelectionHandle[] handles = SelectionHandle.createHandles(bounds);
            for (SelectionHandle handle : handles) {
                boolean isHovered = (hoveredHandle != null && hoveredHandle.type() == handle.type());
                float hr = isHovered ? 1.0f : 0.22f;
                float hg = isHovered ? 0.9f : 0.74f;
                float hb = isHovered ? 0.2f : 0.97f;
                context.renderSelectionHandle(handle.position(), handle.radius(), hr, hg, hb, isHovered ? 1.0f : 0.7f);
            }
        }
    }
}