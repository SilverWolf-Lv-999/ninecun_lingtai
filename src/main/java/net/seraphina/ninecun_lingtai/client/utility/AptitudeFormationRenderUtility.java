package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.render.AptitudeFormationShader;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class AptitudeFormationRenderUtility {

    public static void drawFormation(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            long currentTime,
            float alpha
    ) {
        if (radius <= 0.0F || alpha <= 0.0F) {
            return;
        }
        ShaderInstance shaderInstance = AptitudeFormationShader.getAptitudeFormationShader();
        if (shaderInstance == null) {
            drawFallbackFormation(guiGraphics, centerX, centerY, radius, alpha);
            return;
        }
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("FormationTime").set((currentTime % 240000L) / 1000.0F);

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

    public static void drawNodeSeal(
            GuiGraphics guiGraphics,
            ScreenPoint position,
            float radius,
            ElementType element,
            int elementColor,
            boolean selected,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = selected
                ? 0.5F + 0.5F * Mth.sin(currentTime * 0.0048F + element.ordinal() * 0.9F)
                : 0.0F;
        float sealRadius = radius * (1.5F + pulse * 0.05F);
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                position.x(),
                position.y(),
                sealRadius,
                ColorUtility.withAlpha(0x090C12, alpha * (selected ? 0.64F : 0.46F))
        );
        int sealColor = selected
                ? ColorUtility.brighten(elementColor, 0.3F)
                : ColorUtility.blend(elementColor, 0xD8C9A4, 0.64F);
        float rotation = currentTime * (selected ? 0.018F : 0.006F) + element.ordinal() * 31.0F;
        for (int arc = 0; arc < 4; arc++) {
            float start = rotation + arc * 90.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    position.x(),
                    position.y(),
                    sealRadius,
                    start,
                    start + (selected ? 48.0F : 34.0F),
                    selected ? 1.35F : 0.8F,
                    ColorUtility.withAlpha(sealColor, alpha * (selected ? 0.68F : 0.25F))
            );
        }
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                position.x(),
                position.y(),
                sealRadius * 0.82F,
                0.55F,
                ColorUtility.withAlpha(0xD9C998, alpha * (selected ? 0.24F : 0.12F))
        );
    }

    private static void drawFallbackFormation(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            float alpha
    ) {
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                centerX,
                centerY,
                radius,
                ColorUtility.withAlpha(0x070A10, alpha * 0.82F)
        );
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                centerX,
                centerY,
                radius * 0.84F,
                ColorUtility.withAlpha(0x111722, alpha * 0.52F)
        );
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                centerX,
                centerY,
                radius * 0.94F,
                1.2F,
                ColorUtility.withAlpha(0xB49A5A, alpha * 0.32F)
        );
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                centerX,
                centerY,
                radius * 0.68F,
                0.7F,
                ColorUtility.withAlpha(0x71849A, alpha * 0.2F)
        );
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
