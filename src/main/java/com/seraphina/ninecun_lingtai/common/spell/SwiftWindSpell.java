package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** 御风术的神通实现；名称、品级与消耗见 data/ninecun_lingtai/techniques/swift_wind_spell.json。 */
public final class SwiftWindSpell extends Spell {
    private static final int FULL_DURATION_TICKS = 20 * 60;
    private static final int MAXIMUM_AMPLIFIER = 4;

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        // 每进一个大境界，御风的身法便快上一分；掌握不足则难以尽展其妙。
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
