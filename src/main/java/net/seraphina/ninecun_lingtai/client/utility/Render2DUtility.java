package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class Render2DUtility {

    public static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int applyOpacity(int color, float opacity) {
        int alpha = color >>> 24 & 0xFF;
        return withAlpha(color, Math.round(alpha * Mth.clamp(opacity, 0.0F, 1.0F)));
    }

    public static int mix(int firstColor, int secondColor, float progress) {
        float weight = Mth.clamp(progress, 0.0F, 1.0F);
        int alpha = Mth.lerpInt(weight, firstColor >>> 24 & 0xFF, secondColor >>> 24 & 0xFF);
        int red = Mth.lerpInt(weight, firstColor >>> 16 & 0xFF, secondColor >>> 16 & 0xFF);
        int green = Mth.lerpInt(weight, firstColor >>> 8 & 0xFF, secondColor >>> 8 & 0xFF);
        int blue = Mth.lerpInt(weight, firstColor & 0xFF, secondColor & 0xFF);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static void drawRoundedRect(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int color
    ) {
        drawRoundedRect(graphics, x, y, width, height, radius, radius, radius, radius, color);
    }

    public static void drawRoundedRect(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            int color
    ) {
        if (!canDraw(width, height, color)) {
            return;
        }
        drawFan(
                graphics,
                roundedRectPoints(
                        x,
                        y,
                        width,
                        height,
                        topLeftRadius,
                        topRightRadius,
                        bottomRightRadius,
                        bottomLeftRadius
                ),
                color
        );
    }

    public static void drawRoundedVerticalGradientRect(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int topColor,
            int bottomColor
    ) {
        if (!canDraw(width, height, topColor) && !canDraw(width, height, bottomColor)) {
            return;
        }
        float[] points = roundedRectPoints(x, y, width, height, radius, radius, radius, radius);
        int[] colors = new int[points.length / 2];
        for (int index = 0; index < colors.length; index++) {
            float pointY = points[index * 2 + 1];
            float progress = Mth.clamp((pointY - y) / Math.max(0.001F, height), 0.0F, 1.0F);
            colors[index] = mix(topColor, bottomColor, progress);
        }
        drawFan(graphics, points, colors);
    }

    public static void drawRoundedHorizontalGradientRect(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int leftColor,
            int rightColor
    ) {
        if (!canDraw(width, height, leftColor) && !canDraw(width, height, rightColor)) {
            return;
        }
        float[] points = roundedRectPoints(x, y, width, height, radius, radius, radius, radius);
        int[] colors = new int[points.length / 2];
        for (int index = 0; index < colors.length; index++) {
            float pointX = points[index * 2];
            float progress = Mth.clamp((pointX - x) / Math.max(0.001F, width), 0.0F, 1.0F);
            colors[index] = mix(leftColor, rightColor, progress);
        }
        drawFan(graphics, points, colors);
    }

    public static void drawOutlineRoundedRect(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            float strokeWidth,
            int color
    ) {
        if (!canDraw(width, height, color) || strokeWidth <= 0.0F) {
            return;
        }
        float safeStroke = Math.min(strokeWidth, Math.min(width, height) * 0.5F);
        float[] outer = roundedRectPoints(x, y, width, height, radius, radius, radius, radius);
        float innerX = x + safeStroke;
        float innerY = y + safeStroke;
        float innerWidth = Math.max(0.0F, width - safeStroke * 2.0F);
        float innerHeight = Math.max(0.0F, height - safeStroke * 2.0F);
        float innerRadius = Math.max(0.0F, radius - safeStroke);
        float[] inner = roundedRectPoints(
                innerX,
                innerY,
                innerWidth,
                innerHeight,
                innerRadius,
                innerRadius,
                innerRadius,
                innerRadius
        );

        submit(graphics, VertexFormat.Mode.TRIANGLE_STRIP, (bufferBuilder, matrix) -> {
            int pointCount = Math.min(outer.length, inner.length) / 2;
            for (int index = 0; index <= pointCount; index++) {
                int pointIndex = index % pointCount;
                vertex(
                        bufferBuilder,
                        matrix,
                        outer[pointIndex * 2],
                        outer[pointIndex * 2 + 1],
                        color
                );
                vertex(
                        bufferBuilder,
                        matrix,
                        inner[pointIndex * 2],
                        inner[pointIndex * 2 + 1],
                        color
                );
            }
        });
    }

    public static void drawCircle(
            GuiGraphics graphics,
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        if (radius <= 0.0F || (color >>> 24) == 0) {
            return;
        }
        int segments = Math.max(16, Math.min(40, Math.round(radius * 2.5F)));
        float[] points = new float[segments * 2];
        for (int index = 0; index < segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            points[index * 2] = centerX + (float) Math.cos(angle) * radius;
            points[index * 2 + 1] = centerY + (float) Math.sin(angle) * radius;
        }
        drawFan(graphics, points, color);
    }

    public static void drawDropShadow(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            float offsetX,
            float offsetY,
            float blurRadius,
            int color
    ) {
        if (!canDraw(width, height, color) || blurRadius <= 0.0F) {
            return;
        }
        int rings = Math.max(6, Math.min(18, Math.round(blurRadius * 0.9F)));
        for (int index = rings; index >= 0; index--) {
            float progress = index / (float) rings;
            float spread = blurRadius * progress;
            float opacity = (1.0F - progress) * (1.0F - progress) * 0.55F;
            drawRoundedRect(
                    graphics,
                    x + offsetX - spread,
                    y + offsetY - spread,
                    width + spread * 2.0F,
                    height + spread * 2.0F,
                    radius + spread,
                    applyOpacity(color, opacity)
            );
        }
    }

    public static void drawSoftCard(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int fillColor,
            int borderColor
    ) {
        drawDropShadow(graphics, x, y, width, height, radius, 0.0F, 3.0F, 12.0F, 0x78000000);
        drawRoundedRect(graphics, x, y, width, height, radius, fillColor);
        drawOutlineRoundedRect(graphics, x, y, width, height, radius, 1.0F, borderColor);
    }

    public static void drawPill(
            GuiGraphics graphics,
            float x,
            float y,
            float width,
            float height,
            int fillColor,
            int borderColor
    ) {
        drawRoundedRect(graphics, x, y, width, height, height * 0.5F, fillColor);
        if ((borderColor >>> 24) != 0) {
            drawOutlineRoundedRect(graphics, x, y, width, height, height * 0.5F, 1.0F, borderColor);
        }
    }

    private static boolean canDraw(float width, float height, int color) {
        return width > 0.0F && height > 0.0F && (color >>> 24) != 0;
    }

    private static float[] roundedRectPoints(
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius
    ) {
        float maxRadius = Math.min(width, height) * 0.5F;
        float topLeft = Mth.clamp(topLeftRadius, 0.0F, maxRadius);
        float topRight = Mth.clamp(topRightRadius, 0.0F, maxRadius);
        float bottomRight = Mth.clamp(bottomRightRadius, 0.0F, maxRadius);
        float bottomLeft = Mth.clamp(bottomLeftRadius, 0.0F, maxRadius);
        int segments = 8;
        float[] points = new float[(segments * 4 + 4) * 2];
        int pointIndex = 0;
        pointIndex = appendArc(points, pointIndex, x + topLeft, y + topLeft, topLeft, 180.0F, 270.0F, segments);
        pointIndex = appendArc(points, pointIndex, x + width - topRight, y + topRight, topRight, 270.0F, 360.0F, segments);
        pointIndex = appendArc(points, pointIndex, x + width - bottomRight, y + height - bottomRight, bottomRight, 0.0F, 90.0F, segments);
        appendArc(points, pointIndex, x + bottomLeft, y + height - bottomLeft, bottomLeft, 90.0F, 180.0F, segments);
        return points;
    }

    private static int appendArc(
            float[] points,
            int pointIndex,
            float centerX,
            float centerY,
            float radius,
            float startAngle,
            float endAngle,
            int segments
    ) {
        for (int index = 0; index <= segments; index++) {
            float angle = (float) Math.toRadians(
                    Mth.lerp(index / (float) segments, startAngle, endAngle)
            );
            points[pointIndex++] = centerX + Mth.cos(angle) * radius;
            points[pointIndex++] = centerY + Mth.sin(angle) * radius;
        }
        return pointIndex;
    }

    private static void drawFan(GuiGraphics graphics, float[] points, int color) {
        int[] colors = new int[points.length / 2];
        for (int index = 0; index < colors.length; index++) {
            colors[index] = color;
        }
        drawFan(graphics, points, colors);
    }

    private static void drawFan(GuiGraphics graphics, float[] points, int[] colors) {
        if (points.length < 6) {
            return;
        }
        submit(graphics, VertexFormat.Mode.TRIANGLE_FAN, (bufferBuilder, matrix) -> {
            float minX = points[0];
            float maxX = points[0];
            float minY = points[1];
            float maxY = points[1];
            for (int index = 2; index < points.length; index += 2) {
                minX = Math.min(minX, points[index]);
                maxX = Math.max(maxX, points[index]);
                minY = Math.min(minY, points[index + 1]);
                maxY = Math.max(maxY, points[index + 1]);
            }
            vertex(bufferBuilder, matrix, (minX + maxX) * 0.5F, (minY + maxY) * 0.5F, colors[0]);
            for (int index = 0; index <= points.length; index += 2) {
                int pointIndex = (index / 2) % (points.length / 2);
                int colorIndex = Math.min(pointIndex, colors.length - 1);
                vertex(
                        bufferBuilder,
                        matrix,
                        points[pointIndex * 2],
                        points[pointIndex * 2 + 1],
                        colors[colorIndex]
                );
            }
        });
    }

    private static void submit(
            GuiGraphics graphics,
            VertexFormat.Mode mode,
            VertexWriter writer
    ) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        writer.write(bufferBuilder, matrix);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            int color
    ) {
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(color >>> 16 & 0xFF, color >>> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF)
                .endVertex();
    }

    @FunctionalInterface
    private interface VertexWriter {
        void write(BufferBuilder bufferBuilder, Matrix4f matrix);
    }
}
