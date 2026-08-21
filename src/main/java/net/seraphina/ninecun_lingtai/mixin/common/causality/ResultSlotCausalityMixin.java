package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotCausalityMixin {
    @Shadow
    @Final
    private Player player;

    @Inject(method = "onQuickCraft", at = @At("HEAD"))
    private void ninecunLingtai$voidQuickCraftResult(
            ItemStack itemStack,
            int amount,
            CallbackInfo callbackInfo
    ) {
        voidCraftResult(itemStack);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ninecunLingtai$voidCraftResult(
            Player player,
            ItemStack itemStack,
            CallbackInfo callbackInfo
    ) {
        voidCraftResult(itemStack);
    }

    private void voidCraftResult(ItemStack itemStack) {
        if (!(this.player instanceof ServerPlayer target)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }
        ServerPlayer tamperingCaster = GreatCausalityUtility.findTamperingCaster(
                target,
                GreatCausalitySettings::voidCrafting
        );
        if (tamperingCaster != null) {
            itemStack.setCount(0);
        }
    }
}
