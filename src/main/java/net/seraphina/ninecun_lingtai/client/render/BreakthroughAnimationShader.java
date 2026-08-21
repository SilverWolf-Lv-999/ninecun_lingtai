package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class BreakthroughAnimationShader {
    @Nullable
    private static ShaderInstance breakthroughAnimationShader;

    private BreakthroughAnimationShader() {
    }

    @Nullable
    public static ShaderInstance getBreakthroughAnimationShader() {
        return breakthroughAnimationShader;
    }

    public static void setBreakthroughAnimationShader(ShaderInstance shaderInstance) {
        breakthroughAnimationShader = shaderInstance;
    }
}
