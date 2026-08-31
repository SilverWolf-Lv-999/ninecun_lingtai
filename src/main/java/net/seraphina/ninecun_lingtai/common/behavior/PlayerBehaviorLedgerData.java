package net.seraphina.ninecun_lingtai.common.behavior;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.data.migration.DataMigrationRegistry;
import net.seraphina.ninecun_lingtai.common.data.migration.DataMigrator;
import net.seraphina.ninecun_lingtai.common.data.migration.SchemaVersions;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化行为账本（心魔即己 T15）：事件驱动的杀孽/夺舍/失败/服丹/施法计数，
 * 是心魔镜像快照（{@code MindDemonSnapshot}）的唯一数值输入。
 *
 * <p>持久化：{@code player.getPersistentData()}，根键 {@value #ROOT_KEY}，写法与
 * {@code PlayerScarData} 一致，读路径接 schema 迁移框架（域
 * {@value #DOMAIN}，当前版本 {@value #CURRENT_VERSION}）。脱敏约定：只存计数与
 * spell id，不存坐标轨迹（坐标类记录留在因果账本）。</p>
 *
 * <p>快照锁定：天劫 {@code TribulationManager.request} 成功那一刻调用
 * {@link #lockSnapshot}，心魔开战读锁定值——天劫过程中临时换套路无效。</p>
 *
 * <p>跨死亡保留需在 clone 事件调用 {@link #copy}（接线点：
 * {@code PlayerAptitudeEventHandler.onPlayerClone}，见开发日志"需集中接线"）。</p>
 */
public final class PlayerBehaviorLedgerData {
    public static final String ROOT_KEY = ModMain.MOD_ID + ":behavior_ledger";
    /** schema 迁移域名；常量建议后续并入 SchemaVersions（该文件归属其它系统，此处不改）。 */
    public static final String DOMAIN = "player_behavior_ledger";
    public static final int CURRENT_VERSION = 1;

    private static final String KILLS_TAG = "Kills";
    private static final String POSSESS_COUNT_TAG = "PossessCount";
    private static final String POSSESSED_BY_COUNT_TAG = "PossessedByCount";
    private static final String BREAKTHROUGH_FAILS_TAG = "BreakthroughFails";
    private static final String TRIBULATION_FAILS_TAG = "TribulationFails";
    private static final String LAST_FAIL_CAUSE_TAG = "LastFailCause";
    private static final String PILL_USES_TAG = "PillUses";
    private static final String PILL_TOTAL_TAG = "Total";
    private static final String PILL_SOUL_TAG = "SoulPill";
    private static final String PILL_TOXIN_BURSTS_TAG = "ToxinBursts";
    private static final String SPELL_WINDOW_TAG = "SpellWindow";
    private static final String SCARS_ACTIVE_TAG = "ScarsActive";
    private static final String SCARS_RESOLVED_TAG = "ScarsResolved";
    private static final String LOCKED_SNAPSHOT_TAG = "LedgerLockedSnapshot";
    private static final String KILL_WEIGHT_REMAINDER_TAG = "KillWeightRemainder";
    /** 杀孽已消解量（道心杀孽 §3）：原始击杀账目永不篡改，消解记另一笔账，随 copy 整体复制。 */
    private static final String KARMA_REDEEMED_TAG = "KarmaRedeemed";
    /** 最近一次击杀发生的修真历日（嗜杀心障"7 日零击杀"化解判定用，-1 表示无记录）。 */
    private static final String LAST_KILL_CALENDAR_DAY_TAG = "LastKillCalendarDay";

    /** 药执心障：丹药效果乘区（PillItem.consumePill 单点读取）。 */
    private static final double PILL_BOND_EFFECT_MULTIPLIER = 0.7D;
    /** 遁逃之执心障：位移法术耗灵乘区（施法/持续耗灵点单点读取）。 */
    private static final double FLIGHT_GRIP_COST_MULTIPLIER = 1.3D;

    /** R6：账本整体 NBT 超过该字节数时 warn 一次（每次写入都检查）。 */
    private static final long SIZE_WARN_BYTES = 8 * 1024L;

    static {
        DataMigrationRegistry.register(new InitialMigration());
    }

    /** 击杀分类，字段名即 NBT 键（设计文档 §5.1 kills 结构）。 */
    public enum KillCategory {
        VILLAGER("villager"),
        CULTIVATOR("cultivator"),
        ALLY("ally"),
        BEAST("beast");

        private final String serializedName;

        KillCategory(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return this.serializedName;
        }
    }

    private PlayerBehaviorLedgerData() {
    }

    // ------------------------------------------------------------------ 采样写入

    /**
     * 记录一次击杀。带 {@code mind_scar_bloodthirst} 心障时按 ×1.5 增速累计
     * （小数部分存进 {@value #KILL_WEIGHT_REMAINDER_TAG} 累加器，攒满 1 再进位）。
     */
    public static void recordKill(ServerPlayer player, KillCategory category) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag kills = root.getCompound(KILLS_TAG);
        double weight = 1.0D;
        if (hasActiveMindScar(player, MindScarIds.BLOODTHIRST)) {
            weight = 1.5D;
        }
        double remainder = root.getDouble(KILL_WEIGHT_REMAINDER_TAG) + weight;
        int increments = (int) Math.floor(remainder);
        root.putDouble(KILL_WEIGHT_REMAINDER_TAG, remainder - increments);
        kills.putInt(
                category.getSerializedName(),
                kills.getInt(category.getSerializedName()) + increments
        );
        root.put(KILLS_TAG, kills);
        // 嗜杀心障化解窗口：任何击杀都重置"零击杀"计日基线。
        root.putLong(
                LAST_KILL_CALENDAR_DAY_TAG,
                MonotonicCalendarUtility.getCalendarDay(player.server)
        );
        save(player, root);
    }

    /** 最近一次击杀的修真历日；无记录返回 -1。 */
    public static long getLastKillCalendarDay(Player player) {
        CompoundTag root = getRoot(player);
        return root.contains(LAST_KILL_CALENDAR_DAY_TAG, Tag.TAG_LONG)
                ? root.getLong(LAST_KILL_CALENDAR_DAY_TAG)
                : -1L;
    }

    /** 重置"零击杀"计日基线（嗜杀心障获得时调用，从当日起算 7 日窗口）。 */
    public static void setLastKillCalendarDay(ServerPlayer player, long calendarDay) {
        CompoundTag root = getOrCreateRoot(player);
        root.putLong(LAST_KILL_CALENDAR_DAY_TAG, calendarDay);
        save(player, root);
    }

    public static void recordPossess(ServerPlayer player) {
        increment(player, POSSESS_COUNT_TAG);
    }

    public static void recordPossessedBy(ServerPlayer player) {
        increment(player, POSSESSED_BY_COUNT_TAG);
    }

    public static void recordBreakthroughFail(ServerPlayer player, String cause) {
        CompoundTag root = getOrCreateRoot(player);
        root.putInt(BREAKTHROUGH_FAILS_TAG, root.getInt(BREAKTHROUGH_FAILS_TAG) + 1);
        root.putString(LAST_FAIL_CAUSE_TAG, cause == null ? "" : cause);
        save(player, root);
    }

    public static void recordTribulationFail(ServerPlayer player, String cause) {
        CompoundTag root = getOrCreateRoot(player);
        root.putInt(TRIBULATION_FAILS_TAG, root.getInt(TRIBULATION_FAILS_TAG) + 1);
        root.putString(LAST_FAIL_CAUSE_TAG, cause == null ? "" : cause);
        save(player, root);
    }

    public static void recordPillUse(ServerPlayer player, boolean soulPill, boolean toxinBurst) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag pillUses = root.getCompound(PILL_USES_TAG);
        pillUses.putInt(PILL_TOTAL_TAG, pillUses.getInt(PILL_TOTAL_TAG) + 1);
        if (soulPill) {
            pillUses.putInt(PILL_SOUL_TAG, pillUses.getInt(PILL_SOUL_TAG) + 1);
        }
        if (toxinBurst) {
            pillUses.putInt(PILL_TOXIN_BURSTS_TAG, pillUses.getInt(PILL_TOXIN_BURSTS_TAG) + 1);
        }
        root.put(PILL_USES_TAG, pillUses);
        save(player, root);
    }

    public static void recordSpellCast(ServerPlayer player, ResourceLocation spellId) {
        CompoundTag root = getOrCreateRoot(player);
        SpellUsageWindow window = SpellUsageWindow.fromNbt(
                root.getList(SPELL_WINDOW_TAG, Tag.TAG_COMPOUND)
        );
        window.record(player.level().getGameTime(), spellId);
        root.put(SPELL_WINDOW_TAG, window.save());
        save(player, root);
    }

    // ------------------------------------------------------------------ 读取

    public static int getKills(Player player, KillCategory category) {
        return killsFrom(getRoot(player), category);
    }

    public static int getTotalKills(Player player) {
        return totalKillsFrom(getRoot(player));
    }

    public static int getPossessCount(Player player) {
        return getRoot(player).getInt(POSSESS_COUNT_TAG);
    }

    public static int getTribulationFails(Player player) {
        return getRoot(player).getInt(TRIBULATION_FAILS_TAG);
    }

    public static int getBreakthroughFails(Player player) {
        return getRoot(player).getInt(BREAKTHROUGH_FAILS_TAG);
    }

    public static SpellUsageWindow getSpellWindow(Player player) {
        return spellWindowFrom(getRoot(player));
    }

    // ------------------------------------------------------------------ 杀孽消解账（道心杀孽）

    /** 杀孽已消解量（≥ 0）。有效杀孽的换算口径见 {@code common/karma/KarmaUtility}。 */
    public static double getKarmaRedeemed(Player player) {
        return Math.max(0.0D, getRoot(player).getDouble(KARMA_REDEEMED_TAG));
    }

    /**
     * 覆写杀孽已消解量（clamp 逻辑由 {@code KarmaUtility.redeem} 负责，
     * 本方法只做非负收拢与落盘）。
     */
    public static void setKarmaRedeemed(ServerPlayer player, double redeemed) {
        CompoundTag root = getOrCreateRoot(player);
        root.putDouble(KARMA_REDEEMED_TAG, Math.max(0.0D, redeemed));
        save(player, root);
    }

    // -------- 同名静态读取（针对任意账本根 tag，供镜像快照读"锁定值"时复用） --------

    public static int killsFrom(CompoundTag root, KillCategory category) {
        return root.getCompound(KILLS_TAG).getInt(category.getSerializedName());
    }

    public static int totalKillsFrom(CompoundTag root) {
        int total = 0;
        for (KillCategory category : KillCategory.values()) {
            total += killsFrom(root, category);
        }
        return total;
    }

    public static int possessCountFrom(CompoundTag root) {
        return root.getInt(POSSESS_COUNT_TAG);
    }

    public static int tribulationFailsFrom(CompoundTag root) {
        return root.getInt(TRIBULATION_FAILS_TAG);
    }

    public static int breakthroughFailsFrom(CompoundTag root) {
        return root.getInt(BREAKTHROUGH_FAILS_TAG);
    }

    public static String lastFailCauseFrom(CompoundTag root) {
        return root.getString(LAST_FAIL_CAUSE_TAG);
    }

    public static int pillTotalFrom(CompoundTag root) {
        return root.getCompound(PILL_USES_TAG).getInt(PILL_TOTAL_TAG);
    }

    public static int soulPillUsesFrom(CompoundTag root) {
        return root.getCompound(PILL_USES_TAG).getInt(PILL_SOUL_TAG);
    }

    public static int toxinBurstsFrom(CompoundTag root) {
        return root.getCompound(PILL_USES_TAG).getInt(PILL_TOXIN_BURSTS_TAG);
    }

    public static SpellUsageWindow spellWindowFrom(CompoundTag root) {
        return SpellUsageWindow.fromNbt(root.getList(SPELL_WINDOW_TAG, Tag.TAG_COMPOUND));
    }

    public static List<String> activeScarIdsFrom(CompoundTag root) {
        return readStringList(root, SCARS_ACTIVE_TAG);
    }

    // ------------------------------------------------------------------ 快照锁定

    /** 深拷贝当前账本进锁定槽（天劫开始时调用，防临时改套路）。 */
    public static void lockSnapshot(ServerPlayer player) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag snapshot = root.copy();
        snapshot.remove(LOCKED_SNAPSHOT_TAG);
        root.put(LOCKED_SNAPSHOT_TAG, snapshot);
        save(player, root);
    }

    public static void clearLockedSnapshot(ServerPlayer player) {
        CompoundTag root = getOrCreateRoot(player);
        if (root.contains(LOCKED_SNAPSHOT_TAG)) {
            root.remove(LOCKED_SNAPSHOT_TAG);
            save(player, root);
        }
    }

    /** 锁定快照（无锁定时回落为当前账本的副本），供心魔镜像生成读取。 */
    public static CompoundTag getLockedSnapshotOrLive(Player player) {
        CompoundTag root = getRoot(player);
        if (root.contains(LOCKED_SNAPSHOT_TAG, Tag.TAG_COMPOUND)) {
            return root.getCompound(LOCKED_SNAPSHOT_TAG).copy();
        }
        CompoundTag live = root.copy();
        live.remove(LOCKED_SNAPSHOT_TAG);
        return live;
    }

    // ------------------------------------------------------------------ 心障镜像

    /**
     * 记录一条命名心障（如 {@code mind_scar_thunder_fear}）。Scar 框架的
     * {@code ScarType.HEART_DEMON} 同类型至多一条，具体心障 id 由账本这份镜像区分。
     */
    public static void addActiveScar(ServerPlayer player, String scarId) {
        CompoundTag root = getOrCreateRoot(player);
        List<String> active = readStringList(root, SCARS_ACTIVE_TAG);
        if (!active.contains(scarId)) {
            active.add(scarId);
            writeStringList(root, SCARS_ACTIVE_TAG, active);
            save(player, root);
        }
    }

    /** 心障化解：从 active 移到 resolved。 */
    public static boolean resolveActiveScar(ServerPlayer player, String scarId) {
        CompoundTag root = getOrCreateRoot(player);
        List<String> active = readStringList(root, SCARS_ACTIVE_TAG);
        if (!active.remove(scarId)) {
            return false;
        }
        List<String> resolved = readStringList(root, SCARS_RESOLVED_TAG);
        if (!resolved.contains(scarId)) {
            resolved.add(scarId);
        }
        writeStringList(root, SCARS_ACTIVE_TAG, active);
        writeStringList(root, SCARS_RESOLVED_TAG, resolved);
        save(player, root);
        return true;
    }

    public static List<String> getActiveScarIds(Player player) {
        return activeScarIdsFrom(getRoot(player));
    }

    public static boolean hasActiveMindScar(Player player, String scarId) {
        return getActiveScarIds(player).contains(scarId);
    }

    /** 是否还有任何活跃的命名心障（全部化解后才移除 HEART_DEMON Scar）。 */
    public static boolean hasAnyActiveMindScar(Player player) {
        return getActiveScarIds(player).stream()
                .anyMatch(scarId -> scarId.startsWith("mind_scar_"));
    }

    // ------------------------------------------------------------------ 心障效果单点查询门面

    /** 药执心障效果：丹药效果乘 0.7，未带心障时 1.0（PillItem.consumePill 读取）。 */
    public static double getPillEffectMultiplier(Player player) {
        return hasActiveMindScar(player, MindScarIds.PILL_BOND)
                ? PILL_BOND_EFFECT_MULTIPLIER
                : 1.0D;
    }

    /** 遁逃之执心障效果：位移法术耗灵乘 1.3，未带心障时 1.0（各耗灵点读取）。 */
    public static double getMobilityCostMultiplier(Player player) {
        return hasActiveMindScar(player, MindScarIds.FLIGHT_GRIP)
                ? FLIGHT_GRIP_COST_MULTIPLIER
                : 1.0D;
    }

    // ------------------------------------------------------------------ 生命周期

    /** 跨死亡/跨维度克隆整体复制，接线点：PlayerEvent.Clone。 */
    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    ROOT_KEY,
                    sourceData.getCompound(ROOT_KEY).copy()
            );
        }
    }

    /** debug 用：单行摘要。 */
    public static String describe(Player player) {
        CompoundTag root = getRoot(player);
        return String.format(
                "kills[v=%d,c=%d,a=%d,b=%d] possess=%d fails[bt=%d,tr=%d] pills[t=%d,s=%d,x=%d] spells=%d scars=%s locked=%s",
                killsFrom(root, KillCategory.VILLAGER),
                killsFrom(root, KillCategory.CULTIVATOR),
                killsFrom(root, KillCategory.ALLY),
                killsFrom(root, KillCategory.BEAST),
                possessCountFrom(root),
                breakthroughFailsFrom(root),
                tribulationFailsFrom(root),
                pillTotalFrom(root),
                soulPillUsesFrom(root),
                toxinBurstsFrom(root),
                spellWindowFrom(root).totalUses(),
                activeScarIdsFrom(root),
                root.contains(LOCKED_SNAPSHOT_TAG)
        );
    }

    // ------------------------------------------------------------------ 内部

    private static void increment(ServerPlayer player, String key) {
        CompoundTag root = getOrCreateRoot(player);
        root.putInt(key, root.getInt(key) + 1);
        save(player, root);
    }

    private static CompoundTag getRoot(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        CompoundTag root = persistentData.getCompound(ROOT_KEY);
        DataMigrationRegistry.migrateToVersion(DOMAIN, root, CURRENT_VERSION);
        return root;
    }

    private static CompoundTag getOrCreateRoot(ServerPlayer player) {
        return getRoot(player);
    }

    private static void save(ServerPlayer player, CompoundTag root) {
        SchemaVersions.setVersion(root, CURRENT_VERSION);
        player.getPersistentData().put(ROOT_KEY, root);
        warnIfOversized(player, root);
    }

    private static void warnIfOversized(ServerPlayer player, CompoundTag root) {
        try {
            long size = root.sizeInBytes();
            if (size > SIZE_WARN_BYTES) {
                ModMain.LOGGER.warn(
                        "[behavior_ledger] ledger of {} is {} bytes (> {} bytes), consider trimming",
                        player.getName().getString(),
                        size,
                        SIZE_WARN_BYTES
                );
            }
        } catch (RuntimeException ignored) {
            // sizeInBytes 只是诊断，估算失败不影响玩法。
        }
    }

    private static List<String> readStringList(CompoundTag root, String key) {
        ListTag listTag = root.getList(key, Tag.TAG_STRING);
        List<String> values = new ArrayList<>(listTag.size());
        for (int index = 0; index < listTag.size(); index++) {
            values.add(listTag.getString(index));
        }
        return values;
    }

    private static void writeStringList(CompoundTag root, String key, List<String> values) {
        ListTag listTag = new ListTag();
        values.forEach(value -> listTag.add(StringTag.valueOf(value)));
        root.put(key, listTag);
    }

    /** 心障 id 常量（P8 落地，此处仅供采样增速判断引用）。 */
    public static final class MindScarIds {
        public static final String THUNDER_FEAR = "mind_scar_thunder_fear";
        public static final String BLOODTHIRST = "mind_scar_bloodthirst";
        public static final String PILL_BOND = "mind_scar_pill_bond";
        public static final String FLIGHT_GRIP = "mind_scar_flight_grip";

        private MindScarIds() {
        }
    }

    /**
     * 版本 0（无标记的历史数据）→ 1：补齐空 Kills/PillUses 容器，保证读取方
     * 不需要判空。与 {@code PlayerScarSchemaMigration} 同款的初始迁移器。
     */
    private static final class InitialMigration implements DataMigrator {
        @Override
        public String domain() {
            return DOMAIN;
        }

        @Override
        public int fromVersion() {
            return 0;
        }

        @Override
        public int toVersion() {
            return 1;
        }

        @Override
        public void migrate(CompoundTag data) {
            if (!data.contains("Kills", Tag.TAG_COMPOUND)) {
                data.put("Kills", new CompoundTag());
            }
            if (!data.contains("PillUses", Tag.TAG_COMPOUND)) {
                data.put("PillUses", new CompoundTag());
            }
        }
    }
}
