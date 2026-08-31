package net.seraphina.ninecun_lingtai.common.block.entity;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyBrewScore;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.alchemy.FireSource;
import net.seraphina.ninecun_lingtai.api.alchemy.FurnaceTier;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatBand;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatControlMode;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;
import net.seraphina.ninecun_lingtai.api.alchemy.HerbDefinition;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementRelation;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.data.AlchemyFurnaceDefinitionManager;
import net.seraphina.ninecun_lingtai.common.data.AlchemyHerbManager;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import net.seraphina.ninecun_lingtai.utility.HerbAgeUtility;
import net.seraphina.ninecun_lingtai.common.insight.InsightEffectUtility;
import net.seraphina.ninecun_lingtai.common.item.PillItem;
import net.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
import net.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyProcessUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyResultCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AlchemyFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity, Container, MenuProvider, ContainerData {
    public static final int HEAT_DATA_INDEX = 0;
    public static final int MAXIMUM_HEAT_DATA_INDEX = 1;
    public static final int REFINING_DATA_INDEX = 2;
    public static final int ENVIRONMENT_MULTIPLIER_X100_DATA_INDEX = 3;
    public static final int PRIMARY_ELEMENT_DATA_INDEX = 4;
    public static final int OPEN_FEED_MASK_DATA_INDEX = 5;
    public static final int MISSED_FEED_MASK_DATA_INDEX = 6;
    public static final int FED_FEED_MASK_DATA_INDEX = 7;
    public static final int LATE_FEED_MASK_DATA_INDEX = 8;
    public static final int HEAT_CONTROL_MODE_DATA_INDEX = 9;
    public static final int PLANNED_FEED_MASK_DATA_INDEX = 10;
    public static final int DATA_COUNT = 11;

    private static final int CLIENT_SYNCHRONIZATION_INTERVAL = 20;
    private static final int ENVIRONMENT_CACHE_INTERVAL = 20;
    private static final double MAINTAIN_RANGE_SQR = 64.0D;
    private static final int SCENT_TOAST_DURATION_TICKS = 100;
    private static final int RESULT_TOAST_DURATION_TICKS = 120;
    private static final String REFINING_KEY = "Refining";
    private static final String REFINING_TICKS_KEY = "RefiningTicks";
    private static final String LEGACY_BREW_PROGRESS_KEY = "BrewProgress";
    private static final String HEAT_KEY = "Heat";
    private static final String HIT_TICKS_KEY = "HitTicks";
    private static final String EFFECTIVE_DURATION_KEY = "EffectiveDuration";
    private static final String ENVIRONMENT_MULTIPLIER_KEY = "EnvMultiplier";
    private static final String ROOT_SCORE_KEY = "BrewRootScore";
    private static final String SOUL_SCORE_KEY = "BrewSoulScore";
    private static final String REALM_SCORE_KEY = "BrewRealmScore";
    private static final String ENVIRONMENT_SCORE_KEY = "BrewEnvScore";
    private static final String ROOT_QUALITY_KEY = "BrewRootQuality";
    private static final String SCENT_PLAN_KEY = "ScentPlan";
    private static final String SCENT_SENT_KEY = "ScentSent";
    private static final String REFINER_KEY = "Refiner";
    private static final String DURABILITY_KEY = "Durability";
    private static final String FIRE_SOURCE_KEY = "FireSource";

    /** 灵药药性与丹方主元素相生/相同的环境分加成（炼丹 M4-c）。 */
    private static final double HERB_AFFINITY_BONUS = 0.03D;
    /** 灵药药性与丹方主元素相克的环境分扣减。 */
    private static final double HERB_AFFINITY_PENALTY = 0.05D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> items = NonNullList.withSize(
            AlchemyFurnaceMenu.FURNACE_SLOT_COUNT,
            ItemStack.EMPTY
    );

    private boolean refining;
    @Nullable
    private ResourceLocation selectedRecipeId;
    @Nullable
    private ResourceLocation activeRecipeId;
    private int refiningTicks;
    private int heat;

    private int hitTicks;
    private int effectiveDuration;
    private double environmentMultiplier = 1.0D;
    private double rootScore;
    private double soulScore;
    private double realmScore;
    private double environmentScore = 1.0D;
    private double rootQuality;
    private int scentPlanTicks = -1;
    private boolean scentSent;
    @Nullable
    private UUID refinerId;
    /** 分段投药窗口（机械拆分产物，行为与拆分前逐位一致）。 */
    private final AlchemyFeedWindowTracker feedWindow = new AlchemyFeedWindowTracker();
    /** 本炉次火源（开炉时判定一次，M4-b）。 */
    private FireSource fireSource = FireSource.MUNDANE;
    /** 炉耐久（-1 = 未初始化，首次读取按阶别上限补），随炉次损耗。 */
    private int durability = -1;
    /** 保温率的小数降温余数（凡铁 1.0 时行为与旧版逐 tick −1 完全一致）。 */
    private double heatDecayRemainder;

    @Nullable
    private UUID heatControllerPlayerId;
    private HeatControlMode heatControlMode = HeatControlMode.NONE;

    private int cachedEnvironmentMultiplierX100 = 100;
    private int cachedPrimaryElementOrdinal;
    private int environmentCacheCooldown;

    public AlchemyFurnaceBlockEntity(BlockPos blockPosition, BlockState blockState) {
        super(ModBlockEntityTypes.ALCHEMY_FURNACE.get(), blockPosition, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static void serverTick(Level level, BlockPos blockPosition, BlockState blockState, AlchemyFurnaceBlockEntity furnace) {
        if (--furnace.environmentCacheCooldown <= 0) {
            furnace.environmentCacheCooldown = ENVIRONMENT_CACHE_INTERVAL;
            furnace.updateEnvironmentCache();
        }
        if (furnace.heat <= 0 && !furnace.refining) {
            furnace.releaseHeatControl();
            return;
        }

        boolean heatControlled = furnace.tickHeatControl();
        if (!heatControlled && furnace.heat > 0) {
            // 保温率降温：凡铁 1.0 = 旧版逐 tick −1；高阶炉 <1.0 按余数累积慢降。
            furnace.heatDecayRemainder += furnace.getFurnaceTier().heatDecayRate();
            int wholeDecay = (int) furnace.heatDecayRemainder;
            if (wholeDecay > 0) {
                furnace.heatDecayRemainder -= wholeDecay;
                furnace.heat = Math.max(0, furnace.heat - wholeDecay);
            }
        }

        if (furnace.refining) {
            furnace.refiningTicks++;
            furnace.updateFeedWindowState();
            if (furnace.heat <= 0) {
                furnace.failAshes();
                return;
            }
            furnace.trackFireScore();
            furnace.tickScent();
        }
        furnace.setChanged();
        if (furnace.heat % CLIENT_SYNCHRONIZATION_INTERVAL == 0) {
            furnace.synchronizeToClients();
        }
    }

    public void handleAction(ServerPlayer player, AlchemyFurnaceActionPacket.Action action) {
        switch (action) {
            case START_REFINING -> {
                if (!this.refining) {
                    startRefining(player);
                }
            }
            case FINISH_REFINING -> {
                if (this.refining) {
                    finishRefining(player);
                }
            }
        }
    }

    public boolean selectRecipe(ServerPlayer player, ResourceLocation recipeId) {
        if (this.refining) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_select_while_refining"),
                    true
            );
            return false;
        }
        AlchemyRecipeDefinition recipe = AlchemyRecipeManager.INSTANCE.getRecipe(recipeId).orElse(null);
        if (recipe == null) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_alchemy_recipe"),
                    true
            );
            return false;
        }
        if (!PlayerLearnedAlchemyRecipeData.hasRecipe(player, recipeId)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_recipe_not_learned"),
                    true
            );
            return false;
        }
        if (!meetsRefiningRealm(player, recipe)) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.alchemy_realm_restricted",
                            recipe.getMinimumRefiningRealmName()
                    ),
                    true
            );
            return false;
        }
        if (!meetsFurnaceTier(recipe)) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.alchemy_furnace_tier_too_low",
                            "此炉阶别不足，炼不得这等丹方"
                    ),
                    true
            );
            return false;
        }
        if (!canSelectWithCurrentContents(recipe)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_selection_conflicts"),
                    true
            );
            return false;
        }
        this.selectedRecipeId = recipeId;
        this.feedWindow.setPlannedMask(AlchemyProcessUtility.getRequiredFeedMask(recipe.feedPlan()));
        this.feedWindow.reset();
        this.setChangedAndSynchronize();
        return true;
    }

    // ==================== 炉阶与火源（炼丹 M4-a/b） ====================

    /** 当前炉方块的阶别定义（数据驱动，未定义回落凡铁兜底）。 */
    public FurnaceTier getFurnaceTier() {
        return AlchemyFurnaceDefinitionManager.INSTANCE.getTierForBlock(getBlockState().getBlock());
    }

    /** 炉温上限 = 阶别上限 + 火源加成（地火/三昧真火抬升上限）。 */
    private int getMaxHeat() {
        int fireSourceBonus = switch (this.fireSource) {
            case EARTH -> CommonConfig.ALCHEMY_FIRE_SOURCE_EARTH_HEAT_BONUS.get();
            case SAMADHI -> CommonConfig.ALCHEMY_FIRE_SOURCE_SAMADHI_HEAT_BONUS.get();
            case MUNDANE, SPIRITUAL -> 0;
        };
        return getFurnaceTier().heatCap() + fireSourceBonus;
    }

    /** 当前耐久（-1 未初始化时按阶别上限补）。 */
    public int getDurability() {
        if (this.durability < 0) {
            this.durability = getFurnaceTier().maxDurability();
        }
        return this.durability;
    }

    /** 丹方阶别不得超过炉可炼上限。 */
    private boolean meetsFurnaceTier(AlchemyRecipeDefinition recipe) {
        return recipe.grade().tier() <= getFurnaceTier().maxRecipeTier();
    }

    /**
     * 火源判定（开炉时一次，优先级：三昧真火 ＞ 灵火 ＞ 地火 ＞ 凡火）：
     * 三昧真火看炉主境界 ≥ 化神；灵火看炉腹正下方灵魂火系方块；
     * 地火看炉下两格与四邻是否有岩浆。
     */
    private FireSource detectFireSource(ServerPlayer player) {
        boolean samadhiCapable = PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false);
        if (samadhiCapable) {
            return FireSource.SAMADHI;
        }
        if (this.level == null) {
            return FireSource.MUNDANE;
        }
        BlockPos below = this.worldPosition.below();
        BlockState belowState = this.level.getBlockState(below);
        if (belowState.is(Blocks.SOUL_FIRE)
                || belowState.is(Blocks.SOUL_CAMPFIRE)
                || belowState.is(Blocks.SOUL_TORCH)
                || belowState.is(Blocks.SOUL_LANTERN)) {
            return FireSource.SPIRITUAL;
        }
        List<BlockPos> lavaProbePositions = List.of(
                below,
                below.below(),
                below.north(),
                below.south(),
                below.east(),
                below.west()
        );
        for (BlockPos probePosition : lavaProbePositions) {
            if (this.level.getFluidState(probePosition).is(FluidTags.LAVA)) {
                return FireSource.EARTH;
            }
        }
        return FireSource.MUNDANE;
    }

    /** 火源稳定度乘区（进火候评分，凡火 1.0 基线）。 */
    private double getFireSourceStabilityMultiplier() {
        return switch (this.fireSource) {
            case MUNDANE -> 1.0D;
            case SPIRITUAL -> CommonConfig.ALCHEMY_FIRE_SOURCE_SPIRITUAL_STABILITY.get();
            case EARTH -> CommonConfig.ALCHEMY_FIRE_SOURCE_EARTH_STABILITY.get();
            case SAMADHI -> CommonConfig.ALCHEMY_FIRE_SOURCE_SAMADHI_STABILITY.get();
        };
    }

    /** 炉容差（炉性与丹方主元素相克时清零，扩展设计 2.1）。 */
    private double getEffectiveToleranceBonus(AlchemyRecipeDefinition recipe) {
        FurnaceTier tier = getFurnaceTier();
        ElementType furnaceElement = tier.element();
        ElementType primaryElement = recipe.getPrimaryElement().orElse(null);
        if (furnaceElement == null || primaryElement == null || furnaceElement == ElementType.CHAOS) {
            return tier.toleranceBonus();
        }
        ElementRelation relation = furnaceElement.getRelationTo(primaryElement);
        if (relation == ElementRelation.OVERCOMES || relation == ElementRelation.OVERCOME_BY) {
            return 0.0D;
        }
        return tier.toleranceBonus();
    }

    /** 炉性与丹方主元素相生/相同（完美产量判定翻倍用）。 */
    private boolean hasFurnaceElementSynergy(AlchemyRecipeDefinition recipe) {
        FurnaceTier tier = getFurnaceTier();
        ElementType furnaceElement = tier.element();
        ElementType primaryElement = recipe.getPrimaryElement().orElse(null);
        if (furnaceElement == null || primaryElement == null || furnaceElement == ElementType.CHAOS) {
            return false;
        }
        ElementRelation relation = furnaceElement.getRelationTo(primaryElement);
        return relation == ElementRelation.SAME || relation == ElementRelation.GENERATES;
    }

    /**
     * 灵药药性与药龄进环境分（炼丹 M4-c）：主药与丹方主元素相生/相同 +0.03、
     * 相克 −0.05；再乘主药药龄药效乘区（{@link HerbAgeUtility}，封顶走配置）。
     */
    private double adjustEnvironmentScoreForHerbs(AlchemyRecipeDefinition recipe, double environmentScore) {
        ItemStack mainIngredient = getItem(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT);
        if (mainIngredient.isEmpty() || this.level == null || this.level.getServer() == null) {
            return environmentScore;
        }
        double adjustedScore = environmentScore;
        ElementType primaryElement = recipe.getPrimaryElement().orElse(null);
        Optional<HerbDefinition> herb = AlchemyHerbManager.INSTANCE.getDefinitionForItem(
                BuiltInRegistries.ITEM.getKey(mainIngredient.getItem())
        );
        if (herb.isPresent() && herb.get().element() != null && primaryElement != null) {
            ElementRelation relation = herb.get().element().getRelationTo(primaryElement);
            if (relation == ElementRelation.SAME || relation == ElementRelation.GENERATES) {
                adjustedScore += HERB_AFFINITY_BONUS;
            } else if (relation == ElementRelation.OVERCOMES || relation == ElementRelation.OVERCOME_BY) {
                adjustedScore -= HERB_AFFINITY_PENALTY;
            }
        }
        adjustedScore *= HerbAgeUtility.getPotencyMultiplier(mainIngredient, this.level.getServer());
        return Math.max(0.0D, adjustedScore);
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return this.selectedRecipeId;
    }

    public void ignite(ServerPlayer player, InteractionHand interactionHand) {
        ItemStack ignitionTool = player.getItemInHand(interactionHand);
        this.heat = getMaxHeat();
        ignitionTool.hurtAndBreak(1, player, ignored -> player.broadcastBreakEvent(interactionHand));
        this.setChangedAndSynchronize();
    }

    public void setHeatControl(ServerPlayer player, HeatControlMode mode) {
        if (mode == HeatControlMode.NONE) {
            if (player.getUUID().equals(this.heatControllerPlayerId)) {
                releaseHeatControl();
            }
            return;
        }
        if (!this.refining || this.heat <= 0) {
            return;
        }
        this.heatControllerPlayerId = player.getUUID();
        this.heatControlMode = mode;
        setChanged();
    }

    private boolean tickHeatControl() {
        if (this.heatControllerPlayerId == null
                || this.heatControlMode == HeatControlMode.NONE
                || this.level == null
                || !this.refining
                || this.heat <= 0) {
            releaseHeatControl();
            return false;
        }
        ServerPlayer player = this.level.getServer() == null
                ? null
                : this.level.getServer().getPlayerList().getPlayer(this.heatControllerPlayerId);
        if (player == null
                || !player.isAlive()
                || player.level() != this.level
                || !(player.containerMenu instanceof AlchemyFurnaceMenu menu)
                || !menu.getBlockPosition().equals(this.worldPosition)
                || player.distanceToSqr(
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D
                ) > MAINTAIN_RANGE_SQR) {
            releaseHeatControl();
            return false;
        }
        AlchemyRecipeDefinition recipe = this.activeRecipeId == null
                ? null
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId).orElse(null);
        HeatBand currentBand = HeatBand.ofHeat(this.heat).orElse(null);
        if (recipe == null || currentBand == null) {
            releaseHeatControl();
            return false;
        }
        double costPerTick = getHeatControlCostPerTick(
                recipe.grade().tier(),
                currentBand,
                this.heatControlMode
        );
        if (!consumeHeatControlCost(
                player,
                recipe.getPrimaryElement().orElse(null),
                costPerTick
        )) {
            releaseHeatControl();
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_heat_control_exhausted"),
                    true
            );
            return false;
        }
        switch (this.heatControlMode) {
            case STOKE -> this.heat = Math.min(
                    getMaxHeat(),
                    this.heat + Math.max(1, CommonConfig.ALCHEMY_STOKE_HEAT_PER_TICK.get())
            );
            case VENT -> this.heat = Math.max(
                    0,
                    this.heat - Math.max(1, CommonConfig.ALCHEMY_VENT_HEAT_PER_TICK.get())
            );
            case MAINTAIN -> this.heat = currentBand.getCenterHeat();
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    private void releaseHeatControl() {
        this.heatControllerPlayerId = null;
        this.heatControlMode = HeatControlMode.NONE;
    }

    private static double getHeatControlCostPerTick(
            int tier,
            HeatBand band,
            HeatControlMode mode
    ) {
        double bandFactor = switch (band) {
            case GENTLE -> 0.6D;
            case MEDIUM -> 1.0D;
            case FIERCE -> 1.6D;
        };
        double modeFactor = switch (mode) {
            case STOKE -> CommonConfig.ALCHEMY_STOKE_COST_MULTIPLIER.get();
            case VENT -> CommonConfig.ALCHEMY_VENT_COST_MULTIPLIER.get();
            case MAINTAIN -> CommonConfig.ALCHEMY_MAINTAIN_COST_MULTIPLIER.get();
            case NONE -> 0.0D;
        };
        return CommonConfig.ALCHEMY_MAINTAIN_BASE_COST.get()
                * Math.pow(CommonConfig.ALCHEMY_MAINTAIN_TIER_GROWTH.get(), tier - 1)
                * bandFactor
                * modeFactor
                / 20.0D;
    }

    private static boolean consumeHeatControlCost(
            ServerPlayer player,
            @Nullable ElementType primaryElement,
            double cost
    ) {
        if (cost <= 0.0D) {
            return true;
        }
        if (primaryElement == null) {
            return PlayerSpiritualEnergyData.consumeEnergyAny(player, cost) != null;
        }
        if (PlayerSpiritualEnergyData.consumeEnergy(player, primaryElement, cost)) {
            return true;
        }
        for (ElementType element : ElementType.FIVE_ELEMENTS) {
            if (element.getRelationTo(primaryElement) == ElementRelation.GENERATES
                    && PlayerSpiritualEnergyData.consumeEnergy(player, element, cost)) {
                return true;
            }
        }
        return PlayerSpiritualEnergyData.consumeEnergyAny(player, cost * 1.25D) != null;
    }

    private void startRefining(ServerPlayer player) {
        if (this.heat <= 0) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_requires_ignition"),
                    true
            );
            return;
        }
        if (getDurability() <= 0) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.alchemy_furnace_worn",
                            "此炉炉体已朽，须得养炉修复方可再用"
                    ),
                    true
            );
            return;
        }
        AlchemyRecipeDefinition recipe = this.selectedRecipeId == null
                ? getMatchingRecipe().orElse(null)
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.selectedRecipeId).orElse(null);
        boolean validIngredients = recipe != null
                && (recipe.feedPlan().isEmpty()
                ? matchesRecipe(recipe)
                : matchesInitialIngredients(recipe));
        if (!validIngredients) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_invalid_ingredients"),
                    true
            );
            return;
        }
        if (!PlayerLearnedAlchemyRecipeData.hasRecipe(player, recipe.id())) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_recipe_not_learned"),
                    true
            );
            return;
        }
        Optional<net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage> stage =
                PlayerCultivationData.getStage(player);
        if (!meetsRefiningRealm(player, recipe)) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.alchemy_realm_restricted",
                            recipe.getMinimumRefiningRealmName()
                    ),
                    true
            );
            return;
        }
        if (!meetsFurnaceTier(recipe)) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.alchemy_furnace_tier_too_low",
                            "此炉阶别不足，炼不得这等丹方"
                    ),
                    true
            );
            return;
        }
        if (!canAcceptOutput(recipe)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_output_blocked"),
                    true
            );
            return;
        }
        double multiplier = 1.0D;
        ElementType primaryElement = recipe.getPrimaryElement().orElse(null);
        if (primaryElement != null && this.level != null) {
            multiplier = ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(
                    this.level,
                    this.worldPosition,
                    primaryElement
            );
            if (multiplier <= 0.0D) {
                player.displayClientMessage(
                        Component.translatable("message.ninecun_lingtai.alchemy_environment_dead"),
                        true
                );
                return;
            }
        }

        this.fireSource = detectFireSource(player);
        this.environmentMultiplier = primaryElement == null ? 1.0D : multiplier;
        this.effectiveDuration = AlchemyResultCalculator.getEffectiveDuration(
                recipe.brewingDuration(),
                this.environmentMultiplier
        );
        this.environmentScore = adjustEnvironmentScoreForHerbs(
                recipe,
                AlchemyResultCalculator.calculateEnvironmentScore(
                        multiplier,
                        primaryElement != null
                )
        );
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        this.rootScore = AlchemyResultCalculator.calculateRootScore(spiritualRoot, primaryElement);
        this.rootQuality = spiritualRoot.map(SpiritualRoot::quality).orElse(0.0F);
        this.soulScore = AlchemyResultCalculator.calculateSoulScore(PlayerAptitudeData.getSoulStrength(player));
        this.realmScore = AlchemyResultCalculator.calculateRealmScore(stage, recipe.grade().tier());
        this.scentPlanTicks = rollScentPlan(player);
        this.scentSent = false;
        this.refinerId = player.getUUID();
        this.hitTicks = 0;
        this.selectedRecipeId = recipe.id();
        this.feedWindow.setPlannedMask(AlchemyProcessUtility.getRequiredFeedMask(recipe.feedPlan()));
        this.refining = true;
        this.activeRecipeId = recipe.id();
        this.refiningTicks = 0;
        this.feedWindow.reset();
        updateFeedWindowState();
        this.setChangedAndSynchronize();
    }

    private static boolean meetsRefiningRealm(
            ServerPlayer player,
            AlchemyRecipeDefinition recipe
    ) {
        return PlayerCultivationData.getStage(player)
                .map(currentStage -> currentStage.realm().ordinal()
                        >= recipe.minimumRefiningRealm().ordinal()
                        && currentStage.realm().ordinal() >= recipe.grade().tier() - 1)
                .orElse(false);
    }

    private int rollScentPlan(ServerPlayer player) {
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(1);
        if (stars >= 5) {
            return this.effectiveDuration;
        }
        if (stars >= 3) {
            if (player.getRandom().nextDouble() < 0.8D) {
                return this.effectiveDuration
                        + player.getRandom().nextInt(Math.max(1, this.effectiveDuration / 20));
            }
            return -1;
        }
        return player.getRandom().nextDouble() < 0.4D ? this.effectiveDuration : -1;
    }

    private void trackFireScore() {
        Optional<AlchemyRecipeDefinition> recipe = this.activeRecipeId == null
                ? Optional.empty()
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId);
        if (recipe.isEmpty()) {
            return;
        }
        List<HeatStage> stages = recipe.get().heatStages();
        if (stages.isEmpty()) {
            this.hitTicks++;
            return;
        }
        double progress = this.refiningTicks / (double) Math.max(1, this.effectiveDuration);
        HeatBand targetBand = HeatStage.getTargetBand(stages, progress);
        if (HeatBand.ofHeat(this.heat).filter(targetBand::equals).isPresent()) {
            this.hitTicks++;
        }
    }

    private void updateFeedWindowState() {
        AlchemyRecipeDefinition recipe = this.activeRecipeId == null
                ? null
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId).orElse(null);
        if (!this.refining || recipe == null || recipe.feedPlan().isEmpty()) {
            this.feedWindow.setOpenMask(0);
            return;
        }
        this.feedWindow.updateWindowState(recipe, this.refiningTicks, this.effectiveDuration);
    }

    private void captureDelayedFeed(int slot, ItemStack itemStack) {
        if (!this.refining
                || this.level == null
                || this.level.isClientSide
                || slot < AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT
                || slot >= AlchemyFurnaceMenu.OUTPUT_SLOT) {
            return;
        }
        int slotMask = 1 << slot;
        if (!this.feedWindow.canFeed(slotMask)) {
            return;
        }
        AlchemyRecipeDefinition recipe = this.activeRecipeId == null
                ? null
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId).orElse(null);
        if (recipe == null) {
            return;
        }
        AlchemyFeedStep step = recipe.getFeedStep(AlchemyIngredientSlot.fromSlotIndex(slot))
                .orElse(null);
        if (step == null || !matchesIngredientStack(itemStack, recipe.getIngredient(step.slot()))) {
            return;
        }
        AlchemyProcessUtility.StageProgress stageProgress = AlchemyProcessUtility.locateStage(
                recipe.heatStages(),
                this.refiningTicks / (double) Math.max(1, this.effectiveDuration)
        );
        double accuracy = AlchemyProcessUtility.calculateFeedAccuracy(step, stageProgress);
        if (accuracy <= 0.0D) {
            return;
        }
        this.feedWindow.recordFeed(slotMask, accuracy, step, stageProgress);
    }

    private void tickScent() {
        if (this.scentSent || this.scentPlanTicks < 0 || this.refiningTicks < this.scentPlanTicks) {
            return;
        }
        this.scentSent = true;
        ServerPlayer refiner = getRefiner();
        if (refiner != null) {
            ModNetwork.sendNotification(
                    refiner,
                    Component.translatable("notification.ninecun_lingtai.alchemy_scent_title"),
                    Component.translatable("notification.ninecun_lingtai.alchemy_scent"),
                    SCENT_TOAST_DURATION_TICKS
            );
        }
    }

    @Nullable
    private ServerPlayer getRefiner() {
        if (this.refinerId == null || this.level == null || this.level.getServer() == null) {
            return null;
        }
        ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(this.refinerId);
        if (player == null
                || player.level() != this.level
                || player.distanceToSqr(
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D
                ) > 32.0D * 32.0D) {
            return null;
        }
        return player;
    }

    private void finishRefining(ServerPlayer player) {
        AlchemyRecipeDefinition recipe = this.activeRecipeId == null
                ? null
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId).orElse(null);
        if (recipe == null) {
            burnIngredients();
            return;
        }
        // 炉耐久随炉次损耗（不论成败），耗尽后须养炉修复。
        // TODO(炼丹 M5 养炉)：投喂灵材/灵石修复耐久的养炉玩法。
        this.durability = Math.max(
                0,
                getDurability() - Math.max(0, CommonConfig.ALCHEMY_FURNACE_DURABILITY_LOSS_PER_REFINE.get())
        );
        double duration = this.effectiveDuration > 0 ? this.effectiveDuration : recipe.brewingDuration();
        double harvestTiming = this.refiningTicks / duration;

        if (harvestTiming < AlchemyResultCalculator.EXPLOSION_THRESHOLD) {
            failExplosion(recipe, player, "message.ninecun_lingtai.alchemy_result_exploded_early");
            return;
        }
        if (harvestTiming < AlchemyResultCalculator.WINDOW_START) {
            failDregs(recipe, player, "message.ninecun_lingtai.alchemy_result_dregs_early");
            return;
        }
        if (harvestTiming > AlchemyResultCalculator.CHARRED_THRESHOLD) {
            failCharred(recipe, player);
            return;
        }
        if (this.feedWindow.hasMissingFeed(recipe) || !matchesRecipe(recipe)) {
            failMissingFeed(recipe, player);
            return;
        }

        boolean hasHeatCurve = !recipe.heatStages().isEmpty();
        double fireScore = AlchemyResultCalculator.calculateFireScore(
                this.hitTicks,
                this.refiningTicks,
                this.rootQuality,
                hasHeatCurve
        );
        // 悟道印记"丹火纯青"：火候评分小幅宽容乘区（经 InsightEffectUtility 单点读取，
        // 上限 1.0，不改变满分口径——最小可行接线，评分权重链不动）。
        double insightFireBonus = InsightEffectUtility.query(
                player,
                InsightEffectUtility.EFFECT_ALCHEMY_QUALITY_BONUS
        );
        if (hasHeatCurve && insightFireBonus > 0.0D) {
            fireScore = Math.min(1.0D, fireScore * (1.0D + insightFireBonus));
        }
        // 炼丹 M4：炉容差（炉性相克清零）与火源稳定度乘区同样只作用于火候分，
        // 上限 1.0——凡铁炉+凡火时两者均为 1.0/0.0，行为与旧版逐位一致。
        double furnaceTolerance = getEffectiveToleranceBonus(recipe);
        double fireSourceStability = getFireSourceStabilityMultiplier();
        if (hasHeatCurve) {
            fireScore = Math.min(
                    1.0D,
                    fireScore * (1.0D + furnaceTolerance) * fireSourceStability
            );
        }
        double rawFireScore = AlchemyResultCalculator.getRawFireScore(
                this.hitTicks,
                this.refiningTicks,
                hasHeatCurve
        );
        double feedScore = this.feedWindow.getFeedScore(recipe);
        double operationScore = AlchemyProcessUtility.combineOperationScore(
                fireScore,
                feedScore,
                !recipe.feedPlan().isEmpty()
        );
        double rawOperationScore = AlchemyProcessUtility.combineOperationScore(
                rawFireScore,
                feedScore,
                !recipe.feedPlan().isEmpty()
        );
        AlchemyBrewScore score = AlchemyResultCalculator.combine(
                operationScore,
                this.environmentScore,
                this.rootScore,
                this.soulScore,
                this.realmScore,
                recipe.baseSuccessRate()
        );
        if (player.getRandom().nextDouble() >= score.successRate()) {
            if (score.totalScore() < 0.5D) {
                failExplosion(recipe, player, "message.ninecun_lingtai.alchemy_result_exploded");
            } else {
                failDregs(recipe, player, "message.ninecun_lingtai.alchemy_result_dregs");
            }
            return;
        }

        int rankOffset = AlchemyResultCalculator.getRankOffset(score.totalScore(), rawOperationScore)
                - AlchemyResultCalculator.getOverfireRankPenalty(harvestTiming);
        if (this.feedWindow.getLateMask() != 0) {
            rankOffset = Math.min(0, rankOffset);
        }
        int resultRank = Math.max(1, Math.min(9, recipe.grade().rank() + rankOffset));
        AlchemyGrade resultGrade = new AlchemyGrade(recipe.grade().tier(), resultRank);
        // 炉性与丹方主元素相生：额外产量判定掷两次取或（扩展设计 2.1）。
        boolean bonusYield = false;
        if (this.feedWindow.getLateMask() == 0) {
            bonusYield = AlchemyResultCalculator.rollBonusYield(
                    score.totalScore(),
                    rawOperationScore,
                    player.getRandom()
            );
            if (!bonusYield && hasFurnaceElementSynergy(recipe)) {
                bonusYield = AlchemyResultCalculator.rollBonusYield(
                        score.totalScore(),
                        rawOperationScore,
                        player.getRandom()
                );
            }
        }
        int outputCount = recipe.outputCount() + (bonusYield ? 1 : 0);

        consumeIngredients(recipe);
        insertOrDropOutput(createPillOutputStack(recipe, resultGrade, outputCount));
        ModNetwork.sendNotification(
                player,
                Component.translatable("notification.ninecun_lingtai.alchemy_success_title"),
                Component.translatable(
                        "notification.ninecun_lingtai.alchemy_success",
                        recipe.name(),
                        resultGrade.getDisplayName(),
                        outputCount
                ),
                RESULT_TOAST_DURATION_TICKS
        );
        resetBrewSession();
        this.setChangedAndSynchronize();
    }

    private void failExplosion(AlchemyRecipeDefinition recipe, ServerPlayer player, String messageKey) {
        clearIngredientSlots();
        this.heat = 0;
        resetBrewSession();
        if (this.level instanceof ServerLevel serverLevel) {
            double centerX = this.worldPosition.getX() + 0.5D;
            double centerY = this.worldPosition.getY() + 1.0D;
            double centerZ = this.worldPosition.getZ() + 0.5D;
            serverLevel.explode(
                    null,
                    centerX,
                    centerY,
                    centerZ,
                    (float) recipe.failureExplosionRadius(),
                    CommonConfig.ALCHEMY_EXPLOSION_BREAKS_BLOCKS.get()
                            ? Level.ExplosionInteraction.BLOCK
                            : Level.ExplosionInteraction.NONE
            );
            for (Entity entity : serverLevel.getEntities(
                    null,
                    new AABB(this.worldPosition).inflate(recipe.failureExplosionRadius() + 1.0D)
            )) {
                if (entity instanceof LivingEntity) {
                    entity.setSecondsOnFire(3);
                }
            }
        }
        ModNetwork.sendNotification(
                player,
                Component.translatable("notification.ninecun_lingtai.alchemy_failure_title"),
                Component.translatable(messageKey),
                RESULT_TOAST_DURATION_TICKS
        );
        this.setChangedAndSynchronize();
    }

    private void failDregs(AlchemyRecipeDefinition recipe, ServerPlayer player, String messageKey) {
        consumeIngredients(recipe);
        int dregCount = Math.max(1, recipe.failureDregCount());
        insertOrDropOutput(new ItemStack(ModItems.DREGS_PILL.get(), dregCount));
        ModNetwork.sendNotification(
                player,
                Component.translatable("notification.ninecun_lingtai.alchemy_failure_title"),
                Component.translatable(messageKey, dregCount),
                RESULT_TOAST_DURATION_TICKS
        );
        resetBrewSession();
        this.setChangedAndSynchronize();
    }

    private void failMissingFeed(AlchemyRecipeDefinition recipe, ServerPlayer player) {
        clearIngredientSlots();
        int dregCount = Math.max(1, recipe.failureDregCount());
        insertOrDropOutput(new ItemStack(ModItems.DREGS_PILL.get(), dregCount));
        ModNetwork.sendNotification(
                player,
                Component.translatable("notification.ninecun_lingtai.alchemy_failure_title"),
                Component.translatable(
                        "message.ninecun_lingtai.alchemy_result_missing_feed",
                        dregCount
                ),
                RESULT_TOAST_DURATION_TICKS
        );
        resetBrewSession();
        this.setChangedAndSynchronize();
    }

    private void failCharred(AlchemyRecipeDefinition recipe, ServerPlayer player) {
        clearIngredientSlots();
        resetBrewSession();
        spawnFailureParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, 20);
        ModNetwork.sendNotification(
                player,
                Component.translatable("notification.ninecun_lingtai.alchemy_failure_title"),
                Component.translatable("message.ninecun_lingtai.alchemy_result_charred"),
                RESULT_TOAST_DURATION_TICKS
        );
        this.setChangedAndSynchronize();
    }

    private void failAshes() {
        ServerPlayer refiner = getRefiner();
        burnIngredients();
        spawnFailureParticles(ParticleTypes.ASH, 24);
        if (refiner != null) {
            ModNetwork.sendNotification(
                    refiner,
                    Component.translatable("notification.ninecun_lingtai.alchemy_failure_title"),
                    Component.translatable("message.ninecun_lingtai.alchemy_result_ashes"),
                    RESULT_TOAST_DURATION_TICKS
            );
        }
    }

    private void spawnFailureParticles(net.minecraft.core.particles.SimpleParticleType particleType, int count) {
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    particleType,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 1.5D,
                    this.worldPosition.getZ() + 0.5D,
                    count,
                    0.4D,
                    0.6D,
                    0.4D,
                    0.01D
            );
        }
    }

    private static ItemStack createPillOutputStack(
            AlchemyRecipeDefinition recipe,
            AlchemyGrade grade,
            int count
    ) {
        ItemStack outputStack = AlchemyPillUtility.createStack(
                BuiltInRegistries.ITEM.get(recipe.outputItem()),
                count,
                grade,
                recipe.primaryElement()
        );
        double pillToxin = AlchemyResultCalculator.calculateToxin(recipe, grade.rank());
        if (pillToxin > 0.0D) {
            outputStack.getOrCreateTag().putDouble(PillItem.PILL_TOXIN_TAG_KEY, pillToxin);
        }
        return outputStack;
    }

    private void insertOrDropOutput(ItemStack output) {
        ItemStack existingOutput = getItem(AlchemyFurnaceMenu.OUTPUT_SLOT);
        if (existingOutput.isEmpty()) {
            setItem(AlchemyFurnaceMenu.OUTPUT_SLOT, output);
            return;
        }
        if (ItemStack.isSameItemSameTags(existingOutput, output)
                && existingOutput.getCount() + output.getCount() <= existingOutput.getMaxStackSize()) {
            existingOutput.grow(output.getCount());
            setItem(AlchemyFurnaceMenu.OUTPUT_SLOT, existingOutput);
            return;
        }
        if (this.level != null) {
            Containers.dropItemStack(
                    this.level,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 1.0D,
                    this.worldPosition.getZ() + 0.5D,
                    output
            );
        }
    }

    private void resetBrewSession() {
        this.refining = false;
        this.activeRecipeId = null;
        this.refiningTicks = 0;
        this.hitTicks = 0;
        this.effectiveDuration = 0;
        this.environmentMultiplier = 1.0D;
        this.rootScore = 0.0D;
        this.soulScore = 0.0D;
        this.realmScore = 0.0D;
        this.environmentScore = 1.0D;
        this.rootQuality = 0.0D;
        this.scentPlanTicks = -1;
        this.scentSent = false;
        this.refinerId = null;
        releaseHeatControl();
        this.feedWindow.reset();
    }

    private void burnIngredients() {
        resetBrewSession();
        this.heat = 0;
        clearIngredientSlots();
        this.setChangedAndSynchronize();
    }

    private void updateEnvironmentCache() {
        Optional<AlchemyRecipeDefinition> recipe;
        if (this.refining && this.activeRecipeId != null) {
            recipe = AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId);
        } else if (this.selectedRecipeId != null) {
            recipe = AlchemyRecipeManager.INSTANCE.getRecipe(this.selectedRecipeId);
        } else {
            recipe = getMatchingRecipe();
        }
        ElementType primaryElement = recipe.flatMap(AlchemyRecipeDefinition::getPrimaryElement).orElse(null);
        if (primaryElement == null || this.level == null) {
            this.cachedPrimaryElementOrdinal = 0;
            this.cachedEnvironmentMultiplierX100 = 100;
            return;
        }
        double multiplier = this.refining
                ? this.environmentMultiplier
                : ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(
                        this.level,
                        this.worldPosition,
                        primaryElement
                );
        this.cachedPrimaryElementOrdinal = primaryElement.ordinal() + 1;
        this.cachedEnvironmentMultiplierX100 = (int) Math.round(multiplier * 100.0D);
    }

    private Optional<AlchemyRecipeDefinition> getMatchingRecipe() {
        return AlchemyRecipeManager.INSTANCE.getRecipes().stream()
                .filter(recipe -> recipe.feedPlan().isEmpty())
                .filter(this::matchesRecipe)
                .findFirst();
    }

    private boolean matchesInitialIngredients(AlchemyRecipeDefinition recipe) {
        for (AlchemyIngredientSlot slot : AlchemyIngredientSlot.values()) {
            ItemStack itemStack = getItem(slot.getSlotIndex());
            if (recipe.getFeedStep(slot).isPresent()) {
                if (!itemStack.isEmpty()) {
                    return false;
                }
            } else if (!matchesIngredient(slot.getSlotIndex(), recipe.getIngredient(slot))) {
                return false;
            }
        }
        return true;
    }

    private boolean canSelectWithCurrentContents(AlchemyRecipeDefinition recipe) {
        for (AlchemyIngredientSlot slot : AlchemyIngredientSlot.values()) {
            ItemStack itemStack = getItem(slot.getSlotIndex());
            if (itemStack.isEmpty()) {
                continue;
            }
            if (recipe.getFeedStep(slot).isPresent()
                    || !matchesIngredientItem(itemStack, recipe.getIngredient(slot))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesRecipe(AlchemyRecipeDefinition recipe) {
        return matchesIngredient(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT, recipe.primaryIngredient())
                && matchesIngredient(
                        AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT,
                        recipe.secondaryIngredients().get(0)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT,
                        recipe.secondaryIngredients().get(1)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT,
                        recipe.auxiliaryIngredients().get(0)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT,
                        recipe.auxiliaryIngredients().get(1)
                );
    }

    private boolean matchesIngredient(int slot, AlchemyIngredient ingredient) {
        return matchesIngredientStack(getItem(slot), ingredient);
    }

    private static boolean matchesIngredientStack(
            ItemStack itemStack,
            AlchemyIngredient ingredient
    ) {
        return itemStack.getCount() >= ingredient.count()
                && matchesIngredientItem(itemStack, ingredient);
    }

    private static boolean matchesIngredientItem(
            ItemStack itemStack,
            AlchemyIngredient ingredient
    ) {
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).equals(ingredient.item());
    }

    private boolean canAcceptOutput(AlchemyRecipeDefinition recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.outputItem());
        if (outputItem == Items.AIR) {
            return false;
        }
        ItemStack expectedOutput = createPillOutputStack(recipe, recipe.grade(), recipe.outputCount());
        ItemStack output = getItem(AlchemyFurnaceMenu.OUTPUT_SLOT);
        return output.isEmpty()
                || (ItemStack.isSameItemSameTags(output, expectedOutput)
                && output.getCount() + expectedOutput.getCount() <= output.getMaxStackSize());
    }

    private void consumeIngredients(AlchemyRecipeDefinition recipe) {
        shrinkIngredient(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT, recipe.primaryIngredient());
        shrinkIngredient(AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT, recipe.secondaryIngredients().get(0));
        shrinkIngredient(AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT, recipe.secondaryIngredients().get(1));
        shrinkIngredient(AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT, recipe.auxiliaryIngredients().get(0));
        shrinkIngredient(AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT, recipe.auxiliaryIngredients().get(1));
    }

    private void shrinkIngredient(int slot, AlchemyIngredient ingredient) {
        ItemStack itemStack = getItem(slot);
        itemStack.shrink(ingredient.count());
        setItem(slot, itemStack);
    }

    private void clearIngredientSlots() {
        for (int slot = AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT;
             slot < AlchemyFurnaceMenu.OUTPUT_SLOT;
             slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }

    public void openMenu(ServerPlayer player) {
        NetworkHooks.openScreen(player, this, this.worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + ModMain.MOD_ID + ".alchemy_furnace");
    }

    @Nullable
    @Override
    public AlchemyFurnaceMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new AlchemyFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (this.refining
                && slot >= AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT
                && slot < AlchemyFurnaceMenu.OUTPUT_SLOT) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            setChangedAndSynchronize();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.items.set(slot, itemStack);
        if (!itemStack.isEmpty() && itemStack.getCount() > getMaxStackSize()) {
            itemStack.setCount(getMaxStackSize());
        }
        captureDelayedFeed(slot, itemStack);
        setChangedAndSynchronize();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D
                ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        clearAllItemsWithoutUpdates();
        setChangedAndSynchronize();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        if (slot == AlchemyFurnaceMenu.OUTPUT_SLOT
                || slot < AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT
                || slot >= AlchemyFurnaceMenu.OUTPUT_SLOT) {
            return false;
        }
        AlchemyRecipeDefinition selectedRecipe = null;
        ResourceLocation recipeId = this.refining ? this.activeRecipeId : this.selectedRecipeId;
        if (recipeId != null) {
            selectedRecipe = AlchemyRecipeManager.INSTANCE.getRecipe(recipeId).orElse(null);
        }
        if (selectedRecipe != null) {
            AlchemyIngredientSlot ingredientSlot = AlchemyIngredientSlot.fromSlotIndex(slot);
            if (!matchesIngredientItem(itemStack, selectedRecipe.getIngredient(ingredientSlot))) {
                return false;
            }
            if (!this.refining) {
                return selectedRecipe.getFeedStep(ingredientSlot).isEmpty();
            }
            int slotMask = 1 << slot;
            return this.feedWindow.canFeed(slotMask);
        }
        if (this.refining) {
            return false;
        }
        return AlchemyRecipeManager.INSTANCE.getRecipes().stream()
                .anyMatch(recipe -> getIngredientForSlot(recipe, slot)
                        .map(ingredient -> BuiltInRegistries.ITEM.getKey(itemStack.getItem()).equals(ingredient.item()))
                        .orElse(false));
    }

    private static Optional<AlchemyIngredient> getIngredientForSlot(AlchemyRecipeDefinition recipe, int slot) {
        return switch (slot) {
            case AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT -> Optional.of(recipe.primaryIngredient());
            case AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT ->
                    Optional.of(recipe.secondaryIngredients().get(0));
            case AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT ->
                    Optional.of(recipe.secondaryIngredients().get(1));
            case AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT ->
                    Optional.of(recipe.auxiliaryIngredients().get(0));
            case AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT ->
                    Optional.of(recipe.auxiliaryIngredients().get(1));
            default -> Optional.empty();
        };
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case HEAT_DATA_INDEX -> this.heat;
            case MAXIMUM_HEAT_DATA_INDEX -> getMaxHeat();
            case REFINING_DATA_INDEX -> this.refining ? 1 : 0;
            case ENVIRONMENT_MULTIPLIER_X100_DATA_INDEX -> this.cachedEnvironmentMultiplierX100;
            case PRIMARY_ELEMENT_DATA_INDEX -> this.cachedPrimaryElementOrdinal;
            case OPEN_FEED_MASK_DATA_INDEX -> this.feedWindow.getOpenMask();
            case MISSED_FEED_MASK_DATA_INDEX -> this.feedWindow.getMissedMask();
            case FED_FEED_MASK_DATA_INDEX -> this.feedWindow.getFedMask();
            case LATE_FEED_MASK_DATA_INDEX -> this.feedWindow.getLateMask();
            case HEAT_CONTROL_MODE_DATA_INDEX -> this.heatControlMode.ordinal();
            case PLANNED_FEED_MASK_DATA_INDEX -> this.feedWindow.getPlannedMask();
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case HEAT_DATA_INDEX -> this.heat = value;
            case REFINING_DATA_INDEX -> this.refining = value != 0;
            case ENVIRONMENT_MULTIPLIER_X100_DATA_INDEX -> this.cachedEnvironmentMultiplierX100 = value;
            case PRIMARY_ELEMENT_DATA_INDEX -> this.cachedPrimaryElementOrdinal = value;
            case OPEN_FEED_MASK_DATA_INDEX -> this.feedWindow.setOpenMask(value);
            case MISSED_FEED_MASK_DATA_INDEX -> this.feedWindow.setMissedMask(value);
            case FED_FEED_MASK_DATA_INDEX -> this.feedWindow.setFedMask(value);
            case LATE_FEED_MASK_DATA_INDEX -> this.feedWindow.setLateMask(value);
            case HEAT_CONTROL_MODE_DATA_INDEX -> this.heatControlMode =
                    value >= 0 && value < HeatControlMode.values().length
                            ? HeatControlMode.values()[value]
                            : HeatControlMode.NONE;
            case PLANNED_FEED_MASK_DATA_INDEX -> this.feedWindow.setPlannedMask(value);
            default -> {
            }
        }
    }

    @Override
    public int getCount() {
        return DATA_COUNT;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putBoolean(REFINING_KEY, this.refining);
        tag.putInt(REFINING_TICKS_KEY, this.refiningTicks);
        tag.putInt(HEAT_KEY, this.heat);
        tag.putInt(HIT_TICKS_KEY, this.hitTicks);
        tag.putInt(EFFECTIVE_DURATION_KEY, this.effectiveDuration);
        tag.putDouble(ENVIRONMENT_MULTIPLIER_KEY, this.environmentMultiplier);
        tag.putDouble(ROOT_SCORE_KEY, this.rootScore);
        tag.putDouble(SOUL_SCORE_KEY, this.soulScore);
        tag.putDouble(REALM_SCORE_KEY, this.realmScore);
        tag.putDouble(ENVIRONMENT_SCORE_KEY, this.environmentScore);
        tag.putDouble(ROOT_QUALITY_KEY, this.rootQuality);
        tag.putInt(SCENT_PLAN_KEY, this.scentPlanTicks);
        tag.putBoolean(SCENT_SENT_KEY, this.scentSent);
        if (this.refinerId != null) {
            tag.putUUID(REFINER_KEY, this.refinerId);
        }
        tag.putInt(DURABILITY_KEY, getDurability());
        tag.putInt(FIRE_SOURCE_KEY, this.fireSource.ordinal());
        AlchemyFurnaceSessionState.save(
                tag,
                new AlchemyFurnaceSessionState.PersistedState(
                        this.selectedRecipeId,
                        this.activeRecipeId,
                        this.feedWindow.getFedMask(),
                        this.feedWindow.getMissedMask(),
                        this.feedWindow.getLateMask(),
                        this.feedWindow.getScoreTotal(),
                        this.feedWindow.getScoreCount(),
                        this.feedWindow.getPlannedMask()
                )
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearAllItemsWithoutUpdates();
        ContainerHelper.loadAllItems(tag, this.items);
        this.refining = tag.getBoolean(REFINING_KEY);
        AlchemyFurnaceSessionState.PersistedState sessionState =
                AlchemyFurnaceSessionState.load(tag);
        this.selectedRecipeId = sessionState.selectedRecipeId();
        this.activeRecipeId = sessionState.activeRecipeId();
        this.refiningTicks = Math.max(
                0,
                tag.contains(REFINING_TICKS_KEY)
                        ? tag.getInt(REFINING_TICKS_KEY)
                        : tag.getInt(LEGACY_BREW_PROGRESS_KEY)
        );
        this.heat = Math.max(0, tag.getInt(HEAT_KEY));
        this.hitTicks = Math.max(0, tag.getInt(HIT_TICKS_KEY));
        this.effectiveDuration = Math.max(0, tag.getInt(EFFECTIVE_DURATION_KEY));
        this.environmentMultiplier = tag.contains(ENVIRONMENT_MULTIPLIER_KEY)
                ? tag.getDouble(ENVIRONMENT_MULTIPLIER_KEY)
                : 1.0D;
        this.rootScore = tag.getDouble(ROOT_SCORE_KEY);
        this.soulScore = tag.getDouble(SOUL_SCORE_KEY);
        this.realmScore = tag.getDouble(REALM_SCORE_KEY);
        this.environmentScore = tag.contains(ENVIRONMENT_SCORE_KEY)
                ? tag.getDouble(ENVIRONMENT_SCORE_KEY)
                : 1.0D;
        this.rootQuality = tag.getDouble(ROOT_QUALITY_KEY);
        this.scentPlanTicks = tag.contains(SCENT_PLAN_KEY) ? tag.getInt(SCENT_PLAN_KEY) : -1;
        this.scentSent = tag.getBoolean(SCENT_SENT_KEY);
        this.refinerId = tag.hasUUID(REFINER_KEY) ? tag.getUUID(REFINER_KEY) : null;
        this.durability = tag.contains(DURABILITY_KEY) ? Math.max(0, tag.getInt(DURABILITY_KEY)) : -1;
        this.fireSource = FireSource.fromOrdinal(tag.getInt(FIRE_SOURCE_KEY));
        this.feedWindow.setFedMask(sessionState.fedFeedMask());
        this.feedWindow.setMissedMask(sessionState.missedFeedMask());
        this.feedWindow.setLateMask(sessionState.lateFeedMask());
        this.feedWindow.restoreScores(
                sessionState.feedScoreTotal(),
                sessionState.feedScoreCount()
        );
        this.feedWindow.setPlannedMask(sessionState.plannedFeedMask());
        this.feedWindow.setOpenMask(0);
        releaseHeatControl();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putBoolean(REFINING_KEY, this.refining);
        tag.putInt(HEAT_KEY, this.heat);
        if (this.selectedRecipeId != null) {
            tag.putString(
                    AlchemyFurnaceSessionState.SELECTED_RECIPE_KEY,
                    this.selectedRecipeId.toString()
            );
        }
        tag.putInt(
                AlchemyFurnaceSessionState.PLANNED_FEED_MASK_KEY,
                this.feedWindow.getPlannedMask()
        );
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection connection,
            ClientboundBlockEntityDataPacket packet
    ) {
        load(packet.getTag());
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                this.worldPosition.getX() - 1.0625D,
                this.worldPosition.getY(),
                this.worldPosition.getZ() - 1.0625D,
                this.worldPosition.getX() + 2.0625D,
                this.worldPosition.getY() + 3.0D,
                this.worldPosition.getZ() + 2.0625D
        );
    }

    private void setChangedAndSynchronize() {
        setChanged();
        synchronizeToClients();
    }

    private void synchronizeToClients() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(
                    this.worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    private void clearAllItemsWithoutUpdates() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
    }
}
