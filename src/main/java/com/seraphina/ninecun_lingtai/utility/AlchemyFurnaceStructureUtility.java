package com.seraphina.ninecun_lingtai.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class AlchemyFurnaceStructureUtility {
    public static final int MINIMUM_HORIZONTAL_OFFSET = -1;
    public static final int MAXIMUM_HORIZONTAL_OFFSET = 1;
    public static final int MINIMUM_STATE_OFFSET = 0;
    public static final int MAXIMUM_STATE_OFFSET = 2;
    private static final double FURNACE_MINIMUM_HORIZONTAL_COORDINATE = -24.5D;
    private static final double FURNACE_MAXIMUM_HORIZONTAL_COORDINATE = 24.5D;
    private static final double FURNACE_MAXIMUM_VERTICAL_COORDINATE = 48.0D;
    private static final int FURNACE_STRUCTURE_SIZE = MAXIMUM_HORIZONTAL_OFFSET
            - MINIMUM_HORIZONTAL_OFFSET + 1;
    private static final VoxelShape[] FURNACE_STRUCTURE_SHAPES = createFurnaceStructureShapes();

    public static boolean canPlaceAt(
            Level level,
            BlockPos furnacePosition,
            BlockPlaceContext context
    ) {
        for (BlockPos partPosition : getPartPositions(furnacePosition)) {
            if (!level.getWorldBorder().isWithinBounds(partPosition)
                    || !level.getBlockState(partPosition).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> getPartPositions(BlockPos furnacePosition) {
        List<BlockPos> partPositions = new ArrayList<>(8);
        for (int xOffset = MINIMUM_HORIZONTAL_OFFSET;
             xOffset <= MAXIMUM_HORIZONTAL_OFFSET;
             xOffset++) {
            for (int zOffset = MINIMUM_HORIZONTAL_OFFSET;
                 zOffset <= MAXIMUM_HORIZONTAL_OFFSET;
                 zOffset++) {
                if (xOffset != 0 || zOffset != 0) {
                    partPositions.add(furnacePosition.offset(xOffset, 0, zOffset));
                }
            }
        }
        return partPositions;
    }

    public static BlockPos getFurnacePosition(
            BlockPos partPosition,
            int xOffset,
            int zOffset
    ) {
        return partPosition.offset(-xOffset, 0, -zOffset);
    }

    public static int getStateOffset(int horizontalOffset) {
        if (horizontalOffset < MINIMUM_HORIZONTAL_OFFSET
                || horizontalOffset > MAXIMUM_HORIZONTAL_OFFSET) {
            throw new IllegalArgumentException("Horizontal offset must be between -1 and 1.");
        }
        return horizontalOffset - MINIMUM_HORIZONTAL_OFFSET;
    }

    public static int getHorizontalOffset(int stateOffset) {
        if (stateOffset < MINIMUM_STATE_OFFSET || stateOffset > MAXIMUM_STATE_OFFSET) {
            throw new IllegalArgumentException("State offset must be between 0 and 2.");
        }
        return stateOffset + MINIMUM_HORIZONTAL_OFFSET;
    }

    public static VoxelShape getFurnaceStructureShape(int xOffset, int zOffset) {
        if (xOffset < MINIMUM_HORIZONTAL_OFFSET || xOffset > MAXIMUM_HORIZONTAL_OFFSET
                || zOffset < MINIMUM_HORIZONTAL_OFFSET || zOffset > MAXIMUM_HORIZONTAL_OFFSET) {
            throw new IllegalArgumentException("Furnace offsets must be between -1 and 1.");
        }
        return FURNACE_STRUCTURE_SHAPES[
                (xOffset - MINIMUM_HORIZONTAL_OFFSET) * FURNACE_STRUCTURE_SIZE
                        + zOffset - MINIMUM_HORIZONTAL_OFFSET
        ];
    }

    private static VoxelShape[] createFurnaceStructureShapes() {
        VoxelShape[] furnaceStructureShapes = new VoxelShape[FURNACE_STRUCTURE_SIZE * FURNACE_STRUCTURE_SIZE];
        for (int xOffset = MINIMUM_HORIZONTAL_OFFSET;
             xOffset <= MAXIMUM_HORIZONTAL_OFFSET;
             xOffset++) {
            for (int zOffset = MINIMUM_HORIZONTAL_OFFSET;
                 zOffset <= MAXIMUM_HORIZONTAL_OFFSET;
                 zOffset++) {
                double xTranslation = -xOffset * 16.0D;
                double zTranslation = -zOffset * 16.0D;
                furnaceStructureShapes[
                        (xOffset - MINIMUM_HORIZONTAL_OFFSET) * FURNACE_STRUCTURE_SIZE
                                + zOffset - MINIMUM_HORIZONTAL_OFFSET
                        ] = Block.box(
                        FURNACE_MINIMUM_HORIZONTAL_COORDINATE + xTranslation,
                        0.0D,
                        FURNACE_MINIMUM_HORIZONTAL_COORDINATE + zTranslation,
                        FURNACE_MAXIMUM_HORIZONTAL_COORDINATE + xTranslation,
                        FURNACE_MAXIMUM_VERTICAL_COORDINATE,
                        FURNACE_MAXIMUM_HORIZONTAL_COORDINATE + zTranslation
                );
            }
        }
        return furnaceStructureShapes;
    }
}
