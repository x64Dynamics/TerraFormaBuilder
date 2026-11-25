package com.x64Dynamics.tfb.architectural.settlement;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.Collections;
import java.util.List;

public record SettlementPlan(
    BoundingBox3i settlementBounds,
    List<SettlementBuildingPlot> buildingPlots,
    List<Vec3i> mainRoadPath,
    List<PlacementTarget> streetlightsAndDecorations
) {
    public SettlementPlan {
        buildingPlots = Collections.unmodifiableList(buildingPlots);
        mainRoadPath = Collections.unmodifiableList(mainRoadPath);
        streetlightsAndDecorations = Collections.unmodifiableList(streetlightsAndDecorations);
    }
}
