package net.seraphina.ninecun_lingtai.common.block;

import net.seraphina.ninecun_lingtai.common.block.entity.FormationDiscBlockEntity;
import net.seraphina.ninecun_lingtai.common.formation.FormationInteractionService;
import net.seraphina.ninecun_lingtai.common.formation.FormationRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 阵盘（阵法核心方块）：储灵 + 载阵图 + 激活入口。
 * 交互路由全在服务端 {@link FormationInteractionService}：
 * 持灵石右键充能；潜行右键（闲置）换阵图 / （运转，阵主）点睛→驱阵；
 * 空手右键激活（做一次性几何校验）/ 停转。
 */
public class FormationDiscBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D);

    public FormationDiscBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.POLISHED_DEEPSLATE)
                .strength(1.5F)
                .noOcclusion());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FormationDiscBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(
            BlockState state,
            BlockGetter blockGetter,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return FormationInteractionService.handleDiscUse(
                (ServerLevel) level, pos, serverPlayer, hand);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            FormationRuntime.onDiscRemoved(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
