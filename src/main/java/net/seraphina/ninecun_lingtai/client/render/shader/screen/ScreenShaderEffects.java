package net.seraphina.ninecun_lingtai.client.render.shader.screen;

import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostEffectHandler;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom.BreakthroughBlurPostEffect;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom.AptitudeBlurPostEffect;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom.PressurePostEffect;

public final class ScreenShaderEffects {
    public static final PressurePostEffect PRESSURE = new PressurePostEffect();
    public static final AptitudeBlurPostEffect APTITUDE_BLUR = new AptitudeBlurPostEffect();
    public static final BreakthroughBlurPostEffect BREAKTHROUGH_BLUR = new BreakthroughBlurPostEffect();

    private static boolean registered;
    private static boolean pressureActive;
    private static boolean aptitudeBlurActive;
    private static boolean breakthroughBlurActive;

    private ScreenShaderEffects() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PostEffectHandler.registerEffect(PRESSURE);
        PostEffectHandler.registerEffect(APTITUDE_BLUR);
        PostEffectHandler.registerEffect(BREAKTHROUGH_BLUR);
        PostEffectHandler.setActive(PRESSURE, pressureActive);
        PostEffectHandler.setActive(APTITUDE_BLUR, aptitudeBlurActive);
        PostEffectHandler.setActive(BREAKTHROUGH_BLUR, breakthroughBlurActive);
    }

    public static void setPressureActive(boolean active) {
        pressureActive = active;
        PostEffectHandler.setActive(PRESSURE, active);
    }

    public static void setAptitudeBlurActive(boolean active) {
        aptitudeBlurActive = active;
        PostEffectHandler.setActive(APTITUDE_BLUR, active);
    }

    public static void setBreakthroughBlurActive(boolean active) {
        breakthroughBlurActive = active;
        PostEffectHandler.setActive(BREAKTHROUGH_BLUR, active);
    }
}
