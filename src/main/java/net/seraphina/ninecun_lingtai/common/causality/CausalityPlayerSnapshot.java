package net.seraphina.ninecun_lingtai.common.causality;

import java.util.Objects;
import java.util.UUID;

public record CausalityPlayerSnapshot(
        UUID playerId,
        String playerName,
        String dimension,
        String cultivation
) {
    public CausalityPlayerSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        playerName = playerName == null || playerName.isBlank() ? "Unknown" : playerName;
        dimension = dimension == null || dimension.isBlank() ? "unknown" : dimension;
        cultivation = cultivation == null || cultivation.isBlank() ? "unknown" : cultivation;
    }
}
