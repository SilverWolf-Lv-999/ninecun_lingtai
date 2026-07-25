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
    private static final int SPIRITUAL_ROOT_STRAND_COUNT = 3;
    private static final int SPIRITUAL_ROOT_NODE_COUNT = 9;
    private static final int SPIRITUAL_ROOT_MOTE_COUNT = 12;
    private static final int SPIRITUAL_ROOT_NODE_COLOR = 0xF2D98B;
    private static final int[] EARLIER_HEAVEN_TRIGRAM_PATTERNS = {
            0b111,
            0b110,
            0b010,
            0b100,
            0b000,
            0b001,
            0b101,
            0b011
    };

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

    public static ScreenPoint drawLivingSpiritualRoot(
            GuiGraphics guiGraphics,
            float centerX,
            float bottomY,
            float topY,
            float maximumTopY,
            int color,
            long currentTime,
            float alpha,
            boolean focused
    ) {
        if (alpha <= 0.0F) {
            return new ScreenPoint(centerX, topY);
        }
        guiGraphics.flush();
        int rootColor = color == 0x000000 ? 0xD7E3DF : color;
        int glowColor = ColorUtility.brighten(rootColor, 0.5F);
        PoseStack poseStack = guiGraphics.pose();
        drawDantianTaiji(poseStack, centerX, bottomY, rootColor, currentTime, alpha);
        float fullHeight = Math.max(1.0F, bottomY - maximumTopY);
        float rootProgress = Mth.clamp((bottomY - topY) / fullHeight, 0.0F, 1.0F);
        List<ScreenPoint> fullMeridian = createSpiritualRootMeridian(
                centerX,
                bottomY,
                maximumTopY,
                currentTime
        );
        List<ScreenPoint> activeMeridian = slicePolyline(fullMeridian, rootProgress);
        ScreenPoint rootMarker = interpolatePolyline(fullMeridian, rootProgress);

        drawSpiritualRootPotential(poseStack, fullMeridian, rootColor, glowColor, alpha);
        if (activeMeridian.size() > 1) {
            drawEnchantingAura(poseStack, activeMeridian, glowColor, currentTime, alpha);
            drawSpiritualRootBody(poseStack, activeMeridian, rootColor, glowColor, alpha);
            drawBraidedMeridians(poseStack, activeMeridian, rootColor, glowColor, currentTime, alpha);
            drawAscendingSpiritMotes(poseStack, activeMeridian, glowColor, currentTime, alpha);
        }
        drawSpiritNodes(poseStack, fullMeridian, rootProgress, rootColor, glowColor, alpha);
        drawCurrentSpiritNode(poseStack, rootMarker, rootColor, glowColor, currentTime, alpha, focused);
        return rootMarker;
    }

    private static void drawDantianTaiji(
            PoseStack poseStack,
            float centerX,
            float bottomY,
            int rootColor,
            long currentTime,
            float alpha
    ) {
        float centerY = bottomY + 5.0F;
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.0022F);
        int taijiColor = ColorUtility.darken(rootColor, 0.12F);
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.0F);
        poseStack.scale(1.0F, 0.34F, 1.0F);
        drawEarlierHeavenBagua(poseStack, taijiColor, rootColor, alpha, pulse);
        poseStack.popPose();
    }

    private static void drawEarlierHeavenBagua(
            PoseStack poseStack,
            int taijiColor,
            int rootColor,
            float alpha,
            float pulse
    ) {
        drawTransparentTaiji(
                poseStack,
                0.0F,
                0.0F,
                22.0F,
                22.0F,
                ColorUtility.withAlpha(taijiColor, alpha * 0.9F)
        );
        drawPolyline(
                poseStack,
                createEllipsePoints(0.0F, 0.0F, 23.8F, 23.8F, 0.0F, 360.0F, CIRCLE_SEGMENTS),
                2.8F + pulse * 0.25F,
                ColorUtility.withAlpha(
                        ColorUtility.brighten(rootColor, 0.3F),
                        alpha * (0.56F + pulse * 0.12F)
                )
        );
        int trigramColor = ColorUtility.withAlpha(
                ColorUtility.brighten(rootColor, 0.08F),
                alpha * (0.82F + pulse * 0.08F)
        );
        for (int trigramIndex = 0; trigramIndex < EARLIER_HEAVEN_TRIGRAM_PATTERNS.length; trigramIndex++) {
            float angleDegrees = -90.0F + trigramIndex * 45.0F;
            float angleRadians = (float) Math.toRadians(angleDegrees);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angleRadians) * 37.0F, Mth.sin(angleRadians) * 37.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angleDegrees + 90.0F));
            drawTrigram(poseStack, EARLIER_HEAVEN_TRIGRAM_PATTERNS[trigramIndex], trigramColor);
            poseStack.popPose();
        }
    }

    private static void drawTrigram(PoseStack poseStack, int pattern, int color) {
        float halfWidth = 7.0F;
        float gapHalfWidth = 1.7F;
        float lineSpacing = 4.4F;
        float lineThickness = 3.4F;
        for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
            float lineY = (lineIndex - 1) * lineSpacing;
            boolean solid = (pattern & 1 << (2 - lineIndex)) != 0;
            if (solid) {
                drawLine(poseStack, -halfWidth, lineY, halfWidth, lineY, lineThickness, color);
                continue;
            }
            drawLine(poseStack, -halfWidth, lineY, -gapHalfWidth, lineY, lineThickness, color);
            drawLine(poseStack, gapHalfWidth, lineY, halfWidth, lineY, lineThickness, color);
        }
    }

    private static void drawTransparentTaiji(
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
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.0F);
        poseStack.scale(radiusX, radiusY, 1.0F);
        drawTransparentTaijiLobe(poseStack, color);
        drawCircle(poseStack, 0.0F, -0.5F, 0.115F, color);
        poseStack.popPose();
    }

    private static void drawTransparentTaijiLobe(PoseStack poseStack, int color) {
        beginPositionColorRender();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
            float startY = -1.0F + 2.0F * segment / CIRCLE_SEGMENTS;
            float endY = -1.0F + 2.0F * (segment + 1) / CIRCLE_SEGMENTS;
            appendTaijiStrip(bufferBuilder, matrix, startY, endY, color);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        endPositionColorRender();
    }

    private static void appendTaijiStrip(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float startY,
            float endY,
            int color
    ) {
        float startLeft = -getCircleHalfWidth(startY, 0.0F, 1.0F);
        float endLeft = -getCircleHalfWidth(endY, 0.0F, 1.0F);
        float startRight = getTaijiLobeRight(startY);
        float endRight = getTaijiLobeRight(endY);
        if (endY <= 0.0F) {
            appendQuad(
                    bufferBuilder,
                    matrix,
                    startLeft,
                    startY,
                    startRight,
                    startY,
                    endRight,
                    endY,
                    endLeft,
                    endY,
                    color
            );
            return;
        }
        float startHole = getCircleHalfWidth(startY, 0.5F, 0.115F);
        float endHole = getCircleHalfWidth(endY, 0.5F, 0.115F);
        appendQuad(
                bufferBuilder,
                matrix,
                startLeft,
                startY,
                -startHole,
                startY,
                -endHole,
                endY,
                endLeft,
                endY,
                color
        );
        appendQuad(
                bufferBuilder,
                matrix,
                startHole,
                startY,
                startRight,
                startY,
                endRight,
                endY,
                endHole,
                endY,
                color
        );
    }

    private static float getTaijiLobeRight(float y) {
        return y < 0.0F
                ? -getCircleHalfWidth(y, -0.5F, 0.5F)
                : getCircleHalfWidth(y, 0.5F, 0.5F);
    }

    private static float getCircleHalfWidth(float y, float centerY, float radius) {
        float verticalDistance = y - centerY;
        float squaredWidth = radius * radius - verticalDistance * verticalDistance;
        return squaredWidth <= 0.0F ? 0.0F : Mth.sqrt(squaredWidth);
    }

    private static void drawSpiritualRootPotential(
            PoseStack poseStack,
            List<ScreenPoint> fullMeridian,
            int rootColor,
            int glowColor,
            float alpha
    ) {
        int dormantColor = ColorUtility.blend(ColorUtility.darken(rootColor, 0.68F), 0x68758A, 0.56F);
        drawTaperedGradientPolyline(
                poseStack,
                fullMeridian,
                8.0F,
                13.0F,
                5.0F,
                dormantColor,
                glowColor,
                alpha * 0.035F
        );
    }

    private static void drawSpiritualRootBody(
            PoseStack poseStack,
            List<ScreenPoint> activeMeridian,
            int rootColor,
            int glowColor,
            float alpha
    ) {
        int rootShadowColor = ColorUtility.blend(ColorUtility.darken(rootColor, 0.7F), 0x0C1420, 0.42F);
        int rootBaseColor = ColorUtility.darken(rootColor, 0.48F);
        int rootTipColor = ColorUtility.brighten(rootColor, 0.42F);
        drawTaperedGradientPolyline(
                poseStack,
                activeMeridian,
                23.0F,
                38.0F,
                16.0F,
                rootColor,
                glowColor,
                alpha * 0.045F
        );
        drawTaperedGradientPolyline(
                poseStack,
                activeMeridian,
                16.0F,
                28.0F,
                11.0F,
                rootColor,
                glowColor,
                alpha * 0.085F
        );
        drawTaperedGradientPolyline(
                poseStack,
                activeMeridian,
                10.5F,
                20.0F,
                7.5F,
                rootShadowColor,
                rootBaseColor,
                alpha * 0.94F
        );
        drawTaperedGradientPolyline(
                poseStack,
                activeMeridian,
                7.5F,
                15.5F,
                5.2F,
                rootBaseColor,
                rootTipColor,
                alpha
        );
        drawTaperedGradientPolyline(
                poseStack,
                activeMeridian,
                2.2F,
                4.2F,
                1.2F,
                rootColor,
                glowColor,
                alpha * 0.82F
        );
    }

    private static void drawBraidedMeridians(
            PoseStack poseStack,
            List<ScreenPoint> activeMeridian,
            int rootColor,
            int glowColor,
            long currentTime,
            float alpha
    ) {
        for (int strandIndex = 0; strandIndex < SPIRITUAL_ROOT_STRAND_COUNT; strandIndex++) {
            List<ScreenPoint> braidedMeridian = createBraidedMeridian(
                    activeMeridian,
                    strandIndex,
                    currentTime
            );
            int strandColor = switch (strandIndex) {
                case 0 -> ColorUtility.brighten(rootColor, 0.3F);
                case 1 -> ColorUtility.blend(glowColor, SPIRITUAL_ROOT_NODE_COLOR, 0.34F);
                default -> ColorUtility.brighten(glowColor, 0.22F);
            };
            drawTaperedGradientPolyline(
                    poseStack,
                    braidedMeridian,
                    3.2F,
                    5.0F,
                    1.6F,
                    ColorUtility.darken(rootColor, 0.3F),
                    strandColor,
                    alpha * 0.3F
            );
            drawTaperedGradientPolyline(
                    poseStack,
                    braidedMeridian,
                    0.8F,
                    1.8F,
                    0.55F,
                    rootColor,
                    strandColor,
                    alpha * 0.94F
            );
        }
    }

    private static void drawEnchantingAura(
            PoseStack poseStack,
            List<ScreenPoint> activeMeridian,
            int glowColor,
            long currentTime,
            float alpha
    ) {
        for (int auraIndex = 0; auraIndex < 2; auraIndex++) {
            List<ScreenPoint> auraMeridian = createAuraMeridian(activeMeridian, auraIndex, currentTime);
            drawPolyline(
                    poseStack,
                    auraMeridian,
                    7.0F,
                    ColorUtility.withAlpha(glowColor, alpha * 0.035F)
            );
            drawPolyline(
                    poseStack,
                    auraMeridian,
                    1.15F,
                    ColorUtility.withAlpha(glowColor, alpha * 0.24F)
            );
        }
    }

    private static List<ScreenPoint> createSpiritualRootMeridian(
            float centerX,
            float bottomY,
            float maximumTopY,
            long currentTime
    ) {
        float stemHeight = bottomY - maximumTopY;
        float horizontalReach = Mth.clamp(stemHeight * 0.032F, 8.0F, 18.0F);
        float breathing = Math.min(1.25F, stemHeight * 0.004F);
        List<ScreenPoint> points = new ArrayList<>(SPIRITUAL_ROOT_SEGMENTS + 1);
        for (int segment = 0; segment <= SPIRITUAL_ROOT_SEGMENTS; segment++) {
            float progress = (float) segment / SPIRITUAL_ROOT_SEGMENTS;
            float rootEnvelope = Mth.sin(progress * Mth.PI);
            float organicCurve = Mth.sin(progress * Mth.PI * 2.1F + 0.42F) * 0.72F
                    + Mth.sin(progress * Mth.PI * 4.7F - 0.8F) * 0.2F;
            float livingSway = Mth.sin(currentTime * 0.0011F + progress * 4.6F)
                    * breathing
                    * rootEnvelope;
            points.add(new ScreenPoint(
                    centerX + organicCurve * horizontalReach * rootEnvelope + livingSway,
                    Mth.lerp(progress, bottomY, maximumTopY)
            ));
        }
        return points;
    }

    private static List<ScreenPoint> createBraidedMeridian(
            List<ScreenPoint> activeMeridian,
            int strandIndex,
            long currentTime
    ) {
        List<ScreenPoint> braidedMeridian = new ArrayList<>(activeMeridian.size());
        float phase = (float) (Math.PI * 2.0D * strandIndex / SPIRITUAL_ROOT_STRAND_COUNT);
        for (int pointIndex = 0; pointIndex < activeMeridian.size(); pointIndex++) {
            float progress = (float) pointIndex / Math.max(1, activeMeridian.size() - 1);
            ScreenPoint centerPoint = activeMeridian.get(pointIndex);
            ScreenPoint normal = getPolylineNormal(activeMeridian, pointIndex);
            float envelope = Mth.sin(progress * Mth.PI);
            float weave = Mth.sin(progress * Mth.PI * 8.0F + phase + currentTime * 0.00065F)
                    * (3.2F + envelope * 2.1F)
                    * envelope;
            braidedMeridian.add(new ScreenPoint(
                    centerPoint.x + normal.x * weave,
                    centerPoint.y + normal.y * weave
            ));
        }
        return braidedMeridian;
    }

    private static List<ScreenPoint> createAuraMeridian(
            List<ScreenPoint> activeMeridian,
            int auraIndex,
            long currentTime
    ) {
        List<ScreenPoint> auraMeridian = new ArrayList<>(activeMeridian.size());
        float phase = auraIndex * Mth.PI;
        for (int pointIndex = 0; pointIndex < activeMeridian.size(); pointIndex++) {
            float progress = (float) pointIndex / Math.max(1, activeMeridian.size() - 1);
            ScreenPoint centerPoint = activeMeridian.get(pointIndex);
            ScreenPoint normal = getPolylineNormal(activeMeridian, pointIndex);
            float envelope = Mth.sin(progress * Mth.PI);
            float orbit = Mth.sin(progress * Mth.PI * 3.6F + phase + currentTime * 0.0015F)
                    * (7.0F + envelope * 4.5F)
                    * envelope;
            auraMeridian.add(new ScreenPoint(
                    centerPoint.x + normal.x * orbit,
                    centerPoint.y + normal.y * orbit
            ));
        }
        return auraMeridian;
    }

    private static void drawAscendingSpiritMotes(
            PoseStack poseStack,
            List<ScreenPoint> activeMeridian,
            int glowColor,
            long currentTime,
            float alpha
    ) {
        float timeProgress = currentTime % 3600L / 3600.0F;
        int moteColor = ColorUtility.blend(glowColor, SPIRITUAL_ROOT_NODE_COLOR, 0.16F);
        for (int moteIndex = 0; moteIndex < SPIRITUAL_ROOT_MOTE_COUNT; moteIndex++) {
            float progress = (timeProgress + (float) moteIndex / SPIRITUAL_ROOT_MOTE_COUNT) % 1.0F;
            ScreenPoint point = interpolatePolyline(activeMeridian, progress);
            int pointIndex = Math.min(
                    Math.round(progress * (activeMeridian.size() - 1)),
                    activeMeridian.size() - 1
            );
            ScreenPoint normal = getPolylineNormal(activeMeridian, pointIndex);
            float orbitPhase = currentTime * 0.0024F + moteIndex * 1.83F;
            float orbitalDistance = moteIndex % 3 == 0
                    ? 0.0F
                    : Mth.sin(orbitPhase) * (7.0F + moteIndex % 4 * 1.7F);
            float pulse = 0.62F + 0.38F * Mth.sin(currentTime * 0.005F + moteIndex * 1.4F);
            float moteX = point.x + normal.x * orbitalDistance;
            float moteY = point.y + normal.y * orbitalDistance;
            drawCircle(
                    poseStack,
                    moteX,
                    moteY,
                    4.6F * pulse,
                    ColorUtility.withAlpha(moteColor, alpha * 0.075F)
            );
            drawCircle(
                    poseStack,
                    moteX,
                    moteY,
                    1.25F * pulse,
                    ColorUtility.withAlpha(moteColor, alpha * 0.9F)
            );
        }
    }

    private static void drawSpiritNodes(
            PoseStack poseStack,
            List<ScreenPoint> fullMeridian,
            float rootProgress,
            int rootColor,
            int glowColor,
            float alpha
    ) {
        int nodeColor = ColorUtility.blend(glowColor, SPIRITUAL_ROOT_NODE_COLOR, 0.58F);
        for (int inch = 1; inch <= SPIRITUAL_ROOT_NODE_COUNT; inch++) {
            float nodeProgress = (float) inch / SPIRITUAL_ROOT_NODE_COUNT;
            ScreenPoint nodePoint = interpolatePolyline(fullMeridian, nodeProgress);
            boolean reached = nodeProgress <= rootProgress + 0.001F;
            boolean maximumNode = inch == SPIRITUAL_ROOT_NODE_COUNT;
            float nodeAlpha = alpha * (reached ? 0.78F : 0.22F);
            float radiusX = maximumNode ? 8.5F : 6.5F;
            float radiusY = maximumNode ? 3.1F : 2.4F;
            drawEllipse(
                    poseStack,
                    nodePoint.x,
                    nodePoint.y,
                    radiusX + 4.5F,
                    radiusY + 2.2F,
                    ColorUtility.withAlpha(nodeColor, nodeAlpha * 0.08F)
            );
            drawPolyline(
                    poseStack,
                    createEllipsePoints(
                            nodePoint.x,
                            nodePoint.y,
                            radiusX,
                            radiusY,
                            0.0F,
                            360.0F,
                            CIRCLE_SEGMENTS
                    ),
                    maximumNode ? 1.55F : 1.15F,
                    ColorUtility.withAlpha(nodeColor, nodeAlpha)
            );
            drawCircle(
                    poseStack,
                    nodePoint.x,
                    nodePoint.y,
                    reached ? 1.45F : 0.9F,
                    ColorUtility.withAlpha(
                            reached ? ColorUtility.brighten(rootColor, 0.46F) : nodeColor,
                            nodeAlpha
                    )
            );
        }
    }

    private static void drawCurrentSpiritNode(
            PoseStack poseStack,
            ScreenPoint rootMarker,
            int rootColor,
            int glowColor,
            long currentTime,
            float alpha,
            boolean focused
    ) {
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * (focused ? 0.006F : 0.004F));
        int nodeColor = ColorUtility.blend(glowColor, SPIRITUAL_ROOT_NODE_COLOR, 0.48F);
        float ringRadiusX = (focused ? 14.0F : 12.0F) + pulse * 1.8F;
        float ringRadiusY = (focused ? 5.2F : 4.4F) + pulse * 0.65F;
        drawEllipse(
                poseStack,
                rootMarker.x,
                rootMarker.y,
                ringRadiusX + 6.0F,
                ringRadiusY + 3.0F,
                ColorUtility.withAlpha(nodeColor, alpha * (0.07F + pulse * 0.05F))
        );
        drawPolyline(
                poseStack,
                createEllipsePoints(
                        rootMarker.x,
                        rootMarker.y,
                        ringRadiusX,
                        ringRadiusY,
                        0.0F,
                        360.0F,
                        CIRCLE_SEGMENTS
                ),
                focused ? 2.4F : 1.8F,
                ColorUtility.withAlpha(nodeColor, alpha * (0.78F + pulse * 0.2F))
        );
        drawPolyline(
                poseStack,
                createEllipsePoints(
                        rootMarker.x,
                        rootMarker.y,
                        ringRadiusX * 0.68F,
                        ringRadiusY * 0.64F,
                        0.0F,
                        360.0F,
                        CIRCLE_SEGMENTS
                ),
                1.0F,
                ColorUtility.withAlpha(glowColor, alpha * 0.72F)
        );
        drawCircle(
                poseStack,
                rootMarker.x,
                rootMarker.y,
                5.2F + pulse * 0.75F,
                ColorUtility.withAlpha(glowColor, alpha * 0.16F)
        );
        drawCircle(
                poseStack,
                rootMarker.x,
                rootMarker.y,
                2.35F + pulse * 0.28F,
                ColorUtility.withAlpha(ColorUtility.brighten(rootColor, 0.58F), alpha)
        );
    }

    private static List<ScreenPoint> slicePolyline(List<ScreenPoint> points, float progress) {
        float scaledIndex = Mth.clamp(progress, 0.0F, 1.0F) * (points.size() - 1);
        int endIndex = Math.min((int) scaledIndex, points.size() - 1);
        List<ScreenPoint> slicedPoints = new ArrayList<>(endIndex + 2);
        for (int pointIndex = 0; pointIndex <= endIndex; pointIndex++) {
            slicedPoints.add(points.get(pointIndex));
        }
        if (endIndex < points.size() - 1) {
            float segmentProgress = scaledIndex - endIndex;
            if (segmentProgress > 0.0001F) {
                ScreenPoint start = points.get(endIndex);
                ScreenPoint end = points.get(endIndex + 1);
                slicedPoints.add(new ScreenPoint(
                        Mth.lerp(segmentProgress, start.x, end.x),
                        Mth.lerp(segmentProgress, start.y, end.y)
                ));
            }
        }
        return slicedPoints;
    }

    private static ScreenPoint getPolylineNormal(List<ScreenPoint> points, int pointIndex) {
        ScreenPoint previous = points.get(Math.max(0, pointIndex - 1));
        ScreenPoint next = points.get(Math.min(points.size() - 1, pointIndex + 1));
        float deltaX = next.x - previous.x;
        float deltaY = next.y - previous.y;
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return new ScreenPoint(1.0F, 0.0F);
        }
        return new ScreenPoint(-deltaY / length, deltaX / length);
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

    private static void drawTaperedGradientPolyline(
            PoseStack poseStack,
            List<ScreenPoint> points,
            float startThickness,
            float middleThickness,
            float endThickness,
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
            float progress = (index + 0.5F) / (points.size() - 1);
            float thickness = progress < 0.52F
                    ? Mth.lerp(progress / 0.52F, startThickness, middleThickness)
                    : Mth.lerp((progress - 0.52F) / 0.48F, middleThickness, endThickness);
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

    private static void appendQuad(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float firstX,
            float firstY,
            float secondX,
            float secondY,
            float thirdX,
            float thirdY,
            float fourthX,
            float fourthY,
            int color
    ) {
        vertex(bufferBuilder, matrix, firstX, firstY, color);
        vertex(bufferBuilder, matrix, secondX, secondY, color);
        vertex(bufferBuilder, matrix, thirdX, thirdY, color);
        vertex(bufferBuilder, matrix, fourthX, fourthY, color);
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
