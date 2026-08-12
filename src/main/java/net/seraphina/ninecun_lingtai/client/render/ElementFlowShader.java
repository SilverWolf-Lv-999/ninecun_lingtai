package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class ElementFlowShader {
    @Nullable
    private static ShaderInstance elementFlowShader;

    private ElementFlowShader() {
    }

    @Nullable
    public static ShaderInstance getElementFlowShader() {
        return elementFlowShader;
    }

    public static void setElementFlowShader(ShaderInstance shaderInstance) {
        elementFlowShader = shaderInstance;
    }
}
