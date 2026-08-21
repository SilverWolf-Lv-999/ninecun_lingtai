package net.seraphina.ninecun_lingtai.client.render.shader.screen.pre;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PreEffectHandler {
    private static final Int2ObjectLinkedOpenHashMap<PreScreenEffect> DATA =
            new Int2ObjectLinkedOpenHashMap<>();
    private static final Set<PreScreenEffect> REGISTERED_EFFECTS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<PreScreenEffect> ACTIVE_EFFECTS = new LinkedHashSet<>();
    private static int nextIndex;

    private PreEffectHandler() {
    }

    public static void registerEffect(PreScreenEffect effect) {
        if (effect == null) {
            throw new NullPointerException("pre effect");
        }
        if (!REGISTERED_EFFECTS.add(effect)) {
            return;
        }
        DATA.put(nextIndex++, effect);
    }

    public static Iterable<PreScreenEffect> getEffects() {
        return DATA.values();
    }

    public static Iterable<PreScreenEffect> getActiveEffects() {
        return ACTIVE_EFFECTS;
    }

    public static void setActive(PreScreenEffect effect, boolean active) {
        if (effect == null || !REGISTERED_EFFECTS.contains(effect)) {
            return;
        }
        if (active) {
            ACTIVE_EFFECTS.add(effect);
        } else {
            ACTIVE_EFFECTS.remove(effect);
        }
    }
}
