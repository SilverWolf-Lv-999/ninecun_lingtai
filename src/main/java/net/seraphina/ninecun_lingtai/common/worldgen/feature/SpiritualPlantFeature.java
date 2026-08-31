package net.seraphina.ninecun_lingtai.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.seraphina.ninecun_lingtai.common.block.plant.SpiritualPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 通用野生灵植 feature（照 {@link FireLingzhiFeature} 的水塘采样法泛化）：
 * 在区块内扫描指定宿主原木，从所有可附着面里等概率选一处生成成熟野株。
 */
public final class SpiritualPlantFeature extends Feature<NoneFeatureConfiguration> {
    private final Supplier<Block> plantBlock;
    private final Supplier<Block> hostLogBlock;

    public SpiritualPlantFeature(
            Codec<NoneFeatureConfiguration> codec,
            Supplier<Block> plantBlock,
            Supplier<Block> hostLogBlock
    ) {
        super(codec);
        this.plantBlock = Objects.requireNonNull(plantBlock, "plantBlock");
        this.hostLogBlock = Objects.requireNonNull(hostLogBlock, "hostLogBlock");
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkPos chunkPosition = new ChunkPos(context.origin());
        BlockPos.MutableBlockPos stemPosition = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos plantPosition = new BlockPos.MutableBlockPos();
        Block hostLog = this.hostLogBlock.get();
        Block plant = this.plantBlock.get();

        BlockPos selectedPosition = null;
        Direction selectedFacing = null;
        int candidateCount = 0;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            for (int z = chunkPosition.getMinBlockZ(); z <= chunkPosition.getMaxBlockZ(); z++) {
                for (int x = chunkPosition.getMinBlockX(); x <= chunkPosition.getMaxBlockX(); x++) {
                    stemPosition.set(x, y, z);
                    if (!level.getBlockState(stemPosition).is(hostLog)) {
                        continue;
                    }

                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        plantPosition.setWithOffset(stemPosition, facing);
                        BlockState plantState = plant
                                .defaultBlockState()
                                .setValue(SpiritualPlantBlock.FACING, facing);
                        if (!level.isEmptyBlock(plantPosition) || !plantState.canSurvive(level, plantPosition)) {
                            continue;
                        }

                        candidateCount++;
                        if (random.nextInt(candidateCount) == 0) {
                            selectedPosition = plantPosition.immutable();
                            selectedFacing = facing;
                        }
                    }
                }
            }
        }

        if (selectedPosition == null || selectedFacing == null) {
            return false;
        }

        BlockState plantState = plant
                .defaultBlockState()
                .setValue(SpiritualPlantBlock.FACING, selectedFacing);
        level.setBlock(selectedPosition, plantState, Block.UPDATE_CLIENTS);
        return true;
    }
}
