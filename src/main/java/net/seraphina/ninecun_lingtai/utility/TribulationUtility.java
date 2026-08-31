package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class TribulationUtility {
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

    public static TribulationType selectTypeForTarget(
            CultivationRealm targetRealm,
            SpiritualRoot spiritualRoot,
            SoulStrength soulStrength
    ) {
        if (targetRealm.ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()) {
            return TribulationType.NINE_NINE;
        }
        if (targetRealm == CultivationRealm.NASCENT_SOUL) {
            return TribulationType.SIX_NINE;
        }
        return selectType(spiritualRoot, soulStrength);
    }

    public static double getRealmStrengthMultiplier(CultivationRealm targetRealm) {
        int realmsBeyondSpiritTransformation = Math.max(
                0,
                targetRealm.ordinal() - CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()
        );
        return 1.0D
                + CommonConfig.TRIBULATION_REALM_STRENGTH_GROWTH.get() * realmsBeyondSpiritTransformation;
    }

    public static boolean requiresInnerDemonTrial(CultivationRealm targetRealm) {
        return targetRealm.ordinal() >= CultivationRealm.NASCENT_SOUL.ordinal();
    }

    public static double getStrikeRadius(float aptitudeScore) {
        return CommonConfig.TRIBULATION_BASE_RADIUS.get()
                + CommonConfig.TRIBULATION_APTITUDE_RADIUS_BONUS.get() * Mth.clamp(aptitudeScore, 0.0F, 1.0F);
    }

    public static float getStrikeDamage(
            TribulationType type,
            int strikeIndex,
            float aptitudeScore
    ) {
        return getStrikeDamage(type, strikeIndex, aptitudeScore, 1.0D);
    }

    public static float getStrikeDamage(
            TribulationType type,
            int strikeIndex,
            float aptitudeScore,
            double realmMultiplier
    ) {
        double increasingMultiplier = 1.0D
                + Math.max(0, strikeIndex - 1) * CommonConfig.TRIBULATION_STRIKE_GROWTH.get();
        double aptitudeMultiplier = CommonConfig.TRIBULATION_MINIMUM_DAMAGE_MULTIPLIER.get()
                + Mth.clamp(aptitudeScore, 0.0F, 1.0F)
                * CommonConfig.TRIBULATION_APTITUDE_DAMAGE_BONUS.get();
        double damage = CommonConfig.TRIBULATION_BASE_DAMAGE.get()
                * type.strengthMultiplier()
                * increasingMultiplier
                * aptitudeMultiplier
                * Math.max(0.0D, realmMultiplier);
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
        noHurtDelay(livingEntity, 0);
    }

    public static void noHurtDelay(LivingEntity livingEntity, int delay) {
        livingEntity.hurtTime = 10;
        livingEntity.invulnerableTime = delay;
        livingEntity.invulnerableDuration = delay;
        livingEntity.hurtDuration = delay;
    }
}
