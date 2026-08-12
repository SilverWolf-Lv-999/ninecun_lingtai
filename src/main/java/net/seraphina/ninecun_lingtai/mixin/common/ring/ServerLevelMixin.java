package net.seraphina.ninecun_lingtai.mixin.common.ring;

import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void redirectStorageContainerItemDrop(
            Entity entity,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (RingStorageManager.redirectStorageItemDrop((ServerLevel) (Object) this, entity)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
