package com.x64Dynamics.tfb.ui.hotbar;

import com.x64Dynamics.tfb.brush.IConstructionBrush;

public record BuilderHotbarSlot(
    String code,
    String name,
    String category,
    int accentColor,
    IConstructionBrush brush,
    String contextualHint
) {}
