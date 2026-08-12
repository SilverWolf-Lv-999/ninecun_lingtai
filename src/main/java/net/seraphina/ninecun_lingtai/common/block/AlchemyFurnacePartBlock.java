package net.seraphina.ninecun_lingtai.common.block;

import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.seraphina.ninecun_lingtai.utility.AlchemyFurnaceStructureUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AlchemyFurnacePartBlock extends Block {
    public static final IntegerProperty X_OFFSET = IntegerProperty.create(
            "x_offset",
            AlchemyFurnaceStructureUtility.MINIMUM_STATE_OFFSET,
            AlchemyFurnaceStructureUtility.MAXIMUM_STATE_OFFSET
    );
    public static final IntegerProperty Z_OFFSET = IntegerProperty.create(
            "z_offset",
            AlchemyFurnaceStructureUtility.MINIMUM_STATE_OFFSET,
            AlchemyFurnaceStructureUtility.MAXIMUM_STATE_OFFSET
    );

    public AlchemyFurnacePartBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().noLootTable());
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(X_OFFSET, AlchemyFurnaceStructureUtility.getStateOffset(0))
                        .setValue(Z_OFFSET, AlchemyFurnaceStructureUtility.getStateOffset(0))
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPosition,
            CollisionContext collisionContext
    ) {
        return getFurnaceStructureShape(blockState);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPosition,
            CollisionContext collisionContext
    ) {
        return getFurnaceStructureShape(blockState);
    }

    @Override
    public InteractionResult use(
            BlockState blockState,
            Level level,
            BlockPos blockPosition,
            Player player,
            InteractionHand interactionHand,
            BlockHitResult blockHitResult
    ) {
        BlockPos furnacePosition = getFurnacePosition(blockState, blockPosition);
        if (!level.getBlockState(furnacePosition).is(ModBlocks.ALCHEMY_FURNACE.get())) {
            return InteractionResult.PASS;
        }
        return AlchemyFurnaceBlock.useFurnace(level, furnacePosition, player, interactionHand);
    }

    @Override
    public void playerWillDestroy(
            Level level,
            BlockPos blockPosition,
            BlockState blockState,
            Player player
    ) {
        if (!level.isClientSide) {
            BlockPos furnacePosition = getFurnacePosition(blockState, blockPosition);
            if (level.getBlockState(furnacePosition).is(ModBlocks.ALCHEMY_FURNACE.get())) {
                level.destroyBlock(furnacePosition, !player.isCreative(), player);
            }
        }
        super.playerWillDestroy(level, blockPosition, blockState, player);
    }

    @Override
    public void onRemove(
            BlockState blockState,
            Level level,
            BlockPos blockPosition,
            BlockState newBlockState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide && !blockState.is(newBlockState.getBlock())) {
            BlockPos furnacePosition = getFurnacePosition(blockState, blockPosition);
            if (level.getBlockState(furnacePosition).is(ModBlocks.ALCHEMY_FURNACE.get())) {
                level.destroyBlock(furnacePosition, true);
            }
        }
        super.onRemove(blockState, level, blockPosition, newBlockState, movedByPiston);
    }

    public static BlockPos getFurnacePosition(
            BlockState blockState,
            BlockPos partPosition
    ) {
        return AlchemyFurnaceStructureUtility.getFurnacePosition(
                partPosition,
                AlchemyFurnaceStructureUtility.getHorizontalOffset(blockState.getValue(X_OFFSET)),
                AlchemyFurnaceStructureUtility.getHorizontalOffset(blockState.getValue(Z_OFFSET))
        );
    }

    private static VoxelShape getFurnaceStructureShape(BlockState blockState) {
        return AlchemyFurnaceStructureUtility.getFurnaceStructureShape(
                AlchemyFurnaceStructureUtility.getHorizontalOffset(blockState.getValue(X_OFFSET)),
                AlchemyFurnaceStructureUtility.getHorizontalOffset(blockState.getValue(Z_OFFSET))
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X_OFFSET, Z_OFFSET);
    }
}
