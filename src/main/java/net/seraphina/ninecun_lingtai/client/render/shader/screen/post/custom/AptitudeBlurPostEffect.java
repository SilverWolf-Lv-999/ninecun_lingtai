package net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom;

import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;

public final class AptitudeBlurPostEffect implements CustomScreenEffect {
    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "aptitude_blur.json"
    );

    @Override
    public String getName() {
        return "aptitude_blur";
    }

    @Override
    public ResourceLocation getShaderLocation() {
        return SHADER_LOCATION;
    }

    @Override
    public void onRenderTick(float partialTicks) {
    }
}
