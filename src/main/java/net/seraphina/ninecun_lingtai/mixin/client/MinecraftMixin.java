package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if (pGuiScreen instanceof DeathScreen && CultivationDefenseUtility.hasActiveDefense(player)) {
            this.screen = null;
            EntityUtility.protectEntity(player);
            ci.cancel();
        }
    }

    @Inject(method = "forceSetScreen", at = @At("HEAD"), cancellable = true)
    private void forceSetScreen(Screen pScreen, CallbackInfo ci) {
        if (pScreen instanceof DeathScreen && CultivationDefenseUtility.hasActiveDefense(player)) {
            this.screen = null;
            EntityUtility.protectEntity(player);
            ci.cancel();
        }
    }
}
