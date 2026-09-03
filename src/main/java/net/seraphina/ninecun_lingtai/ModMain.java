package net.seraphina.ninecun_lingtai;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughChanceCalculator;
import net.seraphina.ninecun_lingtai.common.config.ClientConfig;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.karma.KarmaUtility;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.registry.*;
import net.seraphina.ninecun_lingtai.utility.jvm.HookUtility;
import net.seraphina.ninecun_lingtai.utility.jvm.JvmUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 三千大道，魔道、妖道被世人称呼为邪道
 * 然，此道大道竟允，即有其存在之理
 * 何来邪道一说？剑道亦可为邪道，刀道亦可，琴道亦可
 * 皆看修炼之人之品性
 * 如此，世间并无邪道、正道之分，只有道
 * */
@Mod(ModMain.MOD_ID)
@SuppressWarnings("all")
public final class ModMain {
    public static final String MOD_ID = "ninecun_lingtai";
    public static final Logger LOGGER = LogManager.getLogger();

    public ModMain(FMLJavaModLoadingContext context) {
        LOGGER.info("init mod main......");
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModWorldgenSerializers.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModFormationContent.register(modEventBus);
        ModTombContent.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModSpells.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        ModNetwork.register();
        // 道心杀孽：把破境公式的道心系数从常数 1.0 换成杀孽四档曲线（主控接线点）。
        BreakthroughChanceCalculator.setDaoHeartProvider(KarmaUtility::getDaoHeartCoefficient);
        if (Static.isDev() && Static.isApfDay() && JvmUtility.isJDK17()) {
            if (Static.isClient()) {
                HookUtility.clientHooks();
                HookUtility.clientReplaceMethod();
            }
            HookUtility.commonHooks();
            HookUtility.commonReplaceMethod();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Static.println("何处有仙？");
            Static.println("悠悠半载，慕然回首");
            Static.println("一梦春秋，恍如隔世");
        }));
    }
}
