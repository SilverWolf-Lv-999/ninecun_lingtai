package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellShape;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientDefenseSpellManager {
    private static final Map<Integer, DefenseSpellSnapshot> BODY_SHIELDS = new HashMap<>();
    private static final Map<Integer, DefenseSpellSnapshot> GUARDIANS = new HashMap<>();

    private ClientDefenseSpellManager() {
    }

    public static void accept(
            int entityId,
            DefenseSpellSnapshot bodyShield,
            DefenseSpellSnapshot guardian
    ) {
        acceptSnapshot(BODY_SHIELDS, entityId, bodyShield, DefenseSpellShape.BODY_SHIELD);
        acceptSnapshot(GUARDIANS, entityId, guardian, DefenseSpellShape.GUARDIAN);
    }

    public static Map<Integer, DefenseSpellSnapshot> getBodyShields() {
        return Collections.unmodifiableMap(BODY_SHIELDS);
    }

    public static Optional<DefenseSpellSnapshot> getBodyShield(int entityId) {
        return Optional.ofNullable(BODY_SHIELDS.get(entityId));
    }

    public static List<DefenseSpellSnapshot> getActiveSpells(int entityId) {
        List<DefenseSpellSnapshot> activeSpells = new ArrayList<>(2);
        DefenseSpellSnapshot bodyShield = BODY_SHIELDS.get(entityId);
        if (bodyShield != null) {
            activeSpells.add(bodyShield);
        }
        DefenseSpellSnapshot guardian = GUARDIANS.get(entityId);
        if (guardian != null) {
            activeSpells.add(guardian);
        }
        return Collections.unmodifiableList(activeSpells);
    }

    public static void tick() {
        tickSnapshots(BODY_SHIELDS);
        tickSnapshots(GUARDIANS);
    }

    public static void removeEntity(int entityId) {
        BODY_SHIELDS.remove(entityId);
        GUARDIANS.remove(entityId);
    }

    public static void reset() {
        BODY_SHIELDS.clear();
        GUARDIANS.clear();
    }

    private static void acceptSnapshot(
            Map<Integer, DefenseSpellSnapshot> snapshots,
            int entityId,
            DefenseSpellSnapshot snapshot,
            DefenseSpellShape expectedShape
    ) {
        if (snapshot == null
                || snapshot.kind().getShape() != expectedShape
                || snapshot.current() <= 0.0D
                || snapshot.remainingTicks() <= 0) {
            snapshots.remove(entityId);
        } else {
            snapshots.put(entityId, snapshot);
        }
    }

    private static void tickSnapshots(Map<Integer, DefenseSpellSnapshot> snapshots) {
        Iterator<Map.Entry<Integer, DefenseSpellSnapshot>> iterator = snapshots.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, DefenseSpellSnapshot> entry = iterator.next();
            DefenseSpellSnapshot snapshot = entry.getValue();
            if (snapshot.remainingTicks() <= 1) {
                iterator.remove();
                continue;
            }
            entry.setValue(new DefenseSpellSnapshot(
                    snapshot.kind(),
                    snapshot.current(),
                    snapshot.maximum(),
                    snapshot.elementMask(),
                    snapshot.remainingTicks() - 1
            ));
        }
    }
}
