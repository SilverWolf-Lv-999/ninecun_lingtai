package net.seraphina.ninecun_lingtai.api.font;

import net.minecraft.ChatFormatting;

public final class CustomChatFormatting {
    public static final String SHAN_BEI_FONT_NAME = "SHAN_BEI_FONT";
    public static final char SHAN_BEI_FONT_CODE = 'z';

    private static ChatFormatting shanBeiFont;

    private CustomChatFormatting() {
    }

    public static ChatFormatting shanBeiFont() {
        if (shanBeiFont == null) {
            ChatFormatting.values();
        }
        if (shanBeiFont == null) {
            throw new IllegalStateException("Shan Bei font formatting has not been registered");
        }
        return shanBeiFont;
    }

    public static boolean isShanBeiFont(ChatFormatting formatting) {
        return formatting == shanBeiFont;
    }

    public static void registerShanBeiFont(ChatFormatting formatting) {
        if (shanBeiFont != null) {
            throw new IllegalStateException("Shan Bei font formatting is already registered");
        }
        shanBeiFont = formatting;
    }
}
