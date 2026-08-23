package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class SwordQiShader {
    @Nullable
    private static ShaderInstance swordQiShader;

    private SwordQiShader() {
    }

    @Nullable
    public static ShaderInstance getSwordQiShader() {
        return swordQiShader;
    }

    public static void setSwordQiShader(ShaderInstance shaderInstance) {
        swordQiShader = shaderInstance;
    }
}
