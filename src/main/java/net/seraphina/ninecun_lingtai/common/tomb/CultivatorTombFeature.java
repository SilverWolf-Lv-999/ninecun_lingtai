package net.seraphina.ninecun_lingtai.common.tomb;

import com.mojang.serialization.Codec;
import net.seraphina.ninecun_lingtai.common.tomb.CultivatorTombUtility.TombPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 修士墓穴雕刻 feature（M4 墓穴切片）。
 *
 * <p>照灵界地下生成器的"每区块独立雕刻"手法：placed_feature 对主世界每个区块调用一次
 * {@link #place}，本区块只雕与自己相交的墓穴切片，所有方块由纯函数
 * f(世界坐标, 墓穴参数) 决定，跨区块天然拼合、与生成顺序无关。</p>
 *
 * <p>结构：密封主墓室（无入口，靠挖掘/神识发现）——深板岩砖墙壳、磨制深板岩地板、
 * 棺椁（磨制黑石椁身 + 台阶椁盖）居中，椁下一块强化深板岩基座兼作运行期"墓穴存在"
 * 签名块；长轴两端禁制宝箱（阵禁 SealRecord 挂钩留 TODO），四角雕纹柱 + 灵魂灯笼，
 * 蛛网/骷髅/锁链按哈希散布。</p>
 */
public final class CultivatorTombFeature extends Feature<NoneFeatureConfiguration> {
    private static final int UPDATE_FLAGS = 2;

    public CultivatorTombFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        long seed = level.getSeed();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        List<TombPlacement> tombs = CultivatorTombUtility.getTombsIntersectingChunk(
                seed,
                chunkPos.getMinBlockX(),
                chunkPos.getMinBlockZ()
        );
        boolean placedAny = false;
        for (TombPlacement tomb : tombs) {
            placedAny |= carveTombSlice(level, chunkPos, tomb);
        }
        return placedAny;
    }

    private static boolean carveTombSlice(WorldGenLevel level, ChunkPos chunkPos, TombPlacement tomb) {
        int floorY = tomb.tier().getFloorY();
        int ceilingY = floorY + TombTier.CHAMBER_INNER_HEIGHT + 1;
        if (floorY <= level.getMinBuildHeight() || ceilingY >= level.getMaxBuildHeight()) {
            return false;
        }

        int minX = Math.max(chunkPos.getMinBlockX(), tomb.minBlockX());
        int maxX = Math.min(chunkPos.getMaxBlockX(), tomb.maxBlockX());
        int minZ = Math.max(chunkPos.getMinBlockZ(), tomb.minBlockZ());
        int maxZ = Math.min(chunkPos.getMaxBlockZ(), tomb.maxBlockZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                for (int deltaY = 0; deltaY <= TombTier.CHAMBER_INNER_HEIGHT + 1; deltaY++) {
                    cursor.set(blockX, floorY + deltaY, blockZ);
                    BlockState state = decideBlock(tomb, blockX, blockZ, deltaY);
                    if (state == null) {
                        continue;
                    }
                    level.setBlock(cursor, state, UPDATE_FLAGS);
                    if (state.is(Blocks.CHEST)) {
                        fillChest(level, cursor.immutable(), tomb);
                    }
                }
            }
        }
        return true;
    }

    /**
     * 纯函数决定某坐标的方块；返回 null 表示不动原方块（本结构无此情况，占位安全）。
     * deltaY：0=地板层，1..H=室内，H+1=顶盖层。
     */
    @Nullable
    private static BlockState decideBlock(TombPlacement tomb, int blockX, int blockZ, int deltaY) {
        TombTier tier = tomb.tier();
        int major = tomb.toMajor(blockX, blockZ);
        int minor = tomb.toMinor(blockX, blockZ);
        int absMajor = Math.abs(major);
        int absMinor = Math.abs(minor);
        int halfLong = tier.getHalfLong();
        int halfShort = tier.getHalfShort();
        int innerHeight = TombTier.CHAMBER_INNER_HEIGHT;
        long hash = CultivatorTombUtility.decorHash(tomb.decorSeed(), blockX, tier.getFloorY() + deltaY, blockZ);

        if (deltaY == 0) {
            if (major == 0 && minor == 0) {
                // 棺椁基座：强化深板岩，兼作运行期三态机的"墓穴确已生成"签名块。
                return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
            }
            if (absMajor <= 2 && absMinor <= 1) {
                return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            return Math.floorMod(blockX + blockZ, 2) == 0
                    ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                    : Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (deltaY == innerHeight + 1) {
            return Math.floorMod(hash, 7L) == 0L
                    ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (absMajor == halfLong + 1 || absMinor == halfShort + 1) {
            if (deltaY == 1) {
                return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            }
            return Math.floorMod(hash, 5L) == 0L
                    ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }

        // ——以下为室内——
        if (absMajor == halfLong - 1 && absMinor == halfShort - 1) {
            return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        }
        if (deltaY == innerHeight
                && absMajor == Math.max(1, halfLong - 2)
                && absMinor == Math.max(1, halfShort - 2)) {
            return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        if (minor == 0 && absMajor <= 1) {
            if (deltaY == 1) {
                return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            if (deltaY == 2) {
                return Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState();
            }
        }
        if (minor == 0 && deltaY == 1 && isChestMajor(tomb, major)) {
            // TODO(阵禁): 宝箱禁制 SealRecord 挂钩（阵禁日志），首版为白箱。
            return Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, tomb.getChestFacing(tomb.toWorld(major, 0, 0)));
        }
        if (deltaY == innerHeight && Math.floorMod(hash, 11L) == 0L) {
            return Blocks.COBWEB.defaultBlockState();
        }
        if (deltaY == 1 && minor != 0 && Math.floorMod(hash, 31L) == 0L) {
            return Blocks.SKELETON_SKULL.defaultBlockState()
                    .setValue(SkullBlock.ROTATION, (int) Math.floorMod(hash >> 8, 16L));
        }
        if (major == 0 && absMinor == halfShort && deltaY >= innerHeight - 1) {
            return Blocks.CHAIN.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean isChestMajor(TombPlacement tomb, int major) {
        int chestMajor = tomb.tier().getHalfLong() - 1;
        return major == chestMajor || (tomb.tier().isHighTier() && major == -chestMajor);
    }

    private static void fillChest(WorldGenLevel level, BlockPos chestPos, TombPlacement tomb) {
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof Container container)) {
            return;
        }
        // 墓主信息写箱子 NBT（施工图 §3）：供 tooltip/阵禁/考古类玩法后续读取。
        blockEntity.getPersistentData().putString(
                "ninecun_lingtai:tomb_master_realm",
                tomb.tier().getRealm().getSerializedName()
        );
        blockEntity.getPersistentData().putInt("ninecun_lingtai:tomb_master_layer", tomb.masterLayer());
        int chestIndex = tomb.toMajor(chestPos.getX(), chestPos.getZ()) > 0 ? 0 : 1;
        RandomSource random = RandomSource.create(tomb.lootSeed() + chestIndex * 0x9E3779B9L);
        CultivatorTombLootFiller.fill(container, tomb.tier(), random);
    }
}
