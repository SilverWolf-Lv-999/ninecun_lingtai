package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.item.AlchemyRecipeCreativeTabCache;
import com.seraphina.ninecun_lingtai.common.item.TechniqueCreativeTabCache;
import com.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualItemUtility;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            ModMain.MOD_ID
    );

    public static final RegistryObject<CreativeModeTab> NINECUN_LINGTAI = CREATIVE_MODE_TABS.register(
            "ninecun_lingtai",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ninecun_lingtai"))
                    .icon(() -> ModItems.JADE_SLIP.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SPIRITUAL_STONE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_SPIRITUAL_STONE_ORE.get());
                        output.accept(ModItems.ALCHEMY_FURNACE.get());
                        output.accept(ModItems.GRASS_RESTORATION_PILL.get());
                        output.accept(ModItems.DAO_SCROLL.get());
                        output.acceptAll(SpiritualItemUtility.getCreativeItemStacks(ModItems.HAN_SWORD.get()));
                        output.acceptAll(SpiritualStoneUtility.getCreativeItemStacks());
                        output.acceptAll(TechniqueCreativeTabCache.getItemStacks());
                        output.acceptAll(AlchemyRecipeCreativeTabCache.getItemStacks());
                    })
                    .build()
    );

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
