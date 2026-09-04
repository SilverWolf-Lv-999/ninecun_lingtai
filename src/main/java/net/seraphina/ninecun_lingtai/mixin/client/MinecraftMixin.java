package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaTransitionManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import net.seraphina.ninecun_lingtai.client.utility.ClientCultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void setScreen(Screen pGuiScreen, CallbackInfo ci) {
        if (ClientPalmBuddhaTransitionManager.isActive() && pGuiScreen != null) {
            ci.cancel();
            return;
        }
        if (pGuiScreen instanceof DeathScreen && ClientCultivationDefenseUtility.hasActiveDefense(player)) {
            this.screen = null;
            EntityUtility.protectEntity(player);
            ci.cancel();
        }
    }

    @Inject(method = "forceSetScreen", at = @At("HEAD"), cancellable = true)
    private void forceSetScreen(Screen pScreen, CallbackInfo ci) {
        if (pScreen instanceof DeathScreen && ClientCultivationDefenseUtility.hasActiveDefense(player)) {
            this.screen = null;
            EntityUtility.protectEntity(player);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ninecunLingtai$tickPalmBuddhaTransition(CallbackInfo callbackInfo) {
        ClientPalmBuddhaTransitionManager.tick();
        ClientPalmBuddhaOverlayManager.tick();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$handlePalmBuddhaKeybinds(CallbackInfo callbackInfo) {
        if (ClientPalmBuddhaTransitionManager.isActive()
                || ClientPalmBuddhaOverlayManager.handleKeybinds()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaAttack(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ClientPalmBuddhaTransitionManager.isActive()) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaUseItem(CallbackInfo callbackInfo) {
        if (ClientPalmBuddhaTransitionManager.isActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$lockPalmBuddhaContinuousAttack(
            boolean leftClick,
            CallbackInfo callbackInfo
    ) {
        if (ClientPalmBuddhaTransitionManager.isActive()) {
            callbackInfo.cancel();
        }
    }


//    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
//    private void createTitle(CallbackInfoReturnable<String> cir) {
//        String append = Static.isCN() ? "九寸灵台 " : "Ninecun Lingtai ";
//        String neo = append + cir.getReturnValue();
//        cir.setReturnValue(neo);
//    }
}


