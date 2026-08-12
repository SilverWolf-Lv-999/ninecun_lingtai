package net.seraphina.ninecun_lingtai.mixin.common.food;

import net.seraphina.ninecun_lingtai.utility.CultivationFoodUtility;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustionLevel;

    @Unique
    private Player ninecunLingtai$player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ninecunLingtai$rememberPlayer(Player player, CallbackInfo callbackInfo) {
        this.ninecunLingtai$player = player;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void ninecunLingtai$keepFull(Player player, CallbackInfo callbackInfo) {
        if (CultivationFoodUtility.isFasting(player)) {
            this.ninecunLingtai$setFull();
        }
    }

    @Inject(method = "getFoodLevel", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$getFoodLevel(CallbackInfoReturnable<Integer> callbackInfo) {
        if (this.ninecunLingtai$isFasting()) {
            callbackInfo.setReturnValue(20);
        }
    }

    @Inject(method = "getSaturationLevel", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$getSaturationLevel(CallbackInfoReturnable<Float> callbackInfo) {
        if (this.ninecunLingtai$isFasting()) {
            callbackInfo.setReturnValue(20.0F);
        }
    }

    @Inject(method = "setFoodLevel", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$setFoodLevel(int foodLevel, CallbackInfo callbackInfo) {
        if (this.ninecunLingtai$isFasting()) {
            this.foodLevel = 20;
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setSaturation", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$setSaturation(float saturationLevel, CallbackInfo callbackInfo) {
        if (this.ninecunLingtai$isFasting()) {
            this.saturationLevel = 20.0F;
            callbackInfo.cancel();
        }
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$addExhaustion(float exhaustion, CallbackInfo callbackInfo) {
        if (this.ninecunLingtai$isFasting()) {
            this.exhaustionLevel = 0.0F;
            callbackInfo.cancel();
        }
    }

    @Unique
    private boolean ninecunLingtai$isFasting() {
        return this.ninecunLingtai$player != null
                && CultivationFoodUtility.isFasting(this.ninecunLingtai$player);
    }

    @Unique
    private void ninecunLingtai$setFull() {
        this.foodLevel = 20;
        this.saturationLevel = 20.0F;
        this.exhaustionLevel = 0.0F;
    }
}
