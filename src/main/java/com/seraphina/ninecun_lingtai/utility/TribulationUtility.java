package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.tribulation.TribulationType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class TribulationUtility {
    private TribulationUtility() {
    }

    public static float getAptitudeScore(SpiritualRoot spiritualRoot, SoulStrength soulStrength) {
        return (float) Mth.clamp(
                spiritualRoot.quality() * CommonConfig.TRIBULATION_ROOT_QUALITY_WEIGHT.get()
                        + soulStrength.quality() * CommonConfig.TRIBULATION_SOUL_QUALITY_WEIGHT.get(),
                0.0D,
                1.0D
        );
    }

    public static TribulationType selectType(SpiritualRoot spiritualRoot, SoulStrength soulStrength) {
        return getAptitudeScore(spiritualRoot, soulStrength)
                >= CommonConfig.TRIBULATION_FOUR_NINE_THRESHOLD.get()
                ? TribulationType.FOUR_NINE
                : TribulationType.THREE_NINE;
    }

    public static double getStrikeRadius(float aptitudeScore) {
        // The tribulation area is centered on the player. The base radius is 30 blocks,
        // with the configured aptitude bonus expanding the area for stronger aptitudes.
        return CommonConfig.TRIBULATION_BASE_RADIUS.get()
                + CommonConfig.TRIBULATION_APTITUDE_RADIUS_BONUS.get() * Mth.clamp(aptitudeScore, 0.0F, 1.0F);
    }

    public static float getStrikeDamage(
            TribulationType type,
            int strikeIndex,
            float aptitudeScore
    ) {
        double increasingMultiplier = 1.0D
                + Math.max(0, strikeIndex - 1) * CommonConfig.TRIBULATION_STRIKE_GROWTH.get();
        double aptitudeMultiplier = CommonConfig.TRIBULATION_MINIMUM_DAMAGE_MULTIPLIER.get()
                + Mth.clamp(aptitudeScore, 0.0F, 1.0F)
                * CommonConfig.TRIBULATION_APTITUDE_DAMAGE_BONUS.get();
        double damage = CommonConfig.TRIBULATION_BASE_DAMAGE.get()
                * type.strengthMultiplier()
                * increasingMultiplier
                * aptitudeMultiplier;
        return (float) Mth.clamp(damage, 0.0D, Float.MAX_VALUE);
    }

    public static float getVisualIntensity(
            TribulationType type,
            int strikeIndex,
            float aptitudeScore
    ) {
        return (float) Mth.clamp(
                0.8D
                        + type.strengthMultiplier() * 0.35D
                        + strikeIndex * 0.012D
                        + aptitudeScore * 0.45D,
                0.8D,
                4.0D
        );
    }

    public static void noHurtDelay(LivingEntity livingEntity) {
        livingEntity.hurtTime = 10;
        livingEntity.invulnerableTime = 0;
        livingEntity.invulnerableDuration = 0;
    }
}
