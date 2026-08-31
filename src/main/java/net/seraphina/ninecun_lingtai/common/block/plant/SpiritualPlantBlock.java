package net.seraphina.ninecun_lingtai.common.block.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.plant.ISpiritualPlant;
import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import net.seraphina.ninecun_lingtai.utility.HerbAgeUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualPlantUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 灵植基类（灵气生态闭环，灵气生态施工图 §4.1）：
 * <ul>
 *   <li><b>成熟度</b>：{@link #AGE} 0-3，随机刻生长，生长速率吃灵域质量档；
 *       野生（worldgen 默认态）即成熟；</li>
 *   <li><b>右键采摘</b>：成熟株掉灵药（带药龄采摘日 NBT）并回到幼株根部再生；
 *       采摘计入灵域采收账（回补少量灵气，过采触发质量降档）；</li>
 *   <li><b>连根挖</b>：直接破坏走 loot 掉落，原地不再生；</li>
 *   <li><b>锄头移栽</b>：右手持锄右键起苗，可换地补种，但有概率伤根
 *       （{@link #DEGRADED}，品质降档、药效打折）。</li>
 * </ul>
 */
public abstract class SpiritualPlantBlock extends Block implements ISpiritualPlant {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 成熟度 0-3；worldgen 默认态即 {@link #MAXIMUM_AGE}（野株成熟）。 */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** 移栽伤根标记：品质降一档、采出的灵药药效打折。 */
    public static final BooleanProperty DEGRADED = BooleanProperty.create("degraded");
    public static final int MAXIMUM_AGE = 3;

    /** 随机刻生长基础概率（乘灵域质量档系数 0.70-1.30）。 */
    private static final double BASE_GROWTH_CHANCE = 0.2D;
    /** 锄头移栽伤根概率。 */
    private static final double TRANSPLANT_DEGRADE_CHANCE = 0.3D;
    /** 采摘额外产出概率。 */
    private static final double HARVEST_BONUS_CHANCE = 0.25D;

    private static final int SPIRITUAL_PLANT_LIGHT_LEVEL = 12;
    private static final VoxelShape NORTH_PLANT_SHAPE = Block.box(3.0D, 0.0D, 7.0D, 13.0D, 7.0D, 16.0D);
    private static final VoxelShape SOUTH_PLANT_SHAPE = Block.box(3.0D, 0.0D, 0.0D, 13.0D, 7.0D, 9.0D);
    private static final VoxelShape WEST_PLANT_SHAPE = Block.box(7.0D, 0.0D, 3.0D, 16.0D, 7.0D, 13.0D);
    private static final VoxelShape EAST_PLANT_SHAPE = Block.box(0.0D, 0.0D, 3.0D, 9.0D, 7.0D, 13.0D);

    private final ElementType primaryElement;
    private final SpiritualPlantGrade baseGrade;

    protected SpiritualPlantBlock(
            ElementType primaryElement,
            SpiritualPlantGrade baseGrade,
            BlockBehaviour.Properties properties
    ) {
        super(properties.noCollission().randomTicks().lightLevel(blockState -> SPIRITUAL_PLANT_LIGHT_LEVEL));
        this.primaryElement = Objects.requireNonNull(primaryElement, "primaryElement");
        this.baseGrade = Objects.requireNonNull(baseGrade, "baseGrade");
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(AGE, MAXIMUM_AGE)
                .setValue(DEGRADED, false));
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

    // ==================== 生长（灵域质量联动） ====================

    @Override
    public boolean isRandomlyTicking(BlockState blockState) {
        return blockState.getValue(AGE) < MAXIMUM_AGE;
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel level, BlockPos blockPosition, RandomSource random) {
        int age = blockState.getValue(AGE);
        if (age >= MAXIMUM_AGE) {
            return;
        }
        // 灵气越清的灵域灵植长得越快（丰沛正源），凡浊之地生长迟滞。
        double growthChance = BASE_GROWTH_CHANCE
                * SpiritualAuraManager.getQualityMultiplier(level, blockPosition);
        if (random.nextDouble() < growthChance) {
            level.setBlock(blockPosition, blockState.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    // ==================== 采摘与移栽 ====================

    @Override
    public InteractionResult use(
            BlockState blockState,
            Level level,
            BlockPos blockPosition,
            Player player,
            InteractionHand interactionHand,
            BlockHitResult blockHitResult
    ) {
        ItemStack heldStack = player.getItemInHand(interactionHand);
        if (heldStack.getItem() instanceof HoeItem) {
            if (level instanceof ServerLevel serverLevel) {
                transplant(blockState, serverLevel, blockPosition, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockState.getValue(AGE) < MAXIMUM_AGE) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            harvest(blockState, serverLevel, blockPosition, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 成熟采摘：掉灵药（记采摘日）、回幼株再生、记灵域采收账。 */
    private void harvest(BlockState blockState, ServerLevel level, BlockPos blockPosition, Player player) {
        int count = 1 + (level.getRandom().nextDouble() < HARVEST_BONUS_CHANCE ? 1 : 0);
        ItemStack harvested = new ItemStack(this.asItem(), count);
        HerbAgeUtility.markHarvested(harvested, level.getServer());
        if (blockState.getValue(DEGRADED)) {
            HerbAgeUtility.markDegraded(harvested);
        }
        popResource(level, blockPosition, harvested);
        level.setBlock(blockPosition, blockState.setValue(AGE, 0), Block.UPDATE_CLIENTS);
        boolean overharvested = SpiritualAuraManager.recordPlantHarvest(level, blockPosition);
        if (overharvested) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.aura_overharvest",
                            "此地灵植遭过度采撷，灵气浑浊了几分"
                    ),
                    true
            );
        }
    }

    /** 锄头移栽：整株起苗成物品（可换地补种），有概率伤根降品质。 */
    private void transplant(BlockState blockState, ServerLevel level, BlockPos blockPosition, Player player) {
        ItemStack seedling = new ItemStack(this.asItem());
        HerbAgeUtility.markHarvested(seedling, level.getServer());
        boolean degraded = blockState.getValue(DEGRADED)
                || level.getRandom().nextDouble() < TRANSPLANT_DEGRADE_CHANCE;
        if (degraded) {
            HerbAgeUtility.markDegraded(seedling);
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.plant_transplant_degraded",
                            "起苗时伤了根须，这株灵药品相受损"
                    ),
                    true
            );
        }
        popResource(level, blockPosition, seedling);
        level.removeBlock(blockPosition, false);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) {
            return null;
        }

        CompoundTag stackTag = context.getItemInHand().getTag();
        BlockState blockState = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AGE, 0)
                .setValue(DEGRADED, stackTag != null && stackTag.getBoolean(HerbAgeUtility.DEGRADED_TAG));
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
        return switch (blockState.getValue(FACING)) {
            case NORTH -> NORTH_PLANT_SHAPE;
            case SOUTH -> SOUTH_PLANT_SHAPE;
            case WEST -> WEST_PLANT_SHAPE;
            case EAST -> EAST_PLANT_SHAPE;
            default -> throw new IllegalStateException("Unexpected horizontal direction");
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE, DEGRADED);
    }
}
