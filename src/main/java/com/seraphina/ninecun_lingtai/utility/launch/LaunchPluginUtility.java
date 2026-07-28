package com.seraphina.ninecun_lingtai.utility.launch;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.ModuleLayerHandler;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import sun.misc.Unsafe;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchPluginUtility {
    private static final Unsafe UNSAFE = getUnsafe();

    private LaunchPluginUtility() {
    }

    public static void install(String pluginName, ILaunchPluginService plugin) {
        try {
            Launcher launcher = readStaticField(Launcher.class, "INSTANCE", Launcher.class);
            LaunchPluginHandler handler = readField(launcher, "launchPlugins", LaunchPluginHandler.class);
            Map<String, ILaunchPluginService> plugins = readField(handler, "plugins", Map.class);
            Map<String, ILaunchPluginService> replacement = new LinkedHashMap<>();
            replacement.put(pluginName, plugin);
            if (plugins != null) {
                replacement.putAll(plugins);
            }
            writeField(handler, "plugins", replacement);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install Ninecun Lingtai launch plugin", exception);
        }
    }

    public static void coexistenceCoreAndMod() {
        try {
            Path ownCodeSource = getCodeSourcePath();
            removeFromFoundPaths(ownCodeSource);
            removeFromCompletedModuleLayers();
        } catch (ReflectiveOperationException | URISyntaxException exception) {
            throw new IllegalStateException("Unable to enable Ninecun Lingtai core/mod coexistence", exception);
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Path getCodeSourcePath() throws URISyntaxException {
        if (LaunchPluginUtility.class.getProtectionDomain().getCodeSource() == null) {
            throw new IllegalStateException("Ninecun Lingtai code source is unavailable");
        }
        return Path.of(
                LaunchPluginUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ).toAbsolutePath().normalize();
    }

    private static void removeFromFoundPaths(Path ownCodeSource) throws ReflectiveOperationException {
        List<NamedPath> found = readStaticField(
                ModDirTransformerDiscoverer.class,
                "found",
                List.class
        );
        if (found == null) {
            return;
        }
        found.removeIf(namedPath -> {
            for (Path path : namedPath.paths()) {
                if (ownCodeSource.equals(path.toAbsolutePath().normalize())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static void removeFromCompletedModuleLayers() throws ReflectiveOperationException {
        Launcher launcher = readStaticField(Launcher.class, "INSTANCE", Launcher.class);
        ModuleLayerHandler moduleLayerHandler = readField(
                launcher,
                "moduleLayerHandler",
                ModuleLayerHandler.class
        );
        Map<?, ?> completedLayers = readField(moduleLayerHandler, "completedLayers", Map.class);
        String ownModuleName = LaunchPluginUtility.class.getModule().getName();
        if (ownModuleName == null) {
            return;
        }

        for (Object layerInfo : completedLayers.values()) {
            ModuleLayer layer = readField(layerInfo, "layer", ModuleLayer.class);
            if (layer == null || layer.findModule(ownModuleName).isEmpty()) {
                continue;
            }

            Configuration configuration = layer.configuration();
            Set<ResolvedModule> modules = new HashSet<>(
                    readField(configuration, "modules", Set.class)
            );
            Map<String, ResolvedModule> nameToModule = new HashMap<>(
                    readField(configuration, "nameToModule", Map.class)
            );
            ResolvedModule removedModule = nameToModule.remove(ownModuleName);
            if (removedModule != null) {
                modules.remove(removedModule);
                writeField(configuration, "modules", modules);
                writeField(configuration, "nameToModule", nameToModule);
            }
        }
    }

    private static <T> T readStaticField(
            Class<?> declaringClass,
            String fieldName,
            Class<T> fieldType
    ) throws ReflectiveOperationException {
        Field field = declaringClass.getDeclaredField(fieldName);
        return readFieldValue(field, null, fieldType);
    }

    private static <T> T readField(
            Object target,
            String fieldName,
            Class<T> fieldType
    ) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        return readFieldValue(field, target, fieldType);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readFieldValue(
            Field field,
            Object target,
            Class<T> fieldType
    ) {
        Object base = Modifier.isStatic(field.getModifiers())
                ? UNSAFE.staticFieldBase(field)
                : target;
        long offset = Modifier.isStatic(field.getModifiers())
                ? UNSAFE.staticFieldOffset(field)
                : UNSAFE.objectFieldOffset(field);
        return (T) UNSAFE.getObject(base, offset);
    }

    private static void writeField(
            Object target,
            String fieldName,
            Object value
    ) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(field), value);
    }
}
