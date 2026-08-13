package net.seraphina.ninecun_lingtai.common.block.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.plant.ISpiritualPlant;
import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;
import net.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import net.seraphina.ninecun_lingtai.utility.SpiritualPlantUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class SpiritualPlantBlock extends Block implements ISpiritualPlant {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape PLANT_SHAPE = Block.box(3.0D, 0.0D, 2.0D, 13.0D, 7.0D, 10.0D);

    private final ElementType primaryElement;
    private final SpiritualPlantGrade baseGrade;

    protected SpiritualPlantBlock(
            ElementType primaryElement,
            SpiritualPlantGrade baseGrade,
            BlockBehaviour.Properties properties
    ) {
        super(properties.noCollission());
        this.primaryElement = Objects.requireNonNull(primaryElement, "primaryElement");
        this.baseGrade = Objects.requireNonNull(baseGrade, "baseGrade");
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public final ElementType getPrimaryElement() {
        return this.primaryElement;
    }

    @Override
    public final SpiritualPlantGrade getBaseGrade() {
        return this.baseGrade;
    }

    @Override
    public final SpiritualPlantGrade calculateGrade(double elementEnvironmentMultiplier) {
        return SpiritualPlantUtility.calculateGrade(this.baseGrade, elementEnvironmentMultiplier);
    }

    @Override
    public final SpiritualPlantGrade getGrade(Level level, BlockPos blockPosition) {
        double elementEnvironmentMultiplier = ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(
                level,
                blockPosition,
                this.primaryElement
        );
        return calculateGrade(elementEnvironmentMultiplier);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) {
            return null;
        }

        BlockState blockState = this.defaultBlockState().setValue(FACING, facing);
        return blockState.canSurvive(context.getLevel(), context.getClickedPos()) ? blockState : null;
    }

    @Override
    public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPosition) {
        Direction supportDirection = blockState.getValue(FACING).getOpposite();
        return levelReader.getBlockState(blockPosition.relative(supportDirection)).is(BlockTags.LOGS);
    }

    @Override
    public BlockState updateShape(
            BlockState blockState,
            Direction direction,
            BlockState neighborState,
            LevelAccessor levelAccessor,
            BlockPos blockPosition,
            BlockPos neighborPosition
    ) {
        if (!blockState.canSurvive(levelAccessor, blockPosition)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(
                blockState,
                direction,
                neighborState,
                levelAccessor,
                blockPosition,
                neighborPosition
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPosition,
            CollisionContext collisionContext
    ) {
        return PLANT_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
