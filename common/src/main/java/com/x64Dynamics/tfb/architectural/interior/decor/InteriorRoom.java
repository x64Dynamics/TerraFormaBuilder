package com.x64Dynamics.tfb.architectural.interior.decor;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

public record InteriorRoom(
    BoundingBox3i bounds,
    RoomType type,
    Vec3i doorwayPos
) { }
