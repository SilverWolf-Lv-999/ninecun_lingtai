package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.client.render.TechniqueCloudShader;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

public final class TechniqueImprintRenderUtility {
    public static final int GOLD_COLOR = 0xB58A2A;
    public static final int BRIGHT_GOLD_COLOR = 0xD9A93D;
    public static final int MUTED_GOLD_COLOR = 0x7F6734;

    private static final int CURVE_SEGMENTS = 12;
    private static final float CUSTOM_FONT_RENDER_HEIGHT = 48.0F;

    private TechniqueImprintRenderUtility() {
    }

    public static void drawCloudVeil(
            GuiGraphics guiGraphics,
            int width,
            int height,
            long currentTime,
            float progress
    ) {
        if (progress <= 0.0F) {
            return;
        }
        ShaderInstance shaderInstance = TechniqueCloudShader.getTechniqueCloudShader();
        if (shaderInstance == null) {
            drawCloudFallback(guiGraphics, width, height, progress);
            return;
        }

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("CloudTime").set((currentTime % 240000L) / 1000.0F);
        shaderInstance.safeGetUniform("CloudProgress").set(Mth.clamp(progress, 0.0F, 1.0F));
        shaderInstance.safeGetUniform("ScreenSize").set((float) width, (float) height);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        vertex(bufferBuilder, matrix, 0.0F, 0.0F, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, 0.0F, height, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, width, height, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, width, 0.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawConstellationField(
            GuiGraphics guiGraphics,
            int width,
            int height,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = 0.78F + Mth.sin(currentTime * 0.0017F) * 0.12F;
        int lineColor = ColorUtility.withAlpha(GOLD_COLOR, alpha * 0.22F * pulse);
        int starColor = ColorUtility.withAlpha(BRIGHT_GOLD_COLOR, alpha * 0.48F * pulse);
        List<ScreenPoint> points = List.of(
                new ScreenPoint(width * 0.08F, height * 0.24F),
                new ScreenPoint(width * 0.22F, height * 0.13F),
                new ScreenPoint(width * 0.43F, height * 0.22F),
                new ScreenPoint(width * 0.62F, height * 0.11F),
                new ScreenPoint(width * 0.84F, height * 0.25F),
                new ScreenPoint(width * 0.75F, height * 0.48F),
                new ScreenPoint(width * 0.9F, height * 0.7F),
                new ScreenPoint(width * 0.64F, height * 0.79F),
                new ScreenPoint(width * 0.39F, height * 0.72F),
                new ScreenPoint(width * 0.16F, height * 0.83F),
                new ScreenPoint(width * 0.27F, height * 0.53F)
        );
        int[][] connections = {
                {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6},
                {6, 7}, {7, 8}, {8, 9}, {9, 10}, {10, 0}, {2, 10},
                {2, 5}, {5, 8}, {8, 10}
        };
        for (int[] connection : connections) {
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    points.get(connection[0]),
                    points.get(connection[1]),
                    0.55F,
                    lineColor
            );
        }
        for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
            ScreenPoint point = points.get(pointIndex);
            float pointPulse = 0.8F + Mth.sin(currentTime * 0.0024F + pointIndex * 0.81F) * 0.2F;
            drawStar(guiGraphics, point.x(), point.y(), 2.5F + pointPulse, starColor);
        }
    }

    public static void drawStarButton(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int left,
            int top,
            int width,
            int height,
            boolean active,
            boolean hovered,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = hovered && active
                ? 0.84F + Mth.sin(currentTime * 0.006F) * 0.16F
                : 0.74F;
        float enabledAlpha = active ? alpha : alpha * 0.36F;
        int curveColor = ColorUtility.withAlpha(
                hovered && active ? BRIGHT_GOLD_COLOR : GOLD_COLOR,
                enabledAlpha * pulse
        );
        int textColor = ColorUtility.withAlpha(
                active ? BRIGHT_GOLD_COLOR : MUTED_GOLD_COLOR,
                enabledAlpha
        );
        float centerX = left + width * 0.5F;
        float centerY = top + height * 0.5F;
        float starExpansion = hovered && active ? 2.0F : 0.0F;
        drawCurvedStarFrame(
                guiGraphics,
                centerX,
                centerY,
                width * 0.5F - 2.0F + starExpansion,
                height * 0.5F + 10.0F + starExpansion * 0.45F,
                hovered && active ? 1.15F : 0.82F,
                curveColor
        );
        if (hovered && active) {
            drawCurvedStarFrame(
                    guiGraphics,
                    centerX,
                    centerY,
                    width * 0.5F - 1.0F + starExpansion,
                    height * 0.5F + 11.0F + starExpansion * 0.45F,
                    2.4F,
                    ColorUtility.withAlpha(BRIGHT_GOLD_COLOR, enabledAlpha * 0.12F)
            );
        }
        drawButtonText(
                guiGraphics,
                font,
                text,
                centerX,
                centerY,
                width - 38,
                height - 12,
                textColor
        );
    }

    public static void drawSeal(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            boolean selected,
            boolean hovered,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.004F + centerX * 0.01F);
        int color = ColorUtility.withAlpha(
                selected || hovered ? BRIGHT_GOLD_COLOR : GOLD_COLOR,
                alpha * (selected ? 0.88F : hovered ? 0.66F : 0.34F)
        );
        float rotation = currentTime * (selected ? 0.025F : 0.009F);
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                centerX,
                centerY,
                radius + (selected ? pulse * 1.2F : 0.0F),
                selected ? 1.2F : 0.7F,
                color
        );
        for (int arcIndex = 0; arcIndex < 4; arcIndex++) {
            float start = rotation + arcIndex * 90.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    centerX,
                    centerY,
                    radius * 1.3F,
                    start,
                    start + 38.0F,
                    selected ? 1.0F : 0.65F,
                    color
            );
        }
        drawStar(guiGraphics, centerX, centerY, selected ? 5.0F + pulse : 3.8F, color);
    }

    public static void drawVerticalText(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            float centerX,
            float top,
            float scale,
            float rowSpacing,
            int color,
            float alpha,
            int maximumSymbols,
            boolean displayFont
    ) {
        if (alpha <= 0.0F || text.isEmpty() || maximumSymbols <= 0) {
            return;
        }
        List<String> symbols = text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .limit(maximumSymbols)
                .toList();
        ResourceLocation fontId = displayFont
                ? CustomFontConstants.TECHNIQUE_DISPLAY_FONT_ID
                : CustomFontConstants.TECHNIQUE_FONT_ID;
        int alphaColor = ColorUtility.withAlpha(color, alpha);
        for (int symbolIndex = 0; symbolIndex < symbols.size(); symbolIndex++) {
            Component symbol = Component.literal(symbols.get(symbolIndex))
                    .withStyle(style -> style.withFont(fontId));
            drawScaledCenteredText(
                    guiGraphics,
                    font,
                    symbol,
                    centerX,
                    top + symbolIndex * rowSpacing,
                    scale,
                    Math.max(1, Math.round(rowSpacing * 1.6F)),
                    alphaColor
            );
        }
        if (text.codePointCount(0, text.length()) > maximumSymbols) {
            Component ellipsis = Component.literal("…")
                    .withStyle(style -> style.withFont(fontId));
            drawScaledCenteredText(
                    guiGraphics,
                    font,
                    ellipsis,
                    centerX,
                    top + maximumSymbols * rowSpacing,
                    scale,
                    Math.max(1, Math.round(rowSpacing * 1.6F)),
                    alphaColor
            );
        }
    }

    public static Component techniqueText(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }

    public static Component displayText(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_DISPLAY_FONT_ID)
        );
    }

    public static boolean isInside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            int width,
            int height
    ) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private static void drawCloudFallback(
            GuiGraphics guiGraphics,
            int width,
            int height,
            float progress
    ) {
        float radius = Mth.sqrt(width * width + height * height) * progress * 0.62F;
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                width * 0.5F,
                height * 0.5F,
                radius,
                ColorUtility.withAlpha(0xF5F0DF, progress * 0.9F)
        );
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                width * 0.47F,
                height * 0.53F,
                radius * 0.74F,
                ColorUtility.withAlpha(0xE8EBDD, progress * 0.48F)
        );
    }

    private static void drawQuadraticCurve(
            GuiGraphics guiGraphics,
            float startX,
            float startY,
            float controlX,
            float controlY,
            float endX,
            float endY,
            float thickness,
            int color
    ) {
        ScreenPoint previousPoint = new ScreenPoint(startX, startY);
        for (int segment = 1; segment <= CURVE_SEGMENTS; segment++) {
            float progress = (float) segment / CURVE_SEGMENTS;
            float inverseProgress = 1.0F - progress;
            ScreenPoint nextPoint = new ScreenPoint(
                    inverseProgress * inverseProgress * startX
                            + 2.0F * inverseProgress * progress * controlX
                            + progress * progress * endX,
                    inverseProgress * inverseProgress * startY
                            + 2.0F * inverseProgress * progress * controlY
                            + progress * progress * endY
            );
            AptitudeRenderUtility.drawLine(guiGraphics, previousPoint, nextPoint, thickness, color);
            previousPoint = nextPoint;
        }
    }

    private static void drawCurvedStarFrame(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float horizontalRadius,
            float verticalRadius,
            float thickness,
            int color
    ) {
        ScreenPoint topPoint = new ScreenPoint(centerX, centerY - verticalRadius);
        ScreenPoint rightPoint = new ScreenPoint(centerX + horizontalRadius, centerY);
        ScreenPoint bottomPoint = new ScreenPoint(centerX, centerY + verticalRadius);
        ScreenPoint leftPoint = new ScreenPoint(centerX - horizontalRadius, centerY);
        float controlX = horizontalRadius * 0.32F;
        float controlY = verticalRadius * 0.45F;
        drawQuadraticCurve(
                guiGraphics,
                topPoint.x(),
                topPoint.y(),
                centerX + controlX,
                centerY - controlY,
                rightPoint.x(),
                rightPoint.y(),
                thickness,
                color
        );
        drawQuadraticCurve(
                guiGraphics,
                rightPoint.x(),
                rightPoint.y(),
                centerX + controlX,
                centerY + controlY,
                bottomPoint.x(),
                bottomPoint.y(),
                thickness,
                color
        );
        drawQuadraticCurve(
                guiGraphics,
                bottomPoint.x(),
                bottomPoint.y(),
                centerX - controlX,
                centerY + controlY,
                leftPoint.x(),
                leftPoint.y(),
                thickness,
                color
        );
        drawQuadraticCurve(
                guiGraphics,
                leftPoint.x(),
                leftPoint.y(),
                centerX - controlX,
                centerY - controlY,
                topPoint.x(),
                topPoint.y(),
                thickness,
                color
        );
    }

    private static void drawButtonText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            float centerX,
            float centerY,
            int maximumWidth,
            int maximumHeight,
            int color
    ) {
        float scale = Math.min(
                0.22F,
                Math.min(
                        maximumWidth / (float) Math.max(1, font.width(text)),
                        maximumHeight / CUSTOM_FONT_RENDER_HEIGHT
                )
        );
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(
                centerX - font.width(text) * scale * 0.5F,
                centerY - CUSTOM_FONT_RENDER_HEIGHT * scale * 0.5F,
                222.0F
        );
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    private static void drawStar(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX - radius, centerY),
                new ScreenPoint(centerX + radius, centerY),
                0.7F,
                color
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX, centerY - radius),
                new ScreenPoint(centerX, centerY + radius),
                0.7F,
                color
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX - radius * 0.48F, centerY - radius * 0.48F),
                new ScreenPoint(centerX + radius * 0.48F, centerY + radius * 0.48F),
                0.45F,
                color
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX + radius * 0.48F, centerY - radius * 0.48F),
                new ScreenPoint(centerX - radius * 0.48F, centerY + radius * 0.48F),
                0.45F,
                color
        );
    }

    private static void drawScaledCenteredText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            float centerX,
            float centerY,
            float preferredScale,
            int maximumWidth,
            int color
    ) {
        float scale = Math.min(
                preferredScale,
                maximumWidth / (float) Math.max(1, font.width(text))
        );
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(
                centerX - font.width(text) * scale * 0.5F,
                centerY - font.lineHeight * scale * 0.5F,
                220.0F
        );
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            float textureU,
            float textureV
    ) {
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .endVertex();
    }
}
