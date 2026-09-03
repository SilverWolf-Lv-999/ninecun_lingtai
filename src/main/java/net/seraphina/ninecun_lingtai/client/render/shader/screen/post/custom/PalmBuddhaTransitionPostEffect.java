package net.seraphina.ninecun_lingtai.client.render.shader.screen.post.custom;

import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaTransitionManager;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostEffectHandler;

/** Shader-only whiteout and ink-wash veil for the real Palm Buddha transfer. */
public final class PalmBuddhaTransitionPostEffect implements CustomScreenEffect {
    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/palm_buddha_transition.json"
    );

    @Override
    public String getName() {
        return "palm_buddha_transition";
    }

    @Override
    public ResourceLocation getShaderLocation() {
        return SHADER_LOCATION;
    }

    @Override
    public void onRenderTick(float partialTicks) {
        PostEffectHandler.updateUniform(
                this,
                "TransitionProgress",
                ClientPalmBuddhaTransitionManager.getProgress(partialTicks)
        );
        PostEffectHandler.updateUniform(
                this,
                "TransitionTime",
                ClientPalmBuddhaTransitionManager.getTransitionTime(partialTicks)
        );
        PostEffectHandler.updateUniform(this, "InkStrength", 0.92F);
    }
}
