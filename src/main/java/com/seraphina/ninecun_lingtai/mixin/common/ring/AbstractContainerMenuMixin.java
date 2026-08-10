package com.seraphina.ninecun_lingtai.mixin.common.ring;

import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private static void allowStorageRingContainerAccess(
            ContainerLevelAccess access,
            Player player,
            Block block,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        boolean valid = access.evaluate(
                (level, worldPosition) -> level.getBlockState(worldPosition).is(block)
                        && RingStorageManager.isStorageContainerValid(player, level, worldPosition),
                false
        );
        if (valid) {
            callbackInfo.setReturnValue(true);
        }
    }
}
