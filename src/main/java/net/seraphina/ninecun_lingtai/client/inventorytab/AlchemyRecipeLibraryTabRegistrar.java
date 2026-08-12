package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.api.TabProviderRegistry;
import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.resources.ResourceLocation;

public final class AlchemyRecipeLibraryTabRegistrar {
    private static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "alchemy_recipe_library"
    );

    private AlchemyRecipeLibraryTabRegistrar() {
    }

    public static void register() {
        TabProviderRegistry.register(PROVIDER_ID, new AlchemyRecipeLibraryTabProvider());
    }
}
