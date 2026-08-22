package net.seraphina.ninecun_lingtai.transform;

import net.seraphina.ninecun_lingtai.utility.launch.LaunchPluginUtility;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformationService;

import java.util.List;
import java.util.Set;

public final class SeraTService implements ITransformationService {
    private static final String LAUNCH_PLUGIN_NAME = "!NinecunLingtaiHealth";
    private static final String DEV_SERVICE_PROPERTY =
            "ninecun_lingtai.devTransformationService";

    static {
        LaunchPluginUtility.install(LAUNCH_PLUGIN_NAME, new SeraLPService());
        if (!Boolean.getBoolean(DEV_SERVICE_PROPERTY)) {
            LaunchPluginUtility.coexistenceCoreAndMod();
        }
    }

    @Override
    public String name() {
        return "Ninecun Lingtai Health TransformationService";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
    }

    @Override
    public List<ITransformer> transformers() {
        return List.of();
    }
}
