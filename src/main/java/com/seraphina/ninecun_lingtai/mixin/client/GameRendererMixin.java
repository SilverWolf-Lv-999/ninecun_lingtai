package com.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationCameraManager;
import com.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
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
        return ClientMeditationCameraManager.pick(cameraEntity, hitDistance, partialTick, hitFluids);
    }

    @Redirect(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 ninecunLingtai$getMeditationCameraViewVector(Entity cameraEntity, float partialTick) {
        return ClientMeditationCameraManager.getCameraViewVector(cameraEntity, partialTick);
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"
            )
    )
    private void ninecunLingtai$renderShrinkEarthCompressedLevel(
            LevelRenderer levelRenderer,
            PoseStack poseStack,
            float partialTick,
            long finishTimeNano,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix
    ) {
        float depthRenderScale = ClientShrinkEarthManager.getLocalDepthRenderScale();
        if (depthRenderScale >= 0.999F) {
            levelRenderer.renderLevel(
                    poseStack,
                    partialTick,
                    finishTimeNano,
                    renderBlockOutline,
                    camera,
                    gameRenderer,
                    lightTexture,
                    projectionMatrix
            );
            return;
        }
        Matrix4f compressedProjectionMatrix = new Matrix4f(projectionMatrix);
        compressedProjectionMatrix.scale(1.0F, 1.0F, depthRenderScale);
        gameRenderer.resetProjectionMatrix(compressedProjectionMatrix);
        try {
            levelRenderer.renderLevel(
                    poseStack,
                    partialTick,
                    finishTimeNano,
                    renderBlockOutline,
                    camera,
                    gameRenderer,
                    lightTexture,
                    compressedProjectionMatrix
            );
        } finally {
            gameRenderer.resetProjectionMatrix(projectionMatrix);
        }
    }
}
