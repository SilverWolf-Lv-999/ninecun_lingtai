package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;

import java.util.Optional;

/** 掌中佛国的境界影响判定，集中维护法术的特殊越阶规则。 */
public final class PalmBuddhaCultivationUtility {
    private PalmBuddhaCultivationUtility() {
    }

    public static boolean canAffect(ServerPlayer caster, ServerPlayer target) {
        Optional<CultivationStage> casterStage = CultivationApi.getStage(caster);
        Optional<CultivationStage> targetStage = CultivationApi.getStage(target);
        if (casterStage.isEmpty()) {
            return false;
        }
        if (targetStage.isEmpty()) {
            return true;
        }

        CultivationRealm maximumTargetRealm = casterStage.get().realm() == CultivationRealm.QI_REFINING
                ? CultivationRealm.FOUNDATION_ESTABLISHMENT
                : casterStage.get().realm();
        return targetStage.get().realm().ordinal() <= maximumTargetRealm.ordinal();
    }

    public static String getRestrictionMessage(ServerPlayer caster, ServerPlayer target) {
        Optional<CultivationStage> casterStage = CultivationApi.getStage(caster);
        Optional<CultivationStage> targetStage = CultivationApi.getStage(target);
        if (casterStage.isEmpty()) {
            return "凡人之躯，无法张开佛国";
        }
        if (targetStage.isEmpty()) {
            return "目标境界不可判定";
        }
        return casterStage.get().realm() == CultivationRealm.QI_REFINING
                ? "练气修士最多摄取筑基目标"
                : "目标境界高于自身，掌中佛国无法容纳";
    }
}
