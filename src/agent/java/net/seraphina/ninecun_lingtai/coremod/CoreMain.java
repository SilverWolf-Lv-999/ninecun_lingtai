package net.seraphina.ninecun_lingtai.coremod;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

public final class CoreMain {
    public static final Logger LOGGER = Logger.getLogger(CoreMain.class.getName());

    private static final String AGENT_TARGET_CLASS =
            "net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility";
    private static final String GUARD_CLASS_RESOURCE =
            "/net/seraphina/ninecun_lingtai/utility/jvm/ReflectiveAccessGuard.class";
    private static final String BOOTSTRAP_JAR_PREFIX = "ninecun-lingtai-bootstrap-";

    private static JarFile bootstrapGuardJar;

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        LOGGER.info("agentmain");
        Class<?> agentTargetClass = findAgentTargetClass(instrumentation);
        if (agentTargetClass == null) {
            return;
        }

        try {
            appendGuardToBootstrap(instrumentation, agentTargetClass);
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }

        try {
            Method agentCall = agentTargetClass.getDeclaredMethod(
                    "agentCall",
                    String.class,
                    Instrumentation.class
            );
            agentCall.setAccessible(true);
            agentCall.invoke(null, arguments, instrumentation);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    private static Class<?> findAgentTargetClass(Instrumentation instrumentation) {
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            if (AGENT_TARGET_CLASS.equals(loadedClass.getName())) {
                return loadedClass;
            }
        }
        return null;
    }

    private static synchronized void appendGuardToBootstrap(
            Instrumentation instrumentation,
            Class<?> agentTargetClass
    ) throws Exception {
        if (bootstrapGuardJar != null) {
            return;
        }

        Path bridgeFile = Files.createTempFile(
                BOOTSTRAP_JAR_PREFIX,
                ".jar"
        );
        try (InputStream input = agentTargetClass.getResourceAsStream(GUARD_CLASS_RESOURCE);
             JarOutputStream output = new JarOutputStream(
                     Files.newOutputStream(bridgeFile)
             )) {
            if (input == null) {
                throw new IllegalStateException(
                        "The main mod class loader cannot provide the reflective guard"
                );
            }
            output.putNextEntry(new JarEntry(GUARD_CLASS_RESOURCE.substring(1)));
            input.transferTo(output);
            output.closeEntry();
        }
        bridgeFile.toFile().deleteOnExit();

        JarFile jarFile = new JarFile(bridgeFile.toFile());
        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
        bootstrapGuardJar = jarFile;
    }
}
