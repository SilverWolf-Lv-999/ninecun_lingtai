package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.render.ElementFlowShader;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ElementRelationRenderUtility {
    private static final int RING_SEGMENTS = 56;
    private static final float GENERATION_RING_SCALE = 1.34F;
    private static final float RESTRAINT_RING_SCALE = 1.24F;

    private ElementRelationRenderUtility() {
    }

    public static void drawRelation(
            GuiGraphics guiGraphics,
            ElementType sourceElement,
            ElementType targetElement,
            ScreenPoint sourcePosition,
            ScreenPoint targetPosition,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F || sourceElement == targetElement) {
            return;
        }
        ElementRelation relation = sourceElement.getRelationTo(targetElement);
        ScreenPath path = trimPath(sourcePosition, targetPosition, nodeRadius, nodeRadius);
        int sourceColor = SpiritualRootUtility.getElementColor(sourceElement);
        int targetColor = SpiritualRootUtility.getElementColor(targetElement);
        float flowThickness = Mth.clamp(nodeRadius * 0.19F, 2.8F, 6.2F);
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.0042F + targetElement.ordinal());

        switch (relation) {
            case GENERATES -> {
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        1.0F,
                        1.0F,
                        alpha
                );
                drawFlowingRing(
                        guiGraphics,
                        targetPosition,
                        nodeRadius * GENERATION_RING_SCALE + pulse * 1.6F,
                        flowThickness * 0.72F,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        1.0F,
                        alpha * 0.9F
                );
            }
            case GENERATED_BY -> {
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        1.0F,
                        1.0F,
                        alpha * 0.52F
                );
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness * 0.62F,
                        targetColor,
                        targetElement,
                        currentTime,
                        -1.0F,
                        1.0F,
                        alpha * 0.82F
                );
                drawFlowingRing(
                        guiGraphics,
                        sourcePosition,
                        nodeRadius * GENERATION_RING_SCALE + pulse * 1.3F,
                        flowThickness * 0.58F,
                        targetColor,
                        targetElement,
                        currentTime,
                        -1.0F,
                        alpha * 0.68F
                );
            }
            case OVERCOMES -> {
                float cutoff = 0.84F;
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        1.0F,
                        cutoff,
                        alpha
                );
                drawBarrier(guiGraphics, path, cutoff, targetColor, sourceColor, nodeRadius, currentTime, alpha, false);
                drawFlowingArc(
                        guiGraphics,
                        targetPosition,
                        nodeRadius * RESTRAINT_RING_SCALE,
                        flowThickness * 0.7F,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        alpha * 0.82F
                );
            }
            case OVERCOME_BY -> {
                float cutoff = 0.7F;
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        1.0F,
                        cutoff,
                        alpha
                );
                drawFlowPath(
                        guiGraphics,
                        List.of(path.start(), path.end()),
                        flowThickness * 0.58F,
                        sourceColor,
                        sourceElement,
                        currentTime,
                        -1.0F,
                        cutoff,
                        alpha * 0.42F
                );
                drawBarrier(guiGraphics, path, cutoff, targetColor, sourceColor, nodeRadius, currentTime, alpha, true);
            }
            case NEUTRAL -> drawFlowPath(
                    guiGraphics,
                    List.of(path.start(), path.end()),
                    flowThickness,
                    sourceColor,
                    sourceElement,
                    currentTime,
                    1.0F,
                    1.0F,
                    alpha * 0.76F
            );
            case SAME, SUPPRESSES, SUPPRESSED_BY -> {
            }
        }
    }

    public static void drawYinYangBalance(
            GuiGraphics guiGraphics,
            ScreenPoint centerPosition,
            float taijiRadius,
            List<ScreenPoint> elementPositions,
            Set<ElementType> selectedElements,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F || elementPositions.size() != 5) {
            return;
        }
        float flowThickness = Mth.clamp(nodeRadius * 0.13F, 2.2F, 4.5F);
        float balancePulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.003F);
        for (int index = 0; index < elementPositions.size(); index++) {
            ElementType targetElement = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            ScreenPoint targetPosition = elementPositions.get(index);
            ScreenPath path = trimPath(centerPosition, targetPosition, taijiRadius * 1.18F, nodeRadius);
            boolean drawingExcessInward = selectedElements.contains(targetElement);
            int targetColor = SpiritualRootUtility.getElementColor(targetElement);
            drawFlowPath(
                    guiGraphics,
                    List.of(path.start(), path.end()),
                    flowThickness,
                    targetColor,
                    targetElement,
                    currentTime,
                    drawingExcessInward ? -1.0F : 1.0F,
                    1.0F,
                    alpha * (drawingExcessInward ? 0.62F : 0.46F)
            );
            AptitudeRenderUtility.drawRing(
                    guiGraphics,
                    targetPosition.x(),
                    targetPosition.y(),
                    nodeRadius * 1.2F + balancePulse * 1.4F,
                    flowThickness * 0.42F,
                    ColorUtility.withAlpha(targetColor, alpha * 0.28F)
            );
        }
        drawFlowingRing(
                guiGraphics,
                centerPosition,
                taijiRadius * (1.38F + balancePulse * 0.08F),
                flowThickness * 0.72F,
                0xF2EFE7,
                ElementType.YIN_YANG,
                currentTime,
                1.0F,
                alpha * 0.88F
        );
        drawFlowingRing(
                guiGraphics,
                centerPosition,
                taijiRadius * (1.67F - balancePulse * 0.08F),
                flowThickness * 0.56F,
                0xD8D5CF,
                ElementType.YIN_YANG,
                currentTime,
                -1.0F,
                alpha * 0.62F
        );
    }

    private static void drawBarrier(
            GuiGraphics guiGraphics,
            ScreenPath path,
            float cutoff,
            int barrierColor,
            int flowColor,
            float nodeRadius,
            long currentTime,
            float alpha,
            boolean strongBarrier
    ) {
        ScreenPoint impactPosition = interpolate(path.start(), path.end(), cutoff);
        float deltaX = path.end().x() - path.start().x();
        float deltaY = path.end().y() - path.start().y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return;
        }
        float normalX = -deltaY / length;
        float normalY = deltaX / length;
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * (strongBarrier ? 0.012F : 0.018F));
        float halfLength = nodeRadius * (strongBarrier ? 0.62F : 0.48F) + pulse * 2.0F;
        ScreenPoint barrierStart = new ScreenPoint(
                impactPosition.x() - normalX * halfLength,
                impactPosition.y() - normalY * halfLength
        );
        ScreenPoint barrierEnd = new ScreenPoint(
                impactPosition.x() + normalX * halfLength,
                impactPosition.y() + normalY * halfLength
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                barrierStart,
                barrierEnd,
                strongBarrier ? 3.2F : 2.1F,
                ColorUtility.withAlpha(ColorUtility.brighten(barrierColor, 0.34F), alpha * (strongBarrier ? 0.92F : 0.66F))
        );
        if (strongBarrier) {
            float offset = 3.5F + pulse * 1.5F;
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    new ScreenPoint(barrierStart.x() - deltaX / length * offset, barrierStart.y() - deltaY / length * offset),
                    new ScreenPoint(barrierEnd.x() - deltaX / length * offset, barrierEnd.y() - deltaY / length * offset),
                    1.3F,
                    ColorUtility.withAlpha(barrierColor, alpha * 0.58F)
            );
        }
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                impactPosition.x(),
                impactPosition.y(),
                3.0F + pulse * (strongBarrier ? 8.0F : 5.0F),
                strongBarrier ? 1.8F : 1.2F,
                ColorUtility.withAlpha(flowColor, alpha * (1.0F - pulse) * 0.72F)
        );
    }

    private static void drawFlowingArc(
            GuiGraphics guiGraphics,
            ScreenPoint centerPosition,
            float radius,
            float thickness,
            int color,
            ElementType element,
            long currentTime,
            float alpha
    ) {
        List<ScreenPoint> arcPoints = new ArrayList<>(RING_SEGMENTS);
        int arcSegments = Math.round(RING_SEGMENTS * 0.72F);
        float rotation = currentTime * 0.05F % 360.0F;
        for (int segment = 0; segment <= arcSegments; segment++) {
            float progress = (float) segment / RING_SEGMENTS;
            float angle = (float) Math.toRadians(rotation + progress * 360.0F);
            arcPoints.add(new ScreenPoint(
                    centerPosition.x() + Mth.cos(angle) * radius,
                    centerPosition.y() + Mth.sin(angle) * radius
            ));
        }
        drawFlowPath(guiGraphics, arcPoints, thickness, color, element, currentTime, 1.0F, 1.0F, alpha);
    }

    private static void drawFlowingRing(
            GuiGraphics guiGraphics,
            ScreenPoint centerPosition,
            float radius,
            float thickness,
            int color,
            ElementType element,
            long currentTime,
            float direction,
            float alpha
    ) {
        List<ScreenPoint> ringPoints = new ArrayList<>(RING_SEGMENTS + 1);
        for (int segment = 0; segment <= RING_SEGMENTS; segment++) {
            float angle = (float) (Math.PI * 2.0D * segment / RING_SEGMENTS);
            ringPoints.add(new ScreenPoint(
                    centerPosition.x() + Mth.cos(angle) * radius,
                    centerPosition.y() + Mth.sin(angle) * radius
            ));
        }
        drawFlowPath(guiGraphics, ringPoints, thickness, color, element, currentTime, direction, 1.0F, alpha);
    }

    private static void drawFlowPath(
            GuiGraphics guiGraphics,
            List<ScreenPoint> points,
            float thickness,
            int color,
            ElementType element,
            long currentTime,
            float direction,
            float cutoff,
            float alpha
    ) {
        if (points.size() < 2 || thickness <= 0.0F || alpha <= 0.0F) {
            return;
        }
        ShaderInstance shaderInstance = ElementFlowShader.getElementFlowShader();
        if (shaderInstance == null) {
            int fallbackColor = ColorUtility.withAlpha(color, alpha * 0.72F);
            for (int index = 0; index < points.size() - 1; index++) {
                AptitudeRenderUtility.drawLine(guiGraphics, points.get(index), points.get(index + 1), thickness, fallbackColor);
            }
            return;
        }

        float totalLength = calculateLength(points);
        if (totalLength <= 0.0001F) {
            return;
        }
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("FlowTime").set((currentTime % 120000L) / 1000.0F);
        shaderInstance.safeGetUniform("FlowStyle").set(getFlowStyle(element));
        shaderInstance.safeGetUniform("FlowCutoff").set(Mth.clamp(cutoff, 0.0F, 1.0F));
        shaderInstance.safeGetUniform("FlowDirection").set(direction);

        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        float traversedLength = 0.0F;
        int renderedColor = ColorUtility.withAlpha(color, alpha);
        for (int index = 0; index < points.size() - 1; index++) {
            ScreenPoint start = points.get(index);
            ScreenPoint end = points.get(index + 1);
            float segmentLength = distance(start, end);
            float startProgress = traversedLength / totalLength;
            float endProgress = (traversedLength + segmentLength) / totalLength;
            appendFlowQuad(
                    bufferBuilder,
                    matrix,
                    start,
                    end,
                    thickness,
                    renderedColor,
                    startProgress,
                    endProgress
            );
            traversedLength += segmentLength;
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void appendFlowQuad(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            ScreenPoint start,
            ScreenPoint end,
            float thickness,
            int color,
            float startProgress,
            float endProgress
    ) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return;
        }
        float normalX = -deltaY / length * thickness * 0.5F;
        float normalY = deltaX / length * thickness * 0.5F;
        vertex(bufferBuilder, matrix, start.x() + normalX, start.y() + normalY, color, startProgress, 0.0F);
        vertex(bufferBuilder, matrix, start.x() - normalX, start.y() - normalY, color, startProgress, 1.0F);
        vertex(bufferBuilder, matrix, end.x() - normalX, end.y() - normalY, color, endProgress, 1.0F);
        vertex(bufferBuilder, matrix, end.x() + normalX, end.y() + normalY, color, endProgress, 0.0F);
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            int color,
            float textureU,
            float textureV
    ) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .endVertex();
    }

    private static ScreenPath trimPath(
            ScreenPoint start,
            ScreenPoint end,
            float startInset,
            float endInset
    ) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= startInset + endInset + 1.0F) {
            return new ScreenPath(start, end);
        }
        float directionX = deltaX / length;
        float directionY = deltaY / length;
        return new ScreenPath(
                new ScreenPoint(start.x() + directionX * startInset, start.y() + directionY * startInset),
                new ScreenPoint(end.x() - directionX * endInset, end.y() - directionY * endInset)
        );
    }

    private static ScreenPoint interpolate(ScreenPoint start, ScreenPoint end, float progress) {
        return new ScreenPoint(
                Mth.lerp(progress, start.x(), end.x()),
                Mth.lerp(progress, start.y(), end.y())
        );
    }

    private static float calculateLength(List<ScreenPoint> points) {
        float length = 0.0F;
        for (int index = 0; index < points.size() - 1; index++) {
            length += distance(points.get(index), points.get(index + 1));
        }
        return length;
    }

    private static float distance(ScreenPoint start, ScreenPoint end) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        return Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static float getFlowStyle(ElementType element) {
        return switch (element) {
            case METAL -> 0.0F;
            case WOOD -> 1.0F;
            case WATER -> 2.0F;
            case FIRE -> 3.0F;
            case EARTH -> 4.0F;
            case YIN_YANG -> 5.0F;
            case THUNDER, CHAOS -> 0.0F;
        };
    }

    private record ScreenPath(ScreenPoint start, ScreenPoint end) {
    }
}
