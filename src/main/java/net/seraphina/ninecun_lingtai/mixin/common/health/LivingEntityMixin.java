package net.seraphina.ninecun_lingtai.mixin.common.health;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import net.seraphina.ninecun_lingtai.utility.StackTraceEffectImmunityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ninecunLingtai$initializeEntityDataAccessors(CallbackInfo callbackInfo) {
        EntityUtility.initializeKilledEntityDataAccessor();
    }

    @Unique
    public float ninecunLingtai$protectHealth(float original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return EntityUtility.isKilledEntity(entity)
                ? 0.0F
                : CultivationDefenseUtility.getProtectedHealth(entity, original);
    }

    @Unique
    public boolean ninecunLingtai$protectDeadOrDying(boolean original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return CultivationDefenseUtility.getProtectedDeadOrDying(entity, original);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ninecunLingtai$defineKilledEntityData(CallbackInfo callbackInfo) {
        EntityUtility.defineKilledEntityData((LivingEntity) (Object) this);
    }

    @Inject(method = "getHealth()F", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectHealth(CallbackInfoReturnable<Float> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callbackInfo.setReturnValue(
                CultivationDefenseUtility.getProtectedHealth(entity, callbackInfo.getReturnValue())
        );
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$protectHealth(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityUtility.clearRemovalIntercepted(entity);
        if (EntityUtility.isKilledEntity(entity)) {
            EntityUtility.tickKilledEntity(entity);
            ci.cancel();
            return;
        }
        if (EntityUtility.hasActiveDefense(entity))
            EntityUtility.protectEntity(entity);
    }

    @Inject(method = "setHealth(F)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$protectSetHealth(float health, CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!EntityUtility.isRestoringHealth() && EntityUtility.hasActiveDefense(entity)) {
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void injectHurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (EntityUtility.tryInstantlyKillLowerRealmCultivator(entity, pSource)) {
            cir.setReturnValue(false);
            return;
        }
        if (EntityUtility.hasActiveDefense(entity)) {
            EntityUtility.protectEntity(entity);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockDirectDamage(
            DamageSource damageSource,
            float amount,
            CallbackInfo callbackInfo
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (EntityUtility.hasActiveDefense(entity)) {
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockDeath(DamageSource damageSource, CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (EntityUtility.hasActiveDefense(entity)) {
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ninecunLingtai$blockGeneratedSlowness(
            MobEffectInstance effectInstance,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player
                && StackTraceEffectImmunityUtility.shouldBlockSlowness(player, effectInstance)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(
            method = "removeEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ninecunLingtai$blockGeneratedBeneficialEffectRemoval(
            MobEffect effect,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player
                && StackTraceEffectImmunityUtility.shouldBlockBeneficialEffectRemoval(player, effect)) {
            callbackInfo.setReturnValue(false);
        }
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
