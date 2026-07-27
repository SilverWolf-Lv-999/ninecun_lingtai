package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import com.seraphina.ninecun_lingtai.client.player.LightFlightVisualState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightFlightRenderer {
    private static final int DISK_SEGMENTS = 36;
    private static final float BASE_RADIUS = 1.16F;
    private static final float BOOST_RADIUS_BONUS = 0.26F;
    private static final float CENTER_ALPHA = 0.34F;
    private static final float EDGE_ALPHA = 0.02F;

    private LightFlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        LightFlightVisualState visualState = ClientLightFlightManager.getVisualState(player.getUUID());
        if (visualState == null) {
            return;
        }
        float visibility = visualState.visibility(event.getPartialTick());
        if (visibility <= 0.0F) {
            return;
        }

        float boostVisibility = visualState.boostVisibility(event.getPartialTick());
        float pulse = 0.5F + 0.5F * Mth.sin((player.tickCount + event.getPartialTick()) * 0.32F);
        float radius = BASE_RADIUS + boostVisibility * BOOST_RADIUS_BONUS + pulse * 0.045F;
        float alphaScale = visibility * (1.0F + boostVisibility * 0.45F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0F, player.getBbHeight() * 0.52F, 0.0F);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        appendDisk(bufferBuilder, matrix, Plane.XY, radius, visualState.color(), alphaScale);
        appendDisk(bufferBuilder, matrix, Plane.XZ, radius * 0.92F, visualState.color(), alphaScale * 0.72F);
        appendDisk(bufferBuilder, matrix, Plane.YZ, radius, visualState.color(), alphaScale);
        appendDisk(bufferBuilder, matrix, Plane.DIAGONAL, radius * 1.04F, visualState.color(), alphaScale * 0.8F);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void appendDisk(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Plane plane,
            float radius,
            int color,
            float alphaScale
    ) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int centerAlpha = alphaChannel(CENTER_ALPHA * alphaScale);
        int edgeAlpha = alphaChannel(EDGE_ALPHA * alphaScale);
        for (int segment = 0; segment < DISK_SEGMENTS; segment++) {
            float angle = (float) (Math.PI * 2.0D * segment / DISK_SEGMENTS);
            float nextAngle = (float) (Math.PI * 2.0D * (segment + 1) / DISK_SEGMENTS);
            vertex(bufferBuilder, matrix, plane, 0.0F, 0.0F, red, green, blue, centerAlpha);
            vertex(
                    bufferBuilder,
                    matrix,
                    plane,
                    Mth.cos(angle) * radius,
                    Mth.sin(angle) * radius,
                    red,
                    green,
                    blue,
                    edgeAlpha
            );
            vertex(
                    bufferBuilder,
                    matrix,
                    plane,
                    Mth.cos(nextAngle) * radius,
                    Mth.sin(nextAngle) * radius,
                    red,
                    green,
                    blue,
                    edgeAlpha
            );
        }
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Plane plane,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float x;
        float y;
        float z;
        switch (plane) {
            case XY -> {
                x = u;
                y = v;
                z = 0.0F;
            }
            case XZ -> {
                x = u;
                y = 0.0F;
                z = v;
            }
            case YZ -> {
                x = 0.0F;
                y = v;
                z = u;
            }
            case DIAGONAL -> {
                x = u * 0.7071F;
                y = v;
                z = u * 0.7071F;
            }
            default -> throw new IncompatibleClassChangeError();
        }
        bufferBuilder.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static int alphaChannel(float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
    }

    private enum Plane {
        XY,
        XZ,
        YZ,
        DIAGONAL
    }
}
