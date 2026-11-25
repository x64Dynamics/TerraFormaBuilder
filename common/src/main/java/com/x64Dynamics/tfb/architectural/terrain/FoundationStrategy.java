package com.x64Dynamics.tfb.architectural.terrain;

public enum FoundationStrategy {
    PILLARS_STILTS("pillars_stilts", "Pillars & Stilts"),
    CLIFF_CANTILEVER("cliff_cantilever", "Cliff Cantilever"),
    MOUNTAIN_CAVE_EMBED("mountain_cave_embed", "Mountain Cave Embed"),
    STEPPED_TERRACE("stepped_terrace", "Stepped Terrace"),
    CUT_AND_FILL("cut_and_fill", "Cut & Fill"),
    SOLID_SUB_BASEMENT("solid_sub_basement", "Solid Sub-Basement");

    private final String id;
    private final String displayName;

    FoundationStrategy(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
}
