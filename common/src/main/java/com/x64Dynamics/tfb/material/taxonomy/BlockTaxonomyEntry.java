package com.x64Dynamics.tfb.material.taxonomy;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record BlockTaxonomyEntry(
    BlockIdentifier block,
    Set<BlockTrait> traits,
    MaterialSubstance substance,
    ColorTone primaryTone,
    ColorTone accentTone,
    int luminance
) {
    public BlockTaxonomyEntry {
        traits = traits == null || traits.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(traits));
    }

    public boolean hasTrait(BlockTrait trait) {
        return traits.contains(trait);
    }
}
