package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClientDefenseSpellManager {
    private static final Map<Integer, DefenseSpellSnapshot> BODY_SHIELDS = new HashMap<>();

    private ClientDefenseSpellManager() {
    }

    public static void accept(int entityId, DefenseSpellSnapshot snapshot) {
        if (snapshot == null || snapshot.current() <= 0.0D) {
            BODY_SHIELDS.remove(entityId);
        } else {
            BODY_SHIELDS.put(entityId, snapshot);
        }
    }

    public static Map<Integer, DefenseSpellSnapshot> getBodyShields() {
        return Collections.unmodifiableMap(BODY_SHIELDS);
    }

    public static void removeEntity(int entityId) {
        BODY_SHIELDS.remove(entityId);
    }

    public static void reset() {
        BODY_SHIELDS.clear();
    }
}
