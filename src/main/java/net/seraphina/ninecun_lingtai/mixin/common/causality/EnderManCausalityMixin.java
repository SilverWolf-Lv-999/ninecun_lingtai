package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EnderManCausalityMixin {
    @Unique
    private boolean thisIsApplyingCausalityProjectileHit;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$makeCausalityProjectileHit(
            DamageSource damageSource,
            float amount,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (this.thisIsApplyingCausalityProjectileHit
                || !damageSource.is(DamageTypeTags.IS_PROJECTILE)
                || !(damageSource.getEntity() instanceof Player player)
                || !GreatCausalityUtility.hasTechnique(player)) {
            return;
        }

        EnderMan enderMan = (EnderMan) (Object) this;
        this.thisIsApplyingCausalityProjectileHit = true;
        try {
            callbackInfo.setReturnValue(
                    enderMan.hurt(enderMan.damageSources().playerAttack(player), amount)
            );
        } finally {
            this.thisIsApplyingCausalityProjectileHit = false;
        }
    }
}
