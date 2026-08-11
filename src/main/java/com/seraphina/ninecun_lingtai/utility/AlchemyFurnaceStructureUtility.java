package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.common.block.AlchemyFurnacePartBlock;
import com.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class AlchemyFurnaceStructureUtility {
    public static final int MINIMUM_HORIZONTAL_OFFSET = -1;
    public static final int MAXIMUM_HORIZONTAL_OFFSET = 1;
    public static final int MINIMUM_STATE_OFFSET = 0;
    public static final int MAXIMUM_STATE_OFFSET = 2;

    private static final double FURNACE_CENTER_COORDINATE = 0.5D;
    private static final double OCTAGON_CORNER_CUT_RATIO = 0.2928932188D;
    private static final int FURNACE_STRUCTURE_SIZE = MAXIMUM_HORIZONTAL_OFFSET
            - MINIMUM_HORIZONTAL_OFFSET + 1;
    private static final List<FurnaceOutlinePoint> FURNACE_OUTLINE_POINTS = List.of(
            new FurnaceOutlinePoint(0.375D, 0.65D),
            new FurnaceOutlinePoint(0.75D, 0.89125D),
            new FurnaceOutlinePoint(1.40625D, 1.29375D),
            new FurnaceOutlinePoint(1.6875D, 1.15D),
            new FurnaceOutlinePoint(1.9375D, 0.94875D),
            new FurnaceOutlinePoint(2.375D, 0.85664D),
            new FurnaceOutlinePoint(2.65D, 0.6765D),
            new FurnaceOutlinePoint(2.8125D, 0.56438D),
            new FurnaceOutlinePoint(3.0D, 0.40635D)
    );
    private static final List<AABB> FURNACE_COLLISION_BOXES = createFurnaceCollisionBoxes();
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
        validateFurnaceOffsets(xOffset, zOffset);
        return FURNACE_STRUCTURE_SHAPES[
                (xOffset - MINIMUM_HORIZONTAL_OFFSET) * FURNACE_STRUCTURE_SIZE
                        + zOffset - MINIMUM_HORIZONTAL_OFFSET
        ];
    }

    public static List<FurnaceOutlinePoint> getFurnaceOutlinePoints() {
        return FURNACE_OUTLINE_POINTS;
    }

    @Nullable
    public static BlockPos getMainFurnacePosition(Level level, BlockPos blockPosition) {
        BlockState blockState = level.getBlockState(blockPosition);
        if (blockState.is(ModBlocks.ALCHEMY_FURNACE.get())) {
            return blockPosition;
        }
        if (!blockState.is(ModBlocks.ALCHEMY_FURNACE_PART.get())) {
            return null;
        }
        BlockPos furnacePosition = AlchemyFurnacePartBlock.getFurnacePosition(blockState, blockPosition);
        return level.getBlockState(furnacePosition).is(ModBlocks.ALCHEMY_FURNACE.get())
                ? furnacePosition
                : null;
    }

    private static List<AABB> createFurnaceCollisionBoxes() {
        List<AABB> collisionBoxes = new ArrayList<>();
        addFurnaceFeet(collisionBoxes);
        for (int index = 0; index < FURNACE_OUTLINE_POINTS.size() - 1; index++) {
            FurnaceOutlinePoint lowerPoint = FURNACE_OUTLINE_POINTS.get(index);
            FurnaceOutlinePoint upperPoint = FURNACE_OUTLINE_POINTS.get(index + 1);
            addOctagonalTier(
                    collisionBoxes,
                    lowerPoint.yCoordinate(),
                    upperPoint.yCoordinate(),
                    Math.max(lowerPoint.horizontalRadius(), upperPoint.horizontalRadius())
            );
        }
        addFurnaceHandles(collisionBoxes);
        return List.copyOf(collisionBoxes);
    }

    private static void addFurnaceFeet(List<AABB> collisionBoxes) {
        collisionBoxes.add(new AABB(0.3125D, 0.0D, -0.5D, 0.6875D, 0.15625D, -0.0625D));
        collisionBoxes.add(new AABB(0.28125D, 0.125D, -0.46875D, 0.71875D, 0.375D, -0.03125D));
        collisionBoxes.add(new AABB(0.25D, 0.3125D, -0.5D, 0.75D, 0.625D, 0.0D));

        collisionBoxes.add(new AABB(0.0D, 0.0D, 0.05D, 0.7D, 0.625D, 0.7D));
        collisionBoxes.add(new AABB(0.3D, 0.0D, 0.05D, 1.0D, 0.625D, 0.7D));
    }

    private static void addFurnaceHandles(List<AABB> collisionBoxes) {
        collisionBoxes.add(new AABB(-1.0D, 0.75D, 0.28125D, -0.75D, 2.125D, 0.71875D));
        collisionBoxes.add(new AABB(-0.875D, 0.84375D, 0.25D, -0.4D, 1.875D, 0.75D));
        collisionBoxes.add(new AABB(1.75D, 0.75D, 0.28125D, 2.0D, 2.125D, 0.71875D));
        collisionBoxes.add(new AABB(1.4D, 0.84375D, 0.25D, 1.875D, 1.875D, 0.75D));
    }

    private static void addOctagonalTier(
            List<AABB> collisionBoxes,
            double minimumY,
            double maximumY,
            double horizontalRadius
    ) {
        double minimumCoordinate = FURNACE_CENTER_COORDINATE - horizontalRadius;
        double maximumCoordinate = FURNACE_CENTER_COORDINATE + horizontalRadius;
        double cornerCut = horizontalRadius * OCTAGON_CORNER_CUT_RATIO;
        double firstStep = cornerCut * 0.5D;
        double secondStep = cornerCut;

        addBox(
                collisionBoxes,
                minimumCoordinate + secondStep,
                minimumY,
                minimumCoordinate,
                maximumCoordinate - secondStep,
                maximumY,
                minimumCoordinate + firstStep
        );
        addBox(
                collisionBoxes,
                minimumCoordinate + firstStep,
                minimumY,
                minimumCoordinate + firstStep,
                maximumCoordinate - firstStep,
                maximumY,
                minimumCoordinate + secondStep
        );
        addBox(
                collisionBoxes,
                minimumCoordinate,
                minimumY,
                minimumCoordinate + secondStep,
                maximumCoordinate,
                maximumY,
                maximumCoordinate - secondStep
        );
        addBox(
                collisionBoxes,
                minimumCoordinate + firstStep,
                minimumY,
                maximumCoordinate - secondStep,
                maximumCoordinate - firstStep,
                maximumY,
                maximumCoordinate - firstStep
        );
        addBox(
                collisionBoxes,
                minimumCoordinate + secondStep,
                minimumY,
                maximumCoordinate - firstStep,
                maximumCoordinate - secondStep,
                maximumY,
                maximumCoordinate
        );
    }

    private static void addBox(
            List<AABB> collisionBoxes,
            double minimumX,
            double minimumY,
            double minimumZ,
            double maximumX,
            double maximumY,
            double maximumZ
    ) {
        if (minimumX < maximumX && minimumY < maximumY && minimumZ < maximumZ) {
            collisionBoxes.add(new AABB(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ));
        }
    }

    private static VoxelShape[] createFurnaceStructureShapes() {
        VoxelShape[] furnaceStructureShapes = new VoxelShape[FURNACE_STRUCTURE_SIZE * FURNACE_STRUCTURE_SIZE];
        for (int xOffset = MINIMUM_HORIZONTAL_OFFSET;
             xOffset <= MAXIMUM_HORIZONTAL_OFFSET;
             xOffset++) {
            for (int zOffset = MINIMUM_HORIZONTAL_OFFSET;
                 zOffset <= MAXIMUM_HORIZONTAL_OFFSET;
                 zOffset++) {
                VoxelShape furnaceStructureShape = Shapes.empty();
                for (AABB collisionBox : FURNACE_COLLISION_BOXES) {
                    VoxelShape clippedShape = clipCollisionBoxToBlock(collisionBox, xOffset, zOffset);
                    if (!clippedShape.isEmpty()) {
                        furnaceStructureShape = Shapes.or(furnaceStructureShape, clippedShape);
                    }
                }
                furnaceStructureShapes[
                        (xOffset - MINIMUM_HORIZONTAL_OFFSET) * FURNACE_STRUCTURE_SIZE
                                + zOffset - MINIMUM_HORIZONTAL_OFFSET
                        ] = furnaceStructureShape;
            }
        }
        return furnaceStructureShapes;
    }

    private static VoxelShape clipCollisionBoxToBlock(AABB collisionBox, int xOffset, int zOffset) {
        double minimumX = Math.max(collisionBox.minX, xOffset);
        double maximumX = Math.min(collisionBox.maxX, xOffset + 1.0D);
        double minimumZ = Math.max(collisionBox.minZ, zOffset);
        double maximumZ = Math.min(collisionBox.maxZ, zOffset + 1.0D);
        if (minimumX >= maximumX || minimumZ >= maximumZ) {
            return Shapes.empty();
        }

        return Block.box(
                (minimumX - xOffset) * 16.0D,
                collisionBox.minY * 16.0D,
                (minimumZ - zOffset) * 16.0D,
                (maximumX - xOffset) * 16.0D,
                collisionBox.maxY * 16.0D,
                (maximumZ - zOffset) * 16.0D
        );
    }

    private static void validateFurnaceOffsets(int xOffset, int zOffset) {
        if (xOffset < MINIMUM_HORIZONTAL_OFFSET || xOffset > MAXIMUM_HORIZONTAL_OFFSET
                || zOffset < MINIMUM_HORIZONTAL_OFFSET || zOffset > MAXIMUM_HORIZONTAL_OFFSET) {
            throw new IllegalArgumentException("Furnace offsets must be between -1 and 1.");
        }
    }

    public record FurnaceOutlinePoint(double yCoordinate, double horizontalRadius) {
    }
}
