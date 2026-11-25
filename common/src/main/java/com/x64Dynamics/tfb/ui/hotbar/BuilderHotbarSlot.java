package com.x64Dynamics.tfb.ui.hotbar;

import com.x64Dynamics.tfb.architectural.brush.IConstructionBrush;

public record BuilderHotbarSlot(
    String code,
    String name,
    String category,
    String iconItemKey,
    int accentColor,
    IConstructionBrush brush,
    String contextualHint
) {}
