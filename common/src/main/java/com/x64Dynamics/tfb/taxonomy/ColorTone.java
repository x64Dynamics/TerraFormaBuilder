package com.x64Dynamics.tfb.taxonomy;

public enum ColorTone {
    WHITE(235, 235, 235),
    LIGHT_GRAY(165, 165, 165),
    DARK_GRAY(85, 85, 90),
    BLACK(32, 32, 36),
    BROWN_WARM(154, 108, 66),
    BROWN_COOL(104, 78, 52),
    RED_WARM(170, 72, 62),
    ORANGE(204, 118, 58),
    YELLOW(216, 188, 86),
    GREEN(86, 134, 68),
    BLUE_AQUA(76, 138, 166),
    PURPLE(128, 78, 148);

    private final int r;
    private final int g;
    private final int b;

    ColorTone(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int r() { return r; }
    public int g() { return g; }
    public int b() { return b; }

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
