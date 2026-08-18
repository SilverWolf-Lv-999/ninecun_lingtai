package net.seraphina.ninecun_lingtai.mixin.common.flight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerLightFlightMixin {
    @Shadow
    private double lastGoodX;

    @Shadow
    private double lastGoodY;

    @Shadow
    private double lastGoodZ;

    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerChunkCache;"
                            + "move(Lnet/minecraft/server/level/ServerPlayer;)V",
                    ordinal = 0
            )
    )
    private void ninecunLingtai$suppressPassengerChunkTracking(
            ServerChunkCache chunkSource,
            ServerPlayer player
    ) {
        ninecunLingtai$moveChunkSourceIfAllowed(chunkSource, player);
    }

    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerChunkCache;"
                            + "move(Lnet/minecraft/server/level/ServerPlayer;)V",
                    ordinal = 1
            )
    )
    private void ninecunLingtai$suppressPlayerChunkTracking(
            ServerChunkCache chunkSource,
            ServerPlayer player
    ) {
        ninecunLingtai$moveChunkSourceIfAllowed(chunkSource, player);
    }

    private void ninecunLingtai$moveChunkSourceIfAllowed(
            ServerChunkCache chunkSource,
            ServerPlayer player
    ) {
        double deltaX = player.getX() - this.lastGoodX;
        double deltaY = player.getY() - this.lastGoodY;
        double deltaZ = player.getZ() - this.lastGoodZ;
        double movementDistanceSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        if (!LightFlightManager.shouldSuppressChunkLoading(player, movementDistanceSqr)) {
            chunkSource.move(player);
        }
    }
}
