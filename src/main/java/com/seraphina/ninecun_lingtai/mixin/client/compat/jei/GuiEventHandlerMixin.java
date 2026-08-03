package com.seraphina.ninecun_lingtai.mixin.client.compat.jei;

import com.seraphina.ninecun_lingtai.client.screen.AlchemyFurnaceScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.gui.events.GuiEventHandler", remap = false)
public abstract class GuiEventHandlerMixin {
    @Inject(method = "onDrawBackgroundPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideBackgroundOverlay(
            Screen screen,
            GuiGraphics guiGraphics,
            CallbackInfo callbackInfo
    ) {
        if (screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onDrawForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideForegroundOverlay(
            AbstractContainerScreen<?> screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onDrawScreenPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideScreenOverlay(
            Screen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }
}
