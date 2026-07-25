package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class AptitudeRenderUtility {
    private static final int CIRCLE_SEGMENTS = 72;

    private AptitudeRenderUtility() {
    }

    public static void drawCenteredText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            float centerX,
            float centerY,
            float scale,
            int color,
            int outlineColor,
            float outlineAlpha
    ) {
        FormattedCharSequence visualText = text.getVisualOrderText();
        float textWidth = font.width(visualText);
        float textLeft = Math.round(centerX - textWidth * scale * 0.5F);
        float textTop = Math.round(centerY - font.lineHeight * scale * 0.5F);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(textLeft, textTop, 200.0F);
        poseStack.scale(scale, scale, 1.0F);
        float textX = 0.0F;
        float textY = 0.0F;
        int outline = ColorUtility.withAlpha(outlineColor, outlineAlpha);
        if ((outline >>> 24) > 3) {
            float outlineOffset = 1.0F / Math.max(1.0F, scale);
            guiGraphics.drawString(font, visualText, textX - outlineOffset, textY, outline, false);
            guiGraphics.drawString(font, visualText, textX + outlineOffset, textY, outline, false);
            guiGraphics.drawString(font, visualText, textX, textY - outlineOffset, outline, false);
            guiGraphics.drawString(font, visualText, textX, textY + outlineOffset, outline, false);
            guiGraphics.drawString(font, visualText, textX - outlineOffset, textY - outlineOffset, outline, false);
            guiGraphics.drawString(font, visualText, textX + outlineOffset, textY - outlineOffset, outline, false);
            guiGraphics.drawString(font, visualText, textX - outlineOffset, textY + outlineOffset, outline, false);
            guiGraphics.drawString(font, visualText, textX + outlineOffset, textY + outlineOffset, outline, false);
        }
        guiGraphics.drawString(font, visualText, textX, textY, color, false);
        poseStack.popPose();
    }

    public static void drawFiveElementConnections(
            GuiGraphics guiGraphics,
            List<ScreenPoint> elementPositions,
            float alpha
    ) {
        if (alpha <= 0.0F || elementPositions.size() != 5) {
            return;
        }
        guiGraphics.flush();
        int generationColor = ColorUtility.withAlpha(0xE8DFC8, alpha * 0.55F);
        int restraintColor = ColorUtility.withAlpha(0x9CA5B5, alpha * 0.32F);
        PoseStack poseStack = guiGraphics.pose();
        for (int index = 0; index < elementPositions.size(); index++) {
            ScreenPoint start = elementPositions.get(index);
            ScreenPoint end = elementPositions.get((index + 1) % elementPositions.size());
            drawLine(poseStack, start.x, start.y, end.x, end.y, 1.1F, generationColor);
        }
        int[] restraintOrder = {0, 2, 4, 1, 3, 0};
        for (int index = 0; index < restraintOrder.length - 1; index++) {
            ScreenPoint start = elementPositions.get(restraintOrder[index]);
            ScreenPoint end = elementPositions.get(restraintOrder[index + 1]);
            drawLine(poseStack, start.x, start.y, end.x, end.y, 0.8F, restraintColor);
        }
    }

    public static void drawTaiji(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            float rotationDegrees,
            float scale,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        guiGraphics.flush();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 180.0F);
        poseStack.scale(scale, scale, 1.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        drawCircle(poseStack, 0.0F, 0.0F, radius + 1.6F, ColorUtility.withAlpha(0xD9D5CA, alpha * 0.8F));
        drawCircle(poseStack, 0.0F, 0.0F, radius, ColorUtility.withAlpha(0x08090C, alpha));
        drawSector(
                poseStack,
                0.0F,
                0.0F,
                radius,
                -90.0F,
                90.0F,
                ColorUtility.withAlpha(0xF0EEE7, alpha)
        );
        drawCircle(poseStack, 0.0F, -radius * 0.5F, radius * 0.5F, ColorUtility.withAlpha(0xF0EEE7, alpha));
        drawCircle(poseStack, 0.0F, radius * 0.5F, radius * 0.5F, ColorUtility.withAlpha(0x08090C, alpha));
        drawCircle(poseStack, 0.0F, -radius * 0.5F, radius * 0.115F, ColorUtility.withAlpha(0x08090C, alpha));
        drawCircle(poseStack, 0.0F, radius * 0.5F, radius * 0.115F, ColorUtility.withAlpha(0xF0EEE7, alpha));
        poseStack.popPose();
    }

    public static void drawSprout(
            GuiGraphics guiGraphics,
            float centerX,
            float bottomY,
            float topY,
            int color,
            float alpha
    ) {
        if (alpha <= 0.0F || bottomY <= topY) {
            return;
        }
        guiGraphics.flush();
        float stemHeight = bottomY - topY;
        float sway = Math.min(8.0F, stemHeight * 0.035F);
        List<ScreenPoint> stemPoints = new ArrayList<>();
        int segmentCount = 18;
        for (int segment = 0; segment <= segmentCount; segment++) {
            float progress = (float) segment / segmentCount;
            float pointY = Mth.lerp(progress, bottomY, topY);
            float pointX = centerX
                    + Mth.sin(progress * Mth.PI) * sway
                    - Mth.sin(progress * Mth.PI * 2.0F) * sway * 0.22F;
            stemPoints.add(new ScreenPoint(pointX, pointY));
        }
        int glowColor = color == 0x000000 ? 0xF0EDE4 : ColorUtility.brighten(color, 0.48F);
        PoseStack poseStack = guiGraphics.pose();
        drawPolyline(poseStack, stemPoints, 6.5F, ColorUtility.withAlpha(glowColor, alpha * 0.24F));
        drawPolyline(poseStack, stemPoints, 2.8F, ColorUtility.withAlpha(color, alpha));
        drawEllipse(
                poseStack,
                centerX,
                bottomY + 1.5F,
                7.5F,
                3.2F,
                ColorUtility.withAlpha(ColorUtility.darken(color, 0.18F), alpha)
        );
    }

    private static void drawPolyline(PoseStack poseStack, List<ScreenPoint> points, float thickness, int color) {
        if ((color >>> 24) == 0 || points.size() < 2) {
            return;
        }
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 0; index < points.size() - 1; index++) {
            appendLineQuad(bufferBuilder, matrix, points.get(index), points.get(index + 1), thickness, color);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
    }

    private static void drawLine(
            PoseStack poseStack,
            float startX,
            float startY,
            float endX,
            float endY,
            float thickness,
            int color
    ) {
        if ((color >>> 24) == 0) {
            return;
        }
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        appendLineQuad(
                bufferBuilder,
                matrix,
                new ScreenPoint(startX, startY),
                new ScreenPoint(endX, endY),
                thickness,
                color
        );
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
    }

    private static void appendLineQuad(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            ScreenPoint start,
            ScreenPoint end,
            float thickness,
            int color
    ) {
        float deltaX = end.x - start.x;
        float deltaY = end.y - start.y;
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return;
        }
        float normalX = -deltaY / length * thickness * 0.5F;
        float normalY = deltaX / length * thickness * 0.5F;
        vertex(bufferBuilder, matrix, start.x + normalX, start.y + normalY, color);
        vertex(bufferBuilder, matrix, start.x - normalX, start.y - normalY, color);
        vertex(bufferBuilder, matrix, end.x - normalX, end.y - normalY, color);
        vertex(bufferBuilder, matrix, end.x + normalX, end.y + normalY, color);
    }

    private static void drawCircle(PoseStack poseStack, float centerX, float centerY, float radius, int color) {
        drawEllipse(poseStack, centerX, centerY, radius, radius, color);
    }

    private static void drawEllipse(
            PoseStack poseStack,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            int color
    ) {
        if ((color >>> 24) == 0 || radiusX <= 0.0F || radiusY <= 0.0F) {
            return;
        }
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vertex(bufferBuilder, matrix, centerX, centerY, color);
        for (int segment = 0; segment <= CIRCLE_SEGMENTS; segment++) {
            float angle = (float) (Math.PI * 2.0D * segment / CIRCLE_SEGMENTS);
            vertex(
                    bufferBuilder,
                    matrix,
                    centerX + Mth.cos(angle) * radiusX,
                    centerY + Mth.sin(angle) * radiusY,
                    color
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
    }

    private static void drawSector(
            PoseStack poseStack,
            float centerX,
            float centerY,
            float radius,
            float startDegrees,
            float endDegrees,
            int color
    ) {
        if ((color >>> 24) == 0 || radius <= 0.0F) {
            return;
        }
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vertex(bufferBuilder, matrix, centerX, centerY, color);
        int segmentCount = CIRCLE_SEGMENTS / 2;
        for (int segment = 0; segment <= segmentCount; segment++) {
            float progress = (float) segment / segmentCount;
            float angle = (float) Math.toRadians(Mth.lerp(progress, startDegrees, endDegrees));
            vertex(
                    bufferBuilder,
                    matrix,
                    centerX + Mth.cos(angle) * radius,
                    centerY + Mth.sin(angle) * radius,
                    color
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
    }

    private static void beginPositionColorRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void endPositionColorRender() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, int color) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    public record ScreenPoint(float x, float y) {
    }
}
