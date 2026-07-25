package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.player.ClientMeditationCameraManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
}
