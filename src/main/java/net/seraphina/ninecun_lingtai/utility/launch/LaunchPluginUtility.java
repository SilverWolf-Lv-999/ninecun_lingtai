package net.seraphina.ninecun_lingtai.utility.launch;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class LaunchPluginUtility {
    private static final Unsafe UNSAFE = getUnsafe();

    public static final Logger LOGGER = LoggerFactory.getLogger(LaunchPluginUtility.class);

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
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to enable Ninecun Lingtai core/mod coexistence", exception);
        } finally {
            LaunchPluginUtility.clearPlugins();
        }
    }

    public static void clearPlugins() {
        try {
            Launcher launcher = Launcher.INSTANCE;
            Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
            launchPluginsField.setAccessible(true);
            Object handler = launchPluginsField.get(launcher);
            LOGGER.info("Handler type: {}", handler.getClass().getName());

            Map<String, ILaunchPluginService> plugins = getPluginsFromHandler(handler);
            if (plugins == null) {
                LOGGER.error("Unable to retrieve plugins map");
                return;
            }
            performCleanup(plugins);
        } catch (Exception e) {
            LOGGER.error("Forced cleanup failed", e);
        }
    }

    private static void performCleanup(Map<String, ILaunchPluginService> plugins) {
        LOGGER.info("Start cleaning the plugins");
        ArrayList<String> toRemove = getRemove(plugins);

        for (String name : toRemove) {
            ILaunchPluginService removed = plugins.remove(name);
            if (removed == null) continue;
            LOGGER.info("Plugin removed: {} -> {}", name, removed.getClass().getSimpleName());
        }
        LOGGER.info("Cleanup complete, remaining plugins: {}", plugins.size());
    }

    private static @NotNull ArrayList<String> getRemove(Map<String, ILaunchPluginService> plugins) {
        Set<String> corePlugins = new HashSet<>(Set.of(
                "mixin", "eventbus", "slf4jfixer", "object_holder_definalize",
                "runtime_enum_extender", "capability_token_subclass",
                "accesstransformer", "runtimedistcleaner", "!SeraLPService"
        ));

        ArrayList<String> toRemove = new ArrayList<>();
        for (String pluginName : plugins.keySet()) {
            if (corePlugins.contains(pluginName)) continue;
            toRemove.add(pluginName);
        }
        return toRemove;
    }

    private static Map<String, ILaunchPluginService> getPluginsFromHandler(Object handler) {
        Class<?> handlerClass = handler.getClass();
        LOGGER.info("Handler actual type: {}", handlerClass.getName());

        if (!handlerClass.equals(LaunchPluginHandler.class)) {
            LOGGER.info("Detects a custom Handler and tries to get the plugins field from the parent class");
            Map<String, ILaunchPluginService> plugins = getPluginsFromParentClass(handler);
            if (plugins != null) {
                return plugins;
            }
        }

        LOGGER.info("Try to get the plugins field from the current class ===");
        return getPluginsFromCurrentClass(handler, handlerClass);
    }

    private static Map<String, ILaunchPluginService> getPluginsFromParentClass(Object handler) {
        try {
            for (Class<?> currentClass = handler.getClass();
                 currentClass != null && currentClass != Object.class;
                 currentClass = currentClass.getSuperclass()) {

                LOGGER.info("Inspection Class: {}", currentClass.getName());
                if (!currentClass.getSimpleName().equals("LaunchPluginHandler")
                        && !currentClass.getName().endsWith("LaunchPluginHandler")) {
                    continue;
                }

                LOGGER.info("Find LaunchPluginHandler Class: {}", currentClass.getName());
                Field pluginsField = getPluginsFieldFromClass(currentClass);
                if (pluginsField == null) continue;

                pluginsField.setAccessible(true);
                Object pluginsObj = pluginsField.get(handler);
                if (!(pluginsObj instanceof Map)) continue;

                LOGGER.info("Successfully obtained the plugins field from the parent class {}", currentClass.getSimpleName());
                @SuppressWarnings("unchecked")
                Map<String, ILaunchPluginService> map = (Map<String, ILaunchPluginService>) pluginsObj;
                return map;
            }
            LOGGER.warn("No valid plugin fields were found in the inheritance chain");
            return null;
        } catch (Exception e) {
            LOGGER.error("Failing to get plugins from the parent class", e);
            return null;
        }
    }

    private static Field getPluginsFieldFromClass(Class<?> clazz) {
        String[] possibleFieldNames = {"plugins", "pluginMap", "launchPlugins"};
        for (String fieldName : possibleFieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                LOGGER.info("Find the Map type field in class {}: {}", clazz.getSimpleName(), fieldName);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            LOGGER.info("Discover the Map type field in class {}: {}", clazz.getSimpleName(), field.getName());
            return field;
        }
        return null;
    }

    private static Map<String, ILaunchPluginService> getPluginsFromCurrentClass(Object handler, Class<?> handlerClass) {
        try {
            Field pluginsField = getPluginsFieldFromClass(handlerClass);
            if (pluginsField != null) {
                pluginsField.setAccessible(true);
                Object pluginsObj = pluginsField.get(handler);
                if (pluginsObj instanceof Map) {
                    LOGGER.info("Retrieves the plugins field from the current class");
                    @SuppressWarnings("unchecked")
                    Map<String, ILaunchPluginService> map = (Map<String, ILaunchPluginService>) pluginsObj;
                    return map;
                }
            }
            LOGGER.warn("No valid plugins fields have been found in the current class");
            return null;
        } catch (Exception e) {
            LOGGER.error("Failing to get plugins from the current class", e);
            return null;
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

    private static Path getCodeSourcePath() {
        if (LaunchPluginUtility.class.getProtectionDomain().getCodeSource() == null) {
            throw new IllegalStateException("Ninecun Lingtai code source is unavailable");
        }
        return normalizeCodeSourcePath(
                LaunchPluginUtility.class.getProtectionDomain().getCodeSource().getLocation().getPath()
        );
    }

    static Path normalizeCodeSourcePath(String codeSourcePath) {
        String decodedPath = URLDecoder.decode(codeSourcePath, StandardCharsets.UTF_8);
        if (decodedPath.startsWith("union:")) {
            decodedPath = decodedPath.substring("union:".length());
        }

        int jarSuffixIndex = decodedPath.toLowerCase().lastIndexOf(".jar");
        if (jarSuffixIndex >= 0) {
            decodedPath = decodedPath.substring(0, jarSuffixIndex + ".jar".length());
        }
        if (decodedPath.matches("^/[A-Za-z]:[/\\\\].*")) {
            decodedPath = decodedPath.substring(1);
        }

        return Path.of(decodedPath).toAbsolutePath().normalize();
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
        removeFromFoundPaths(found, ownCodeSource);
    }

    static void removeFromFoundPaths(List<NamedPath> found, Path ownCodeSource) {
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
        IModuleLayerManager moduleLayerManager = launcher.findLayerManager()
                .orElseThrow(() -> new IllegalStateException(
                        "ModLauncher module layer manager is unavailable"
                ));
        String ownModuleName = LaunchPluginUtility.class.getModule().getName();
        if (ownModuleName == null) {
            return;
        }

        for (ModuleLayer layer : getCompletedModuleLayers(moduleLayerManager)) {
            boolean containsOwnModule = layer.modules().stream()
                    .anyMatch(module -> ownModuleName.equals(module.getName()));
            if (!containsOwnModule) {
                continue;
            }

            removeModuleFromConfiguration(layer.configuration(), ownModuleName);
        }
    }

    static List<ModuleLayer> getCompletedModuleLayers(IModuleLayerManager moduleLayerManager) {
        List<ModuleLayer> completedLayers = new ArrayList<>();
        for (IModuleLayerManager.Layer layerType : IModuleLayerManager.Layer.values()) {
            moduleLayerManager.getLayer(layerType).ifPresent(completedLayers::add);
        }
        return completedLayers;
    }

    private static void removeModuleFromConfiguration(
            Configuration configuration,
            String moduleName
    ) throws ReflectiveOperationException {
        Set<ResolvedModule> modules = new HashSet<>(
                readField(configuration, "modules", Set.class)
        );
        Map<String, ResolvedModule> nameToModule = new HashMap<>(
                readField(configuration, "nameToModule", Map.class)
        );

        ResolvedModule removedModule = nameToModule.remove(moduleName);
        if (removedModule != null) {
            modules.remove(removedModule);
            writeField(configuration, "modules", modules);
            writeField(configuration, "nameToModule", nameToModule);
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
