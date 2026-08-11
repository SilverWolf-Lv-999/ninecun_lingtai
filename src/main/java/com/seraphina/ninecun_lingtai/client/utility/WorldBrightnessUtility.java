package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.utility.CultivationVisionUtility;

public final class WorldBrightnessUtility {
    private static final float FULL_WORLD_BRIGHTNESS = 1.0F;

    public static boolean shouldRenderWorldAtFullBrightness() {
        return CultivationVisionUtility.seesNightAsDay(ClientCultivationInfoManager.getCultivationStage());
    }

    public static float applySkyDarken(float skyDarken) {
        float tribulationDarkness = ClientTribulationManager.getLocalDarkness(0.0F);
        float baseSkyDarken = shouldRenderWorldAtFullBrightness()
                ? FULL_WORLD_BRIGHTNESS
                : skyDarken;
        return baseSkyDarken * (1.0F - tribulationDarkness * 0.85F);
    }

    public static float applyLightTextureBrightness(float brightness) {
        float darkness = ClientTribulationManager.getLocalDarkness(0.0F);
        float baseBrightness = shouldRenderWorldAtFullBrightness()
                ? FULL_WORLD_BRIGHTNESS
                : brightness;
        return baseBrightness * (1.0F - darkness * 0.65F);
    }
}
