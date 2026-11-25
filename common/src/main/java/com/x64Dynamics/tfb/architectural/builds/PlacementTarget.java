package com.x64Dynamics.tfb.architectural.builds;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;

public record PlacementTarget(Vec3i pos, BlockIdentifier block, MaterialRole role) {
}
