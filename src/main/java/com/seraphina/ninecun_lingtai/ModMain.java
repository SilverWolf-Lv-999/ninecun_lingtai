package com.seraphina.ninecun_lingtai;

import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import com.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.common.registry.ModSpells;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "ninecun_lingtai";

    public ModMain(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModSpells.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModNetwork.register();
    }
}
