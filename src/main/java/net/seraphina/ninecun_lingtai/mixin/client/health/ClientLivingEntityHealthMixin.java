package net.seraphina.ninecun_lingtai.mixin.client.health;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.seraphina.ninecun_lingtai.client.utility.ClientCultivationDefenseUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityHealthMixin {
    @Inject(method = "getHealth()F", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectLocalHealth(CallbackInfoReturnable<Float> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                ClientCultivationDefenseUtility.getProtectedHealth(entity, callbackInfo.getReturnValue())
        );
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$protectLocalHurt(
            DamageSource damageSource,
            float amount,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (ClientCultivationDefenseUtility.hasActiveDefense(entity)
                && damageSource.getEntity() == null) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "isDeadOrDying()Z", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectLocalDeadOrDying(CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                ClientCultivationDefenseUtility.getProtectedDeadOrDying(entity, callbackInfo.getReturnValue())
        );
    }
}
