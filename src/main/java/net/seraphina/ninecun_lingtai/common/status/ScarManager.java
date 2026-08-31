package net.seraphina.ninecun_lingtai.common.status;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 伤痕系统对外静态门面。破境/天劫/死亡/走火入魔等系统统一通过本类给、查、移除伤痕。
 *
 * <p>约定：写操作要求 {@link ServerPlayer}（服务端权威），读操作接受任意 {@link Player}。
 * 本类只负责状态存取，不做玩法数值判断——例如"有瓶颈期则不可破境"应由破境系统自查：</p>
 * <pre>
 * if (ScarManager.hasScar(player, ScarType.BOTTLENECK)) { return; // 拒绝破境 }
 * double penalty = 0.05D * ScarManager.getSeverity(player, ScarType.DAO_BASE_INJURY);
 * </pre>
 */
public final class ScarManager {

    /** 按类型默认时长施加伤痕（无期限类型即永久），同类型已存在时按合并规则处理。 */
    public static Scar applyScar(ServerPlayer player, ScarType type, int severity) {
        Objects.requireNonNull(type, "type");
        return applyScar(player, type, severity, type.getDefaultDurationTicks());
    }

    /**
     * 按指定持续时长施加伤痕。
     *
     * @param durationTicks 持续 tick 数，小于 0 表示无期限
     * @return 实际存储的伤痕（与已有同类型伤痕合并后的结果）
     */
    public static Scar applyScar(ServerPlayer player, ScarType type, int severity, long durationTicks) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");
        long expireGameTick = durationTicks < 0L
                ? Scar.NO_EXPIRY
                : player.level().getGameTime() + durationTicks;
        return applyScar(player, new Scar(type, severity, expireGameTick));
    }

    /** 直接施加一条已构造好的伤痕记录。 */
    public static Scar applyScar(ServerPlayer player, Scar scar) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(scar, "scar");
        Scar stored = PlayerScarData.addScar(player, scar);
        ModMain.LOGGER.info(
                "[scar] applied {} (severity {}, expire {}) to {}",
                stored.type().getSerializedName(),
                stored.severity(),
                stored.isPermanent() ? "never" : stored.expireGameTick(),
                player.getName().getString()
        );
        return stored;
    }

    public static boolean hasScar(Player player, ScarType type) {
        return PlayerScarData.hasScar(player, type);
    }

    public static Optional<Scar> getScar(Player player, ScarType type) {
        return PlayerScarData.getScar(player, type);
    }

    /** 某类型伤痕的严重度，没有该伤痕时返回 0，方便直接参与数值计算。 */
    public static int getSeverity(Player player, ScarType type) {
        return PlayerScarData.getScar(player, type).map(Scar::severity).orElse(0);
    }

    public static List<Scar> getScars(Player player) {
        return PlayerScarData.getScars(player);
    }

    /**
     * 行为化解：仅当类型允许被行为化解（{@link ScarType#isResolvableByAction()}）时移除。
     * 供丹药、闭关、顿悟等玩法调用。
     *
     * @return 是否成功化解
     */
    public static boolean resolveScar(ServerPlayer player, ScarType type) {
        Objects.requireNonNull(type, "type");
        if (!type.isResolvableByAction()) {
            return false;
        }
        boolean resolved = PlayerScarData.removeScar(player, type);
        if (resolved) {
            ModMain.LOGGER.info(
                    "[scar] resolved {} on {}",
                    type.getSerializedName(),
                    player.getName().getString()
            );
        }
        return resolved;
    }

    /** 无条件移除（管理命令、特殊机缘等），不检查是否可行为化解。 */
    public static boolean removeScar(ServerPlayer player, ScarType type) {
        boolean removed = PlayerScarData.removeScar(player, type);
        if (removed) {
            ModMain.LOGGER.info(
                    "[scar] removed {} from {}",
                    type.getSerializedName(),
                    player.getName().getString()
            );
        }
        return removed;
    }

    /**
     * 降低某类型伤痕的严重度（渐进治疗），降到 0 及以下时整条移除。
     *
     * @return 是否发生了变化（不存在该伤痕时返回 false）
     */
    public static boolean reduceSeverity(ServerPlayer player, ScarType type, int amount) {
        if (amount <= 0) {
            return false;
        }
        Optional<Scar> existing = PlayerScarData.getScar(player, type);
        if (existing.isEmpty()) {
            return false;
        }
        int newSeverity = existing.get().severity() - amount;
        if (newSeverity < Scar.MIN_SEVERITY) {
            return removeScar(player, type);
        }
        PlayerScarData.putScar(player, existing.get().withSeverity(newSeverity));
        return true;
    }

    /**
     * 清理该玩家所有已到期的伤痕并返回被移除的列表。
     * 接线点：服务端玩家 tick（建议每 20 tick 调一次，见开发日志"需集中接线"）。
     */
    public static List<Scar> tickExpire(ServerPlayer player) {
        return PlayerScarData.tickExpire(
                player,
                player.level().getGameTime(),
                expired -> ModMain.LOGGER.debug(
                        "[scar] {} expired on {}",
                        expired.type().getSerializedName(),
                        player.getName().getString()
                )
        );
    }
}
