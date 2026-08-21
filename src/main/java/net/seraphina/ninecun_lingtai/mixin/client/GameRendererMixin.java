package net.seraphina.ninecun_lingtai.mixin.client;

import net.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.post.PostProcessingShaders;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.pre.PreProcessingShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ninecunLingtai$renderPostEffects(
            float partialTicks,
            long nanoTime,
            PoseStack poseStack,
            CallbackInfo callbackInfo
    ) {
        PostProcessingShaders.INSTANCE.renderShaders(partialTicks);
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void ninecunLingtai$resizeScreenEffects(
            int width,
            int height,
            CallbackInfo callbackInfo
    ) {
        PostProcessingShaders.INSTANCE.resize(width, height);
        PreProcessingShaders.INSTANCE.resize(width, height);
    }

    @Redirect(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"
            )
    )
    private HitResult ninecunLingtai$pickFromMeditationCamera(
            Entity cameraEntity,
            double hitDistance,
            float partialTick,
            boolean hitFluids
    ) {
        return ClientSoulRoamingManager.pick(cameraEntity, hitDistance, partialTick, hitFluids);
    }

    @Redirect(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 ninecunLingtai$getMeditationCameraViewVector(Entity cameraEntity, float partialTick) {
        return ClientSoulRoamingManager.getCameraViewVector(cameraEntity, partialTick);
    }
}
