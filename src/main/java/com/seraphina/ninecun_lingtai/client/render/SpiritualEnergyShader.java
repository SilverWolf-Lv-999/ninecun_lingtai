package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class SpiritualEnergyShader {
    @Nullable
    private static ShaderInstance spiritualEnergyShader;

    private SpiritualEnergyShader() {
    }

    @Nullable
    public static ShaderInstance getSpiritualEnergyShader() {
        return spiritualEnergyShader;
    }

    public static void setSpiritualEnergyShader(ShaderInstance shaderInstance) {
        spiritualEnergyShader = shaderInstance;
    }
}
