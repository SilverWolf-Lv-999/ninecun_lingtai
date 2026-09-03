package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the shader-backed Palm Buddha overlay a direct Escape close path. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$closePalmBuddhaOverlay(
            long windowPointer,
            int key,
            int scanCode,
            int action,
            int modifiers,
            CallbackInfo callbackInfo
    ) {
        if (ClientPalmBuddhaOverlayManager.isActive()
                && key == GLFW.GLFW_KEY_ESCAPE
                && action == GLFW.GLFW_PRESS) {
            ClientPalmBuddhaOverlayManager.close();
            callbackInfo.cancel();
        }
    }
}
