package net.seraphina.ninecun_lingtai.utility.jvm;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.seraphina.ninecun_lingtai.utility.jvm.trans.AntiModifyByteCodeTransformer;
import net.seraphina.ninecun_lingtai.utility.jvm.trans.BytecodeRepairTransformer;
import net.seraphina.ninecun_lingtai.utility.jvm.trans.ReflectiveAccessTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public final class AgentUtility {
    private static final String DISABLE_AGENT_PROPERTY =
            "ninecun_lingtai.disableInstrumentPatch";
    private static final String AGENT_RESOURCE =
            "/assets/ninecun_lingtai/coremod/agent.jar";
    private static final String JVM_LIBRARY = "jvm";
    private static final String INSTRUMENT_LIBRARY = "instrument";
    private static final long AGENT_CALLBACK_TIMEOUT_MILLIS = 1000L;

    private static final Object AGENT_LOCK = new Object();

    public static final Logger LOGGER = LogManager.getLogger();

    private static volatile boolean agentLoadStarted;
    private static volatile Instrumentation instrumentation;
    private static BytecodeRepairTransformer transformer;
    private static ReflectiveAccessTransformer reflectiveAccessTransformer;
    private static AntiModifyByteCodeTransformer antiModifyByteCodeTransformer;

    public static void patchAgentMain() {
        if (Boolean.getBoolean(DISABLE_AGENT_PROPERTY)) {
            LOGGER.info("Instrumentation agent is disabled by system property");
            return;
        }
        if (agentLoadStarted) {
            return;
        }

        synchronized (AGENT_LOCK) {
            if (agentLoadStarted) {
                return;
            }
            agentLoadStarted = true;
        }

        Path agentFile = null;
        try {
            agentFile = copyAgentToTemporaryFile();
            attachAgent(agentFile);
        } catch (Throwable throwable) {
            LOGGER.error("Could not load the Ninecun Lingtai instrumentation agent", throwable);
        } finally {
            deleteTemporaryAgent(agentFile);
        }
    }

    public static void agentCall(String arguments, Instrumentation agentInstrumentation) {
        synchronized (AGENT_LOCK) {
            if (instrumentation != null && instrumentation != agentInstrumentation) {
                LOGGER.warn("Received a second Instrumentation instance; keeping the first one");
                return;
            }

            instrumentation = agentInstrumentation;
            if (transformer == null) {
                transformer = new BytecodeRepairTransformer();
                try {
                    agentInstrumentation.addTransformer(transformer, true);
                } catch (RuntimeException exception) {
                    transformer = null;
                    LOGGER.error("Could not register the bytecode repair transformer", exception);
                    return;
                }
            }
            if (reflectiveAccessTransformer == null) {
                reflectiveAccessTransformer = new ReflectiveAccessTransformer(
                        agentInstrumentation,
                        AgentUtility.class.getProtectionDomain()
                );
                try {
                    agentInstrumentation.addTransformer(reflectiveAccessTransformer, true);
                } catch (RuntimeException exception) {
                    reflectiveAccessTransformer = null;
                    LOGGER.error("Could not register the reflective access transformer", exception);
                }
            }
            if (antiModifyByteCodeTransformer == null) {
                antiModifyByteCodeTransformer = new AntiModifyByteCodeTransformer();
                try {
                    agentInstrumentation.addTransformer(antiModifyByteCodeTransformer, true);
                } catch (RuntimeException exception) {
                    antiModifyByteCodeTransformer = null;
                    LOGGER.error(
                            "Could not register the anti-modification bytecode transformer",
                            exception
                    );
                }
            }
        }

        retransformLoadedClasses(agentInstrumentation);
        retransformLoadedModClasses(agentInstrumentation);
        retransformLoadedTransformerClasses(agentInstrumentation);
        LOGGER.info("Instrumentation agent has been successfully loaded");
    }

    private static void retransformLoadedTransformerClasses(
            Instrumentation agentInstrumentation
    ) {
        if (antiModifyByteCodeTransformer == null
                || !agentInstrumentation.isRetransformClassesSupported()) {
            return;
        }

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> loadedClass : agentInstrumentation.getAllLoadedClasses()) {
            if (!isTargetTransformerClass(loadedClass)
                    || !agentInstrumentation.isModifiableClass(loadedClass)) {
                continue;
            }
            classes.add(loadedClass);
        }

        int retransformedClasses = 0;
        for (Class<?> loadedClass : classes) {
            try {
                agentInstrumentation.retransformClasses(loadedClass);
                retransformedClasses++;
            } catch (Throwable throwable) {
                LOGGER.debug("Could not retransform transformer class {}",
                        loadedClass.getName(), throwable);
            }
        }
        LOGGER.info("Registered anti-modification guard and retransformed {} transformer classes",
                retransformedClasses);
    }

    private static boolean isTargetTransformerClass(Class<?> loadedClass) {
        return ClassFileTransformer.class.isAssignableFrom(loadedClass)
                || ILaunchPluginService.class.isAssignableFrom(loadedClass);
    }

    private static void retransformLoadedClasses(Instrumentation agentInstrumentation) {
        if (!agentInstrumentation.isRetransformClassesSupported()) {
            LOGGER.warn("The current JVM does not support class retransformation");
            return;
        }

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> loadedClass : agentInstrumentation.getAllLoadedClasses()) {
            if (!loadedClass.getName().startsWith(
                    BytecodeRepairTransformer.TARGET_PACKAGE_PREFIX)) {
                continue;
            }
            if (agentInstrumentation.isModifiableClass(loadedClass)) {
                classes.add(loadedClass);
            }
        }

        int retransformedClasses = 0;
        for (Class<?> loadedClass : classes) {
            try {
                agentInstrumentation.retransformClasses(loadedClass);
                retransformedClasses++;
            } catch (Throwable throwable) {
                LOGGER.debug("Could not retransform {}", loadedClass.getName(), throwable);
            }
        }
        LOGGER.info("Registered bytecode repair transformer and retransformed {} classes",
                retransformedClasses);
    }

    private static void retransformLoadedModClasses(Instrumentation agentInstrumentation) {
        if (reflectiveAccessTransformer == null
                || !agentInstrumentation.isRetransformClassesSupported()) {
            return;
        }

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> loadedClass : agentInstrumentation.getAllLoadedClasses()) {
            if (loadedClass.getClassLoader() == null
                    || !ReflectiveAccessTransformer.isExternalModClass(
                    loadedClass.getProtectionDomain())
                    || !agentInstrumentation.isModifiableClass(loadedClass)) {
                continue;
            }
            classes.add(loadedClass);
        }

        int retransformedClasses = 0;
        for (Class<?> loadedClass : classes) {
            try {
                agentInstrumentation.retransformClasses(loadedClass);
                retransformedClasses++;
            } catch (Throwable throwable) {
                LOGGER.debug("Could not retransform external class {}",
                        loadedClass.getName(), throwable);
            }
        }
        LOGGER.info("Registered reflective access guard and retransformed {} external classes",
                retransformedClasses);
    }

    private static Path copyAgentToTemporaryFile() throws IOException {
        InputStream resource = AgentUtility.class.getResourceAsStream(AGENT_RESOURCE);
        if (resource == null) {
            throw new IOException("Embedded agent JAR was not found: " + AGENT_RESOURCE);
        }

        Path agentFile = Files.createTempFile("ninecun-lingtai-agent-", ".jar");
        try (resource) {
            Files.copy(resource, agentFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        agentFile.toFile().deleteOnExit();
        return agentFile;
    }

    private static void attachAgent(Path agentFile) throws InterruptedException {
        Function getCreatedJavaVms =
                Function.getFunction(JVM_LIBRARY, "JNI_GetCreatedJavaVMs");
        Pointer[] createdJavaVms = new Pointer[1];
        IntByReference virtualMachineCount = new IntByReference();
        int getCreatedJavaVmsResult = getCreatedJavaVms.invokeInt(new Object[]{
                createdJavaVms,
                1,
                virtualMachineCount.getPointer()
        });
        if (getCreatedJavaVmsResult != 0) {
            throw new IllegalStateException(
                    "JNI_GetCreatedJavaVMs failed with code " + getCreatedJavaVmsResult);
        }
        if (virtualMachineCount.getValue() == 0 || createdJavaVms[0] == null) {
            throw new IllegalStateException("The current JVM could not be located");
        }

        Function agentOnAttach =
                Function.getFunction(INSTRUMENT_LIBRARY, "Agent_OnAttach");
        int agentOnAttachResult = agentOnAttach.invokeInt(new Object[]{
                createdJavaVms[0],
                agentFile.toAbsolutePath().toString(),
                null
        });
        if (agentOnAttachResult != 0) {
            throw new IllegalStateException(
                    "Agent_OnAttach failed with code " + agentOnAttachResult);
        }

        waitForAgentCallback();
    }

    private static void waitForAgentCallback() throws InterruptedException {
        long deadline = System.nanoTime()
                + AGENT_CALLBACK_TIMEOUT_MILLIS * 1_000_000L;
        while (instrumentation == null && System.nanoTime() < deadline) {
            Thread.sleep(1L);
        }
        if (instrumentation == null) {
            throw new IllegalStateException(
                    "The instrumentation agent did not invoke agentmain in time");
        }
    }

    private static void deleteTemporaryAgent(Path agentFile) {
        if (agentFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(agentFile);
        } catch (IOException exception) {
            LOGGER.debug("Could not delete temporary agent JAR {}", agentFile, exception);
        }
    }
}
