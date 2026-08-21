package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCausalityMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$redirectIncomingDamage(
            DamageSource damageSource,
            float amount,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!((Object) this instanceof ServerPlayer player)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(player);
        if (!PlayerGreatCausalityData.isFeatureEnabled(player, settings.transferDamage())) {
            return;
        }
        ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(player);
        if (transferTarget == null) {
            return;
        }
        callbackInfo.setReturnValue(
                GreatCausalityUtility.runWithCausalityRedirect(
                        () -> transferTarget.hurt(damageSource, amount)
                )
        );
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ninecunLingtai$rewriteEffect(
            MobEffectInstance effectInstance,
            Entity source,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!((Object) this instanceof ServerPlayer player)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return;
        }
        if (effectInstance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(player);
            if (!PlayerGreatCausalityData.isFeatureEnabled(player, settings.transferNegativeEffects())) {
                return;
            }
            ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(player);
            if (transferTarget == null) {
                return;
            }
            callbackInfo.setReturnValue(
                    GreatCausalityUtility.runWithCausalityRedirect(
                            () -> transferTarget.addEffect(new MobEffectInstance(effectInstance), source)
                    )
            );
            return;
        }
        if (effectInstance.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) {
            return;
        }
        ServerPlayer tamperingCaster = GreatCausalityUtility.findTamperingCaster(
                player,
                GreatCausalitySettings::corruptBeneficialEffects
        );
        if (tamperingCaster == null) {
            return;
        }
        MobEffectInstance harmfulEffect = GreatCausalityUtility.createRandomHarmfulEffect(
                tamperingCaster,
                effectInstance
        );
        callbackInfo.setReturnValue(
                GreatCausalityUtility.runWithCausalityRedirect(
                        () -> player.addEffect(harmfulEffect, source)
                )
        );
    }
}
