package com.x64Dynamics.tfb.brush;

import com.x64Dynamics.tfb.architectural.PlacementTarget;

import java.util.List;

public interface IConstructionBrush {
    String id();
    String displayName();
    BrushCategory category();
    List<PlacementTarget> generate(BrushContext context);
}
