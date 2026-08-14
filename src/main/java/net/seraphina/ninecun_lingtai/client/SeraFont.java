package net.seraphina.ninecun_lingtai.client;

import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public final class SeraFont extends Font {
    private static final float FULL_CIRCLE = (float) (Math.PI * 2.0D);
    private static final int WHITE = 0xFFFFFF;
    private static final Map<ItemFontTheme, SeraFont> FONTS = new HashMap<>();

    private final ItemFontTheme fontTheme;

    private SeraFont(
            Function<ResourceLocation, FontSet> fonts,
            boolean filterFishyGlyphs,
            ItemFontTheme fontTheme
    ) {
        super(fonts, filterFishyGlyphs);
        this.fontTheme = fontTheme;
    }

    public static Font get(ItemFontStyle fontStyle) {
        return get(ItemFontTheme.of(fontStyle));
    }

    public static Font get(ItemFontTheme fontTheme) {
        return FONTS.computeIfAbsent(fontTheme, SeraFont::create);
    }

    private static SeraFont create(ItemFontTheme fontTheme) {
        FontManager fontManager = Minecraft.getInstance().fontManager;
        return new SeraFont(
                fontId -> fontManager.fontSets.getOrDefault(
                        fontManager.renames.getOrDefault(fontId, fontId),
                        fontManager.missingFontSet
                ),
                false,
                fontTheme
        );
    }

    @Override
    public int drawInBatch(
            FormattedCharSequence charSequence,
            float startX,
            float startY,
            int color,
            boolean dropShadow,
            Matrix4f matrix,
            MultiBufferSource bufferSource,
            DisplayMode displayMode,
            int overlay,
            int light
    ) {
        long currentTime = Util.getMillis();
        float glowStrength = this.getGlowStrength();
        if (glowStrength > 0.12F) {
            FormattedCharSequence glowText = this.recolor(
                    charSequence,
                    currentTime,
                    0.5F,
                    0.52F,
                    true
            );
            float glowOffset = glowStrength * 0.55F;
            super.drawInBatch(
                    glowText,
                    startX - glowOffset,
                    startY,
                    color,
                    false,
                    matrix,
                    bufferSource,
                    displayMode,
                    overlay,
                    light
            );
            super.drawInBatch(
                    glowText,
                    startX + glowOffset,
                    startY,
                    color,
                    false,
                    matrix,
                    bufferSource,
                    displayMode,
                    overlay,
                    light
            );
            super.drawInBatch(
                    glowText,
                    startX,
                    startY - glowOffset,
                    color,
                    false,
                    matrix,
                    bufferSource,
                    displayMode,
                    overlay,
                    light
            );
            super.drawInBatch(
                    glowText,
                    startX,
                    startY + glowOffset,
                    color,
                    false,
                    matrix,
                    bufferSource,
                    displayMode,
                    overlay,
                    light
            );
        }
        return super.drawInBatch(
                this.recolor(charSequence, currentTime, 0.0F, 1.0F, false),
                startX,
                startY,
                color,
                dropShadow,
                matrix,
                bufferSource,
                displayMode,
                overlay,
                light
        );
    }

    private FormattedCharSequence recolor(
            FormattedCharSequence charSequence,
            long currentTime,
            float phaseOffset,
            float brightness,
            boolean themedTextOnly
    ) {
        return output -> charSequence.accept((index, style, codePoint) -> {
            if (!usesThemeColor(style)) {
                return themedTextOnly || output.accept(index, style, codePoint);
            }
            return output.accept(
                    index,
                    style.withColor(this.getColor(index, currentTime, phaseOffset, brightness)),
                    codePoint
            );
        });
    }

    private int getColor(int characterIndex, long currentTime, float phaseOffset, float brightness) {
        ItemFontStyle primaryStyle = this.fontTheme.primaryStyle();
        float progress = (currentTime % primaryStyle.getCycleDuration())
                / (float) primaryStyle.getCycleDuration();
        float wave = 0.5F + 0.5F * (float) Math.sin(FULL_CIRCLE * (
                progress + characterIndex * primaryStyle.getCharacterPhase() + phaseOffset
        ));
        int primaryColor = mixColors(
                primaryStyle.getPrimaryColor(),
                primaryStyle.getSecondaryColor(),
                wave
        );
        ItemFontStyle accentStyle = this.fontTheme.accentStyle();
        if (primaryStyle != accentStyle) {
            float accentProgress = (currentTime % accentStyle.getCycleDuration())
                    / (float) accentStyle.getCycleDuration();
            float accentWave = 0.5F + 0.5F * (float) Math.sin(FULL_CIRCLE * (
                    accentProgress
                            + characterIndex * accentStyle.getCharacterPhase()
                            + phaseOffset
                            + 0.25F
            ));
            int accentColor = mixColors(
                    accentStyle.getPrimaryColor(),
                    accentStyle.getSecondaryColor(),
                    accentWave
            );
            float accentWeight = 0.22F + this.fontTheme.rank() * 0.045F;
            primaryColor = mixColors(primaryColor, accentColor, accentWeight);
        }
        float rankBrightness = 0.82F + this.fontTheme.rank() * 0.02F;
        return scaleColor(primaryColor, brightness * rankBrightness);
    }

    private static int mixColors(int firstColor, int secondColor, float progress) {
        int red = mixChannel(firstColor >> 16 & 255, secondColor >> 16 & 255, progress);
        int green = mixChannel(firstColor >> 8 & 255, secondColor >> 8 & 255, progress);
        int blue = mixChannel(firstColor & 255, secondColor & 255, progress);
        return red << 16 | green << 8 | blue;
    }

    private static int scaleColor(int color, float brightness) {
        int red = Math.round((color >> 16 & 255) * brightness);
        int green = Math.round((color >> 8 & 255) * brightness);
        int blue = Math.round((color & 255) * brightness);
        return red << 16 | green << 8 | blue;
    }

    private static int mixChannel(int firstChannel, int secondChannel, float progress) {
        return Math.round(firstChannel + (secondChannel - firstChannel) * progress);
    }

    private static boolean usesThemeColor(Style style) {
        TextColor textColor = style.getColor();
        return textColor == null || textColor.getValue() == WHITE;
    }

    private float getGlowStrength() {
        float baseGlow = Math.max(
                this.fontTheme.primaryStyle().getGlowStrength(),
                this.fontTheme.accentStyle().getGlowStrength()
        );
        return Math.min(0.9F, baseGlow + this.fontTheme.rank() * 0.018F);
    }
}
