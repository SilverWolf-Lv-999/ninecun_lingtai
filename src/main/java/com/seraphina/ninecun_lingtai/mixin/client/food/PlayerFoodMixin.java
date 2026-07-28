package com.seraphina.ninecun_lingtai.mixin.client.food;

import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.utility.CultivationFoodUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$allowLocalFastingPlayerToEat(
            boolean canAlwaysEat,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Player player = (Player) (Object) this;
        if (player == Minecraft.getInstance().player
                && CultivationFoodUtility.isFasting(
                ClientCultivationInfoManager.getCultivationStage()
        )) {
            callbackInfo.setReturnValue(true);
        }
    }
}
