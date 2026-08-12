package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class SwiftWindSpell extends Spell {
    private static final int FULL_DURATION_TICKS = 20 * 60;
    private static final int MAXIMUM_AMPLIFIER = 4;

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        int realmIndex = (context.cultivationStrength() - 1) / CultivationStage.MAXIMUM_LAYER;
        int amplifier = (int) Math.round(
                Math.min(MAXIMUM_AMPLIFIER, realmIndex) * context.powerEfficiency()
        );
        int duration = (int) Math.round(FULL_DURATION_TICKS * context.powerEfficiency());
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                Math.max(20, duration),
                amplifier
        ));
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS,
                0.6F,
                1.6F
        );
        return true;
    }
}
