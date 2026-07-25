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
    private static final int SPIRITUAL_ROOT_SEGMENTS = 32;

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

    public static void drawLine(
            GuiGraphics guiGraphics,
            ScreenPoint start,
            ScreenPoint end,
            float thickness,
            int color
    ) {
        guiGraphics.flush();
        drawLine(guiGraphics.pose(), start.x, start.y, end.x, end.y, thickness, color);
    }

    public static void drawRing(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            float thickness,
            int color
    ) {
        if ((color >>> 24) == 0 || radius <= 0.0F || thickness <= 0.0F) {
            return;
        }
        List<ScreenPoint> ringPoints = new ArrayList<>(CIRCLE_SEGMENTS + 1);
        for (int segment = 0; segment <= CIRCLE_SEGMENTS; segment++) {
            float angle = (float) (Math.PI * 2.0D * segment / CIRCLE_SEGMENTS);
            ringPoints.add(new ScreenPoint(
                    centerX + Mth.cos(angle) * radius,
                    centerY + Mth.sin(angle) * radius
            ));
        }
        guiGraphics.flush();
        drawPolyline(guiGraphics.pose(), ringPoints, thickness, color);
    }

    public static void drawArc(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            float startDegrees,
            float endDegrees,
            float thickness,
            int color
    ) {
        if ((color >>> 24) == 0 || radius <= 0.0F || thickness <= 0.0F) {
            return;
        }
        float arcLength = Math.abs(endDegrees - startDegrees) / 360.0F;
        int segmentCount = Math.max(4, Math.round(CIRCLE_SEGMENTS * arcLength));
        List<ScreenPoint> arcPoints = new ArrayList<>(segmentCount + 1);
        for (int segment = 0; segment <= segmentCount; segment++) {
            float progress = (float) segment / segmentCount;
            float angle = (float) Math.toRadians(Mth.lerp(progress, startDegrees, endDegrees));
            arcPoints.add(new ScreenPoint(
                    centerX + Mth.cos(angle) * radius,
                    centerY + Mth.sin(angle) * radius
            ));
        }
        guiGraphics.flush();
        drawPolyline(guiGraphics.pose(), arcPoints, thickness, color);
    }

    public static void drawDisc(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        if ((color >>> 24) == 0 || radius <= 0.0F) {
            return;
        }
        guiGraphics.flush();
        drawCircle(guiGraphics.pose(), centerX, centerY, radius, color);
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

    public static void drawLivingSpiritualRoot(
            GuiGraphics guiGraphics,
            float centerX,
            float bottomY,
            float topY,
            int color,
            long currentTime,
            float alpha,
            boolean focused
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        guiGraphics.flush();
        int rootColor = color == 0x000000 ? 0xD7E3DF : color;
        int glowColor = ColorUtility.brighten(rootColor, 0.5F);
        PoseStack poseStack = guiGraphics.pose();
        drawDantianFormation(poseStack, centerX, bottomY, rootColor, currentTime, alpha);
        if (bottomY <= topY) {
            return;
        }

        float stemHeight = bottomY - topY;
        List<ScreenPoint> mainMeridian = createMainMeridian(centerX, bottomY, topY, currentTime);
        List<List<ScreenPoint>> branchMeridians = createBranchMeridians(
                mainMeridian,
                centerX,
                stemHeight,
                currentTime
        );

        drawPolyline(
                poseStack,
                mainMeridian,
                11.0F,
                ColorUtility.withAlpha(glowColor, alpha * 0.13F)
        );
        for (List<ScreenPoint> branchMeridian : branchMeridians) {
            drawPolyline(
                    poseStack,
                    branchMeridian,
                    6.5F,
                    ColorUtility.withAlpha(glowColor, alpha * 0.1F)
            );
        }

        int rootBaseColor = ColorUtility.darken(rootColor, 0.48F);
        int rootTipColor = ColorUtility.brighten(rootColor, 0.36F);
        drawGradientPolyline(poseStack, mainMeridian, 4.4F, rootBaseColor, rootTipColor, alpha);
        drawGradientPolyline(poseStack, mainMeridian, 1.25F, rootColor, glowColor, alpha * 0.92F);
        for (int index = 0; index < branchMeridians.size(); index++) {
            List<ScreenPoint> branchMeridian = branchMeridians.get(index);
            float branchAlpha = alpha * (0.72F - index * 0.035F);
            drawGradientPolyline(
                    poseStack,
                    branchMeridian,
                    2.35F,
                    ColorUtility.darken(rootColor, 0.36F),
                    rootTipColor,
                    branchAlpha
            );
        }

        drawMeridianFlow(poseStack, mainMeridian, glowColor, currentTime, alpha);
        drawSpiritBud(
                poseStack,
                centerX,
                topY,
                rootColor,
                glowColor,
                stemHeight,
                currentTime,
                alpha,
                focused
        );
    }

    private static void drawDantianFormation(
            PoseStack poseStack,
            float centerX,
            float bottomY,
            int rootColor,
            long currentTime,
            float alpha
    ) {
        float centerY = bottomY + 5.0F;
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.0022F);
        int formationColor = ColorUtility.blend(ColorUtility.darken(rootColor, 0.62F), 0x282632, 0.58F);
        drawEllipse(poseStack, centerX, centerY + 3.0F, 48.0F, 16.0F, ColorUtility.withAlpha(0x05070A, alpha * 0.7F));
        drawEllipse(poseStack, centerX, centerY, 36.0F, 11.0F, ColorUtility.withAlpha(formationColor, alpha * 0.66F));
        drawPolyline(
                poseStack,
                createEllipsePoints(centerX, centerY, 42.0F, 13.5F, 0.0F, 360.0F, CIRCLE_SEGMENTS),
                1.8F,
                ColorUtility.withAlpha(ColorUtility.brighten(rootColor, 0.22F), alpha * (0.22F + pulse * 0.09F))
        );
        drawPolyline(
                poseStack,
                createEllipsePoints(centerX, centerY, 29.0F, 8.5F, 0.0F, 360.0F, CIRCLE_SEGMENTS),
                1.1F,
                ColorUtility.withAlpha(ColorUtility.brighten(rootColor, 0.38F), alpha * 0.34F)
        );
        float rotation = currentTime * 0.018F;
        for (int ray = 0; ray < 8; ray++) {
            float angle = (float) Math.toRadians(rotation + ray * 45.0F);
            float innerX = centerX + Mth.cos(angle) * 12.0F;
            float innerY = centerY + Mth.sin(angle) * 3.8F;
            float outerX = centerX + Mth.cos(angle) * 34.0F;
            float outerY = centerY + Mth.sin(angle) * 10.5F;
            drawLine(
                    poseStack,
                    innerX,
                    innerY,
                    outerX,
                    outerY,
                    0.8F,
                    ColorUtility.withAlpha(rootColor, alpha * 0.24F)
            );
        }
        drawEllipse(
                poseStack,
                centerX,
                centerY,
                7.5F + pulse * 1.1F,
                3.1F + pulse * 0.45F,
                ColorUtility.withAlpha(ColorUtility.brighten(rootColor, 0.42F), alpha * 0.72F)
        );
    }

    private static List<ScreenPoint> createMainMeridian(
            float centerX,
            float bottomY,
            float topY,
            long currentTime
    ) {
        float stemHeight = bottomY - topY;
        float horizontalReach = Mth.clamp(stemHeight * 0.075F, 7.0F, 31.0F);
        float breathing = Math.min(1.6F, stemHeight * 0.006F);
        List<ScreenPoint> points = new ArrayList<>(SPIRITUAL_ROOT_SEGMENTS + 1);
        for (int segment = 0; segment <= SPIRITUAL_ROOT_SEGMENTS; segment++) {
            float progress = (float) segment / SPIRITUAL_ROOT_SEGMENTS;
            float rootEnvelope = Mth.sin(progress * Mth.PI);
            float organicCurve = Mth.sin(progress * Mth.PI * 2.25F + 0.35F) * 0.58F
                    + Mth.sin(progress * Mth.PI * 5.4F - 0.7F) * 0.23F
                    + Mth.sin(progress * Mth.PI * 9.0F) * 0.08F;
            float livingSway = Mth.sin(currentTime * 0.00135F + progress * 5.0F)
                    * breathing
                    * rootEnvelope;
            points.add(new ScreenPoint(
                    centerX + organicCurve * horizontalReach * rootEnvelope + livingSway,
                    Mth.lerp(progress, bottomY, topY)
            ));
        }
        return points;
    }

    private static List<List<ScreenPoint>> createBranchMeridians(
            List<ScreenPoint> mainMeridian,
            float centerX,
            float stemHeight,
            long currentTime
    ) {
        float[] branchProgresses = {0.13F, 0.22F, 0.34F, 0.47F, 0.61F, 0.74F};
        List<List<ScreenPoint>> branches = new ArrayList<>(branchProgresses.length);
        float horizontalReach = Mth.clamp(stemHeight * 0.11F, 12.0F, 43.0F);
        for (int branchIndex = 0; branchIndex < branchProgresses.length; branchIndex++) {
            float branchProgress = branchProgresses[branchIndex];
            ScreenPoint attachment = interpolatePolyline(mainMeridian, branchProgress);
            float direction = branchIndex % 2 == 0 ? -1.0F : 1.0F;
            float branchLength = horizontalReach * (1.0F - branchProgress * 0.34F);
            float curl = Mth.sin(currentTime * 0.0011F + branchIndex * 1.7F) * 1.2F;
            List<ScreenPoint> branch = new ArrayList<>(9);
            for (int segment = 0; segment <= 8; segment++) {
                float progress = (float) segment / 8.0F;
                float easedProgress = 1.0F - (1.0F - progress) * (1.0F - progress);
                float curlWave = Mth.sin(progress * Mth.PI * (1.45F + branchIndex * 0.08F));
                branch.add(new ScreenPoint(
                        Mth.lerp(easedProgress, attachment.x, centerX + direction * branchLength)
                                + direction * curlWave * branchLength * 0.18F
                                + curl * progress,
                        attachment.y
                                + stemHeight * (0.025F + branchProgress * 0.025F) * progress
                                - curlWave * stemHeight * 0.028F
                ));
            }
            branches.add(branch);
        }
        return branches;
    }

    private static void drawMeridianFlow(
            PoseStack poseStack,
            List<ScreenPoint> mainMeridian,
            int glowColor,
            long currentTime,
            float alpha
    ) {
        float timeProgress = currentTime % 2800L / 2800.0F;
        for (int mote = 0; mote < 4; mote++) {
            float progress = (timeProgress + mote * 0.24F) % 1.0F;
            ScreenPoint point = interpolatePolyline(mainMeridian, progress);
            float pulse = 0.65F + 0.35F * Mth.sin(currentTime * 0.005F + mote * 1.8F);
            drawCircle(poseStack, point.x, point.y, 3.8F * pulse, ColorUtility.withAlpha(glowColor, alpha * 0.12F));
            drawCircle(poseStack, point.x, point.y, 1.35F * pulse, ColorUtility.withAlpha(glowColor, alpha * 0.88F));
        }
    }

    private static void drawSpiritBud(
            PoseStack poseStack,
            float centerX,
            float topY,
            int rootColor,
            int glowColor,
            float stemHeight,
            long currentTime,
            float alpha,
            boolean focused
    ) {
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * (focused ? 0.006F : 0.004F));
        float budScale = Mth.clamp(stemHeight / 95.0F, 0.62F, 1.0F) * (0.94F + pulse * 0.09F);
        drawCircle(
                poseStack,
                centerX,
                topY,
                (focused ? 15.0F : 12.0F) * budScale,
                ColorUtility.withAlpha(glowColor, alpha * (0.08F + pulse * 0.08F))
        );
        for (int petal = 0; petal < 5; petal++) {
            poseStack.pushPose();
            poseStack.translate(centerX, topY, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(petal * 72.0F + currentTime * 0.008F));
            drawEllipse(
                    poseStack,
                    0.0F,
                    -5.0F * budScale,
                    2.5F * budScale,
                    5.2F * budScale,
                    ColorUtility.withAlpha(ColorUtility.brighten(rootColor, 0.28F), alpha * 0.68F)
            );
            poseStack.popPose();
        }
        drawCircle(poseStack, centerX, topY, 4.0F * budScale, ColorUtility.withAlpha(glowColor, alpha * 0.28F));
        drawCircle(
                poseStack,
                centerX,
                topY,
                1.75F * budScale,
                ColorUtility.withAlpha(ColorUtility.brighten(glowColor, 0.35F), alpha)
        );
    }

    private static List<ScreenPoint> createEllipsePoints(
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float startDegrees,
            float endDegrees,
            int segmentCount
    ) {
        List<ScreenPoint> points = new ArrayList<>(segmentCount + 1);
        for (int segment = 0; segment <= segmentCount; segment++) {
            float progress = (float) segment / segmentCount;
            float angle = (float) Math.toRadians(Mth.lerp(progress, startDegrees, endDegrees));
            points.add(new ScreenPoint(
                    centerX + Mth.cos(angle) * radiusX,
                    centerY + Mth.sin(angle) * radiusY
            ));
        }
        return points;
    }

    private static ScreenPoint interpolatePolyline(List<ScreenPoint> points, float progress) {
        float scaledIndex = Mth.clamp(progress, 0.0F, 1.0F) * (points.size() - 1);
        int startIndex = Math.min((int) scaledIndex, points.size() - 2);
        float segmentProgress = scaledIndex - startIndex;
        ScreenPoint start = points.get(startIndex);
        ScreenPoint end = points.get(startIndex + 1);
        return new ScreenPoint(
                Mth.lerp(segmentProgress, start.x, end.x),
                Mth.lerp(segmentProgress, start.y, end.y)
        );
    }

    private static void drawGradientPolyline(
            PoseStack poseStack,
            List<ScreenPoint> points,
            float thickness,
            int startColor,
            int endColor,
            float alpha
    ) {
        if (alpha <= 0.0F || points.size() < 2) {
            return;
        }
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 0; index < points.size() - 1; index++) {
            float progress = (float) index / (points.size() - 2);
            int color = ColorUtility.withAlpha(ColorUtility.blend(startColor, endColor, progress), alpha);
            appendLineQuad(bufferBuilder, matrix, points.get(index), points.get(index + 1), thickness, color);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
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
