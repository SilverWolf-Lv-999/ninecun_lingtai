package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.item.AlchemyRecipeItem;
import net.seraphina.ninecun_lingtai.common.item.DaoScrollItem;
import net.seraphina.ninecun_lingtai.common.item.DregsPillItem;
import net.seraphina.ninecun_lingtai.common.item.HanSwordItem;
import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;
import net.seraphina.ninecun_lingtai.common.item.PillItem;
import net.seraphina.ninecun_lingtai.common.item.SpiritualStoneItem;
import net.seraphina.ninecun_lingtai.common.item.StorageRingItem;
import net.seraphina.ninecun_lingtai.common.item.StyledBlockItem;
import net.seraphina.ninecun_lingtai.common.item.TechniqueItem;
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

    public static final RegistryObject<Item> QI_GATHERING_POWDER = registerPill(
            "qi_gathering_powder",
            0x86C166
    );
    public static final RegistryObject<Item> BREATH_CONDENSING_PILL = registerPill(
            "breath_condensing_pill",
            0xA8C256
    );
    public static final RegistryObject<Item> CLEANSING_DUST_PILL = registerPill(
            "cleansing_dust_pill",
            0xE8F0E0
    );
    public static final RegistryObject<Item> GRAIN_AVOIDANCE_PILL = registerPill(
            "grain_avoidance_pill",
            0xD8B75A
    );

    public static final RegistryObject<Item> JADE_MARROW_PILL = registerPill(
            "jade_marrow_pill",
            0xB48FD8
    );
    public static final RegistryObject<Item> FOUNDATION_PILL = registerPill(
            "foundation_pill",
            0xC8C8D8
    );
    public static final RegistryObject<Item> SPIRIT_DEW_PILL = registerPill(
            "spirit_dew_pill",
            0x6FAED9
    );
    public static final RegistryObject<Item> CALM_MIND_PILL = registerPill(
            "calm_mind_pill",
            0xE0784A
    );
    public static final RegistryObject<Item> VEIN_EXTENSION_PILL = registerPill(
            "vein_extension_pill",
            0xE8C84A
    );

    public static final RegistryObject<Item> GOLDEN_WOUND_PILL = registerPill(
            "golden_wound_pill",
            0xF0D060
    );
    public static final RegistryObject<Item> CORE_FORMATION_PILL = registerPill(
            "core_formation_pill",
            0x86D8D0
    );
    public static final RegistryObject<Item> ORIGIN_RETURN_PILL = registerPill(
            "origin_return_pill",
            0xD86050
    );
    public static final RegistryObject<Item> BODY_TEMPERING_PILL = registerPill(
            "body_tempering_pill",
            0x58A89A
    );
    public static final RegistryObject<Item> SPIRIT_CONDENSING_PILL = registerPill(
            "spirit_condensing_pill",
            0x3E5C68
    );

    public static final RegistryObject<Item> DREGS_PILL = registerDregs();

    public static final RegistryObject<Item> SPIRITUAL_STONE_ORE = ITEMS.register(
            "spiritual_stone_ore",
            () -> new StyledBlockItem(
                    ModBlocks.SPIRITUAL_STONE_ORE.get(),
                    new Item.Properties(),
                    ItemFontStyle.SPIRITUAL_STONE_ORE
            )
    );

    public static final RegistryObject<Item> DEEPSLATE_SPIRITUAL_STONE_ORE = ITEMS.register(
            "deepslate_spiritual_stone_ore",
            () -> new StyledBlockItem(
                    ModBlocks.DEEPSLATE_SPIRITUAL_STONE_ORE.get(),
                    new Item.Properties(),
                    ItemFontStyle.DEEPSLATE_SPIRITUAL_STONE_ORE
            )
    );

    public static final RegistryObject<Item> ALCHEMY_FURNACE = ITEMS.register(
            "alchemy_furnace",
            () -> new StyledBlockItem(
                    ModBlocks.ALCHEMY_FURNACE.get(),
                    new Item.Properties(),
                    ItemFontStyle.ALCHEMY_FURNACE
            )
    );

    public static final RegistryObject<Item> FIRE_LINGZHI = ITEMS.register(
            "fire_lingzhi",
            () -> new StyledBlockItem(
                    ModBlocks.FIRE_LINGZHI.get(),
                    new Item.Properties(),
                    ItemFontStyle.FIRE_LINGZHI
            )
    );

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

    private static RegistryObject<Item> registerDregs() {
        RegistryObject<Item> dregs = ITEMS.register(
                "dregs_pill",
                () -> new DregsPillItem(new Item.Properties().stacksTo(64), 0x6B5D4F)
        );
        COLOR_ITEMS.add(dregs);
        return dregs;
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
