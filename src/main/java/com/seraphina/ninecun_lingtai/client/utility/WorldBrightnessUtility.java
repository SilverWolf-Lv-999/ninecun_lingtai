package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.utility.CultivationVisionUtility;

public final class WorldBrightnessUtility {
    private static final float FULL_WORLD_BRIGHTNESS = 1.0F;

    private WorldBrightnessUtility() {
    }

    public static boolean shouldRenderWorldAtFullBrightness() {
        return CultivationVisionUtility.seesNightAsDay(ClientCultivationInfoManager.getCultivationStage());
    }

    public static float applySkyDarken(float skyDarken) {
        if (shouldRenderWorldAtFullBrightness()) {
            return FULL_WORLD_BRIGHTNESS;
        }
        return Math.min(
                FULL_WORLD_BRIGHTNESS,
                skyDarken + ClientTribulationManager.getLocalDarkness(0.0F) * 0.72F
        );
    }

    public static float applyLightTextureBrightness(float brightness) {
        if (shouldRenderWorldAtFullBrightness()) {
            return FULL_WORLD_BRIGHTNESS;
        }
        float darkness = ClientTribulationManager.getLocalDarkness(0.0F);
        return brightness * (1.0F - darkness * 0.42F);
    }
}
