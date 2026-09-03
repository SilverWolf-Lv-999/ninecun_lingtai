package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.seraphina.ninecun_lingtai.client.animation.BreakthroughAnimationManager;
import net.seraphina.ninecun_lingtai.client.utility.PalmBuddhaOverlayRenderUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void ninecunLingtai$renderBreakthroughAnimation(
            GuiGraphics guiGraphics,
            float partialTicks,
            CallbackInfo callbackInfo
    ) {
        BreakthroughAnimationManager.render(guiGraphics, partialTicks);
        PalmBuddhaOverlayRenderUtility.render(guiGraphics, partialTicks);
    }
}
