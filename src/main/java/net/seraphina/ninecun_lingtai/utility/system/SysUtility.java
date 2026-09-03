package net.seraphina.ninecun_lingtai.utility.system;

import java.util.Locale;

public final class SysUtility {
    private static final String OS_NAME = "os.name";

    private static final String JAVA_RUNTIME_NAME = "java.runtime.name";

    private static final String JAVA_VM_NAME = "java.vm.name";

    private static boolean is(String system) {
        if (system == null || system.isBlank()) {
            return false;
        }
        return getProperty(OS_NAME)
                .toLowerCase(Locale.ROOT)
                .contains(system.toLowerCase(Locale.ROOT));
    }

    public static boolean isWindows() {
        return is("windows");
    }

    public static boolean isMac() {
        return is("mac") || is("darwin");
    }

    public static boolean isLinux() {
        return is("linux") && !isAndroid();
    }

    public static boolean isAndroid() {
        return getProperty(JAVA_RUNTIME_NAME)
                .toLowerCase(Locale.ROOT)
                .contains("android")
                || getProperty(JAVA_VM_NAME)
                .toLowerCase(Locale.ROOT)
                .contains("dalvik");
    }

    private static String getProperty(String key) {
        try {
            return System.getProperty(key, "");
        } catch (SecurityException ignored) {
            return "";
        }
    }
}
