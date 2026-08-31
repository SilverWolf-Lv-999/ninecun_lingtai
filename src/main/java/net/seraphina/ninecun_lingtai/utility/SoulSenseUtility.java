package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.insight.InsightEffectUtility;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SoulSenseUtility {

    public static boolean canUseSoulSense(Player player) {
        return PlayerAptitudeData.getSoulStrength(player).isPresent()
                && PlayerCultivationData.getStage(player).isPresent();
    }

    public static double getRange(Player player) {
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(0);
        if (stars <= 0) {
            return 0.0D;
        }
        int realmIndex = PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal())
                .orElse(0);
        double base = CommonConfig.SOUL_SENSE_BASE_RANGE.get()
                + CommonConfig.SOUL_SENSE_PER_STAR_RANGE.get() * (stars - 1);
        double realmMultiplier = 1.0D
                + CommonConfig.SOUL_SENSE_REALM_RANGE_BONUS.get() * realmIndex;
        // 魂伤乘区：DAZED -30%、SHAKEN/PERIL 减半；
        // 悟道印记"神识浩渺"乘区（经 InsightEffectUtility 单点读取，R9）。
        double insightMultiplier = 1.0D + InsightEffectUtility.query(
                player,
                InsightEffectUtility.EFFECT_SOUL_SENSE_RANGE_BONUS
        );
        return Mth.clamp(
                base * realmMultiplier,
                1.0D,
                CommonConfig.SOUL_SENSE_MAX_RANGE.get()
        ) * SoulInjuryUtility.getSenseRangeMultiplier(player) * insightMultiplier;
    }

    public static double getBlockRange(Player player) {
        return Math.min(getRange(player), CommonConfig.SOUL_SENSE_BLOCK_RANGE.get());
    }

    public static double getEnergyCostPerSecond(Player player) {
        return CommonConfig.SOUL_SENSE_BASE_COST_PER_SECOND.get()
                + getRange(player) * CommonConfig.SOUL_SENSE_RANGE_COST_FACTOR.get();
    }
}
