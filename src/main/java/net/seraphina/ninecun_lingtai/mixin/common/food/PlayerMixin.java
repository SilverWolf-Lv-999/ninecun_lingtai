package net.seraphina.ninecun_lingtai.mixin.common.food;

import net.seraphina.ninecun_lingtai.utility.CultivationFoodUtility;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$allowFastingPlayerToEat(
            boolean canAlwaysEat,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (CultivationFoodUtility.isFasting((Player) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
