package com.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.provider.TabProvider;
import com.kqp.inventorytabs.tabs.tab.Tab;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.List;

public final class PlayerValueControlTabProvider implements TabProvider {
    @Override
    public void addAvailableTabs(AbstractClientPlayer player, List<Tab> tabs) {
        for (Tab tab : tabs) {
            if (tab instanceof PlayerValueControlTab) {
                return;
            }
        }
        tabs.add(new PlayerValueControlTab());
    }
}
