package net.seraphina.ninecun_lingtai.common.aura;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域灵气业务门面（灵气生态施工图 M2）。
 *
 * <p>两个正交维度：
 * <ul>
 *   <li><b>存量</b>（枯竭模型）：满额约支撑单人满速修炼 3 修真日；自然回复每修真日 5%，
 *       以 {@link MonotonicCalendarUtility} 修真历做日界（懒结算，无 tick 轮询）；</li>
 *   <li><b>质量</b>（四档 {@link AuraQuality}）：按群系/高度/人口结构派生（总纲 7.4：
 *       人口结构为负源、荒野高山为正源），派生结果进内存缓存（60 秒 TTL），
 *       过度采集惩罚（降档）持久化在灵域记录里。</li>
 * </ul>
 *
 * <p>性能红线：质量查询走缓存不逐 tick 派生；存量扣减只发生在打坐吸灵 tick
 * （每 20 tick）；SavedData 只在真实写入时置脏。
 */
public final class SpiritualAuraManager {

    /**
     * 灵域边长（区块数）。<b>刻意不进配置</b>：几何口径烧进了 SavedData 的
     * cellKey，改动会让旧存档的灵域记录错位。其余数值全走 CommonConfig 的
     * aura 组，运行期惰性读取。
     */
    public static final int CELL_SIZE_CHUNKS = 4;

    /** 存量上限（单位 = 满速吸灵 tick 数；默认 3 修真日 × 1200 次/日）。 */
    public static double getReserveCapUnits() {
        return CommonConfig.AURA_RESERVE_CAP_UNITS.get();
    }

    /** 质量派生缓存：维度 → (灵域键 → 缓存条目)。纯内存，不落盘。 */
    private static final Map<ResourceKey<Level>, Map<Long, CachedQuality>> QUALITY_CACHE =
            new ConcurrentHashMap<>();

    private SpiritualAuraManager() {
    }

    // ==================== 键与几何 ====================

    /** 灵域键：4×4 区块聚合，((chunkX>>2)<<32) | ((chunkZ>>2)&0xFFFFFFFF)。 */
    public static long getCellKey(BlockPos position) {
        int cellX = SectionPos.blockToSectionCoord(position.getX()) >> 2;
        int cellZ = SectionPos.blockToSectionCoord(position.getZ()) >> 2;
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    /** 灵域中心柱坐标（Y 取 0，仅用于地表高度/群系/人口采样）。 */
    private static BlockPos getCellCenter(long cellKey) {
        int cellX = (int) (cellKey >> 32);
        int cellZ = (int) cellKey;
        return new BlockPos(cellX * CELL_SIZE_CHUNKS * 16 + 32, 0, cellZ * CELL_SIZE_CHUNKS * 16 + 32);
    }

    // ==================== 存量（枯竭模型） ====================

    /** 只读存量比例 0..1（含懒回复的推算，不创建记录、不写盘）。 */
    public static double getReserveRatio(ServerLevel level, BlockPos position) {
        SpiritualAuraSavedData.AuraCell cell = SpiritualAuraSavedData.get(level)
                .peekCell(getCellKey(position));
        if (cell == null) {
            return 1.0D;
        }
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        long elapsedDays = Math.max(0L, currentDay - cell.lastRegenDay);
        double reserveCap = getReserveCapUnits();
        double projected = Math.min(
                reserveCap,
                cell.reserveUnits + reserveCap * CommonConfig.AURA_REGEN_RATIO_PER_DAY.get() * elapsedDays
        );
        return projected / reserveCap;
    }

    /**
     * 打坐吸灵扣账（唯一扣灵点接线，每 20 tick 调一次）：
     * 先懒结算自然回复，再按存量比例得出吸收系数并扣减等值存量。
     *
     * @return 吸收系数（1.0 = 满速；0 = 此地已竭）与扣账后的存量比例
     */
    public static MeditationDrainResult drainMeditationTick(ServerLevel level, BlockPos position) {
        long cellKey = getCellKey(position);
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        SpiritualAuraSavedData savedData = SpiritualAuraSavedData.get(level);
        SpiritualAuraSavedData.AuraCell cell = savedData.getOrCreateCell(cellKey, currentDay);
        applyLazyRegen(cell, currentDay);

        double factor = calculateAbsorptionFactor(cell.reserveUnits);
        if (factor > 0.0D) {
            cell.reserveUnits = Math.max(0.0D, cell.reserveUnits - factor);
        }
        savedData.setDirty();
        double ratio = cell.reserveUnits / getReserveCapUnits();
        savedData.removeCellIfPristine(cellKey, currentDay);
        return new MeditationDrainResult(factor, ratio);
    }

    /** 回补存量（灵植采收等生态回流）。 */
    public static void addReserve(ServerLevel level, BlockPos position, double units) {
        if (units <= 0.0D) {
            return;
        }
        long cellKey = getCellKey(position);
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        SpiritualAuraSavedData savedData = SpiritualAuraSavedData.get(level);
        SpiritualAuraSavedData.AuraCell cell = savedData.peekCell(cellKey);
        if (cell == null) {
            return; // 无记录 = 已满额，回补无意义，保持稀疏。
        }
        applyLazyRegen(cell, currentDay);
        cell.reserveUnits = Math.min(getReserveCapUnits(), cell.reserveUnits + units);
        savedData.setDirty();
        savedData.removeCellIfPristine(cellKey, currentDay);
    }

    /** 存量比例 → 吸收系数：高于满速阈值不衰减，低于则线性衰减；不足竭尽阈值归零。 */
    private static double calculateAbsorptionFactor(double reserveUnits) {
        if (reserveUnits < CommonConfig.AURA_EXHAUSTED_RESERVE_UNITS.get()) {
            return 0.0D;
        }
        double ratio = reserveUnits / getReserveCapUnits();
        double fullSpeedRatio = CommonConfig.AURA_FULL_SPEED_RESERVE_RATIO.get();
        return ratio >= fullSpeedRatio ? 1.0D : ratio / fullSpeedRatio;
    }

    /** 懒结算自然回复与惩罚到期（跨修真历日才动，防 /time set 回拨由修真历保证）。 */
    private static void applyLazyRegen(SpiritualAuraSavedData.AuraCell cell, long currentDay) {
        long elapsedDays = Math.max(0L, currentDay - cell.lastRegenDay);
        if (elapsedDays > 0L) {
            double reserveCap = getReserveCapUnits();
            cell.reserveUnits = Math.min(
                    reserveCap,
                    cell.reserveUnits + reserveCap * CommonConfig.AURA_REGEN_RATIO_PER_DAY.get() * elapsedDays
            );
            cell.lastRegenDay = currentDay;
        }
        if (cell.penaltyTiers > 0 && currentDay >= cell.penaltyExpiryDay) {
            cell.penaltyTiers = 0;
        }
    }

    // ==================== 质量（四档） ====================

    /** 当前灵域灵气质量（派生档缓存读取，再扣过度采集惩罚档）。 */
    public static AuraQuality getQuality(ServerLevel level, BlockPos position) {
        long cellKey = getCellKey(position);
        AuraQuality base = getCachedBaseQuality(level, cellKey);
        SpiritualAuraSavedData.AuraCell cell = SpiritualAuraSavedData.get(level).peekCell(cellKey);
        if (cell == null || cell.penaltyTiers <= 0) {
            return base;
        }
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        if (currentDay >= cell.penaltyExpiryDay) {
            return base; // 已到期，等下次写路径清理，读路径不落盘。
        }
        return base.downgradeBy(cell.penaltyTiers);
    }

    /** 质量档 → 倍率（破境系数口径 0.70/1.00/1.15/1.30，供 combined 便捷方法共用）。 */
    public static double getQualityMultiplier(ServerLevel level, BlockPos position) {
        return getQuality(level, position).getBreakthroughCoefficient();
    }

    /** 带 60 秒 TTL 的派生档缓存（性能红线：不逐 tick 算派生）。 */
    private static AuraQuality getCachedBaseQuality(ServerLevel level, long cellKey) {
        Map<Long, CachedQuality> levelCache = QUALITY_CACHE.computeIfAbsent(
                level.dimension(),
                dimension -> new HashMap<>()
        );
        long gameTime = level.getGameTime();
        CachedQuality cached = levelCache.get(cellKey);
        if (cached != null && gameTime - cached.derivedGameTime < CommonConfig.AURA_QUALITY_CACHE_TTL_TICKS.get()) {
            return cached.quality;
        }
        AuraQuality derived = deriveBaseQuality(level, cellKey);
        levelCache.put(cellKey, new CachedQuality(derived, gameTime));
        return derived;
    }

    /**
     * 派生规则（总纲 7.4）：基准平和；山地群系 +1、高山地表 +1；
     * 人口结构（POI 床位数）轻度 −1、村庄级 −2；结果收拢到四档。
     */
    private static AuraQuality deriveBaseQuality(ServerLevel level, long cellKey) {
        BlockPos center = getCellCenter(cellKey);
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ());
        BlockPos samplePosition = new BlockPos(center.getX(), surfaceY, center.getZ());

        int score = 0;
        if (level.getBiome(samplePosition).is(BiomeTags.IS_MOUNTAIN)) {
            score++;
        }
        if (surfaceY >= CommonConfig.AURA_HIGHLAND_MIN_SURFACE_Y.get()) {
            score++;
        }
        long homeCount = level.getPoiManager().getInRange(
                poiType -> poiType.is(PoiTypes.HOME),
                samplePosition,
                CommonConfig.AURA_POPULATION_SCAN_RADIUS.get(),
                PoiManager.Occupancy.ANY
        ).count();
        if (homeCount >= CommonConfig.AURA_POPULATION_HEAVY_HOMES.get()) {
            score -= 2;
        } else if (homeCount >= CommonConfig.AURA_POPULATION_LIGHT_HOMES.get()) {
            score -= 1;
        }
        return AuraQuality.fromOrdinal(AuraQuality.BALANCED.ordinal() + score);
    }

    // ==================== 灵植生态 ====================

    /**
     * 灵植采收记账：小额回补存量（还灵）+ 当日采收计数；
     * 超过阈值触发过度采集惩罚（质量降档，持续若干修真日）。
     *
     * @return true = 本次触发（或已处于）过度采集降档
     */
    public static boolean recordPlantHarvest(ServerLevel level, BlockPos position) {
        long cellKey = getCellKey(position);
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        SpiritualAuraSavedData savedData = SpiritualAuraSavedData.get(level);
        SpiritualAuraSavedData.AuraCell cell = savedData.getOrCreateCell(cellKey, currentDay);
        applyLazyRegen(cell, currentDay);

        cell.reserveUnits = Math.min(
                getReserveCapUnits(),
                cell.reserveUnits + CommonConfig.AURA_HARVEST_RESERVE_REFUND_UNITS.get()
        );
        if (cell.harvestDay != currentDay) {
            cell.harvestDay = currentDay;
            cell.harvestCount = 0;
        }
        cell.harvestCount++;
        boolean overharvested = cell.harvestCount > CommonConfig.AURA_OVERHARVEST_DAILY_THRESHOLD.get();
        if (overharvested) {
            cell.penaltyTiers = Math.max(cell.penaltyTiers, CommonConfig.AURA_OVERHARVEST_PENALTY_TIERS.get());
            cell.penaltyExpiryDay = currentDay + CommonConfig.AURA_OVERHARVEST_PENALTY_DAYS.get();
        }
        savedData.setDirty();
        return overharvested || cell.penaltyTiers > 0;
    }

    // ==================== 生命周期 ====================

    /** 服务器停机清空派生缓存（防跨存档串档）。 */
    public static void clearQualityCache() {
        QUALITY_CACHE.clear();
    }

    /** debug 命令用：当前灵域记录条数。 */
    public static int getTrackedCellCount(ServerLevel level) {
        return SpiritualAuraSavedData.get(level).getCellCount();
    }

    /** 打坐扣账结果：吸收系数（0..1）与扣账后存量比例（0..1）。 */
    public record MeditationDrainResult(double absorptionFactor, double reserveRatio) {
    }

    /** 派生档缓存条目。 */
    private record CachedQuality(AuraQuality quality, long derivedGameTime) {
    }
}
