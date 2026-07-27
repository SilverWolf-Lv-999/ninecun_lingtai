package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;

import java.util.Optional;

public final class ClientDefenseResourceManager {
    private static DefenseResourceSnapshot snapshot = DefenseResourceSnapshot.empty(0);

    private ClientDefenseResourceManager() {
    }

    public static void accept(DefenseResourceSnapshot updatedSnapshot) {
        snapshot = updatedSnapshot == null ? DefenseResourceSnapshot.empty(0) : updatedSnapshot;
    }

    public static Optional<DefenseResourceSnapshot> getSnapshot() {
        return snapshot.isVisible() ? Optional.of(snapshot) : Optional.empty();
    }

    public static void reset() {
        snapshot = DefenseResourceSnapshot.empty(0);
    }
}
