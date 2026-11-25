package com.x64Dynamics.tfb.architectural.builds.brush;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;

import java.util.List;

public interface IConstructionBrush {
    String id();
    String displayName();
    BrushCategory category();
    List<PlacementTarget> generate(BrushContext context);
}
