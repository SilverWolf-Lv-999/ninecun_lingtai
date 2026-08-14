package net.seraphina.ninecun_lingtai.utility.jvm;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.system.SysUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AgentUtility {
    public static final Logger LOGGER = LogManager.getLogger();

    public static void patchAgentMain() {
        if (!SysUtility.isWindows()) return;
        String[] symbols = {
                "Java_sun_instrument_InstrumentationImpl_setHasTransformers",
                "Java_sun_instrument_InstrumentationImpl_setHasRetransformableTransformers",
                "Java_sun_instrument_InstrumentationImpl_retransformClasses0",
                "Java_sun_instrument_InstrumentationImpl_redefineClasses0",
        };

        try {
            Function[] functions = new Function[symbols.length];
            for (int i = 0; i < symbols.length; i++) {
                functions[i] = Function.getFunction("instrument", symbols[i]);
                LOGGER.info("instrument fn[{}] = {}", i, functions[i]);
            }

            Function virtualProtect = Function.getFunction("kernel32", "VirtualProtect");
            IntByReference oldProtect = new IntByReference(0);

            for (Function function : functions) {
                virtualProtect.invoke(
                        Void.class,
                        new Object[]{function, 4L, 64, oldProtect}
                );
                long addr = Pointer.nativeValue(function);
                JDKUtility.UNSAFE.putInt(addr, -1866219471);
            }

        } catch (Exception e) {
            LOGGER.error("patchInstrumentDll failed", e);
        }
    }
}
