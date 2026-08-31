package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.common.seal.SealAnchorUtility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 点睛（地禁）的锚位账本口径（阵禁篇 3.1：1-3 阶占 2、4-6 阶占 3、7 阶以上占 4）。
 * 与器禁共享同一份神念锚位池：{@code SealAnchorUtility.getUsedAnchors} 已并入本账本。
 */
public final class FormationAnchorLedger {

    private FormationAnchorLedger() {
    }

    /** 某品阶点睛占用的锚位数。 */
    public static int getEnlightenAnchorCost(int grade) {
        if (grade >= 7) {
            return FormationTuning.ENLIGHTEN_ANCHORS_GRADE_7_PLUS;
        }
        if (grade >= 4) {
            return FormationTuning.ENLIGHTEN_ANCHORS_GRADE_4_6;
        }
        return FormationTuning.ENLIGHTEN_ANCHORS_GRADE_1_3;
    }

    /** 该玩家全部维度已点睛阵法占用的锚位总数（SavedData 权威，离线也可查）。 */
    public static int getUsedAnchors(MinecraftServer server, UUID ownerUuid) {
        int used = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
                if (instance.isEnlightened() && instance.ownerUuid().equals(ownerUuid)) {
                    FormationDefinition definition =
                            FormationManager.INSTANCE.getDefinition(instance.formationId());
                    used += getEnlightenAnchorCost(definition == null ? 1 : definition.grade());
                }
            }
        }
        return used;
    }

    /**
     * 灵阵规则层是否生效：阵主在线且魂伤休眠挤占到点睛锚位时，灵阵退化回明阵
     * （阵禁篇 6.5"重伤不下火线的阵主，他的洞府正在裸奔"）。
     * 休眠次序口径：点睛锚位先于器禁层被挤占前提之外——此处只比较
     * "点睛自身占用 > 可用锚位"的硬超额；主人离线视为全部生效（与器禁一致）。
     */
    public static boolean isEnlightenmentActive(MinecraftServer server, FormationInstance instance) {
        if (!instance.isEnlightened()) {
            return false;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(instance.ownerUuid());
        if (owner == null) {
            return true;
        }
        return getUsedAnchors(server, instance.ownerUuid())
                <= SealAnchorUtility.getAvailableAnchors(owner);
    }
}
