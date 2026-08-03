package com.seraphina.ninecun_lingtai.common.menu;

import com.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import com.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import com.seraphina.ninecun_lingtai.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AlchemyFurnaceMenu extends AbstractContainerMenu {
    public static final int MAIN_INGREDIENT_SLOT = 0;
    public static final int FIRST_SECONDARY_INGREDIENT_SLOT = 1;
    public static final int SECOND_SECONDARY_INGREDIENT_SLOT = 2;
    public static final int FIRST_AUXILIARY_INGREDIENT_SLOT = 3;
    public static final int SECOND_AUXILIARY_INGREDIENT_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;
    public static final int FURNACE_SLOT_COUNT = 6;

    private static final int PLAYER_INVENTORY_START = FURNACE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container furnace;
    private final ContainerData furnaceData;
    private final BlockPos blockPosition;
    private final Level level;

    public AlchemyFurnaceMenu(int containerId, Inventory playerInventory, AlchemyFurnaceBlockEntity furnace) {
        this(
                containerId,
                playerInventory,
                furnace,
                furnace,
                furnace.getBlockPos(),
                furnace.getLevel()
        );
    }

    private AlchemyFurnaceMenu(
            int containerId,
            Inventory playerInventory,
            Container furnace,
            ContainerData furnaceData,
            BlockPos blockPosition,
            Level level
    ) {
        super(ModMenuTypes.ALCHEMY_FURNACE.get(), containerId);
        checkContainerSize(furnace, FURNACE_SLOT_COUNT);
        checkContainerDataCount(furnaceData, AlchemyFurnaceBlockEntity.DATA_COUNT);
        this.furnace = furnace;
        this.furnaceData = furnaceData;
        this.blockPosition = blockPosition;
        this.level = level;

        addSlot(new IngredientSlot(furnace, MAIN_INGREDIENT_SLOT, 145, 23));
        addSlot(new IngredientSlot(furnace, FIRST_SECONDARY_INGREDIENT_SLOT, 81, 67));
        addSlot(new IngredientSlot(furnace, SECOND_SECONDARY_INGREDIENT_SLOT, 209, 67));
        addSlot(new IngredientSlot(furnace, FIRST_AUXILIARY_INGREDIENT_SLOT, 81, 118));
        addSlot(new IngredientSlot(furnace, SECOND_AUXILIARY_INGREDIENT_SLOT, 209, 118));
        addSlot(new OutputSlot(furnace, OUTPUT_SLOT, 145, 88));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        73 + column * 18,
                        166 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 73 + column * 18, 224));
        }
        addDataSlots(furnaceData);
    }

    public static AlchemyFurnaceMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos blockPosition = buffer.readBlockPos();
        Level level = playerInventory.player.level();
        Container furnace = level.getBlockEntity(blockPosition) instanceof AlchemyFurnaceBlockEntity blockEntity
                ? blockEntity
                : new SimpleContainer(FURNACE_SLOT_COUNT);
        ContainerData furnaceData = furnace instanceof AlchemyFurnaceBlockEntity blockEntity
                ? blockEntity
                : new SimpleContainerData(AlchemyFurnaceBlockEntity.DATA_COUNT);
        return new AlchemyFurnaceMenu(
                containerId,
                playerInventory,
                furnace,
                furnaceData,
                blockPosition,
                level
        );
    }

    public BlockPos getBlockPosition() {
        return this.blockPosition;
    }

    public int getHeat() {
        return this.furnaceData.get(AlchemyFurnaceBlockEntity.HEAT_DATA_INDEX);
    }

    public int getMaximumHeat() {
        return this.furnaceData.get(AlchemyFurnaceBlockEntity.MAXIMUM_HEAT_DATA_INDEX);
    }

    public boolean isRefining() {
        return this.furnaceData.get(AlchemyFurnaceBlockEntity.REFINING_DATA_INDEX) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level() == this.level
                && this.level.getBlockState(this.blockPosition).is(ModBlocks.ALCHEMY_FURNACE.get())
                && player.distanceToSqr(
                        this.blockPosition.getX() + 0.5D,
                        this.blockPosition.getY() + 0.5D,
                        this.blockPosition.getZ() + 0.5D
                ) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();
        if (slotIndex < FURNACE_SLOT_COUNT) {
            if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(slotStack, MAIN_INGREDIENT_SLOT, OUTPUT_SLOT, false)) {
            if (slotIndex < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (slotStack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, slotStack);
        return result;
    }

    private final class IngredientSlot extends Slot {
        private IngredientSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return !isRefining() && furnace.canPlaceItem(this.getSlotIndex(), itemStack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isRefining();
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return false;
        }
    }
}
