package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager.ClientTribulationState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.world.phys.Vec3;
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
        drawFallback(event, alpha);
    }

    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientTribulationManager.getStates().isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        for (ClientTribulationState state : ClientTribulationManager.getStates().values()) {
            double renderDistance = state.radius() + 48.0D;
            if (state.center().distanceToSqr(cameraPosition) > renderDistance * renderDistance) {
                continue;
            }
            float intensity = ClientTribulationManager.getCloudIntensity(state, event.getPartialTick());
            if (intensity > 0.001F) {
                renderCloudShell(
                        matrix,
                        state.center(),
                        state.radius(),
                        intensity,
                        minecraft.level.getGameTime() + event.getPartialTick()
                );
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderCloudShell(
            Matrix4f matrix,
            Vec3 center,
            float radius,
            float intensity,
            float animationTime
    ) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int layer = 0; layer < 3; layer++) {
            float pulse = 0.97F + Mth.sin(animationTime * 0.045F + layer * 1.8F) * 0.03F;
            float layerScale = (0.76F + layer * 0.13F) * pulse;
            float horizontalRadius = radius * layerScale;
            float verticalRadius = 7.0F + layer * 1.5F;
            Vec3 cloudCenter = center.add(
                    Mth.sin(animationTime * 0.018F + layer * 2.1F) * 1.4D,
                    11.0D + layer * 0.7D + Mth.cos(animationTime * 0.022F + layer) * 0.35D,
                    Mth.cos(animationTime * 0.016F + layer * 1.4F) * 1.4D
            );
            int color = layer == 0 ? 0x1C2745 : layer == 1 ? 0x111A32 : 0x0A1024;
            int alpha = Mth.clamp(
                    Math.round(intensity * (layer == 0 ? 76.0F : layer == 1 ? 58.0F : 42.0F)),
                    0,
                    120
            );
            appendEllipsoid(bufferBuilder, matrix, cloudCenter, horizontalRadius, verticalRadius, color, alpha);
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void appendEllipsoid(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float horizontalRadius,
            float verticalRadius,
            int color,
            int alpha
    ) {
        int latitudeSegments = 8;
        int longitudeSegments = 24;
        for (int latitude = 0; latitude < latitudeSegments; latitude++) {
            float theta = Mth.PI * latitude / latitudeSegments;
            float nextTheta = Mth.PI * (latitude + 1) / latitudeSegments;
            for (int longitude = 0; longitude < longitudeSegments; longitude++) {
                float phi = Mth.TWO_PI * longitude / longitudeSegments;
                float nextPhi = Mth.TWO_PI * (longitude + 1) / longitudeSegments;
                Vec3 first = ellipsoidPoint(center, theta, phi, horizontalRadius, verticalRadius);
                Vec3 second = ellipsoidPoint(center, nextTheta, phi, horizontalRadius, verticalRadius);
                Vec3 third = ellipsoidPoint(center, nextTheta, nextPhi, horizontalRadius, verticalRadius);
                Vec3 fourth = ellipsoidPoint(center, theta, nextPhi, horizontalRadius, verticalRadius);
                appendVertex(bufferBuilder, matrix, first, color, alpha);
                appendVertex(bufferBuilder, matrix, second, color, alpha);
                appendVertex(bufferBuilder, matrix, third, color, alpha);
                appendVertex(bufferBuilder, matrix, first, color, alpha);
                appendVertex(bufferBuilder, matrix, third, color, alpha);
                appendVertex(bufferBuilder, matrix, fourth, color, alpha);
            }
        }
    }

    private static Vec3 ellipsoidPoint(
            Vec3 center,
            float theta,
            float phi,
            float horizontalRadius,
            float verticalRadius
    ) {
        float sinTheta = Mth.sin(theta);
        return center.add(
                Mth.cos(phi) * sinTheta * horizontalRadius,
                Mth.cos(theta) * verticalRadius,
                Mth.sin(phi) * sinTheta * horizontalRadius
        );
    }

    private static void appendVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 point,
            int color,
            int alpha
    ) {
        bufferBuilder.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha)
                .endVertex();
    }

    private static void drawFallback(RenderGuiOverlayEvent.Post event, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        event.getGuiGraphics().fill(
                0,
                0,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                (Mth.clamp(Math.round(alpha * 220.0F), 0, 220) << 24) | 0x10182D
        );
        RenderSystem.disableBlend();
    }
}
