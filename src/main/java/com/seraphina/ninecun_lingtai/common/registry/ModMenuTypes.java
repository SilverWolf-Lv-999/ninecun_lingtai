package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import com.seraphina.ninecun_lingtai.common.menu.StorageRingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES,
            ModMain.MOD_ID
    );

    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> ALCHEMY_FURNACE = MENU_TYPES.register(
            "alchemy_furnace",
            () -> IForgeMenuType.create(AlchemyFurnaceMenu::fromNetwork)
    );

    public static final RegistryObject<MenuType<StorageRingMenu>> STORAGE_RING = MENU_TYPES.register(
            "storage_ring",
            () -> IForgeMenuType.create(StorageRingMenu::fromNetwork)
    );

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
