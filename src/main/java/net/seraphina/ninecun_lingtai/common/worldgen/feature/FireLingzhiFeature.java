package net.seraphina.ninecun_lingtai.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.seraphina.ninecun_lingtai.common.block.plant.SpiritualPlantBlock;
import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class FireLingzhiFeature extends Feature<NoneFeatureConfiguration> {
    public FireLingzhiFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkPos chunkPosition = new ChunkPos(context.origin());
        BlockPos.MutableBlockPos stemPosition = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos plantPosition = new BlockPos.MutableBlockPos();

        BlockPos selectedPosition = null;
        Direction selectedFacing = null;
        int candidateCount = 0;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            for (int z = chunkPosition.getMinBlockZ(); z <= chunkPosition.getMaxBlockZ(); z++) {
                for (int x = chunkPosition.getMinBlockX(); x <= chunkPosition.getMaxBlockX(); x++) {
                    stemPosition.set(x, y, z);
                    if (!level.getBlockState(stemPosition).is(Blocks.CRIMSON_STEM)) {
                        continue;
                    }

                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        plantPosition.setWithOffset(stemPosition, facing);
                        BlockState plantState = ModBlocks.FIRE_LINGZHI.get()
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

        BlockState plantState = ModBlocks.FIRE_LINGZHI.get()
                .defaultBlockState()
                .setValue(SpiritualPlantBlock.FACING, selectedFacing);
        level.setBlock(selectedPosition, plantState, Block.UPDATE_CLIENTS);
        return true;
    }
}
