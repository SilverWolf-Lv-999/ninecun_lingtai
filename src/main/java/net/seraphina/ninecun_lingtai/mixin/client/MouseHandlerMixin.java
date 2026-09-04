package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.MouseHandler;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaTransitionManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPressureManager;
import net.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import net.seraphina.ninecun_lingtai.client.player.ClientMeditationCameraManager;
import net.seraphina.ninecun_lingtai.client.ring.ClientStorageRingImmersionManager;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaMousePress(
            long windowPointer,
            int button,
            int action,
            int modifiers,
            CallbackInfo callbackInfo
    ) {
        if (ClientPalmBuddhaOverlayManager.isActive()) {
            if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                MouseHandler mouseHandler = (MouseHandler) (Object) this;
                ClientPalmBuddhaOverlayManager.selectMemberAtWindowCoordinates(
                        mouseHandler.xpos(),
                        mouseHandler.ypos()
                );
            } else if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                MouseHandler mouseHandler = (MouseHandler) (Object) this;
                ClientPalmBuddhaOverlayManager.releaseSelectedMemberAtWindowCoordinates(
                        mouseHandler.xpos(),
                        mouseHandler.ypos()
                );
            }
            if (action != GLFW.GLFW_RELEASE) {
                callbackInfo.cancel();
            }
            return;
        }
        // Let releases reach MouseHandler during a transfer so a button held
        // before the transfer cannot leave its internal activeButton stuck forever.
        if (ClientPalmBuddhaTransitionManager.isActive()
                && action != GLFW.GLFW_RELEASE) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaScroll(
            long windowPointer,
            double xOffset,
            double yOffset,
            CallbackInfo callbackInfo
    ) {
        if (ClientPalmBuddhaTransitionManager.isActive()
                || ClientPalmBuddhaOverlayManager.isActive()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void ninecunLingtai$rotateMeditationCamera(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (ClientPalmBuddhaTransitionManager.isActive()
                || ClientPalmBuddhaOverlayManager.isActive()) {
            return;
        }
        if (ClientPressureManager.rotateCamera(player, yRotDelta, xRotDelta)) {
            return;
        }
        if (ClientSoulRoamingManager.rotateSoul(player, yRotDelta, xRotDelta)) {
            return;
        }
        if (ClientStorageRingImmersionManager.rotateCamera(player, yRotDelta, xRotDelta)) {
            return;
        }
        if (!ClientMeditationCameraManager.rotateCamera(player, yRotDelta, xRotDelta)) {
            player.turn(yRotDelta, xRotDelta);
        }
    }
}
