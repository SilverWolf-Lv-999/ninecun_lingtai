package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$applyLightFlightMovement(Vec3 movementInput, CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof LocalPlayer player && ClientLightFlightManager.applyFlightMovement(player)) {
            callbackInfo.cancel();
        }
    }
}
