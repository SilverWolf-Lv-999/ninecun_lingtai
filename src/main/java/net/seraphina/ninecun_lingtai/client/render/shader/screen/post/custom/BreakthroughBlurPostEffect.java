package net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom;

import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.animation.BreakthroughAnimationManager;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostEffectHandler;

public final class BreakthroughBlurPostEffect implements CustomScreenEffect {
    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/breakthrough_blur.json"
    );

    @Override
    public String getName() {
        return "breakthrough_blur";
    }

    @Override
    public ResourceLocation getShaderLocation() {
        return SHADER_LOCATION;
    }

    @Override
    public void onRenderTick(float partialTicks) {
        float strength = BreakthroughAnimationManager.getBlurStrength();
        PostEffectHandler.updateUniform(this, "BlurRadius", strength * 10.0F);
        PostEffectHandler.updateUniform(this, "Sigma", Math.max(0.001F, strength * 5.0F));
        PostEffectHandler.updateUniform(this, "Opacity", strength);
        PostEffectHandler.updateUniform(
                this,
                "ShakeStrength",
                BreakthroughAnimationManager.getScreenShakeStrength()
        );
        PostEffectHandler.updateUniform(
                this,
                "ShakeTime",
                BreakthroughAnimationManager.getScreenShakeTime()
        );
    }
}
