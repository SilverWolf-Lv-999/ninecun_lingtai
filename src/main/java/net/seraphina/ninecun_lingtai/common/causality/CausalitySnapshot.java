package net.seraphina.ninecun_lingtai.common.causality;

import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;

import java.util.List;
import java.util.UUID;

public record CausalitySnapshot(
        boolean playerListMode,
        List<CausalityPlayerSnapshot> players,
        List<CausalityPlayerSnapshot> settingsTargets,
        CausalityPlayerSnapshot target,
        List<CausalityRecord> records,
        boolean fullHistory,
        String message,
        GreatCausalitySettings settings,
        boolean settingsAvailable
) {
    public CausalitySnapshot {
        players = players == null ? List.of() : List.copyOf(players);
        settingsTargets = settingsTargets == null ? List.of() : List.copyOf(settingsTargets);
        records = records == null ? List.of() : List.copyOf(records);
        message = message == null ? "" : message;
        settings = settings == null
                ? GreatCausalitySettings.defaultSettings()
                : settings;
    }

    public UUID targetId() {
        return this.target == null ? null : this.target.playerId();
    }
}
