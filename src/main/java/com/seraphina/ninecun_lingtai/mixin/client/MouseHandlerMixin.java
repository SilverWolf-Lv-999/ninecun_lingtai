package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.player.ClientMeditationCameraManager;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void ninecunLingtai$rotateMeditationCamera(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (!ClientMeditationCameraManager.rotateCamera(player, yRotDelta, xRotDelta)) {
            player.turn(yRotDelta, xRotDelta);
        }
    }
}
