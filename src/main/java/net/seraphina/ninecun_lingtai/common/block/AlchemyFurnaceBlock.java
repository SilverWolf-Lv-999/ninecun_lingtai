package net.seraphina.ninecun_lingtai.common.block;

import net.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.seraphina.ninecun_lingtai.utility.AlchemyFurnaceStructureUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;

import javax.annotation.Nullable;

public final class AlchemyFurnaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

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
        return AlchemyFurnaceStructureUtility.getFurnaceStructureShape(0, 0);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPosition,
            CollisionContext collisionContext
    ) {
        return AlchemyFurnaceStructureUtility.getFurnaceStructureShape(0, 0);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!AlchemyFurnaceStructureUtility.canPlaceAt(
                context.getLevel(),
                context.getClickedPos(),
                context
        )) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos blockPosition,
            BlockState blockState,
            @Nullable LivingEntity placer,
            ItemStack itemStack
    ) {
        super.setPlacedBy(level, blockPosition, blockState, placer, itemStack);
        if (!level.isClientSide) {
            placeParts(level, blockPosition);
        }
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
        return useFurnace(level, blockPosition, player, interactionHand);
    }

    static InteractionResult useFurnace(
            Level level,
            BlockPos blockPosition,
            Player player,
            InteractionHand interactionHand
    ) {
        if (!level.isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(blockPosition) instanceof AlchemyFurnaceBlockEntity furnace) {
            InteractionHand ignitionHand = player.getMainHandItem().is(Items.FLINT_AND_STEEL)
                    ? InteractionHand.MAIN_HAND
                    : player.getOffhandItem().is(Items.FLINT_AND_STEEL) ? InteractionHand.OFF_HAND : null;
            if (ignitionHand != null) {
                furnace.ignite(serverPlayer, ignitionHand);
            } else {
                furnace.openMenu(serverPlayer);
            }
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
                        ModBlockEntityTypes.ALCHEMY_FURNACE.get(),
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
            if (!level.isClientSide) {
                removeParts(level, blockPosition);
            }
            if (!level.isClientSide && level.getBlockEntity(blockPosition) instanceof Container container) {
                net.minecraft.world.Containers.dropContents(level, blockPosition, container);
            }
            super.onRemove(blockState, level, blockPosition, newBlockState, movedByPiston);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static void placeParts(Level level, BlockPos furnacePosition) {
        for (BlockPos partPosition : AlchemyFurnaceStructureUtility.getPartPositions(furnacePosition)) {
            BlockState partState = ModBlocks.ALCHEMY_FURNACE_PART.get()
                    .defaultBlockState()
                    .setValue(
                            AlchemyFurnacePartBlock.X_OFFSET,
                            AlchemyFurnaceStructureUtility.getStateOffset(
                                    partPosition.getX() - furnacePosition.getX()
                            )
                    )
                    .setValue(
                            AlchemyFurnacePartBlock.Z_OFFSET,
                            AlchemyFurnaceStructureUtility.getStateOffset(
                                    partPosition.getZ() - furnacePosition.getZ()
                            )
                    );
            level.setBlock(partPosition, partState, Block.UPDATE_ALL);
        }
    }

    private static void removeParts(Level level, BlockPos furnacePosition) {
        for (BlockPos partPosition : AlchemyFurnaceStructureUtility.getPartPositions(furnacePosition)) {
            BlockState partState = level.getBlockState(partPosition);
            if (partState.is(ModBlocks.ALCHEMY_FURNACE_PART.get())
                    && AlchemyFurnacePartBlock.getFurnacePosition(partState, partPosition).equals(furnacePosition)) {
                level.removeBlock(partPosition, false);
            }
        }
    }
}
