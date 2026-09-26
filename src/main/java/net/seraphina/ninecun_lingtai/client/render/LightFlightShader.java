package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class LightFlightShader {
    @Nullable
    private static ShaderInstance lightFlightShader;

    private LightFlightShader() {
    }

    @Nullable
    public static ShaderInstance getLightFlightShader() {
        return lightFlightShader;
    }

    public static void setLightFlightShader(ShaderInstance shaderInstance) {
        lightFlightShader = shaderInstance;
    }
}
