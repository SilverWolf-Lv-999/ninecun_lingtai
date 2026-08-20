package net.seraphina.ninecun_lingtai.mixin.common.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.warden.Warden$VibrationUser")
public abstract class WardenVibrationUserMixin {
    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$ignoreCausalityPlayerVibrations(
            ServerLevel level,
            BlockPos position,
            GameEvent gameEvent,
            GameEvent.Context context,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (context.sourceEntity() instanceof Player player
                && GreatCausalityUtility.hasTechnique(player)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
