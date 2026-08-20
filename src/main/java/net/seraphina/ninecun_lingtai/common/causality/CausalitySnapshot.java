package net.seraphina.ninecun_lingtai.common.causality;

import java.util.List;
import java.util.UUID;

public record CausalitySnapshot(
        boolean playerListMode,
        List<CausalityPlayerSnapshot> players,
        CausalityPlayerSnapshot target,
        List<CausalityRecord> records,
        boolean fullHistory,
        String message
) {
    public CausalitySnapshot {
        players = players == null ? List.of() : List.copyOf(players);
        records = records == null ? List.of() : List.copyOf(records);
        message = message == null ? "" : message;
    }

    public UUID targetId() {
        return this.target == null ? null : this.target.playerId();
    }
}
