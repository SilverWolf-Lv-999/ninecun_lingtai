package net.seraphina.ninecun_lingtai.mixin.common;

import net.seraphina.ninecun_lingtai.api.font.CustomChatFormatting;
import net.minecraft.ChatFormatting;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Arrays;

@Mixin(ChatFormatting.class)
public abstract class ChatFormattingMixin {
    @Shadow
    @Final
    @Mutable
    private static ChatFormatting[] $VALUES;

    @Invoker("<init>")
    public static ChatFormatting invokeConstructor(
            String internalName,
            int ordinal,
            String name,
            char code,
            boolean isFormat,
            int id,
            Integer color
    ) {
        throw new AssertionError();
    }

    @Inject(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/ChatFormatting;$VALUES:[Lnet/minecraft/ChatFormatting;",
                    opcode = Opcodes.PUTSTATIC,
                    shift = At.Shift.AFTER
            )
    )
    private static void addShanBeiFontFormatting(CallbackInfo callbackInfo) {
        ChatFormatting shanBeiFont = invokeConstructor(
                CustomChatFormatting.SHAN_BEI_FONT_NAME,
                $VALUES.length,
                CustomChatFormatting.SHAN_BEI_FONT_NAME,
                CustomChatFormatting.SHAN_BEI_FONT_CODE,
                true,
                -1,
                null
        );
        ChatFormatting[] expandedValues = Arrays.copyOf($VALUES, $VALUES.length + 1);
        expandedValues[$VALUES.length] = shanBeiFont;
        $VALUES = expandedValues;
        CustomChatFormatting.registerShanBeiFont(shanBeiFont);
    }

    @ModifyConstant(
            method = "<clinit>",
            constant = @Constant(stringValue = "(?i)§[0-9A-FK-OR]")
    )
    private static String includeShanBeiFontInStripPattern(String originalPattern) {
        return "(?i)§[0-9A-FK-ORZ]";
    }
}
