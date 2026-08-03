package com.seraphina.ninecun_lingtai;

import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import com.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.common.registry.ModMenuTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModSpells;
import com.seraphina.ninecun_lingtai.common.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "ninecun_lingtai";
    public static final Logger LOGGER = LogManager.getLogger();

    public ModMain(FMLJavaModLoadingContext context) {
        LOGGER.info("Loading Mod ...");
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModSpells.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModNetwork.register();
    }
}
