package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class SpaceRiftShader {
    @Nullable
    private static ShaderInstance spaceRiftShader;

    private SpaceRiftShader() {
    }

    @Nullable
    public static ShaderInstance getSpaceRiftShader() {
        return spaceRiftShader;
    }

    public static void setSpaceRiftShader(ShaderInstance shaderInstance) {
        spaceRiftShader = shaderInstance;
    }
}
