package net.seraphina.ninecun_lingtai.common.aura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 区域灵气账本（每维度一份，灵气生态施工图 M2）。
 *
 * <p>4×4 区块（64×64 格）聚合为一"灵域"，键 = {@link SpiritualAuraManager#getCellKey}。
 * 稀疏存储：只记录被消耗/受罚过的灵域；未记录视为满额默认值，
 * 回满且无惩罚时立即删除记录（{@link #removeCellIfPristine}）。
 *
 * <p>置脏节流：写操作只发生在打坐吸灵 tick（每 20 tick 一次）与采收交互，
 * 不存在逐 tick 置脏；读操作（质量查询）不触碰本类。
 */
public final class SpiritualAuraSavedData extends SavedData {
    /** 存档版本：新增字段一律增量键 + 缺省值，旧档兼容。 */
    private static final int CURRENT_VERSION = 1;
    private static final String DATA_NAME = "ninecun_lingtai_spiritual_aura";
    private static final String VERSION_TAG = "Version";
    private static final String CELLS_TAG = "Cells";
    private static final String CELL_KEY_TAG = "Key";
    private static final String RESERVE_TAG = "Reserve";
    private static final String LAST_REGEN_DAY_TAG = "LastRegenDay";
    private static final String HARVEST_DAY_TAG = "HarvestDay";
    private static final String HARVEST_COUNT_TAG = "HarvestCount";
    private static final String PENALTY_TIERS_TAG = "PenaltyTiers";
    private static final String PENALTY_EXPIRY_DAY_TAG = "PenaltyExpiryDay";

    private final Map<Long, AuraCell> cells = new HashMap<>();

    /** 取本维度账本（懒创建，跟随维度存档持久化）。 */
    public static SpiritualAuraSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SpiritualAuraSavedData::load,
                SpiritualAuraSavedData::new,
                DATA_NAME
        );
    }

    public static SpiritualAuraSavedData load(CompoundTag tag) {
        SpiritualAuraSavedData savedData = new SpiritualAuraSavedData();
        ListTag cellTags = tag.getList(CELLS_TAG, Tag.TAG_COMPOUND);
        for (Tag cellTag : cellTags) {
            if (!(cellTag instanceof CompoundTag cellDataTag) || !cellDataTag.contains(CELL_KEY_TAG)) {
                continue;
            }
            AuraCell cell = new AuraCell();
            cell.reserveUnits = cellDataTag.getDouble(RESERVE_TAG);
            cell.lastRegenDay = cellDataTag.getLong(LAST_REGEN_DAY_TAG);
            cell.harvestDay = cellDataTag.getLong(HARVEST_DAY_TAG);
            cell.harvestCount = cellDataTag.getInt(HARVEST_COUNT_TAG);
            cell.penaltyTiers = cellDataTag.getInt(PENALTY_TIERS_TAG);
            cell.penaltyExpiryDay = cellDataTag.getLong(PENALTY_EXPIRY_DAY_TAG);
            savedData.cells.put(cellDataTag.getLong(CELL_KEY_TAG), cell);
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(VERSION_TAG, CURRENT_VERSION);
        ListTag cellTags = new ListTag();
        for (Map.Entry<Long, AuraCell> entry : this.cells.entrySet()) {
            AuraCell cell = entry.getValue();
            CompoundTag cellDataTag = new CompoundTag();
            cellDataTag.putLong(CELL_KEY_TAG, entry.getKey());
            cellDataTag.putDouble(RESERVE_TAG, cell.reserveUnits);
            cellDataTag.putLong(LAST_REGEN_DAY_TAG, cell.lastRegenDay);
            cellDataTag.putLong(HARVEST_DAY_TAG, cell.harvestDay);
            cellDataTag.putInt(HARVEST_COUNT_TAG, cell.harvestCount);
            cellDataTag.putInt(PENALTY_TIERS_TAG, cell.penaltyTiers);
            cellDataTag.putLong(PENALTY_EXPIRY_DAY_TAG, cell.penaltyExpiryDay);
            cellTags.add(cellDataTag);
        }
        tag.put(CELLS_TAG, cellTags);
        return tag;
    }

    /** 只读查询：无记录返回 null（视为满额默认值，不创建）。 */
    @Nullable
    public AuraCell peekCell(long cellKey) {
        return this.cells.get(cellKey);
    }

    /** 写路径查询：懒创建，初始满额（当前修真历日为回复基准日）。 */
    public AuraCell getOrCreateCell(long cellKey, long currentCalendarDay) {
        return this.cells.computeIfAbsent(cellKey, key -> {
            AuraCell cell = new AuraCell();
            cell.reserveUnits = SpiritualAuraManager.getReserveCapUnits();
            cell.lastRegenDay = currentCalendarDay;
            return cell;
        });
    }

    /** 稀疏存储收敛：回满且无惩罚、无当日采收计数的灵域删记录。 */
    public void removeCellIfPristine(long cellKey, long currentCalendarDay) {
        AuraCell cell = this.cells.get(cellKey);
        if (cell != null && cell.isPristine(currentCalendarDay)) {
            this.cells.remove(cellKey);
            setDirty();
        }
    }

    /** 当前记录条数（debug 命令展示用）。 */
    public int getCellCount() {
        return this.cells.size();
    }

    /**
     * 单个灵域记录。字段全部包内可见，读写统一经 {@link SpiritualAuraManager}
     * 收口（懒回复、置脏、删记录都在门面完成，避免散落）。
     */
    public static final class AuraCell {
        /** 当前存量（单位：满速吸灵 tick 数，上限 {@link SpiritualAuraManager#getReserveCapUnits()}）。 */
        double reserveUnits;
        /** 上次结算自然回复的修真历日（MonotonicCalendarUtility 口径，防 /time set 回拨）。 */
        long lastRegenDay;
        /** 采收计数所属修真历日（跨日清零）。 */
        long harvestDay;
        /** 当日灵植采收次数（过度采集判定）。 */
        int harvestCount;
        /** 过度采集质量降档数（0 = 无惩罚）。 */
        int penaltyTiers;
        /** 惩罚到期修真历日（到期自动清除）。 */
        long penaltyExpiryDay;

        AuraCell() {
        }

        public double getReserveUnits() {
            return this.reserveUnits;
        }

        public int getPenaltyTiers() {
            return this.penaltyTiers;
        }

        public int getHarvestCount() {
            return this.harvestCount;
        }

        /** debug 命令直接改存量（业务路径请走 SpiritualAuraManager）。 */
        public void setReserveUnits(double reserveUnits) {
            this.reserveUnits = Math.max(0.0D, Math.min(SpiritualAuraManager.getReserveCapUnits(), reserveUnits));
        }

        /** debug 命令直接施加/清除质量降档惩罚。 */
        public void setPenalty(int penaltyTiers, long penaltyExpiryDay) {
            this.penaltyTiers = Math.max(0, penaltyTiers);
            this.penaltyExpiryDay = penaltyExpiryDay;
        }

        /** 是否已无信息量（满额、无惩罚、无当日采收计数），可从稀疏表删除。 */
        boolean isPristine(long currentCalendarDay) {
            boolean harvestIdle = this.harvestDay != currentCalendarDay || this.harvestCount <= 0;
            return this.reserveUnits >= SpiritualAuraManager.getReserveCapUnits()
                    && this.penaltyTiers <= 0
                    && harvestIdle;
        }
    }
}
