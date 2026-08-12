package net.seraphina.ninecun_lingtai.mixin.client.compat.jei;

import net.seraphina.ninecun_lingtai.client.screen.AlchemyFurnaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
public abstract class BookmarkOverlayMixin {
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideOnAlchemyFurnace(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (minecraft.screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideTooltipsOnAlchemyFurnace(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (minecraft.screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$hideForegroundOnAlchemyFurnace(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (Minecraft.getInstance().screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "isListDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$disableOnAlchemyFurnace(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (Minecraft.getInstance().screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true, remap = false)
    private void ninecunLingtai$clearMouseCaptureOnAlchemyFurnace(
            double mouseX,
            double mouseY,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (Minecraft.getInstance().screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.setReturnValue(false);
        }
    }
}
