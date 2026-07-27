package com.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.authlib.GameProfile;
import com.seraphina.ninecun_lingtai.client.player.ClientPossessionManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Unique
    private GameProfile ninecunLingtai$possessedProfile;

    @Unique
    private PlayerInfo ninecunLingtai$possessedPlayerInfo;

    @Inject(method = "getPlayerInfo", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$usePossessedProfile(
            CallbackInfoReturnable<PlayerInfo> callbackInfo
    ) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        GameProfile possessedProfile = ClientPossessionManager.getPossessedProfile(player.getUUID());
        if (possessedProfile == null) {
            this.ninecunLingtai$possessedProfile = null;
            this.ninecunLingtai$possessedPlayerInfo = null;
            return;
        }
        if (this.ninecunLingtai$possessedProfile != possessedProfile
                || this.ninecunLingtai$possessedPlayerInfo == null) {
            this.ninecunLingtai$possessedProfile = possessedProfile;
            this.ninecunLingtai$possessedPlayerInfo = new PlayerInfo(possessedProfile, false);
        }
        callbackInfo.setReturnValue(this.ninecunLingtai$possessedPlayerInfo);
    }
}
