package com.x64Dynamics.tfb.architectural.builds.brush;

public enum BrushCategory {
    ARCHITECTURAL("Architectural"),
    STRUCTURAL("Structural"),
    INFRASTRUCTURE("Infrastructure"),
    CUSTOM("Custom");

    private final String displayName;

    BrushCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
