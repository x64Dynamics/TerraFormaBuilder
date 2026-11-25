package com.x64Dynamics.tfb.ui.rendering;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

public interface IWorldRenderContext {
    void renderBoundingBox(BoundingBox3i box, float red, float green, float blue, float alpha, float lineWidth);
    void renderVoxelPreview(Vec3i pos, BlockIdentifier block, float alpha, boolean invalid);
    void renderSelectionHandle(Vec3d pos, float size, float red, float green, float blue, float alpha);
}