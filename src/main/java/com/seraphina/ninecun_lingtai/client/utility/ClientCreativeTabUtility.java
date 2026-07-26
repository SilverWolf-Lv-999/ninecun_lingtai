package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ClientCreativeTabUtility {
    private ClientCreativeTabUtility() {
    }

    public static void rebuildCreativeTabs() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (CreativeModeTabs.tryRebuildTabContents(
                minecraft.level.enabledFeatures(),
                minecraft.player.hasPermissions(2),
                minecraft.level.registryAccess()
        )) {
            return;
        }
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                minecraft.level.enabledFeatures(),
                minecraft.player.hasPermissions(2),
                minecraft.level.registryAccess()
        );
        ModCreativeModeTabs.NINECUN_LINGTAI.get().buildContents(parameters);
        CreativeModeTabs.searchTab().buildContents(parameters);
    }
}
