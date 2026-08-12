package net.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface TechniqueEffectHandler {
    void apply(
            Player player,
            TechniqueDefinition technique,
            TechniqueEffect effect,
            TechniqueProgress progress
    );
}
