package com.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.tab.Tab;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.client.screen.AlchemyRecipeLibraryScreen;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AlchemyRecipeLibraryTab extends Tab {
    public AlchemyRecipeLibraryTab() {
        super(new ItemStack(ModItems.ALCHEMY_RECIPE.get()));
    }

    @Override
    public void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ClientAlchemyRecipeManager.beginOpenLibrary();
        minecraft.setScreen(new AlchemyRecipeLibraryScreen());
    }

    @Override
    public boolean shouldBeRemoved() {
        return Minecraft.getInstance().player == null;
    }

    @Override
    public Component getHoverText() {
        return Component.translatable("inventory_tab.ninecun_lingtai.alchemy_recipes");
    }

    @Override
    public int getPriority() {
        return 87;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AlchemyRecipeLibraryTab;
    }

    @Override
    public int hashCode() {
        return AlchemyRecipeLibraryTab.class.hashCode();
    }
}
