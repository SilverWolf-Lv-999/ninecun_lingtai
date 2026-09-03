package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaTransitionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fades the local audio bus during the real Palm Buddha dimension transfer. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "calculateVolume", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$fadePalmBuddhaTransitionSound(
            SoundInstance soundInstance,
            CallbackInfoReturnable<Float> callbackInfo
    ) {
        if (!ClientPalmBuddhaTransitionManager.isActive()) {
            return;
        }
        callbackInfo.setReturnValue(
                callbackInfo.getReturnValue()
                        * ClientPalmBuddhaTransitionManager.getSoundVolumeFactor()
        );
    }
}
