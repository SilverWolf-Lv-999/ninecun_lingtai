package com.seraphina.ninecun_lingtai.mixin.common;

import com.seraphina.ninecun_lingtai.api.font.CustomChatFormatting;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Style.class)
public abstract class StyleMixin {
    @Inject(method = {"applyFormat", "applyLegacyFormat"}, at = @At("HEAD"), cancellable = true)
    private void applyShanBeiFont(
            ChatFormatting formatting,
            CallbackInfoReturnable<Style> callbackInfo
    ) {
        if (CustomChatFormatting.isShanBeiFont(formatting)) {
            Style thisStyle = (Style) (Object) this;
            callbackInfo.setReturnValue(thisStyle.withFont(CustomFontConstants.SHAN_BEI_FONT_ID));
        }
    }

    @Inject(method = "applyFormats", at = @At("HEAD"), cancellable = true)
    private void applyShanBeiFontFormats(
            ChatFormatting[] formattings,
            CallbackInfoReturnable<Style> callbackInfo
    ) {
        boolean hasShanBeiFont = false;
        for (ChatFormatting formatting : formattings) {
            if (CustomChatFormatting.isShanBeiFont(formatting)) {
                hasShanBeiFont = true;
                break;
            }
        }
        if (!hasShanBeiFont) {
            return;
        }

        Style result = (Style) (Object) this;
        for (ChatFormatting formatting : formattings) {
            if (formatting == ChatFormatting.RESET) {
                callbackInfo.setReturnValue(Style.EMPTY);
                return;
            }
            if (CustomChatFormatting.isShanBeiFont(formatting)) {
                result = result.withFont(CustomFontConstants.SHAN_BEI_FONT_ID);
            } else {
                result = result.applyFormat(formatting);
            }
        }
        callbackInfo.setReturnValue(result);
    }
}
