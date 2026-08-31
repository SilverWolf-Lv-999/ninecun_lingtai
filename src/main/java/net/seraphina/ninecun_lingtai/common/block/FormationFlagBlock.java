package net.seraphina.ninecun_lingtai.common.block;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.formation.FormationRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * 阵旗（节点方块）：五行各注册一种，材料即属性（材料 tag 见
 * data/ninecun_lingtai/tags/blocks/flags/*.json）。
 * 破坏保护（拔错旗反冲）走 FormationEventHandler 的 BreakEvent；
 * 真被移除（生门旗/阵主自拆/爆炸）由 {@link #onRemove} 通知运转层。
 */
public class FormationFlagBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    private final ElementType element;

    public FormationFlagBlock(ElementType element) {
        super(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)
                .strength(0.5F)
                .noOcclusion()
                .noCollission());
        this.element = element;
    }

    public ElementType element() {
        return this.element;
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
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            FormationRuntime.onFlagRemoved(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
