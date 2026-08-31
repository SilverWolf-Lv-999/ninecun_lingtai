package net.seraphina.ninecun_lingtai.common.tomb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 修士墓穴的网格判定与确定性参数（M4 墓穴切片）。
 *
 * <p>雕刻手法照搬灵界地下生成器（{@code SpiritualRealmUndergroundStructureGenerator}）的
 * "每区块独立、全局网格判定"思路，但哈希与盐值为本类自有副本，网格跑主世界，
 * 与灵界矿道网格互不相干（施工图风险 8）。</p>
 *
 * <p><b>密度重评（替代旧"每 128×128 一座"）：</b>768 格网格 × 60% 出率 × ±160 格抖动，
 * 折合平均约每 990×990 格一座。依据：①墓主是境界化 boss + 一次性战利品（含 6→7 星
 * 机缘之物），价值对标远征型内容，须显著稀于矿道/灵植（旧 128 格密度会让全服警告刷屏、
 * 机缘物泛滥摧毁第七星稀缺性）；②参照原版远古城市（仅深暗群系、约五六百格一座），
 * 墓穴全主世界地下均可出，故网格再放宽三成；③抖动打破旧方案肉眼可见的格点规律，
 * 出率掷点让"此地无墓"成为常态，探墓需要神识与运气。</p>
 */
public final class CultivatorTombUtility {
    /** 网格间距（48 区块）。建议配置键 tomb.gridSpacingBlocks。 */
    public static final int GRID_SPACING_BLOCKS = 768;
    /** 墓穴中心在格内的抖动半径。建议配置键 tomb.centerJitterBlocks。 */
    public static final int CENTER_JITTER_BLOCKS = 160;
    /** 每个网格实际出墓的概率。建议配置键 tomb.cellOccupancyChance。 */
    public static final double CELL_OCCUPANCY_CHANCE = 0.60D;

    private static final long OCCUPANCY_SALT = 0x7B3A11C0L;
    private static final long JITTER_X_SALT = 0x7B3A11C1L;
    private static final long JITTER_Z_SALT = 0x7B3A11C2L;
    private static final long TIER_SALT = 0x7B3A11C3L;
    private static final long ORIENTATION_SALT = 0x7B3A11C4L;
    private static final long LAYER_SALT = 0x7B3A11C5L;
    private static final long LOOT_SALT = 0x7B3A11C6L;
    private static final long DECOR_SALT = 0x7B3A11C7L;

    private static final long HASH_MULTIPLIER_X = 0x9E3779B97F4A7C15L;
    private static final long HASH_MULTIPLIER_Y = 0xC2B2AE3D27D4EB4FL;
    private static final long HASH_MULTIPLIER_Z = 0x165667B19E3779F9L;

    private CultivatorTombUtility() {
    }

    /** 一座已确定存在的墓穴的全部确定性参数。 */
    public record TombPlacement(
            int cellX,
            int cellZ,
            int centerX,
            int centerZ,
            TombTier tier,
            boolean alongZ,
            int masterLayer,
            long lootSeed,
            long decorSeed
    ) {
        /** SavedData 键：格坐标打包。 */
        public long cellKey() {
            return ((long) this.cellX << 32) | (this.cellZ & 0xFFFFFFFFL);
        }

        /** 世界坐标 → 长轴局部坐标。 */
        public int toMajor(int blockX, int blockZ) {
            return this.alongZ ? blockZ - this.centerZ : blockX - this.centerX;
        }

        /** 世界坐标 → 短轴局部坐标。 */
        public int toMinor(int blockX, int blockZ) {
            return this.alongZ ? blockX - this.centerX : blockZ - this.centerZ;
        }

        /** 长/短轴局部坐标 → 世界坐标。 */
        public BlockPos toWorld(int major, int minor, int y) {
            return this.alongZ
                    ? new BlockPos(this.centerX + minor, y, this.centerZ + major)
                    : new BlockPos(this.centerX + major, y, this.centerZ + minor);
        }

        public int minBlockX() {
            return this.centerX - (this.alongZ ? this.tier.getHalfShort() : this.tier.getHalfLong()) - 1;
        }

        public int maxBlockX() {
            return this.centerX + (this.alongZ ? this.tier.getHalfShort() : this.tier.getHalfLong()) + 1;
        }

        public int minBlockZ() {
            return this.centerZ - (this.alongZ ? this.tier.getHalfLong() : this.tier.getHalfShort()) - 1;
        }

        public int maxBlockZ() {
            return this.centerZ + (this.alongZ ? this.tier.getHalfLong() : this.tier.getHalfShort()) + 1;
        }

        /** 主室（含墙壳）是否包含该点——供"动棺椁/扰墓"判定。 */
        public boolean isInsideChamber(BlockPos pos) {
            int floorY = this.tier.getFloorY();
            if (pos.getY() < floorY || pos.getY() > floorY + TombTier.CHAMBER_INNER_HEIGHT + 1) {
                return false;
            }
            return pos.getX() >= minBlockX() && pos.getX() <= maxBlockX()
                    && pos.getZ() >= minBlockZ() && pos.getZ() <= maxBlockZ();
        }

        /** 棺椁基座中心（强化深板岩签名块所在位置，地板层）。 */
        public BlockPos getPedestalPos() {
            return new BlockPos(this.centerX, this.tier.getFloorY(), this.centerZ);
        }

        /** 墓主巢位（棺椁位置的可站立面）。 */
        public BlockPos getHomePos() {
            return new BlockPos(this.centerX, this.tier.getFloorY() + 1, this.centerZ);
        }

        /** 棺椁本体占位（长轴三格 × 两层：椁身 + 椁盖）。 */
        public List<BlockPos> getCoffinBlocks() {
            List<BlockPos> blocks = new ArrayList<>(6);
            int floorY = this.tier.getFloorY();
            for (int major = -1; major <= 1; major++) {
                blocks.add(toWorld(major, 0, floorY + 1));
                blocks.add(toWorld(major, 0, floorY + 2));
            }
            return blocks;
        }

        /** 宝箱位置（长轴两端；低阶墓只有正端一只）。 */
        public List<BlockPos> getChestPositions() {
            List<BlockPos> chests = new ArrayList<>(2);
            int y = this.tier.getFloorY() + 1;
            chests.add(toWorld(this.tier.getHalfLong() - 1, 0, y));
            if (this.tier.isHighTier()) {
                chests.add(toWorld(-(this.tier.getHalfLong() - 1), 0, y));
            }
            return chests;
        }

        /** 宝箱朝向（朝主室中心）。 */
        public Direction getChestFacing(BlockPos chestPos) {
            int major = toMajor(chestPos.getX(), chestPos.getZ());
            if (this.alongZ) {
                return major > 0 ? Direction.NORTH : Direction.SOUTH;
            }
            return major > 0 ? Direction.WEST : Direction.EAST;
        }
    }

    /** 该网格是否有墓、有则给出全部确定性参数。 */
    public static Optional<TombPlacement> getTombForCell(long seed, int cellX, int cellZ) {
        if (toUnitDouble(mixSeed(seed, cellX, cellZ, OCCUPANCY_SALT)) >= CELL_OCCUPANCY_CHANCE) {
            return Optional.empty();
        }
        int baseX = cellX * GRID_SPACING_BLOCKS + GRID_SPACING_BLOCKS / 2;
        int baseZ = cellZ * GRID_SPACING_BLOCKS + GRID_SPACING_BLOCKS / 2;
        int jitterSpan = CENTER_JITTER_BLOCKS * 2 + 1;
        int centerX = baseX
                + (int) Math.floorMod(mixSeed(seed, cellX, cellZ, JITTER_X_SALT), jitterSpan)
                - CENTER_JITTER_BLOCKS;
        int centerZ = baseZ
                + (int) Math.floorMod(mixSeed(seed, cellX, cellZ, JITTER_Z_SALT), jitterSpan)
                - CENTER_JITTER_BLOCKS;
        TombTier tier = TombTier.roll(toUnitDouble(mixSeed(seed, cellX, cellZ, TIER_SALT)));
        boolean alongZ = (mixSeed(seed, cellX, cellZ, ORIENTATION_SALT) & 1L) == 0L;
        int masterLayer = 3 + (int) Math.floorMod(mixSeed(seed, cellX, cellZ, LAYER_SALT), 7L);
        return Optional.of(new TombPlacement(
                cellX,
                cellZ,
                centerX,
                centerZ,
                tier,
                alongZ,
                masterLayer,
                mixSeed(seed, cellX, cellZ, LOOT_SALT),
                mixSeed(seed, cellX, cellZ, DECOR_SALT)
        ));
    }

    /** 距该点最近的墓穴（查 3×3 邻格；交互半径远小于网格间距，最近一座即全部）。 */
    public static Optional<TombPlacement> findTombNear(long seed, int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, GRID_SPACING_BLOCKS);
        int cellZ = Math.floorDiv(blockZ, GRID_SPACING_BLOCKS);
        TombPlacement nearest = null;
        long nearestDistSq = Long.MAX_VALUE;
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                Optional<TombPlacement> tomb = getTombForCell(seed, cellX + offsetX, cellZ + offsetZ);
                if (tomb.isEmpty()) {
                    continue;
                }
                long dx = tomb.get().centerX() - blockX;
                long dz = tomb.get().centerZ() - blockZ;
                long distSq = dx * dx + dz * dz;
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = tomb.get();
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** 与该区块 XZ 范围相交的全部墓穴（供 feature 每区块独立雕刻自己的切片）。 */
    public static List<TombPlacement> getTombsIntersectingChunk(long seed, int minBlockX, int minBlockZ) {
        int maxBlockX = minBlockX + 15;
        int maxBlockZ = minBlockZ + 15;
        int cellX = Math.floorDiv(minBlockX + 8, GRID_SPACING_BLOCKS);
        int cellZ = Math.floorDiv(minBlockZ + 8, GRID_SPACING_BLOCKS);
        List<TombPlacement> intersecting = new ArrayList<>(1);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                getTombForCell(seed, cellX + offsetX, cellZ + offsetZ).ifPresent(tomb -> {
                    if (tomb.minBlockX() <= maxBlockX && tomb.maxBlockX() >= minBlockX
                            && tomb.minBlockZ() <= maxBlockZ && tomb.maxBlockZ() >= minBlockZ) {
                        intersecting.add(tomb);
                    }
                });
            }
        }
        return intersecting;
    }

    /** 装饰散布哈希（蛛网/骷髅头/锁链），盐值区别于灵界矿道。 */
    public static long decorHash(long decorSeed, int blockX, int blockY, int blockZ) {
        return mixSeed(decorSeed, blockX, blockY, blockZ, DECOR_SALT);
    }

    public static long mixSeed(long seed, long x, long z, long salt) {
        return avalanche(seed ^ x * HASH_MULTIPLIER_X ^ z * HASH_MULTIPLIER_Z ^ salt);
    }

    public static long mixSeed(long seed, int x, int y, int z, long salt) {
        return avalanche(
                seed
                        ^ (long) x * HASH_MULTIPLIER_X
                        ^ (long) y * HASH_MULTIPLIER_Y
                        ^ (long) z * HASH_MULTIPLIER_Z
                        ^ salt
        );
    }

    public static double toUnitDouble(long value) {
        return (double) (value >>> 11) * 0x1.0p-53;
    }

    private static long avalanche(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
