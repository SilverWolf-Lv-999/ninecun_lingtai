package net.seraphina.ninecun_lingtai.utility.jvm;

import net.seraphina.ninecun_lingtai.utility.system.CPPUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("all")
public final class JvmUtility {
    private static final long RECOVERY_PERIOD_MILLIS = 1000L;
    private static final Object LOCK = new Object();
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean patched;
    private static ScheduledExecutorService recoveryExecutor;

    public static void patchJvmti() {
        synchronized (LOCK) {
            if (patched) {
                return;
            }
            if (!CPPUtility.initializeJvmti()) {
                LOGGER.debug("Could not initialize the Java JVMTI peer neutralizer");
                return;
            }

            CPPUtility.neutralizeAlienEnvironments();
            CPPUtility.disarmAlienEnvironments();
            CPPUtility.recoverJvmti();
            CPPUtility.disarmAlienEnvironments();

            patched = true;
            recoveryExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ninecun-lingtai-jvmti-recovery");
                thread.setDaemon(true);
                return thread;
            });
            recoveryExecutor.scheduleWithFixedDelay(
                    JvmUtility::recoverJvmti,
                    RECOVERY_PERIOD_MILLIS,
                    RECOVERY_PERIOD_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            LOGGER.info("Java JVMTI peer neutralizer initialized");
        }
    }

    public static boolean isJvmtiPatched() {
        return patched;
    }

    public static void shutdownJvmti() {
        synchronized (LOCK) {
            patched = false;
            if (recoveryExecutor != null) {
                recoveryExecutor.shutdownNow();
                recoveryExecutor = null;
            }
            CPPUtility.shutdownJvmti();
        }
    }

    private static void recoverJvmti() {
        if (!patched) {
            return;
        }
        try {
            if (CPPUtility.recoverJvmti() > 0) {
                CPPUtility.disarmAlienEnvironments();
            }
        } catch (Throwable throwable) {
            LOGGER.debug("JVMTI peer neutralizer recovery failed", throwable);
        }
    }
}
