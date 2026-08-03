package com.seraphina.ninecun_lingtai.common.block.entity;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import com.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
import com.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;

public final class AlchemyFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity, Container, MenuProvider, ContainerData {
    public static final int BREW_PROGRESS_DATA_INDEX = 0;
    public static final int BREW_DURATION_DATA_INDEX = 1;
    public static final int HEAT_DATA_INDEX = 2;
    public static final int MAXIMUM_HEAT_DATA_INDEX = 3;
    public static final int REFINING_DATA_INDEX = 4;
    public static final int DATA_COUNT = 5;

    private static final int MAXIMUM_HEAT = 600;
    private static final String REFINING_KEY = "Refining";
    private static final String ACTIVE_RECIPE_KEY = "ActiveRecipe";
    private static final String BREW_PROGRESS_KEY = "BrewProgress";
    private static final String BREW_DURATION_KEY = "BrewDuration";
    private static final String HEAT_KEY = "Heat";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> items = NonNullList.withSize(
            AlchemyFurnaceMenu.FURNACE_SLOT_COUNT,
            ItemStack.EMPTY
    );

    private boolean refining;
    @Nullable
    private ResourceLocation activeRecipeId;
    private int brewProgress;
    private int brewDuration;
    private int heat;

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
        if (!furnace.refining) {
            return;
        }
        if (furnace.heat <= 0) {
            furnace.burnIngredients();
            return;
        }

        furnace.heat--;
        furnace.brewProgress++;
        if (furnace.brewProgress >= furnace.brewDuration) {
            furnace.finishRefining();
        }
        furnace.setChangedAndSynchronize();
    }

    public void handleAction(ServerPlayer player, AlchemyFurnaceActionPacket.Action action) {
        if (this.refining) {
            stopRefining();
        } else {
            startRefining(player);
        }
    }

    public void ignite(ServerPlayer player, InteractionHand interactionHand) {
        ItemStack ignitionTool = player.getItemInHand(interactionHand);
        this.heat = MAXIMUM_HEAT;
        ignitionTool.hurtAndBreak(1, player, ignored -> player.broadcastBreakEvent(interactionHand));
        this.setChangedAndSynchronize();
    }

    private void startRefining(ServerPlayer player) {
        if (this.heat <= 0) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_requires_ignition"),
                    true
            );
            return;
        }
        Optional<AlchemyRecipeDefinition> matchedRecipe = getMatchingRecipe();
        if (matchedRecipe.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_invalid_ingredients"),
                    true
            );
            return;
        }
        AlchemyRecipeDefinition recipe = matchedRecipe.get();
        boolean meetsMinimumRefiningRealm = PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal() >= recipe.minimumRefiningRealm().ordinal())
                .orElse(false);
        if (!meetsMinimumRefiningRealm) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.alchemy_realm_restricted",
                            recipe.getMinimumRefiningRealmName()
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

        this.refining = true;
        this.activeRecipeId = recipe.id();
        this.brewProgress = 0;
        this.brewDuration = recipe.brewingDuration();
        this.setChangedAndSynchronize();
    }

    private void stopRefining() {
        if (this.level == null) {
            return;
        }
        this.refining = false;
        this.activeRecipeId = null;
        this.brewProgress = 0;
        this.brewDuration = 0;
        this.heat = 0;
        clearIngredientSlots();
        this.setChangedAndSynchronize();
        this.level.explode(
                null,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D,
                1.5F,
                Level.ExplosionInteraction.BLOCK
        );
    }

    private void finishRefining() {
        AlchemyRecipeDefinition recipe = this.activeRecipeId == null
                ? null
                : AlchemyRecipeManager.INSTANCE.getRecipe(this.activeRecipeId).orElse(null);
        if (recipe == null || !matchesRecipe(recipe) || !canAcceptOutput(recipe)) {
            burnIngredients();
            return;
        }

        consumeIngredients(recipe);
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.outputItem());
        ItemStack output = new ItemStack(outputItem, recipe.outputCount());
        ItemStack previousOutput = getItem(AlchemyFurnaceMenu.OUTPUT_SLOT);
        if (previousOutput.isEmpty()) {
            setItem(AlchemyFurnaceMenu.OUTPUT_SLOT, output);
        } else {
            previousOutput.grow(output.getCount());
            setItem(AlchemyFurnaceMenu.OUTPUT_SLOT, previousOutput);
        }
        this.refining = false;
        this.activeRecipeId = null;
        this.brewProgress = 0;
        this.brewDuration = 0;
    }

    private void burnIngredients() {
        this.refining = false;
        this.activeRecipeId = null;
        this.brewProgress = 0;
        this.brewDuration = 0;
        this.heat = 0;
        clearIngredientSlots();
        this.setChangedAndSynchronize();
    }

    private Optional<AlchemyRecipeDefinition> getMatchingRecipe() {
        return AlchemyRecipeManager.INSTANCE.getRecipes().stream()
                .filter(this::matchesRecipe)
                .findFirst();
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
        ItemStack itemStack = getItem(slot);
        return itemStack.getCount() >= ingredient.count()
                && BuiltInRegistries.ITEM.getKey(itemStack.getItem()).equals(ingredient.item());
    }

    private boolean canAcceptOutput(AlchemyRecipeDefinition recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.outputItem());
        if (outputItem == Items.AIR) {
            return false;
        }
        ItemStack output = getItem(AlchemyFurnaceMenu.OUTPUT_SLOT);
        return output.isEmpty()
                || (output.is(outputItem)
                && output.getCount() + recipe.outputCount() <= output.getMaxStackSize());
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
        if (this.refining || slot == AlchemyFurnaceMenu.OUTPUT_SLOT) {
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
            case BREW_PROGRESS_DATA_INDEX -> this.brewProgress;
            case BREW_DURATION_DATA_INDEX -> this.brewDuration;
            case HEAT_DATA_INDEX -> this.heat;
            case MAXIMUM_HEAT_DATA_INDEX -> MAXIMUM_HEAT;
            case REFINING_DATA_INDEX -> this.refining ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case BREW_PROGRESS_DATA_INDEX -> this.brewProgress = value;
            case BREW_DURATION_DATA_INDEX -> this.brewDuration = value;
            case HEAT_DATA_INDEX -> this.heat = value;
            case REFINING_DATA_INDEX -> this.refining = value != 0;
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
        if (this.activeRecipeId != null) {
            tag.putString(ACTIVE_RECIPE_KEY, this.activeRecipeId.toString());
        }
        tag.putInt(BREW_PROGRESS_KEY, this.brewProgress);
        tag.putInt(BREW_DURATION_KEY, this.brewDuration);
        tag.putInt(HEAT_KEY, this.heat);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearAllItemsWithoutUpdates();
        ContainerHelper.loadAllItems(tag, this.items);
        this.refining = tag.getBoolean(REFINING_KEY);
        this.activeRecipeId = tag.contains(ACTIVE_RECIPE_KEY)
                ? ResourceLocation.tryParse(tag.getString(ACTIVE_RECIPE_KEY))
                : null;
        this.brewProgress = Math.max(0, tag.getInt(BREW_PROGRESS_KEY));
        this.brewDuration = Math.max(0, tag.getInt(BREW_DURATION_KEY));
        this.heat = Math.max(0, Math.min(MAXIMUM_HEAT, tag.getInt(HEAT_KEY)));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
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
