package net.seraphina.ninecun_lingtai.mixin.common.health;

import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin {
    @Unique
    public float ninecunLingtai$protectHealth(float original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return CultivationDefenseUtility.getProtectedHealth(entity, original);
    }

    @Unique
    public boolean ninecunLingtai$protectDeadOrDying(boolean original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return CultivationDefenseUtility.getProtectedDeadOrDying(entity, original);
    }

    @Inject(method = "getHealth()F", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectHealth(CallbackInfoReturnable<Float> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                CultivationDefenseUtility.getProtectedHealth(entity, callbackInfo.getReturnValue())
        );
    }

    @Inject(method = "isAlive()Z", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectAlive(CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                CultivationDefenseUtility.getProtectedAlive(entity, callbackInfo.getReturnValue())
        );
    }

    @Inject(method = "isDeadOrDying()Z", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectDeadOrDying(CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                CultivationDefenseUtility.getProtectedDeadOrDying(entity, callbackInfo.getReturnValue())
        );
    }
}
