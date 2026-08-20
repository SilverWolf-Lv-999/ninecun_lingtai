package net.seraphina.ninecun_lingtai.common.causality;

import java.util.Objects;

public record CausalityRecord(
        String timestamp,
        long gameTime,
        String dimension,
        int x,
        int y,
        int z,
        String category,
        String description
) {
    public CausalityRecord {
        timestamp = timestamp == null || timestamp.isBlank() ? "unknown" : timestamp;
        dimension = dimension == null || dimension.isBlank() ? "unknown" : dimension;
        category = category == null || category.isBlank() ? "other" : category;
        description = Objects.requireNonNullElse(description, "");
    }
}
