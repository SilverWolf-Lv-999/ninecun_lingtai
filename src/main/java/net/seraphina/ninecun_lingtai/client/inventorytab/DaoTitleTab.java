package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.tab.Tab;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.seraphina.ninecun_lingtai.client.player.ClientDaoTitleManager;
import net.seraphina.ninecun_lingtai.client.screen.DaoTitleScreen;

public final class DaoTitleTab extends Tab {
    public DaoTitleTab() {
        super(new ItemStack(Items.NAME_TAG));
    }

    @Override
    public void open() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ClientDaoTitleManager.beginOpen();
        Minecraft.getInstance().setScreen(new DaoTitleScreen());
    }

    @Override
    public boolean shouldBeRemoved() {
        return Minecraft.getInstance().player == null;
    }

    @Override
    public Component getHoverText() {
        return Component.translatable("inventory_tab.ninecun_lingtai.dao_title");
    }

    @Override
    public int getPriority() {
        return 86;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DaoTitleTab;
    }

    @Override
    public int hashCode() {
        return DaoTitleTab.class.hashCode();
    }
}
