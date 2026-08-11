package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.client.player.ClientPushingMountainsManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class PushingMountainsRenderUtility {
    private static final int RED = 235;
    private static final int GREEN = 190;
    private static final int BLUE = 48;
    private static final int ALPHA = 210;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        ClientPushingMountainsManager.SelectionPreview preview =
                ClientPushingMountainsManager.getSelectionPreview();
        if (preview == null) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        renderBox(bufferBuilder, poseStack.last().pose(), preview.selectionBox());
        if (preview.secondPoint() != null) {
            renderBox(
                    bufferBuilder,
                    poseStack.last().pose(),
                    new AABB(
                            preview.previewPoint().getX(),
                            preview.previewPoint().getY(),
                            preview.previewPoint().getZ(),
                            preview.previewPoint().getX() + 1.0D,
                            preview.previewPoint().getY() + 1.0D,
                            preview.previewPoint().getZ() + 1.0D
                    )
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderBox(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
        double x0 = box.minX;
        double y0 = box.minY;
        double z0 = box.minZ;
        double x1 = box.maxX;
        double y1 = box.maxY;
        double z1 = box.maxZ;
        line(bufferBuilder, matrix, x0, y0, z0, x1, y0, z0);
        line(bufferBuilder, matrix, x1, y0, z0, x1, y0, z1);
        line(bufferBuilder, matrix, x1, y0, z1, x0, y0, z1);
        line(bufferBuilder, matrix, x0, y0, z1, x0, y0, z0);
        line(bufferBuilder, matrix, x0, y1, z0, x1, y1, z0);
        line(bufferBuilder, matrix, x1, y1, z0, x1, y1, z1);
        line(bufferBuilder, matrix, x1, y1, z1, x0, y1, z1);
        line(bufferBuilder, matrix, x0, y1, z1, x0, y1, z0);
        line(bufferBuilder, matrix, x0, y0, z0, x0, y1, z0);
        line(bufferBuilder, matrix, x1, y0, z0, x1, y1, z0);
        line(bufferBuilder, matrix, x1, y0, z1, x1, y1, z1);
        line(bufferBuilder, matrix, x0, y0, z1, x0, y1, z1);
    }

    private static void line(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1
    ) {
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z0)
                .color(RED, GREEN, BLUE, ALPHA)
                .endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z1)
                .color(RED, GREEN, BLUE, ALPHA)
                .endVertex();
    }
}
