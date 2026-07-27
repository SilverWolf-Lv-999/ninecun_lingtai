package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.utility.WorldBrightnessUtility;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F"
            )
    )
    private float ninecunLingtai$getFoundationSkyDarken(ClientLevel level, float partialTick) {
        return WorldBrightnessUtility.applySkyDarken(level.getSkyDarken(partialTick));
    }

    @Inject(
            method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ninecunLingtai$brightenLightTexture(
            DimensionType dimensionType,
            int lightLevel,
            CallbackInfoReturnable<Float> callbackInfo
    ) {
        callbackInfo.setReturnValue(WorldBrightnessUtility.applyLightTextureBrightness(callbackInfo.getReturnValue()));
    }
}
