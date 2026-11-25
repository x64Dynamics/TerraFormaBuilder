package com.x64Dynamics.tfb.construction;

import com.x64Dynamics.tfb.material.MaterialRole;

public enum TopologicalPhase {
    EXCAVATION,
    FOUNDATION,
    STRUCTURE,
    FLOORS_CEILINGS,
    WALLS_OPENINGS,
    ROOF,
    DETAILS_LIGHTING;

    public static TopologicalPhase fromRole(MaterialRole role) {
        return switch (role) {
            case FOUNDATION, SUB_FOUNDATION -> FOUNDATION;
            case PILLAR, BEAM, ARCH -> STRUCTURE;
            case FLOOR, CEILING -> FLOORS_CEILINGS;
            case WALL_PRIMARY, WALL_ACCENT, WALL_CORNER, WINDOW, DOOR, FRAME -> WALLS_OPENINGS;
            case ROOF_SLOPE, ROOF_RIDGE, ROOF_GABLE, ROOF_OVERHANG -> ROOF;
            default -> DETAILS_LIGHTING;
        };
    }
}
