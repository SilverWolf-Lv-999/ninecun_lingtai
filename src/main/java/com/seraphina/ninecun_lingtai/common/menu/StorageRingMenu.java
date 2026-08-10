package com.seraphina.ninecun_lingtai.common.menu;

import com.seraphina.ninecun_lingtai.common.registry.ModMenuTypes;
import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class StorageRingMenu extends AbstractContainerMenu {
    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 256;
    public static final int VIEWPORT_LEFT = 5;
    public static final int VIEWPORT_TOP = 5;
    public static final int VIEWPORT_WIDTH = 246;
    public static final int VIEWPORT_HEIGHT = 141;
    public static final int PLAYER_INVENTORY_TOP = 155;
    public static final int HOTBAR_TOP = 213;

    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_LEFT = 48;
    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final UUID ringId;

    public StorageRingMenu(int containerId, Inventory playerInventory, UUID ringId) {
        super(ModMenuTypes.STORAGE_RING.get(), containerId);
        this.ringId = ringId;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_LEFT + column * SLOT_SIZE,
                        PLAYER_INVENTORY_TOP + row * SLOT_SIZE
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    playerInventory,
                    column,
                    PLAYER_INVENTORY_LEFT + column * SLOT_SIZE,
                    HOTBAR_TOP
            ));
        }
    }

    public static StorageRingMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        return new StorageRingMenu(containerId, playerInventory, buffer.readUUID());
    }

    public UUID getRingId() {
        return this.ringId;
    }

    @Override
    public boolean stillValid(Player player) {
        return RingStorageManager.isSessionActive(player, this.ringId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();
        if (slotIndex < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        RingStorageManager.onRingMenuRemoved(player, this.ringId);
    }
}
