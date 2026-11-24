package com.x64Dynamics.tfb.taxonomy;

public enum ColorTone {
    WHITE,
    LIGHT_GRAY,
    DARK_GRAY,
    BLACK,
    BROWN_WARM,
    BROWN_COOL,
    RED_WARM,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE_AQUA,
    PURPLE;

    public boolean isNeutral() {
        return this == WHITE || this == LIGHT_GRAY || this == DARK_GRAY || this == BLACK;
    }

    public ColorTone complementary() {
        return switch (this) {
            case WHITE -> BLACK;
            case LIGHT_GRAY -> DARK_GRAY;
            case DARK_GRAY -> LIGHT_GRAY;
            case BLACK -> WHITE;
            case BROWN_WARM -> BLUE_AQUA;
            case BROWN_COOL -> ORANGE;
            case RED_WARM -> GREEN;
            case ORANGE -> BLUE_AQUA;
            case YELLOW -> PURPLE;
            case GREEN -> RED_WARM;
            case BLUE_AQUA -> ORANGE;
            case PURPLE -> YELLOW;
        };
    }
}
