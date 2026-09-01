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
//        LaunchPluginUtility.clearPlugins();
    }

    @Override
    public @NotNull String name() {
        return "Ninecun Lingtai Health TransformationService";
    }

    @Override
    public void initialize(IEnvironment environment) {
//        try {
//            final String agent = "net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility";
//            TransformingClassLoader classLoader = (TransformingClassLoader) Launcher.class.getDeclaredField("classLoader").get(Launcher.INSTANCE);
//            classLoader.loadClass(agent);
//            Class<?> klass = Class.forName(agent, false, classLoader);
//            klass.getDeclaredMethod("patchAgentMain").invoke(null);
//            System.out.println(agent);
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {

    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }
}
