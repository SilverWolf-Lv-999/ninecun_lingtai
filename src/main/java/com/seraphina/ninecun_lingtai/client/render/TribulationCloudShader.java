package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class TribulationCloudShader {
    @Nullable
    private static ShaderInstance tribulationCloudShader;

    private TribulationCloudShader() {
    }

    @Nullable
    public static ShaderInstance getTribulationCloudShader() {
        return tribulationCloudShader;
    }

    public static void setTribulationCloudShader(ShaderInstance shaderInstance) {
        tribulationCloudShader = shaderInstance;
    }
}
