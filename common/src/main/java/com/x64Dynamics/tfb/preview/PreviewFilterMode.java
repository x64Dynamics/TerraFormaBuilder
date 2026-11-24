package com.x64Dynamics.tfb.preview;

public enum PreviewFilterMode {
    ALL("All Elements", "Displays the full composite structure"),
    FOUNDATIONS_ONLY("Foundations", "Displays only sub-grade foundations and footings"),
    STRUCTURAL_FRAME("Frame", "Displays pillars, beams, and structural load-bearing frame"),
    WALLS_OPENINGS("Walls & Openings", "Displays perimeter walls, doors, and windows"),
    ROOF_FACADE("Roof & Facade", "Displays roofs, dormers, chimneys, and facade ornaments"),
    INTERIORS_ONLY("Interiors", "Displays internal rooms, partitions, and furnishings"),
    CONFLICTS_ONLY("Conflicts Only", "Highlights only invalid placements and collisions");

    private final String displayName;
    private final String description;

    PreviewFilterMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}