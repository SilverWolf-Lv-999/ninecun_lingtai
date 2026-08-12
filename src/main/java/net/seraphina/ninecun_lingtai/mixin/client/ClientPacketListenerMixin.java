package net.seraphina.ninecun_lingtai.mixin.client;

import net.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique
    private Vec3 ninecunLingtai$shrinkEarthPreviousPosition;
    @Unique
    private boolean ninecunLingtai$shrinkEarthCapturePosition;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void ninecunLingtai$captureShrinkEarthPreviousPosition(
            ClientboundPlayerPositionPacket packet,
            CallbackInfo callbackInfo
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !ClientShrinkEarthManager.isLocalPlayerActive()) {
            this.ninecunLingtai$shrinkEarthPreviousPosition = null;
            this.ninecunLingtai$shrinkEarthCapturePosition = false;
            return;
        }
        this.ninecunLingtai$shrinkEarthPreviousPosition = player.position();
        this.ninecunLingtai$shrinkEarthCapturePosition = true;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void ninecunLingtai$smoothShrinkEarthTeleport(
            ClientboundPlayerPositionPacket packet,
            CallbackInfo callbackInfo
    ) {
        try {
            if (!this.ninecunLingtai$shrinkEarthCapturePosition
                    || this.ninecunLingtai$shrinkEarthPreviousPosition == null) {
                return;
            }
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                ClientShrinkEarthManager.smoothLocalTeleportIfExpected(
                        player,
                        this.ninecunLingtai$shrinkEarthPreviousPosition
                );
            }
        } finally {
            this.ninecunLingtai$shrinkEarthPreviousPosition = null;
            this.ninecunLingtai$shrinkEarthCapturePosition = false;
        }
    }
}
