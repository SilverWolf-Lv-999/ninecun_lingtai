package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsCausalityMixin {
    @Shadow
    @Final
    private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$transferAdvancement(
            Advancement advancement,
            String criterion,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }
        ServerPlayer tamperingCaster = GreatCausalityUtility.findTamperingCaster(
                this.player,
                GreatCausalitySettings::transferAdvancements
        );
        if (tamperingCaster == null) {
            return;
        }
        callbackInfo.setReturnValue(
                GreatCausalityUtility.runWithCausalityRedirect(
                        () -> tamperingCaster.getAdvancements().award(advancement, criterion)
                )
        );
    }
}
