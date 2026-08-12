package net.seraphina.ninecun_lingtai.client.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClientBodyShieldManager {
    private static final Map<Integer, BodyShieldVisualState> VISUAL_STATES = new HashMap<>();

    private ClientBodyShieldManager() {
    }

    public static void trigger(int entityId) {
        VISUAL_STATES.compute(entityId, (id, visualState) -> {
            BodyShieldVisualState activeState = visualState == null ? new BodyShieldVisualState() : visualState;
            activeState.restart();
            return activeState;
        });
    }

    public static void tick() {
        VISUAL_STATES.values().forEach(BodyShieldVisualState::tick);
        VISUAL_STATES.entrySet().removeIf(entry -> entry.getValue().isFinished());
    }

    public static Map<Integer, BodyShieldVisualState> getVisualStates() {
        return Collections.unmodifiableMap(VISUAL_STATES);
    }

    public static void removeEntity(int entityId) {
        VISUAL_STATES.remove(entityId);
    }

    public static void reset() {
        VISUAL_STATES.clear();
    }
}
