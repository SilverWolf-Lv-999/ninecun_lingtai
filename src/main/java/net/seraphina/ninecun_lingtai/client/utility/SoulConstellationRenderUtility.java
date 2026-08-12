package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.render.SoulConstellationShader;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class SoulConstellationRenderUtility {
    public static final int STAR_COUNT = 7;
    public static final int ALL_STARS_MASK = (1 << STAR_COUNT) - 1;
    private static final float[][] STAR_COORDINATES = {
            {0.235F, 0.315F},
            {0.300F, 0.570F},
            {0.470F, 0.535F},
            {0.515F, 0.300F},
            {0.645F, 0.275F},
            {0.760F, 0.365F},
            {0.875F, 0.500F}
    };

    public static void drawConstellation(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            long currentTime,
            int illuminatedStarMask,
            float introProgress,
            float bloomProgress,
            float alpha
    ) {
        if (radius <= 0.0F || alpha <= 0.0F) {
            return;
        }
        ShaderInstance shaderInstance = SoulConstellationShader.getSoulConstellationShader();
        if (shaderInstance == null) {
            drawFallbackConstellation(
                    guiGraphics,
                    centerX,
                    centerY,
                    radius,
                    illuminatedStarMask,
                    introProgress,
                    bloomProgress,
                    alpha
            );
            return;
        }
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("SoulTime").set((currentTime % 240000L) / 1000.0F);
        shaderInstance.safeGetUniform("LightMask").set(illuminatedStarMask & ALL_STARS_MASK);
        shaderInstance.safeGetUniform("IntroProgress").set(Mth.clamp(introProgress, 0.0F, 1.0F));
        shaderInstance.safeGetUniform("BloomProgress").set(Mth.clamp(bloomProgress, 0.0F, 1.0F));

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int color = ColorUtility.withAlpha(0xFFFFFF, alpha);
        vertex(bufferBuilder, matrix, centerX - radius, centerY - radius, color, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, centerX - radius, centerY + radius, color, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, centerX + radius, centerY + radius, color, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, centerX + radius, centerY - radius, color, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static List<ScreenPoint> getStarPositions(float centerX, float centerY, float radius) {
        List<ScreenPoint> positions = new ArrayList<>(STAR_COUNT);
        for (float[] coordinate : STAR_COORDINATES) {
            positions.add(new ScreenPoint(
                    centerX + (coordinate[0] * 2.0F - 1.0F) * radius,
                    centerY + (coordinate[1] * 2.0F - 1.0F) * radius
            ));
        }
        return List.copyOf(positions);
    }

    private static void drawFallbackConstellation(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            int illuminatedStarMask,
            float introProgress,
            float bloomProgress,
            float alpha
    ) {
        List<ScreenPoint> positions = getStarPositions(centerX, centerY, radius);
        int[][] connections = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, {3, 4}, {4, 5}, {5, 6}
        };
        float visibleAlpha = alpha * Mth.clamp(introProgress, 0.0F, 1.0F);
        for (int[] connection : connections) {
            boolean illuminated = isStarIlluminated(illuminatedStarMask, connection[0])
                    && isStarIlluminated(illuminatedStarMask, connection[1]);
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    positions.get(connection[0]),
                    positions.get(connection[1]),
                    illuminated ? 1.5F : 0.75F,
                    ColorUtility.withAlpha(illuminated ? 0xBDEEFF : 0x4B6078, visibleAlpha * 0.7F)
            );
        }
        for (int index = 0; index < positions.size(); index++) {
            ScreenPoint position = positions.get(index);
            boolean illuminated = isStarIlluminated(illuminatedStarMask, index);
            float starRadius = radius * (illuminated ? 0.027F : 0.019F);
            if (illuminated) {
                AptitudeRenderUtility.drawDisc(
                        guiGraphics,
                        position.x(),
                        position.y(),
                        starRadius * (2.8F + bloomProgress * 3.2F),
                        ColorUtility.withAlpha(0x75D8FF, visibleAlpha * (0.18F + bloomProgress * 0.3F))
                );
            }
            AptitudeRenderUtility.drawDisc(
                    guiGraphics,
                    position.x(),
                    position.y(),
                    starRadius,
                    ColorUtility.withAlpha(illuminated ? 0xF5FDFF : 0x6B7890, visibleAlpha)
            );
        }
    }

    private static boolean isStarIlluminated(int illuminatedStarMask, int starIndex) {
        return (illuminatedStarMask & 1 << starIndex) != 0;
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
}
