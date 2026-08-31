package net.seraphina.ninecun_lingtai.common.spell.soul;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SoulSpellUtility {
    public static final double BACKLASH_EFFICIENCY_THRESHOLD = 0.35D;

    public static int getCasterStars(ServerPlayer player) {
        return PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(SoulStrength.MINIMUM_ILLUMINATED_STARS);
    }

    public static int getTargetStars(LivingEntity target) {
        if (target instanceof Player targetPlayer) {
            return PlayerAptitudeData.getSoulStrength(targetPlayer)
                    .map(SoulStrength::illuminatedStars)
                    .orElse(0);
        }
        // 有修为的生物按大境界折算魂强：练气=1 星，每提升一个大境界 +1，封顶 7 星
        return CreatureCultivationData.getStage(target)
                .map(stage -> Math.min(
                        SoulStrength.MAXIMUM_ILLUMINATED_STARS,
                        stage.realm().ordinal() + 1
                ))
                .orElse(0);
    }

    public static int getMajorRealmIndex(int cultivationStrength) {
        return (Math.max(1, cultivationStrength) - 1) / CultivationStage.MAXIMUM_LAYER;
    }

    public static boolean applyWeakSoulBacklash(ServerPlayer player, double powerEfficiency) {
        if (powerEfficiency >= BACKLASH_EFFICIENCY_THRESHOLD) {
            return false;
        }
        player.hurt(player.damageSources().magic(), 2.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
        SoulInjuryUtility.addInjury(
                player,
                CommonConfig.SOUL_INJURY_SOURCE_WEAK_CAST.get(),
                SoulInjuryUtility.InjurySource.SPELL_BACKLASH
        );
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_spell_backlash"),
                true
        );
        return true;
    }

    public static void applyResistedBacklash(ServerPlayer player) {
        player.hurt(player.damageSources().magic(), 3.0F);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        SoulInjuryUtility.addInjury(
                player,
                CommonConfig.SOUL_INJURY_SOURCE_BACKLASH.get(),
                SoulInjuryUtility.InjurySource.SPELL_BACKLASH
        );
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_spell_resisted"),
                true
        );
    }
}
