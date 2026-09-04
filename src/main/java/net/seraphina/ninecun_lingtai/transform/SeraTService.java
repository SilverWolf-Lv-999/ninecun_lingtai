package net.seraphina.ninecun_lingtai.transform;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import net.seraphina.ninecun_lingtai.utility.launch.LaunchPluginUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class SeraTService implements ITransformationService {
    private static final String LAUNCH_PLUGIN_NAME = "!SeraLPService";

    public static final Logger LOGGER = LogManager.getLogger();

    static {
        LOGGER.info("Loading NinecunLingtai....");
        LaunchPluginUtility.install(LAUNCH_PLUGIN_NAME, new SeraLPService());
        LaunchPluginUtility.coexistenceCoreAndMod();
    }

    @Override
    public @NotNull String name() {
        return "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!SeraTService";
    }

    @Override
    public void initialize(IEnvironment environment) {
//        你冯的，佛具是怎么在boot阶段判定是否是server环境的
//        if (!FMLEnvironment.dist.isClient()) {
//            for (int i = 0; i<=5;i++)
//                LOGGER.info("forgedserver environment");
//        }
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
        LOGGER.info("other services: {}", otherServices);
    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }
}
