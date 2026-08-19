package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.seraphina.ninecun_lingtai.client.utility.ClientCultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    protected DeathScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        if (this.minecraft != null && ClientCultivationDefenseUtility.hasActiveDefense(this.minecraft.player)) {
            EntityUtility.protectEntity(this.minecraft.player);
            ci.cancel();
        }
    }
}
