package com.x64Dynamics.tfb.architectural.style;

import com.x64Dynamics.tfb.material.StylePreset;

public final class StyleRuleSetRegistry {
    private StyleRuleSetRegistry() {}

    public static ArchitecturalStyleRuleSet forPreset(StylePreset preset) {
        if (preset == null) return ArchitecturalStyleRuleSet.MEDIEVAL;
        return switch (preset) {
            case MEDIEVAL -> ArchitecturalStyleRuleSet.MEDIEVAL;
            case MODERN -> ArchitecturalStyleRuleSet.MODERN;
            case RUSTIC -> ArchitecturalStyleRuleSet.RUSTIC;
            case FANTASY -> ArchitecturalStyleRuleSet.FANTASY;
            case INDUSTRIAL -> ArchitecturalStyleRuleSet.INDUSTRIAL;
            case TRADITIONAL -> ArchitecturalStyleRuleSet.TRADITIONAL;
            case MINIMALIST -> ArchitecturalStyleRuleSet.MINIMALIST;
        };
    }
}