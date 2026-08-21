package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Villager.class)
public abstract class VillagerCausalityMixin {
    @ModifyVariable(method = "updateSpecialPrices", at = @At("HEAD"), argsOnly = true)
    private Player ninecunLingtai$redirectVillagerPrice(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || GreatCausalityUtility.isCausalityRedirectInProgress()) {
            return player;
        }
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(serverPlayer);
        if (!PlayerGreatCausalityData.isFeatureEnabled(serverPlayer, settings.transferVillagerPrice())) {
            return player;
        }
        ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(serverPlayer);
        return transferTarget == null ? player : transferTarget;
    }
}
