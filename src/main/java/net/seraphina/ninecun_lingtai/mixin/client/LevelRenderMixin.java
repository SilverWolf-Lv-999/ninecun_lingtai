package net.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.seraphina.ninecun_lingtai.client.render.LightFlightRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ninecunLingtai$renderLightFlight(
            PoseStack poseStack,
            float partialTick,
            long finishTimeNano,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        LightFlightRenderer.renderPostWorld(partialTick, poseStack, camera, projectionMatrix);
    }
}
