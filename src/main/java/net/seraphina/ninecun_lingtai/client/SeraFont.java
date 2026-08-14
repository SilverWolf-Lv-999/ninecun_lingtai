package net.seraphina.ninecun_lingtai.client;

import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public final class SeraFont extends Font {
    private static final float FULL_CIRCLE = (float) (Math.PI * 2.0D);
    private static final Map<ItemFontStyle, SeraFont> FONTS = new EnumMap<>(ItemFontStyle.class);

    private final ItemFontStyle fontStyle;

    private SeraFont(
            Function<ResourceLocation, FontSet> fonts,
            boolean filterFishyGlyphs,
            ItemFontStyle fontStyle
    ) {
        super(fonts, filterFishyGlyphs);
        this.fontStyle = fontStyle;
    }

    public static Font get(ItemFontStyle fontStyle) {
        return FONTS.computeIfAbsent(fontStyle, SeraFont::create);
    }

    private static SeraFont create(ItemFontStyle fontStyle) {
        FontManager fontManager = Minecraft.getInstance().fontManager;
        return new SeraFont(
                fontId -> fontManager.fontSets.getOrDefault(
                        fontManager.renames.getOrDefault(fontId, fontId),
                        fontManager.missingFontSet
                ),
                false,
                fontStyle
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
        if (this.fontStyle.getGlowStrength() > 0.12F) {
            FormattedCharSequence glowText = this.recolor(charSequence, currentTime, 0.5F, 0.52F);
            float glowOffset = this.fontStyle.getGlowStrength() * 0.55F;
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
                this.recolor(charSequence, currentTime, 0.0F, 1.0F),
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
            float brightness
    ) {
        return output -> charSequence.accept((index, style, codePoint) -> output.accept(
                index,
                style.withColor(this.getColor(index, currentTime, phaseOffset, brightness)),
                codePoint
        ));
    }

    private int getColor(int characterIndex, long currentTime, float phaseOffset, float brightness) {
        float progress = (currentTime % this.fontStyle.getCycleDuration()) / (float) this.fontStyle.getCycleDuration();
        float wave = 0.5F + 0.5F * (float) Math.sin(FULL_CIRCLE * (
                progress + characterIndex * this.fontStyle.getCharacterPhase() + phaseOffset
        ));
        int color = mixColors(
                this.fontStyle.getPrimaryColor(),
                this.fontStyle.getSecondaryColor(),
                wave
        );
        return scaleColor(color, brightness);
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
}
