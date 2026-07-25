package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class SoulConstellationShader {
    @Nullable
    private static ShaderInstance soulConstellationShader;

    private SoulConstellationShader() {
    }

    @Nullable
    public static ShaderInstance getSoulConstellationShader() {
        return soulConstellationShader;
    }

    public static void setSoulConstellationShader(ShaderInstance shaderInstance) {
        soulConstellationShader = shaderInstance;
    }
}
