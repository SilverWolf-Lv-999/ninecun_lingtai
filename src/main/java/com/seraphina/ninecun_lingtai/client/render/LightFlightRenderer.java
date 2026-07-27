package com.seraphina.ninecun_lingtai.client.render;

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
import com.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
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
    private static final int LATITUDE_SEGMENTS = 14;
    private static final int LONGITUDE_SEGMENTS = 28;
    private static final float BASE_RADIUS = 1.08F;
    private static final float BOOST_RADIUS_BONUS = 0.26F;

    private LightFlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        LightFlightVisualState visualState = ClientLightFlightManager.getVisualState(player.getUUID());
        if (visualState == null) {
            return;
        }
        if (visualState.style() != LightFlightStyle.LIGHT) {
            return;
        }
        float visibility = visualState.visibility(event.getPartialTick());
        if (visibility <= 0.0F) {
            return;
        }

        float boostVisibility = visualState.boostVisibility(event.getPartialTick());
        float pulse = 0.5F + 0.5F * Mth.sin((player.tickCount + event.getPartialTick()) * 0.32F);
        float radius = BASE_RADIUS + boostVisibility * BOOST_RADIUS_BONUS + pulse * 0.045F;
        int color = blendColor(visualState.color(), 0xFFFFFF, 0.16F + boostVisibility * 0.12F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0F, player.getBbHeight() * 0.52F, 0.0F);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        appendLightSphere(bufferBuilder, matrix, radius, color);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableCull();
        poseStack.popPose();
        event.setCanceled(true);
    }

    private static void appendLightSphere(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float radius,
            int color
    ) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            float theta = (float) (Math.PI * latitude / LATITUDE_SEGMENTS);
            float nextTheta = (float) (Math.PI * (latitude + 1) / LATITUDE_SEGMENTS);
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                float phi = (float) (Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS);
                float nextPhi = (float) (Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS);
                appendTriangle(
                    bufferBuilder,
                    matrix,
                    spherePoint(theta, phi, radius),
                    spherePoint(nextTheta, phi, radius),
                    spherePoint(nextTheta, nextPhi, radius),
                    shadeColor(color, theta)
                );
                appendTriangle(
                    bufferBuilder,
                    matrix,
                    spherePoint(theta, phi, radius),
                    spherePoint(nextTheta, nextPhi, radius),
                    spherePoint(theta, nextPhi, radius),
                    shadeColor(color, theta)
                );
            }
        }
    }

    private static void appendTriangle(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            SpherePoint first,
            SpherePoint second,
            SpherePoint third,
            int color
    ) {
        vertex(bufferBuilder, matrix, first, color);
        vertex(bufferBuilder, matrix, second, color);
        vertex(bufferBuilder, matrix, third, color);
    }

    private static SpherePoint spherePoint(float theta, float phi, float radius) {
        float sinTheta = Mth.sin(theta);
        return new SpherePoint(
                Mth.cos(phi) * sinTheta * radius,
                Mth.cos(theta) * radius,
                Mth.sin(phi) * sinTheta * radius
        );
    }

    private static void vertex(BufferBuilder bufferBuilder, Matrix4f matrix, SpherePoint point, int color) {
        bufferBuilder.vertex(matrix, point.x(), point.y(), point.z())
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
                .endVertex();
    }

    private static int shadeColor(int color, float theta) {
        float topLight = 0.22F * Math.max(0.0F, Mth.cos(theta - 0.45F));
        return blendColor(color, 0xFFFFFF, topLight);
    }

    private static int blendColor(int color, int targetColor, float amount) {
        float clampedAmount = Mth.clamp(amount, 0.0F, 1.0F);
        int red = blendChannel(color >> 16 & 0xFF, targetColor >> 16 & 0xFF, clampedAmount);
        int green = blendChannel(color >> 8 & 0xFF, targetColor >> 8 & 0xFF, clampedAmount);
        int blue = blendChannel(color & 0xFF, targetColor & 0xFF, clampedAmount);
        return red << 16 | green << 8 | blue;
    }

    private static int blendChannel(int color, int targetColor, float amount) {
        return Mth.clamp(Math.round(Mth.lerp(amount, color, targetColor)), 0, 255);
    }

    private record SpherePoint(float x, float y, float z) {
    }
}
