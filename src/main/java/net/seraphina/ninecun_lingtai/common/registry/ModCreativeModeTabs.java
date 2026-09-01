package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.item.AlchemyRecipeCreativeTabCache;
import net.seraphina.ninecun_lingtai.common.item.BookCreativeTabCache;
import net.seraphina.ninecun_lingtai.common.item.TechniqueCreativeTabCache;
import net.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualItemUtility;
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
                        output.accept(ModItems.RED_COPPER_ALCHEMY_FURNACE.get());
                        output.accept(ModItems.COLD_JADE_ALCHEMY_FURNACE.get());
                        output.accept(ModItems.DARK_CRYSTAL_ALCHEMY_FURNACE.get());
                        output.accept(ModItems.NINE_DRAGON_ALCHEMY_FURNACE.get());
                        output.accept(ModItems.FIRE_LINGZHI.get());
                        output.accept(ModItems.MOON_DEW_ORCHID.get());
                        output.accept(ModItems.EARTH_MARROW_GINSENG.get());
                        output.accept(ModItems.JADE_MIND_FLOWER.get());
                        ModItems.getPillItems().forEach(
                                pill -> output.acceptAll(AlchemyPillUtility.getCreativeItemStacks(pill))
                        );
                        output.accept(ModItems.DAO_SCROLL.get());
                        output.acceptAll(SpiritualItemUtility.getCreativeItemStacks(ModItems.HAN_SWORD.get()));
                        output.accept(ModItems.STORAGE_RING.get());
                        output.acceptAll(SpiritualStoneUtility.getCreativeItemStacks());
                        output.acceptAll(TechniqueCreativeTabCache.getItemStacks());
                        output.acceptAll(AlchemyRecipeCreativeTabCache.getItemStacks());
                        output.acceptAll(BookCreativeTabCache.getItemStacks());
                        output.accept(ModFormationContent.FORMATION_DISC_ITEM.get());
                        output.accept(ModFormationContent.FORMATION_FLAG_METAL_ITEM.get());
                        output.accept(ModFormationContent.FORMATION_FLAG_WATER_ITEM.get());
                        output.accept(ModFormationContent.FORMATION_FLAG_WOOD_ITEM.get());
                        output.accept(ModFormationContent.FORMATION_FLAG_FIRE_ITEM.get());
                        output.accept(ModFormationContent.FORMATION_FLAG_EARTH_ITEM.get());
                    })
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
