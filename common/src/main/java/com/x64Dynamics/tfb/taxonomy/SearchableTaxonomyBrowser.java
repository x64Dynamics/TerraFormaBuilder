package com.x64Dynamics.tfb.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SearchableTaxonomyBrowser {
    private SearchableTaxonomyBrowser() {}

    public static List<BlockTaxonomyEntry> search(
        String query,
        MaterialSubstance substanceFilter,
        ColorTone colorFilter,
        BlockTrait traitFilter
    ) {
        List<BlockTaxonomyEntry> all = BlockTaxonomyRegistry.getInstance().all();
        List<BlockTaxonomyEntry> matches = new ArrayList<>();

        String cleanQuery = (query != null) ? query.trim().toLowerCase(Locale.ROOT) : "";

        for (BlockTaxonomyEntry entry : all) {
            // Query filter
            if (!cleanQuery.isEmpty() && !entry.block().namespacedId().toLowerCase(Locale.ROOT).contains(cleanQuery)) {
                continue;
            }
            // Substance filter
            if (substanceFilter != null && entry.substance() != substanceFilter) {
                continue;
            }
            // Color filter
            if (colorFilter != null && entry.primaryTone() != colorFilter) {
                continue;
            }
            // Trait filter
            if (traitFilter != null && !entry.hasTrait(traitFilter)) {
                continue;
            }

            matches.add(entry);
        }

        return matches;
    }
}