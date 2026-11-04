package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class MobEffectUtility {
    static final MobEffect[] badEffects = new MobEffect[]{
            MobEffects.DARKNESS,
            MobEffects.BLINDNESS,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.HARM,
    };

    public static boolean isBadMobEffect(MobEffect pEffect) {
        for (MobEffect pMobEffect : badEffects) {
            if (pMobEffect.equals(pEffect)) {
                return true;
            }
        }
        return pEffect.getCategory().equals(MobEffectCategory.HARMFUL);
    }

    public static void cleanBadEffect(LivingEntity entity) {
        if (entity == null) return;
        entity.getActiveEffects().forEach(effect -> {
            if (isBadMobEffect(effect.getEffect())) entity.removeEffect(effect.getEffect());
        });
    }
}
