package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class MobCausalityMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity ninecunLingtai$redirectHatred(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return target;
        }
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(player);
        if (!PlayerGreatCausalityData.isFeatureEnabled(player, settings.transferHate())) {
            return target;
        }
        ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(player);
        return transferTarget == null ? target : transferTarget;
    }
}
