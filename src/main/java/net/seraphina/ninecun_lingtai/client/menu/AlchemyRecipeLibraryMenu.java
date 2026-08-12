package net.seraphina.ninecun_lingtai.client.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AlchemyRecipeLibraryMenu extends AbstractContainerMenu {
    public AlchemyRecipeLibraryMenu() {
        super(null, 0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
