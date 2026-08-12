package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class TechniqueCloudShader {
    @Nullable
    private static ShaderInstance techniqueCloudShader;

    private TechniqueCloudShader() {
    }

    @Nullable
    public static ShaderInstance getTechniqueCloudShader() {
        return techniqueCloudShader;
    }

    public static void setTechniqueCloudShader(ShaderInstance shaderInstance) {
        techniqueCloudShader = shaderInstance;
    }
}
