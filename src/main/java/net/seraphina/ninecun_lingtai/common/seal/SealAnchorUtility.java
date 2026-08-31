package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * 神念锚位口径（阵禁篇第三节）：总数 = 魂强星数 × 2；器禁每层占 1。
 * 魂伤 ≥ 震荡档时可用锚位 = 总数 × (1 − 魂伤/200)，超额刻印从最新往旧休眠。
 */
public final class SealAnchorUtility {

    private SealAnchorUtility() {
    }

    /** 锚位总数 = 魂强星数 × anchorsPerSoulStar（默认 2）。无魂强（凡人）为 0。 */
    public static int getTotalAnchors(Player player) {
        return PlayerAptitudeData.getSoulStrength(player)
                .map(soulStrength ->
                        soulStrength.illuminatedStars() * CommonConfig.SEAL_ANCHORS_PER_SOUL_STAR.get())
                .orElse(0);
    }

    /** 当前可用锚位（含魂伤休眠折减，向下取整）。 */
    public static int getAvailableAnchors(Player player) {
        int total = getTotalAnchors(player);
        return (int) Math.floor(total * SoulInjuryUtility.getAnchorAvailabilityRatio(player));
    }

    /**
     * 已占用锚位 = 器禁存续层数 + 点睛（地禁）占用（SavedData 权威，离线也可查）。
     * 点睛账本见 {@code FormationAnchorLedger}（M4：器禁与阵法抢同一份神念）。
     */
    public static int getUsedAnchors(MinecraftServer server, UUID ownerUuid) {
        return SealRecordSavedData.get(server).getUsedAnchors(ownerUuid)
                + net.seraphina.ninecun_lingtai.common.formation.FormationAnchorLedger
                        .getUsedAnchors(server, ownerUuid);
    }

    /** 单件物品叠层上限 = ⌈魂强星数 / 2⌉。 */
    public static int getLayerCap(Player player) {
        return PlayerAptitudeData.getSoulStrength(player)
                .map(soulStrength -> (soulStrength.illuminatedStars() + 1) / 2)
                .orElse(0);
    }

    /**
     * 某层是否因魂伤休眠：主人在线时按刻印序从新到旧休眠超出可用锚位的层；
     * 主人离线读不到魂伤数据，视为全部生效（文档口径）。
     */
    public static boolean isLayerDormant(MinecraftServer server, UUID ownerUuid, SealLayer layer) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return false;
        }
        // 点睛锚位优先保住（阵是长期基建），器禁层在剩余额度里从新到旧休眠
        int available = getAvailableAnchors(owner)
                - net.seraphina.ninecun_lingtai.common.formation.FormationAnchorLedger
                        .getUsedAnchors(server, ownerUuid);
        List<SealLayer> layersByAge = SealRecordSavedData.get(server).getLayersByAge(ownerUuid);
        if (layersByAge.size() <= available) {
            return false;
        }
        int index = layersByAge.indexOf(layer);
        return index >= available;
    }

    /** 主人在线时的当前休眠层数（钝痛提示用）。 */
    public static int getDormantCount(ServerPlayer owner) {
        int available = getAvailableAnchors(owner);
        int used = getUsedAnchors(owner.server, owner.getUUID());
        return Math.max(0, used - available);
    }
}
