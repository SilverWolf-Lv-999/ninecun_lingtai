package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.client.render.BreakthroughAnimationShader;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import org.joml.Matrix4f;

public final class BreakthroughAnimationRenderUtility {

    public static void render(
            GuiGraphics guiGraphics,
            BreakthroughAnimationType animationType,
            float progress,
            float time,
            int primaryColor,
            int secondaryColor,
            int accentColor
    ) {
        ShaderInstance shaderInstance = BreakthroughAnimationShader.getBreakthroughAnimationShader();
        if (shaderInstance == null) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("AnimationType").set((float) animationType.ordinal());
        shaderInstance.safeGetUniform("Progress").set(Mth.clamp(progress, 0.0F, 1.0F));
        shaderInstance.safeGetUniform("Time").set(time);
        shaderInstance.safeGetUniform("ScreenSize").set((float) width, (float) height);
        setColorUniform(shaderInstance, "PrimaryColor", primaryColor);
        setColorUniform(shaderInstance, "SecondaryColor", secondaryColor);
        setColorUniform(shaderInstance, "AccentColor", accentColor);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int color = ColorUtility.withAlpha(0xFFFFFF, 1.0F);
        vertex(bufferBuilder, matrix, 0.0F, 0.0F, color, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, 0.0F, height, color, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, width, height, color, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, width, 0.0F, color, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void setColorUniform(ShaderInstance shaderInstance, String uniformName, int color) {
        shaderInstance.safeGetUniform(uniformName).set(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
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
