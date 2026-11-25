package com.x64Dynamics.tfb.construction.editing;

public enum ArchitecturalComponentType {
    FOUNDATION("Foundation", "Sub-grade supports and footings"),
    FLOOR("Floor", "Floor surfaces and horizontal decking"),
    CEILING("Ceiling", "Ceiling joists and overhead decking"),
    WALL("Wall", "Perimeter and partition walls"),
    ROOF("Roof", "Sloped roofs, ridges, and gables"),
    FACADE_DETAIL("Facade Detail", "Pilasters, shutters, cornices, and balconies"),
    INTERIOR_ROOM("Interior Room", "Room partitions and doorways"),
    FURNITURE("Furniture", "Beds, tables, hearths, and decorations"),
    INFRASTRUCTURE("Infrastructure", "Roads, paths, and bridges"),
    ENTIRE_STRUCTURE("Entire Structure", "Complete composite building");

    private final String displayName;
    private final String description;

    ArchitecturalComponentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}