package com.seraphina.ninecun_lingtai.utility;

public final class PlayerValueControlUtility {
    public static final double DEFAULT_APPLICATION = 1.0D;
    public static final double MINIMUM_APPLICATION = 0.0D;
    public static final double MAXIMUM_APPLICATION = 1.0D;

    private PlayerValueControlUtility() {
    }

    public static double sanitizeApplication(double application) {
        if (!Double.isFinite(application)) {
            return DEFAULT_APPLICATION;
        }
        return Math.max(MINIMUM_APPLICATION, Math.min(MAXIMUM_APPLICATION, application));
    }

    public static double applyApplication(double value, double application) {
        return value * sanitizeApplication(application);
    }

    public static float applyApplication(float value, double application) {
        return (float) applyApplication((double) value, application);
    }
}
