package com.x64Dynamics.tfb.architectural.layout;

public enum WingConfiguration {
    SINGLE_RECTANGLE("Single Rectangle", "Simple rectangular building footprint"),
    L_SHAPED("L-Shaped", "Main hall with perpendicular side wing"),
    T_SHAPED("T-Shaped", "Central pavilion with balanced cross-wings"),
    U_SHAPED("U-Shaped Courtyard", "Three connected wings enclosing an open courtyard"),
    CORNER_TOWER("Attached Corner Tower", "Main hall with an integrated multi-storey corner tower"),
    CROSS_SHAPED("Cruciform Cross", "Central hall with four balanced projecting transept arms"),
    H_SHAPED("H-Shaped Compound", "Dual parallel wings linked by central transverse gallery"),
    OCTAGONAL_EXTENSION("Octagonal Rotunda", "Main rectangular hall with attached octagonal apsidal extension"),
    COURTYARD_COMPOUND("Courtyard Cloister", "Four surrounding wings completely enclosing an internal courtyard");

    private final String displayName;
    private final String description;

    WingConfiguration(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}