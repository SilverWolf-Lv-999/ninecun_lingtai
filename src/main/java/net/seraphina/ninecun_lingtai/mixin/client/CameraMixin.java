package net.seraphina.ninecun_lingtai.mixin.client;

import net.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
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
        return ClientSoulRoamingManager.getCameraYRot(cameraEntity, partialTick);
    }

    @Redirect(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"
            )
    )
    private float ninecunLingtai$getMeditationCameraXRot(Entity cameraEntity, float partialTick) {
        return ClientSoulRoamingManager.getCameraXRot(cameraEntity, partialTick);
    }
}
