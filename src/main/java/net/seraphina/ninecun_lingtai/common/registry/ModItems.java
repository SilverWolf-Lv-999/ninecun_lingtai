package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.item.AlchemyRecipeItem;
import net.seraphina.ninecun_lingtai.common.item.DaoScrollItem;
import net.seraphina.ninecun_lingtai.common.item.HanSwordItem;
import net.seraphina.ninecun_lingtai.common.item.PillItem;
import net.seraphina.ninecun_lingtai.common.item.SpiritualStoneItem;
import net.seraphina.ninecun_lingtai.common.item.StorageRingItem;
import net.seraphina.ninecun_lingtai.common.item.TechniqueItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ModMain.MOD_ID
    );
    private static final List<RegistryObject<Item>> COLOR_ITEMS = new ArrayList<>();
    private static final List<RegistryObject<Item>> PILL_ITEMS = new ArrayList<>();

    public static final RegistryObject<Item> JADE_SLIP = ITEMS.register(
            "jade_slip",
            () -> new TechniqueItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> ALCHEMY_RECIPE = ITEMS.register(
            "alchemy_recipe",
            () -> new AlchemyRecipeItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SPIRITUAL_STONE = ITEMS.register(
            "spiritual_stone",
            () -> new SpiritualStoneItem(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> DAO_SCROLL = ITEMS.register(
            "dao_scroll",
            () -> new DaoScrollItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> HAN_SWORD = ITEMS.register(
            "han_sword",
            HanSwordItem::new
    );

    public static final RegistryObject<Item> STORAGE_RING = ITEMS.register(
            "storage_ring",
            () -> new StorageRingItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> GRASS_RESTORATION_PILL = registerPill(
            "grass_restoration_pill",
            0x609A4A
    );

    public static final RegistryObject<Item> SPIRITUAL_STONE_ORE = ITEMS.register(
            "spiritual_stone_ore",
            () -> new BlockItem(ModBlocks.SPIRITUAL_STONE_ORE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> DEEPSLATE_SPIRITUAL_STONE_ORE = ITEMS.register(
            "deepslate_spiritual_stone_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_SPIRITUAL_STONE_ORE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> ALCHEMY_FURNACE = ITEMS.register(
            "alchemy_furnace",
            () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static Item[] getColorItems() {
        return COLOR_ITEMS.stream()
                .map(RegistryObject::get)
                .toArray(Item[]::new);
    }

    public static List<Item> getPillItems() {
        return PILL_ITEMS.stream().map(RegistryObject::get).toList();
    }

    private static RegistryObject<Item> registerPill(String name, int color) {
        RegistryObject<Item> pill = ITEMS.register(
                name,
                () -> new PillItem(new Item.Properties().stacksTo(16), color)
        );
        COLOR_ITEMS.add(pill);
        PILL_ITEMS.add(pill);
        return pill;
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
