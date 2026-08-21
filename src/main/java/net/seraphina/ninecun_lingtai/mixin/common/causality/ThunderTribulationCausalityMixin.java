package net.seraphina.ninecun_lingtai.mixin.common.causality;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ThunderTribulationEntity.class)
public abstract class ThunderTribulationCausalityMixin {
    @Unique
    private UUID thisIsRedirectedTribulationTargetId;

    @ModifyVariable(method = "initialize", at = @At("HEAD"), argsOnly = true, remap = false)
    private ServerPlayer ninecunLingtai$redirectTribulationOwner(ServerPlayer owner) {
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(owner);
        if (!PlayerGreatCausalityData.isFeatureEnabled(owner, settings.transferTribulation())) {
            return owner;
        }
        ServerPlayer transferTarget = GreatCausalityUtility.getTransferTarget(owner);
        if (transferTarget == null) {
            return owner;
        }
        this.thisIsRedirectedTribulationTargetId = transferTarget.getUUID();
        return transferTarget;
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void ninecunLingtai$moveRedirectedTribulation(CallbackInfo callbackInfo) {
        if (this.thisIsRedirectedTribulationTargetId == null
                || !((Object) this instanceof ThunderTribulationEntity lightning)
                || !(lightning.level() instanceof ServerLevel level)) {
            return;
        }
        var transferTarget = level.getPlayerByUUID(this.thisIsRedirectedTribulationTargetId);
        if (transferTarget != null && transferTarget.isAlive()) {
            lightning.moveTo(
                    transferTarget.getX(),
                    transferTarget.getY(),
                    transferTarget.getZ(),
                    transferTarget.getYRot(),
                    transferTarget.getXRot()
            );
        }
    }
}
