package net.seraphina.ninecun_lingtai.mixin.common.health;

import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class EntityMixin {
    @Shadow
    private EntityInLevelCallback levelCallback;

    @Unique
    public boolean ninecunLingtai$protectAlive(boolean original) {
        Entity entity = (Entity) (Object) this;
        return CultivationDefenseUtility.getProtectedAlive(entity, original);
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockRemove(RemovalReason reason, CallbackInfo callbackInfo) {
        Entity entity = (Entity) (Object) this;
        if (ninecunLingtai$hasActiveDefense(entity)) {
            EntityUtility.markRemovalIntercepted(entity);
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockSetRemoved(RemovalReason reason, CallbackInfo callbackInfo) {
        Entity entity = (Entity) (Object) this;
        if (ninecunLingtai$hasActiveDefense(entity)) {
            EntityUtility.markRemovalIntercepted(entity);
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "isRemoved()Z", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectRemoved(CallbackInfoReturnable<Boolean> callbackInfo) {
        Entity entity = (Entity) (Object) this;
        if (ninecunLingtai$hasActiveDefense(entity)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(
            method = "getRemovalReason()Lnet/minecraft/world/entity/Entity$RemovalReason;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ninecunLingtai$protectRemovalReason(
            CallbackInfoReturnable<RemovalReason> callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (ninecunLingtai$hasActiveDefense(entity)) {
            callbackInfo.setReturnValue(null);
        }
    }

    @Inject(method = "setLevelCallback(Lnet/minecraft/world/level/entity/EntityInLevelCallback;)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$wrapLevelCallback(
            EntityInLevelCallback callback,
            CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (callback instanceof EntityUtility.ProtectedLevelCallback protectedCallback
                && protectedCallback.getEntity() == entity) {
            callback = protectedCallback.getDelegate();
        }
        this.levelCallback = EntityUtility.wrapLevelCallback(entity, callback);
        callbackInfo.cancel();
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockPostRemovalPosition(
            double x,
            double y,
            double z,
            CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (EntityUtility.shouldBlockPositionChange(entity, x, y, z)) {
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setPosRaw(DDD)V", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$blockPostRemovalRawPosition(
            double x,
            double y,
            double z,
            CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (EntityUtility.shouldBlockPositionChange(entity, x, y, z)) {
            EntityUtility.protectEntity(entity);
            callbackInfo.cancel();
        }
    }

    @Unique
    private static boolean ninecunLingtai$hasActiveDefense(Entity entity) {
        return entity instanceof LivingEntity living && EntityUtility.hasActiveDefense(living);
    }
}
