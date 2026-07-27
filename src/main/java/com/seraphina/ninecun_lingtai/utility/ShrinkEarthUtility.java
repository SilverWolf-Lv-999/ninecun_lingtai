package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;

public final class ShrinkEarthUtility {
    public static final int ENTRY_COMPRESSION_SCALE = 10;
    public static final int MINOR_MASTERY_COMPRESSION_SCALE = 50;
    public static final int MAJOR_MASTERY_COMPRESSION_SCALE = 100;
    public static final int PERFECTION_COMPRESSION_SCALE = 300;

    private ShrinkEarthUtility() {
    }

    public static int getCompressionScale(TechniqueMasteryStage masteryStage) {
        return switch (masteryStage) {
            case ENTRY -> ENTRY_COMPRESSION_SCALE;
            case MINOR_MASTERY -> MINOR_MASTERY_COMPRESSION_SCALE;
            case MAJOR_MASTERY -> MAJOR_MASTERY_COMPRESSION_SCALE;
            case PERFECTION -> PERFECTION_COMPRESSION_SCALE;
        };
    }

    public static float getDepthRenderScale(int compressionScale) {
        return 1.0F / Math.max(1, compressionScale);
    }
}
