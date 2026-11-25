package com.x64Dynamics.tfb.architectural.city;

// Macro urban layout patterns supported by the procedural city planner.
public enum SettlementPattern {
    ORGANIC_VILLAGE("Organic Village"),
    MEDIEVAL_TOWN("Medieval Town"),
    PLANNED_GRID("Planned Grid"),
    SUBURBAN_NEIGHBORHOOD("Suburban Neighborhood"),
    URBAN_DISTRICT("Urban District"),
    INDUSTRIAL_SECTOR("Industrial Sector");

    private final String displayName;

    SettlementPattern(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
