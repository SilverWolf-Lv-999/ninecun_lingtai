package net.seraphina.ninecun_lingtai.client.render.shader.screen;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostProcessingShaders;

public interface CustomScreenEffect {
    String getName();

    ResourceLocation getShaderLocation();

    void onRenderTick(float partialTicks);

    default PostChain current() {
        return PostProcessingShaders.INSTANCE.getPostChain(this);
    }
}
