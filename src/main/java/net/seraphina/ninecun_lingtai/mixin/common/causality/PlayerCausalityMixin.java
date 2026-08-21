package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerCausalityMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$redirectAttack(Entity target, CallbackInfo callbackInfo) {
        if (!((Object) this instanceof ServerPlayer player)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(player);
        if (!PlayerGreatCausalityData.isFeatureEnabled(player, settings.transferAttack())) {
            return;
        }
        ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(player);
        if (transferTarget == null) {
            return;
        }

        GreatCausalityUtility.runWithCausalityRedirect(() -> {
            player.attack(transferTarget);
            return null;
        });
        callbackInfo.cancel();
    }

    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$rewriteObtainedItem(
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!((Object) this instanceof ServerPlayer player)
                || itemStack.isEmpty()
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }

        ServerPlayer tamperingCaster = GreatCausalityUtility.findTamperingCaster(
                player,
                GreatCausalitySettings::transferItems
        );
        if (tamperingCaster != null) {
            callbackInfo.setReturnValue(
                    GreatCausalityUtility.runWithCausalityRedirect(
                            () -> tamperingCaster.addItem(itemStack)
                    )
            );
            return;
        }

        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(player);
        if (!PlayerGreatCausalityData.isFeatureEnabled(player, settings.randomItem())) {
            return;
        }
        ItemStack randomItemStack = GreatCausalityUtility.createRandomItemStack(
                player,
                itemStack.getCount()
        );
        itemStack.setCount(0);
        callbackInfo.setReturnValue(
                GreatCausalityUtility.runWithCausalityRedirect(
                        () -> player.addItem(randomItemStack)
                )
        );
    }
}
