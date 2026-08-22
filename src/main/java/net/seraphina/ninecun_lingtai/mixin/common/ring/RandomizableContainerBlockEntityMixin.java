package net.seraphina.ninecun_lingtai.mixin.common.ring;

import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.seraphina.ninecun_lingtai.utility.JadeSlipLootUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {
    @Shadow
    protected ResourceLocation lootTable;

    @Unique
    private int ninecunLingtai$lootTableUnpackDepth;

    @Unique
    private boolean ninecunLingtai$shouldAddJadeSlipLoot;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void allowStorageRingContainerAccess(Player player, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (RingStorageManager.isStorageContainerValid(player, (BlockEntity) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void ninecunLingtai$rememberLootTable(Player player, CallbackInfo callbackInfo) {
        if (this.ninecunLingtai$lootTableUnpackDepth++ == 0) {
            this.ninecunLingtai$shouldAddJadeSlipLoot = this.lootTable != null;
        }
    }

    @Inject(method = "unpackLootTable", at = @At("TAIL"))
    private void ninecunLingtai$addJadeSlipLoot(Player player, CallbackInfo callbackInfo) {
        if (--this.ninecunLingtai$lootTableUnpackDepth != 0
                || !this.ninecunLingtai$shouldAddJadeSlipLoot) {
            return;
        }
        this.ninecunLingtai$shouldAddJadeSlipLoot = false;

        RandomizableContainerBlockEntity container =
                (RandomizableContainerBlockEntity) (Object) this;
        if (container.getLevel() instanceof ServerLevel serverLevel) {
            JadeSlipLootUtility.tryAddRandomJadeSlip(container, serverLevel.getRandom());
        }
    }
}
