package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.client.render.SwordQiShader;
import net.seraphina.ninecun_lingtai.common.entity.ability.SwordQiEntity;
import org.joml.Matrix4f;

/** Draws the sword-qi mesh in the entity's own forward/up plane. */
public final class SwordQiRenderUtility {
    private static final int SEGMENT_COUNT = 32;
    private static final float ARC_RADIUS = 4.5F;
    private static final float ARC_FORWARD_OFFSET = 0.55F;
    private static final float BASE_BLADE_WIDTH = 0.12F;
    private static final float MAXIMUM_BLADE_WIDTH = 0.48F;

    private SwordQiRenderUtility() {
    }

    public static void render(
            SwordQiEntity entity,
            PoseStack poseStack,
            float partialTick
    ) {
        ShaderInstance shaderInstance = SwordQiShader.getSwordQiShader();
        Minecraft minecraft = Minecraft.getInstance();
        if (shaderInstance == null || minecraft.level == null) {
            return;
        }

        Vec3 forward = entity.getForwardDirection().normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = forward.cross(worldUp);
        if (side.lengthSqr() < 1.0E-8D) {
            side = forward.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }
        side = side.normalize();
        Vec3 up = forward.cross(side).normalize();

        float progress = entity.getProgress(partialTick);
        float angleRadians = (float) Math.toRadians(entity.getArcAngleDegrees());
        float time = (minecraft.level.getGameTime() + partialTick) / 20.0F;

        shaderInstance.safeGetUniform("QiTime").set(time);
        shaderInstance.safeGetUniform("QiProgress").set(progress);
        shaderInstance.safeGetUniform("QiAngle").set(entity.getArcAngleDegrees());

        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shaderInstance);
            drawArc(
                    poseStack.last().pose(),
                    forward,
                    side,
                    up,
                    angleRadians
            );
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }

    private static void drawArc(
            Matrix4f matrix,
            Vec3 forward,
            Vec3 side,
            Vec3 up,
            float angleRadians
    ) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int segment = 0; segment < SEGMENT_COUNT; segment++) {
            float start = segment / (float) SEGMENT_COUNT;
            float end = (segment + 1) / (float) SEGMENT_COUNT;
            appendArcSegment(bufferBuilder, matrix, forward, side, up, angleRadians, start, end);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void appendArcSegment(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 forward,
            Vec3 side,
            Vec3 up,
            float angleRadians,
            float start,
            float end
    ) {
        Vec3 startCenter = getArcPoint(forward, side, up, angleRadians, start);
        Vec3 endCenter = getArcPoint(forward, side, up, angleRadians, end);
        Vec3 startNormal = getArcNormal(side, up, angleRadians, start);
        Vec3 endNormal = getArcNormal(side, up, angleRadians, end);
        float startWidth = getBladeWidth(start);
        float endWidth = getBladeWidth(end);

        addVertex(bufferBuilder, matrix, startCenter.add(startNormal.scale(-startWidth)), start, 0.0F);
        addVertex(bufferBuilder, matrix, endCenter.add(endNormal.scale(-endWidth)), end, 0.0F);
        addVertex(bufferBuilder, matrix, endCenter.add(endNormal.scale(endWidth)), end, 1.0F);
        addVertex(bufferBuilder, matrix, startCenter.add(startNormal.scale(startWidth)), start, 1.0F);
    }

    private static Vec3 getArcPoint(
            Vec3 forward,
            Vec3 side,
            Vec3 up,
            float angleRadians,
            float progress
    ) {
        float theta = Mth.lerp(progress, -angleRadians * 0.5F, angleRadians * 0.5F);
        Vec3 radial = side.scale(Mth.sin(theta)).add(up.scale(Mth.cos(theta)));
        return forward.scale(ARC_FORWARD_OFFSET).add(radial.scale(ARC_RADIUS));
    }

    private static Vec3 getArcNormal(
            Vec3 side,
            Vec3 up,
            float angleRadians,
            float progress
    ) {
        float theta = Mth.lerp(progress, -angleRadians * 0.5F, angleRadians * 0.5F);
        return side.scale(Mth.sin(theta)).add(up.scale(Mth.cos(theta))).normalize();
    }

    private static float getBladeWidth(float progress) {
        return BASE_BLADE_WIDTH + MAXIMUM_BLADE_WIDTH * Mth.sin(progress * Mth.PI);
    }

    private static void addVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 localPosition,
            float progress,
            float across
    ) {
        bufferBuilder.vertex(matrix, (float) localPosition.x, (float) localPosition.y, (float) localPosition.z)
                .color(0.74F, 0.91F, 1.0F, 0.92F)
                .uv(across, progress)
                .endVertex();
    }
}

