package com.x64Dynamics.tfb.architectural.builds.style;

import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;

public record ArchitecturalStyleRuleSet(
    String styleId,
    RoofGenerator.RoofType preferredRoofType,
    float roofPitch,
    int overhang,
    int windowSpacing,
    int wallThickness,
    float ornamentDensity,
    boolean allowPilasters,
    boolean allowShutters,
    boolean allowBalconies,
    boolean allowDormers,
    boolean allowCornice
) {
    public static final ArchitecturalStyleRuleSet MEDIEVAL = new ArchitecturalStyleRuleSet(
        "medieval", RoofGenerator.RoofType.GABLE, 1.5f, 1, 3, 1, 0.75f, true, true, true, true, true
    );

    public static final ArchitecturalStyleRuleSet MODERN = new ArchitecturalStyleRuleSet(
        "modern", RoofGenerator.RoofType.FLAT, 0.0f, 0, 1, 1, 0.15f, false, false, true, false, false
    );

    public static final ArchitecturalStyleRuleSet RUSTIC = new ArchitecturalStyleRuleSet(
        "rustic", RoofGenerator.RoofType.GABLE, 1.0f, 2, 4, 1, 0.5f, false, true, false, true, true
    );

    public static final ArchitecturalStyleRuleSet FANTASY = new ArchitecturalStyleRuleSet(
        "fantasy", RoofGenerator.RoofType.MANSARD, 2.0f, 2, 3, 1, 0.95f, true, true, true, true, true
    );

    public static final ArchitecturalStyleRuleSet INDUSTRIAL = new ArchitecturalStyleRuleSet(
        "industrial", RoofGenerator.RoofType.FLAT, 0.0f, 0, 2, 2, 0.3f, true, false, false, false, false
    );

    public static final ArchitecturalStyleRuleSet TRADITIONAL = new ArchitecturalStyleRuleSet(
        "traditional", RoofGenerator.RoofType.HIP, 1.0f, 1, 3, 1, 0.6f, true, true, true, true, true
    );

    public static final ArchitecturalStyleRuleSet MINIMALIST = new ArchitecturalStyleRuleSet(
        "minimalist", RoofGenerator.RoofType.FLAT, 0.0f, 0, 2, 1, 0.05f, false, false, false, false, false
    );
}