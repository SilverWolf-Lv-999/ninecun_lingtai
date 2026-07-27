package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.utility.CultivationVisionUtility;

public final class WorldBrightnessUtility {
    private static final float FULL_WORLD_BRIGHTNESS = 1.0F;

    private WorldBrightnessUtility() {
    }

    public static boolean shouldRenderWorldAtFullBrightness() {
        return CultivationVisionUtility.seesNightAsDay(ClientCultivationInfoManager.getCultivationStage());
    }

    public static float applySkyDarken(float skyDarken) {
        return shouldRenderWorldAtFullBrightness() ? FULL_WORLD_BRIGHTNESS : skyDarken;
    }

    public static float applyLightTextureBrightness(float brightness) {
        return shouldRenderWorldAtFullBrightness() ? FULL_WORLD_BRIGHTNESS : brightness;
    }
}
