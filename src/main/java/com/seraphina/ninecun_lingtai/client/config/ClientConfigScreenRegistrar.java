package com.seraphina.ninecun_lingtai.client.config;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.screen.CommonConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;

public final class ClientConfigScreenRegistrar {
    private ClientConfigScreenRegistrar() {
    }

    public static void register() {
        ModList.get()
                .getModContainerById(ModMain.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing mod container for " + ModMain.MOD_ID))
                .registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory(CommonConfigScreen::new)
                );
    }
}
