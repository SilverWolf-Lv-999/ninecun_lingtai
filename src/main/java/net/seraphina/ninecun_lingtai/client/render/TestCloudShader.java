package net.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class TestCloudShader {
    @Nullable
    private static ShaderInstance testCloudShader;

    private TestCloudShader() {
    }

    @Nullable
    public static ShaderInstance getTestCloudShader() {
        return testCloudShader;
    }

    public static void setTestCloudShader(ShaderInstance shaderInstance) {
        testCloudShader = shaderInstance;
    }
}
