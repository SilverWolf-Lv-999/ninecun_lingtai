package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;

public final class ShrinkEarthUtility {
    public static final int ENTRY_COMPRESSION_SCALE = 10;
    public static final int MINOR_MASTERY_COMPRESSION_SCALE = 50;
    public static final int MAJOR_MASTERY_COMPRESSION_SCALE = 100;
    public static final int PERFECTION_COMPRESSION_SCALE = 300;
    public static final float MIN_WORLD_RENDER_SCALE = 0.4F;
    public static final double UNCHANGED_WORLD_RENDER_RADIUS = 8.0D;


    public static int getCompressionScale(TechniqueMasteryStage masteryStage) {
        return switch (masteryStage) {
            case ENTRY -> ENTRY_COMPRESSION_SCALE;
            case MINOR_MASTERY -> MINOR_MASTERY_COMPRESSION_SCALE;
            case MAJOR_MASTERY -> MAJOR_MASTERY_COMPRESSION_SCALE;
            case PERFECTION -> PERFECTION_COMPRESSION_SCALE;
        };
    }

    public static float getWorldRenderScale(int compressionScale) {
        return Math.max(MIN_WORLD_RENDER_SCALE, 1.0F / Math.max(1, compressionScale));
    }

    public static float getWorldRenderScale(int compressionScale, double horizontalDistance, double fullCompressionDistance) {
        if (compressionScale <= 1 || horizontalDistance <= UNCHANGED_WORLD_RENDER_RADIUS) {
            return 1.0F;
        }
        double renderDistance = Math.max(UNCHANGED_WORLD_RENDER_RADIUS + 1.0D, fullCompressionDistance);
        double progress = (horizontalDistance - UNCHANGED_WORLD_RENDER_RADIUS) / (renderDistance - UNCHANGED_WORLD_RENDER_RADIUS);
        double clampedProgress = Math.max(0.0D, Math.min(1.0D, progress));
        double easedProgress = clampedProgress * clampedProgress * (3.0D - 2.0D * clampedProgress);
        double minimumScale = getWorldRenderScale(compressionScale);
        return (float) (1.0D - (1.0D - minimumScale) * easedProgress);
    }
}
