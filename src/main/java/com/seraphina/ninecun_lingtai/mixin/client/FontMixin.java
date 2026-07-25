package com.seraphina.ninecun_lingtai.mixin.client;

import com.seraphina.ninecun_lingtai.client.font.CustomFontManager;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
    @Inject(method = "getFontSet", at = @At("HEAD"), cancellable = true)
    private void useShanBeiFontSet(
            ResourceLocation fontId,
            CallbackInfoReturnable<FontSet> callbackInfo
    ) {
        if (CustomFontConstants.SHAN_BEI_FONT_ID.equals(fontId)) {
            callbackInfo.setReturnValue(CustomFontManager.getFontSet());
        }
    }
}
