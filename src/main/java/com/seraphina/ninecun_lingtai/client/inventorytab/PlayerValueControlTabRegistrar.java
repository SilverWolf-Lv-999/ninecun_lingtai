package com.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.api.TabProviderRegistry;
import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.resources.ResourceLocation;

public final class PlayerValueControlTabRegistrar {
    private static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "player_value_control"
    );

    private PlayerValueControlTabRegistrar() {
    }

    public static void register() {
        TabProviderRegistry.register(PROVIDER_ID, new PlayerValueControlTabProvider());
    }
}
