package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.player.KeyboardInput;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaTransitionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents movement packets from being produced while a real realm transfer is veiled. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaMovement(
            boolean isSneaking,
            float sneakingSpeedMultiplier,
            CallbackInfo callbackInfo
    ) {
        if (!ClientPalmBuddhaTransitionManager.isActive()) {
            return;
        }
        KeyboardInput input = (KeyboardInput) (Object) this;
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        callbackInfo.cancel();
    }
}
