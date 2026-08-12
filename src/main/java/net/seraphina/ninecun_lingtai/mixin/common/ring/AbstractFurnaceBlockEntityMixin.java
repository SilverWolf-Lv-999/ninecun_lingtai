package net.seraphina.ninecun_lingtai.mixin.common.ring;

import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void allowStorageRingContainerAccess(Player player, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (RingStorageManager.isStorageContainerValid(player, (BlockEntity) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
