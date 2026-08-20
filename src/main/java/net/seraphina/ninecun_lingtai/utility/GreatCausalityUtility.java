package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;

public final class GreatCausalityUtility {
    public static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "great_causality"
    );

    private static final int ENTRY_HISTORY_LIMIT = 16;
    private static final int MINOR_MASTERY_HISTORY_LIMIT = 64;
    private static final int MAJOR_MASTERY_HISTORY_LIMIT = 256;
    private static final double PASSIVE_DETECTION_MULTIPLIER = 0.35D;
    private static final double MAJOR_DETECTION_MULTIPLIER = 0.15D;

    private GreatCausalityUtility() {
    }

    public static boolean hasTechnique(Player player) {
        return PlayerTechniqueData.hasTechnique(player, TECHNIQUE_ID);
    }

    public static TechniqueMasteryStage getMasteryStage(Player player) {
        if (!hasTechnique(player)) {
            return TechniqueMasteryStage.ENTRY;
        }
        return PlayerTechniqueData.getTechnique(player, TECHNIQUE_ID)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY);
    }

    public static boolean isPerfection(Player player) {
        return getMasteryStage(player) == TechniqueMasteryStage.PERFECTION;
    }

    public static int getMaximumVisibleRecords(Player observer) {
        TechniqueMasteryStage masteryStage = getMasteryStage(observer);
        if (masteryStage == TechniqueMasteryStage.PERFECTION) {
            return Integer.MAX_VALUE;
        }
        int baseLimit = switch (masteryStage) {
            case ENTRY -> ENTRY_HISTORY_LIMIT;
            case MINOR_MASTERY -> MINOR_MASTERY_HISTORY_LIMIT;
            case MAJOR_MASTERY -> MAJOR_MASTERY_HISTORY_LIMIT;
            case PERFECTION -> Integer.MAX_VALUE;
        };
        int realmBonus = CultivationApi.getStage(observer)
                .map(CultivationStage::realm)
                .map(CultivationRealm::ordinal)
                .orElse(0) * 16;
        return Math.min(Integer.MAX_VALUE, baseLimit + realmBonus);
    }

    public static double getDetectionMultiplier(Player observer) {
        if (!hasTechnique(observer)) {
            return 1.0D;
        }
        return switch (getMasteryStage(observer)) {
            case ENTRY -> PASSIVE_DETECTION_MULTIPLIER;
            case MINOR_MASTERY -> PASSIVE_DETECTION_MULTIPLIER;
            case MAJOR_MASTERY -> MAJOR_DETECTION_MULTIPLIER;
            case PERFECTION -> 0.0D;
        };
    }

    public static boolean isImmortal(Player player) {
        return CultivationApi.getStage(player)
                .map(stage -> stage.realm() == CultivationRealm.IMMORTAL)
                .orElse(false);
    }

    public static boolean canQuery(ServerPlayer observer) {
        return observer != null && hasTechnique(observer);
    }

    public static long getProficiency(Player player) {
        return PlayerTechniqueProficiencyData.get(player, TECHNIQUE_ID);
    }
}
