package net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom;

import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;

public final class PressurePostEffect implements CustomScreenEffect {
    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/pressure_blur.json"
    );

    @Override
    public String getName() {
        return "pressure_blur";
    }

    @Override
    public ResourceLocation getShaderLocation() {
        return SHADER_LOCATION;
    }

    @Override
    public void onRenderTick(float partialTicks) {
    }
}
