package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class PalmBuddhaOverlayShader {
    @Nullable
    private static ShaderInstance palmBuddhaOverlayShader;

    private PalmBuddhaOverlayShader() {
    }

    @Nullable
    public static ShaderInstance getPalmBuddhaOverlayShader() {
        return palmBuddhaOverlayShader;
    }

    public static void setPalmBuddhaOverlayShader(ShaderInstance shaderInstance) {
        palmBuddhaOverlayShader = shaderInstance;
    }
}
