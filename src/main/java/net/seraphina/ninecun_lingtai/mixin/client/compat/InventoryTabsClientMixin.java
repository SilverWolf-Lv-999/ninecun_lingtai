package net.seraphina.ninecun_lingtai.mixin.client.compat;

import net.seraphina.ninecun_lingtai.client.screen.AlchemyFurnaceScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.kqp.inventorytabs.init.InventoryTabsClient", remap = false)
public abstract class InventoryTabsClientMixin {
    @Inject(method = "screenSupported", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ninecunLingtai$excludeAlchemyFurnace(
            Screen screen,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "shouldRenderTabs", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ninecunLingtai$hideTabsOnAlchemyFurnace(
            Screen screen,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (screen instanceof AlchemyFurnaceScreen) {
            callbackInfo.setReturnValue(false);
        }
    }
}
