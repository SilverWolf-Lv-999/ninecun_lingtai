package net.seraphina.ninecun_lingtai.mixin.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ForgeHooksMixin {
    @Inject(method = "onLivingHurt", at = @At("HEAD"), cancellable = true)
    private static void onLivingHurt(LivingEntity entity, DamageSource src, float amount, CallbackInfoReturnable<Float> cir) {
        if (CultivationDefenseUtility.hasActiveDefense(entity)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(
            method = "onLivingDeath",
            at = @At("HEAD"),
            cancellable = true)
    private static void onLivingDeath(LivingEntity entity, DamageSource src, CallbackInfoReturnable<Boolean> cir) {
        if (CultivationDefenseUtility.hasActiveDefense(entity)) {
            cir.setReturnValue(false);
        }
    }
}
