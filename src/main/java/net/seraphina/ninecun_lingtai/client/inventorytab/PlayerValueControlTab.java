package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.tab.Tab;
import net.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import net.seraphina.ninecun_lingtai.client.screen.PlayerValueControlScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PlayerValueControlTab extends Tab {
    public PlayerValueControlTab() {
        super(new ItemStack(Items.RECOVERY_COMPASS));
    }

    @Override
    public void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ClientPlayerValueControlManager.beginOpen();
        minecraft.setScreen(new PlayerValueControlScreen());
    }

    @Override
    public boolean shouldBeRemoved() {
        return Minecraft.getInstance().player == null;
    }

    @Override
    public Component getHoverText() {
        return Component.translatable("inventory_tab.ninecun_lingtai.value_control");
    }

    @Override
    public int getPriority() {
        return 88;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PlayerValueControlTab;
    }

    @Override
    public int hashCode() {
        return PlayerValueControlTab.class.hashCode();
    }
}
