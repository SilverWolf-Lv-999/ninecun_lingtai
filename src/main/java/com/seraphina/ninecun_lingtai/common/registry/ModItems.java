package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.item.SpiritualStoneItem;
import com.seraphina.ninecun_lingtai.common.item.TechniqueItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ModMain.MOD_ID
    );

    public static final RegistryObject<Item> JADE_SLIP = ITEMS.register(
            "jade_slip",
            () -> new TechniqueItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SPIRITUAL_STONE = ITEMS.register(
            "spiritual_stone",
            () -> new SpiritualStoneItem(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> SPIRITUAL_STONE_ORE = ITEMS.register(
            "spiritual_stone_ore",
            () -> new BlockItem(ModBlocks.SPIRITUAL_STONE_ORE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> DEEPSLATE_SPIRITUAL_STONE_ORE = ITEMS.register(
            "deepslate_spiritual_stone_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_SPIRITUAL_STONE_ORE.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
