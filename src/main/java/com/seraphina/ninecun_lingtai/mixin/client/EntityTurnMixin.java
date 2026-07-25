package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$preventMeditationTurning(double yRotation, double xRotation, CallbackInfo callbackInfo) {
        if ((Object) this == Minecraft.getInstance().player
                && ClientMeditationManager.isLocalPlayerMeditating()) {
            callbackInfo.cancel();
        }
    }
}
