package net.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TechniqueEffectApi {
    private static final Map<ResourceLocation, TechniqueEffectHandler> EFFECT_HANDLERS = new ConcurrentHashMap<>();

    private TechniqueEffectApi() {
    }

    public static void register(ResourceLocation effectType, TechniqueEffectHandler effectHandler) {
        Objects.requireNonNull(effectType, "effectType");
        Objects.requireNonNull(effectHandler, "effectHandler");
        if (EFFECT_HANDLERS.putIfAbsent(effectType, effectHandler) != null) {
            throw new IllegalArgumentException("Technique effect handler is already registered: " + effectType);
        }
    }

    public static Optional<TechniqueEffectHandler> getHandler(ResourceLocation effectType) {
        return Optional.ofNullable(EFFECT_HANDLERS.get(effectType));
    }

    public static void applyEffects(
            Player player,
            TechniqueDefinition technique,
            TechniqueProgress progress
    ) {
        for (TechniqueEffect effect : technique.effects()) {
            getHandler(effect.type()).ifPresent(handler -> handler.apply(player, technique, effect, progress));
        }
    }
}
