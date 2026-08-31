package net.seraphina.ninecun_lingtai.common.status;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.data.migration.DataMigrationRegistry;
import net.seraphina.ninecun_lingtai.common.data.migration.PlayerScarSchemaMigration;
import net.seraphina.ninecun_lingtai.common.data.migration.SchemaVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 玩家伤痕列表的持久化持有者，写法与仓库其它 PlayerXxxData 一致：
 * 静态工具类 + {@code player.getPersistentData()}，根键为 {@code ninecun_lingtai:scars}。
 *
 * <p>根 tag 结构（schema 版本 {@link SchemaVersions#PLAYER_SCARS_CURRENT}）：</p>
 * <pre>
 * "ninecun_lingtai:scars": {
 *     "SchemaVersion": 1,
 *     "Scars": [ { "Type", "Severity", "ExpireGameTick", "ResolveHint" }, ... ]
 * }
 * </pre>
 *
 * <p>不变式：同一 {@link ScarType} 至多一条记录。{@link #addScar} 遇到同类型时按
 * "severity 取较大值、期限取较久者（无期限优先）"合并，保证重复施加不会无限叠满。</p>
 *
 * <p>本类不做玩法判断，供 {@link ScarManager} 与各系统读写；一般业务代码请优先走
 * {@link ScarManager}。跨死亡保留需要在玩家 Clone 事件里调用 {@link #copy}（接线点见开发日志）。</p>
 */
public final class PlayerScarData {
    private static final String SCARS_ROOT_KEY = ModMain.MOD_ID + ":scars";
    private static final String SCAR_LIST_KEY = "Scars";

    static {
        DataMigrationRegistry.register(new PlayerScarSchemaMigration());
    }

    /** 玩家当前全部伤痕（含已到期未清理的），返回不可变副本。 */
    public static List<Scar> getScars(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SCARS_ROOT_KEY, Tag.TAG_COMPOUND)) {
            return List.of();
        }
        CompoundTag scarsRoot = persistentData.getCompound(SCARS_ROOT_KEY);
        DataMigrationRegistry.migrateToVersion(
                SchemaVersions.PLAYER_SCARS_DOMAIN,
                scarsRoot,
                SchemaVersions.PLAYER_SCARS_CURRENT
        );
        ListTag scarTags = scarsRoot.getList(SCAR_LIST_KEY, Tag.TAG_COMPOUND);
        List<Scar> scars = new ArrayList<>(scarTags.size());
        for (int index = 0; index < scarTags.size(); index++) {
            Scar.fromNBT(scarTags.getCompound(index)).ifPresent(scars::add);
        }
        return List.copyOf(scars);
    }

    public static Optional<Scar> getScar(Player player, ScarType type) {
        return getScars(player).stream()
                .filter(scar -> scar.type() == type)
                .findFirst();
    }

    public static boolean hasScar(Player player, ScarType type) {
        return getScar(player, type).isPresent();
    }

    /**
     * 添加伤痕。已有同类型时合并：severity 取较大值，期限取较久者（任一方无期限则无期限），
     * 化解提示取新值。
     *
     * @return 实际存储的伤痕（合并后的结果）
     */
    public static Scar addScar(Player player, Scar scar) {
        Objects.requireNonNull(scar, "scar");
        List<Scar> scars = new ArrayList<>(getScars(player));
        Scar stored = scar;
        Optional<Scar> existing = scars.stream()
                .filter(candidate -> candidate.type() == scar.type())
                .findFirst();
        if (existing.isPresent()) {
            stored = new Scar(
                    scar.type(),
                    Math.max(existing.get().severity(), scar.severity()),
                    mergeExpiry(existing.get().expireGameTick(), scar.expireGameTick()),
                    scar.resolveHint()
            );
            scars.removeIf(candidate -> candidate.type() == scar.type());
        }
        scars.add(stored);
        saveScars(player, scars);
        return stored;
    }

    /** 无条件写入（替换同类型旧记录），供降级等需要精确覆盖的场景使用。 */
    public static void putScar(Player player, Scar scar) {
        Objects.requireNonNull(scar, "scar");
        List<Scar> scars = new ArrayList<>(getScars(player));
        scars.removeIf(candidate -> candidate.type() == scar.type());
        scars.add(scar);
        saveScars(player, scars);
    }

    /** @return 是否确实移除了一条记录 */
    public static boolean removeScar(Player player, ScarType type) {
        List<Scar> scars = new ArrayList<>(getScars(player));
        boolean removed = scars.removeIf(scar -> scar.type() == type);
        if (removed) {
            saveScars(player, scars);
        }
        return removed;
    }

    /**
     * 清理所有已到期的伤痕，每移除一条回调一次 {@code onExpired}（可为 null）。
     *
     * @return 本次被移除的伤痕列表（不可变），无到期项时为空列表
     */
    public static List<Scar> tickExpire(Player player, long gameTime, Consumer<Scar> onExpired) {
        List<Scar> scars = getScars(player);
        if (scars.isEmpty()) {
            return List.of();
        }
        List<Scar> expired = scars.stream()
                .filter(scar -> scar.isExpired(gameTime))
                .toList();
        if (expired.isEmpty()) {
            return List.of();
        }
        saveScars(player, scars.stream().filter(scar -> !scar.isExpired(gameTime)).toList());
        if (onExpired != null) {
            expired.forEach(onExpired);
        }
        return expired;
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(SCARS_ROOT_KEY);
    }

    /** 跨死亡/跨维度克隆时整体复制，接线点：PlayerEvent.Clone（见开发日志"需集中接线"）。 */
    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SCARS_ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SCARS_ROOT_KEY,
                    sourceData.getCompound(SCARS_ROOT_KEY).copy()
            );
        }
    }

    private static long mergeExpiry(long firstExpireGameTick, long secondExpireGameTick) {
        if (firstExpireGameTick == Scar.NO_EXPIRY || secondExpireGameTick == Scar.NO_EXPIRY) {
            return Scar.NO_EXPIRY;
        }
        return Math.max(firstExpireGameTick, secondExpireGameTick);
    }

    private static void saveScars(Player player, List<Scar> scars) {
        CompoundTag persistentData = player.getPersistentData();
        if (scars.isEmpty()) {
            persistentData.remove(SCARS_ROOT_KEY);
            return;
        }
        ListTag scarTags = new ListTag();
        scars.forEach(scar -> scarTags.add(scar.toNBT()));
        CompoundTag scarsRoot = new CompoundTag();
        SchemaVersions.setVersion(scarsRoot, SchemaVersions.PLAYER_SCARS_CURRENT);
        scarsRoot.put(SCAR_LIST_KEY, scarTags);
        persistentData.put(SCARS_ROOT_KEY, scarsRoot);
    }
}
