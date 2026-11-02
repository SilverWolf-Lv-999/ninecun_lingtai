package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.api.TabProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;

public final class DaoTitleTabRegistrar {
    private static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "dao_title");

    private DaoTitleTabRegistrar() {
    }

    public static void register() {
        TabProviderRegistry.register(PROVIDER_ID, new DaoTitleTabProvider());
    }
}
