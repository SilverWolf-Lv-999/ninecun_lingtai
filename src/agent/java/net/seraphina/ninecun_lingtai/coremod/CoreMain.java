package net.seraphina.ninecun_lingtai.coremod;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public final class CoreMain {
    private static final String AGENT_TARGET_CLASS =
            "net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility";

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            if (!AGENT_TARGET_CLASS.equals(loadedClass.getName())) {
                continue;
            }

            try {
                Method agentCall = loadedClass.getDeclaredMethod(
                        "agentCall",
                        String.class,
                        Instrumentation.class
                );
                agentCall.setAccessible(true);
                agentCall.invoke(null, arguments, instrumentation);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return;
        }
    }
}
