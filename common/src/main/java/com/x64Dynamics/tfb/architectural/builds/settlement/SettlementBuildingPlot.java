package com.x64Dynamics.tfb.architectural.builds.settlement;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;

public record SettlementBuildingPlot(
    BoundingBox3i bounds,
    String brushType,
    Direction entranceFacing
) { }
