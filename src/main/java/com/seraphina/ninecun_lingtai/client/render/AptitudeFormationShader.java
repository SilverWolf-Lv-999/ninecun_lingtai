package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class AptitudeFormationShader {
    @Nullable
    private static ShaderInstance aptitudeFormationShader;

    private AptitudeFormationShader() {
    }

    @Nullable
    public static ShaderInstance getAptitudeFormationShader() {
        return aptitudeFormationShader;
    }

    public static void setAptitudeFormationShader(ShaderInstance shaderInstance) {
        aptitudeFormationShader = shaderInstance;
    }
}
