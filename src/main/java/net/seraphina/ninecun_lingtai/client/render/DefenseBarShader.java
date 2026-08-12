package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class DefenseBarShader {
    @Nullable
    private static ShaderInstance defenseBarShader;

    private DefenseBarShader() {
    }

    @Nullable
    public static ShaderInstance getDefenseBarShader() {
        return defenseBarShader;
    }

    public static void setDefenseBarShader(ShaderInstance shaderInstance) {
        defenseBarShader = shaderInstance;
    }
}
