package com.x64Dynamics.tfb.material;

public enum MaterialRole {
    WALL_PRIMARY,
    WALL_ACCENT,
    WALL_CORNER,
    FLOOR,
    CEILING,
    ROOF_SLOPE,
    ROOF_RIDGE,
    ROOF_GABLE,
    ROOF_OVERHANG,
    PILLAR,
    BEAM,
    ARCH,
    STAIR,
    SLAB,
    FOUNDATION,
    SUB_FOUNDATION,
    DOOR,
    WINDOW,
    FRAME,
    FENCE,
    BALUSTRADE,
    LIGHT_SOURCE,
    DECORATION,
    AIR;

    public MaterialRole fallback() {
        return switch (this) {
            case WALL_ACCENT, WALL_CORNER -> WALL_PRIMARY;
            case CEILING -> FLOOR;
            case ROOF_RIDGE, ROOF_GABLE, ROOF_OVERHANG -> ROOF_SLOPE;
            case BEAM, ARCH -> PILLAR;
            case SUB_FOUNDATION -> FOUNDATION;
            case BALUSTRADE -> FENCE;
            default -> this;
        };
    }
}
