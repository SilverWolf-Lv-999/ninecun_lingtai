package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.client.render.TribulationCloudShader;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.joml.Matrix4f;

public final class TribulationCloudRenderUtility {
    private TribulationCloudRenderUtility() {
    }

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float alpha = ClientTribulationManager.getLocalCloudAlpha(event.getPartialTick());
        if (minecraft.options.hideGui || alpha <= 0.001F) {
            return;
        }
        ShaderInstance shaderInstance = TribulationCloudShader.getTribulationCloudShader();
        if (shaderInstance == null) {
            drawFallback(event, alpha);
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("CloudTime").set(
                (minecraft.level == null ? 0L : minecraft.level.getGameTime()) / 20.0F
        );
        shaderInstance.safeGetUniform("CloudProgress").set(Mth.clamp(alpha, 0.0F, 1.0F));
        shaderInstance.safeGetUniform("ScreenSize").set((float) width, (float) height);
        shaderInstance.safeGetUniform("Darkness").set(Mth.clamp(alpha, 0.0F, 1.0F));

        Matrix4f matrix = event.getGuiGraphics().pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        vertex(bufferBuilder, matrix, 0.0F, 0.0F, 0.0F, 0.0F, alpha);
        vertex(bufferBuilder, matrix, 0.0F, height, 0.0F, 0.0F, alpha);
        vertex(bufferBuilder, matrix, width, height, 1.0F, 1.0F, alpha);
        vertex(bufferBuilder, matrix, width, 0.0F, 1.0F, 0.0F, alpha);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawFallback(RenderGuiOverlayEvent.Post event, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        event.getGuiGraphics().fill(
                0,
                0,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                ((int) (alpha * 170.0F) << 24) | 0x10182D
        );
        RenderSystem.disableBlend();
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            float textureU,
            float textureV,
            float alpha
    ) {
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, Mth.clamp(Math.round(alpha * 255.0F), 0, 255))
                .uv(textureU, textureV)
                .endVertex();
    }
}
