package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.player.ClientMeditationCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Redirect(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"
            )
    )
    private float ninecunLingtai$getMeditationCameraYRot(Entity cameraEntity, float partialTick) {
        return ClientMeditationCameraManager.getCameraYRot(cameraEntity, partialTick);
    }

    @Redirect(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"
            )
    )
    private float ninecunLingtai$getMeditationCameraXRot(Entity cameraEntity, float partialTick) {
        return ClientMeditationCameraManager.getCameraXRot(cameraEntity, partialTick);
    }
}
