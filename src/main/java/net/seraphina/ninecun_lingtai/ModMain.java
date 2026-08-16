package net.seraphina.ninecun_lingtai;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import net.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.seraphina.ninecun_lingtai.common.registry.ModFeatures;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.seraphina.ninecun_lingtai.common.registry.ModMenuTypes;
import net.seraphina.ninecun_lingtai.common.registry.ModSpells;
import net.seraphina.ninecun_lingtai.common.registry.ModSounds;
import net.seraphina.ninecun_lingtai.common.registry.ModWorldgenSerializers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility;
import net.seraphina.ninecun_lingtai.utility.jvm.HookUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "ninecun_lingtai";
    public static final Logger LOGGER = LogManager.getLogger();
    private static final String DISABLE_RUNTIME_HOOKS_PROPERTY =
            "ninecun_lingtai.disableRuntimeHooks";

    public ModMain(FMLJavaModLoadingContext context) {
        LOGGER.info("init mod main......");
        AgentUtility.patchAgentMain();
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModWorldgenSerializers.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModSpells.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModNetwork.register();
        if (Boolean.getBoolean(DISABLE_RUNTIME_HOOKS_PROPERTY)) {
            return;
        }
        if (Static.isClient()) {
            HookUtility.clientHooks();
            HookUtility.clientReplaceMethod();
        }
        HookUtility.commonHooks();
        HookUtility.commonReplaceMethod();
    }
}
