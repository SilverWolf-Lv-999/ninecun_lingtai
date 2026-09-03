package net.seraphina.ninecun_lingtai.mixin.client.compat.badoptimizations;

import forge.me.thosea.badoptimizations.interfaces.EntityMethods;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.seraphina.ninecun_lingtai.mixin.api.MixinEnabled;
import net.seraphina.ninecun_lingtai.mixin.api.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores BadOptimizations' entity-data cache refresh hook when its optional injection misses. */
@Mixin(value = Entity.class, priority = 1000)
@MixinEnabled(type = Type.MOD_LOAD, modId = "badoptimizations")
public abstract class EntityDataCacheMixin {
    @Inject(
            method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",
            at = @At("TAIL")
    )
    private void ninecunLingtai$refreshBadOptimizationsCache(
            EntityDataAccessor<?> accessor,
            CallbackInfo callbackInfo
    ) {
        if ((Object) this instanceof EntityMethods entityMethods) {
            entityMethods.bo$refreshEntityData(accessor.getId());
        }
    }
}
