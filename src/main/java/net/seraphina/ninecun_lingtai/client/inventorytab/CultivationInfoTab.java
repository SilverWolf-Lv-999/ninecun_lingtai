package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.tab.Tab;
import net.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import net.seraphina.ninecun_lingtai.client.screen.CultivationInfoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CultivationInfoTab extends Tab {
    public CultivationInfoTab() {
        super(new ItemStack(Items.AMETHYST_SHARD));
    }

    @Override
    public void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ClientCultivationInfoManager.beginOpen();
        minecraft.setScreen(new CultivationInfoScreen());
    }

    @Override
    public boolean shouldBeRemoved() {
        return Minecraft.getInstance().player == null;
    }

    @Override
    public Component getHoverText() {
        return Component.literal("灵台信息");
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CultivationInfoTab;
    }

    @Override
    public int hashCode() {
        return CultivationInfoTab.class.hashCode();
    }
}
