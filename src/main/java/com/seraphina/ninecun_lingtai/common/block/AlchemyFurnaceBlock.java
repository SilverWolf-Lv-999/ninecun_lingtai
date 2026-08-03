package com.seraphina.ninecun_lingtai.common.block;

import com.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class AlchemyFurnaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(-16.0D, 0.0D, -16.0D, 32.0D, 48.0D, 32.0D);

    public AlchemyFurnaceBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPosition,
            CollisionContext collisionContext
    ) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPosition, BlockState blockState) {
        return new AlchemyFurnaceBlockEntity(blockPosition, blockState);
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
        if (!level.isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(blockPosition) instanceof AlchemyFurnaceBlockEntity furnace) {
            furnace.openMenu(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState blockState,
            BlockEntityType<T> blockEntityType
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        blockEntityType,
                        com.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes.ALCHEMY_FURNACE.get(),
                        AlchemyFurnaceBlockEntity::serverTick
                );
    }

    @Override
    public void onRemove(
            BlockState blockState,
            Level level,
            BlockPos blockPosition,
            BlockState newBlockState,
            boolean movedByPiston
    ) {
        if (!blockState.is(newBlockState.getBlock())) {
            if (level.getBlockEntity(blockPosition) instanceof Container container) {
                net.minecraft.world.Containers.dropContents(level, blockPosition, container);
            }
            super.onRemove(blockState, level, blockPosition, newBlockState, movedByPiston);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
