package com.x64Dynamics.tfb.preview;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;

public record PreviewVoxel(
    Vec3i pos,
    BlockIdentifier block,
    MaterialRole role,
    boolean invalidPlacement,
    boolean isModified,
    float alpha
) {
    public PreviewVoxel withInvalid(boolean invalid) {
        return new PreviewVoxel(pos, block, role, invalid, isModified, alpha);
    }

    public PreviewVoxel withAlpha(float alpha) {
        return new PreviewVoxel(pos, block, role, invalidPlacement, isModified, alpha);
    }
}