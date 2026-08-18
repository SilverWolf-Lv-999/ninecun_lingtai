package net.seraphina.ninecun_lingtai.utility.jvm;

import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.jvm.trans.BytecodeRepairTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    private static final Object AGENT_LOCK = new Object();

    public static final Logger LOGGER = LogManager.getLogger();

    private static volatile boolean agentLoadStarted;
    private static volatile Instrumentation instrumentation;
    private static BytecodeRepairTransformer transformer;

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
            allowSelfAttach();
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
        }

        retransformLoadedClasses(agentInstrumentation);
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

    private static void allowSelfAttach() {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        try {
            Class<?> virtualMachineClass =
                    Class.forName("sun.tools.attach.HotSpotVirtualMachine");
            Field allowAttachSelf = virtualMachineClass.getDeclaredField("ALLOW_ATTACH_SELF");
            Object base = JDKUtility.UNSAFE.staticFieldBase(allowAttachSelf);
            long offset = JDKUtility.UNSAFE.staticFieldOffset(allowAttachSelf);
            JDKUtility.UNSAFE.putBoolean(base, offset, true);
        } catch (ClassNotFoundException exception) {
            LOGGER.debug("HotSpot self-attach flag is unavailable", exception);
        } catch (NoSuchFieldException exception) {
            LOGGER.debug("HotSpot self-attach field is unavailable", exception);
        }
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

    private static void attachAgent(Path agentFile) throws ReflectiveOperationException {
        String processId = currentProcessId();
        Class<?> virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
        Method attach = virtualMachineClass.getMethod("attach", String.class);
        Method loadAgent = virtualMachineClass.getMethod("loadAgent", String.class);
        Method detach = virtualMachineClass.getMethod("detach");

        Object virtualMachine = invoke(attach, null, processId);
        try {
            invoke(loadAgent, virtualMachine, agentFile.toAbsolutePath().toString());
        } finally {
            invoke(detach, virtualMachine);
        }
    }

    private static String currentProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int separator = runtimeName.indexOf('@');
        return separator >= 0 ? runtimeName.substring(0, separator) : runtimeName;
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws ReflectiveOperationException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checkedException) {
                throw new ReflectiveOperationException(cause.getMessage(), checkedException);
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
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
