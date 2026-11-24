package com.x64Dynamics.tfb.architectural;

public enum WingConfiguration {
    SINGLE_RECTANGLE("Single Rectangle", "Simple rectangular building footprint"),
    L_SHAPED("L-Shaped", "Main hall with perpendicular side wing"),
    T_SHAPED("T-Shaped", "Central pavilion with balanced cross-wings"),
    U_SHAPED("U-Shaped Courtyard", "Three connected wings enclosing an open courtyard"),
    CORNER_TOWER("Attached Corner Tower", "Main hall with an integrated multi-storey corner tower");

    private final String displayName;
    private final String description;

    WingConfiguration(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}