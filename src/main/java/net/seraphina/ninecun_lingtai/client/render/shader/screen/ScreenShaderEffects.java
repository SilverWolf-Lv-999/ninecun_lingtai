package net.seraphina.ninecun_lingtai.client.render.shader.screen;

import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostEffectHandler;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom.AptitudeBlurPostEffect;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom.PressurePostEffect;

public final class ScreenShaderEffects {
    public static final PressurePostEffect PRESSURE = new PressurePostEffect();
    public static final AptitudeBlurPostEffect APTITUDE_BLUR = new AptitudeBlurPostEffect();

    private static boolean registered;
    private static boolean pressureActive;
    private static boolean aptitudeBlurActive;

    private ScreenShaderEffects() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PostEffectHandler.registerEffect(PRESSURE);
        PostEffectHandler.registerEffect(APTITUDE_BLUR);
        PostEffectHandler.setActive(PRESSURE, pressureActive);
        PostEffectHandler.setActive(APTITUDE_BLUR, aptitudeBlurActive);
    }

    public static void setPressureActive(boolean active) {
        pressureActive = active;
        PostEffectHandler.setActive(PRESSURE, active);
    }

    public static void setAptitudeBlurActive(boolean active) {
        aptitudeBlurActive = active;
        PostEffectHandler.setActive(APTITUDE_BLUR, active);
    }
}
