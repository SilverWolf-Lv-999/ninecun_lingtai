package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.player.BodyShieldVisualState;
import net.seraphina.ninecun_lingtai.client.player.ClientBodyShieldManager;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import net.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

public final class BodyShieldRenderUtility {
    private static final double MAXIMUM_RENDER_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final int LATITUDE_SEGMENTS = 16;
    private static final int LONGITUDE_SEGMENTS = 32;
    private static final float WIDTH_RADIUS_MULTIPLIER = 0.78F;
    private static final float HEIGHT_RADIUS_MULTIPLIER = 0.56F;
    private static final float BASE_ALPHA = 0.28F;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || (ClientBodyShieldManager.getVisualStates().isEmpty()
                && ClientDefenseSpellManager.getBodyShields().isEmpty())) {
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

        for (Map.Entry<Integer, BodyShieldVisualState> entry : ClientBodyShieldManager.getVisualStates().entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity livingEntity)
                    || livingEntity.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            renderPlayerShield(
                    poseStack.last().pose(),
                    livingEntity,
                    entry.getValue(),
                    event.getPartialTick()
            );
        }
        for (Map.Entry<Integer, DefenseSpellSnapshot> entry
                : ClientDefenseSpellManager.getBodyShields().entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity livingEntity)
                    || livingEntity.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            DefenseSpellSnapshot snapshot = entry.getValue();
            if (DefenseSpellRenderUtility.usesPlayerModelLayer(snapshot.kind())) {
                continue;
            }
            renderShield(
                    poseStack.last().pose(),
                    livingEntity,
                    0.55F + (float) snapshot.ratio() * 0.45F,
                    DefenseSpellUtility.getColor(snapshot.kind(), snapshot.elementMask()),
                    event.getPartialTick()
            );
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderPlayerShield(
            Matrix4f matrix,
            LivingEntity entity,
            BodyShieldVisualState visualState,
            float partialTick
    ) {
        float visibility = visualState.visibility(partialTick);
        if (visibility <= 0.0F) {
            return;
        }
        renderShield(matrix, entity, visibility, 0xFFFFFF, partialTick);
    }

    private static void renderShield(
            Matrix4f matrix,
            LivingEntity entity,
            float visibility,
            int color,
            float partialTick
    ) {
        float pulse = 0.5F + 0.5F * Mth.sin((entity.tickCount + partialTick) * 0.55F);
        float horizontalRadius = entity.getBbWidth() * (WIDTH_RADIUS_MULTIPLIER + pulse * 0.025F);
        float verticalRadius = entity.getBbHeight() * HEIGHT_RADIUS_MULTIPLIER;
        Vec3 position = new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ())
        ).add(0.0D, entity.getBbHeight() * 0.52D, 0.0D);

        int alpha = Mth.clamp(Math.round(BASE_ALPHA * visibility * 255.0F), 0, 255);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        appendEllipsoid(bufferBuilder, matrix, position, horizontalRadius, verticalRadius, color, alpha);
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
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            float theta = (float) (Math.PI * latitude / LATITUDE_SEGMENTS);
            float nextTheta = (float) (Math.PI * (latitude + 1) / LATITUDE_SEGMENTS);
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                float phi = (float) (Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS);
                float nextPhi = (float) (Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS);
                appendTriangle(
                        bufferBuilder,
                        matrix,
                        ellipsoidPoint(center, theta, phi, horizontalRadius, verticalRadius),
                        ellipsoidPoint(center, nextTheta, phi, horizontalRadius, verticalRadius),
                        ellipsoidPoint(center, nextTheta, nextPhi, horizontalRadius, verticalRadius),
                        color,
                        alpha
                );
                appendTriangle(
                        bufferBuilder,
                        matrix,
                        ellipsoidPoint(center, theta, phi, horizontalRadius, verticalRadius),
                        ellipsoidPoint(center, nextTheta, nextPhi, horizontalRadius, verticalRadius),
                        ellipsoidPoint(center, theta, nextPhi, horizontalRadius, verticalRadius),
                        color,
                        alpha
                );
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

    private static void appendTriangle(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 first,
            Vec3 second,
            Vec3 third,
            int color,
            int alpha
    ) {
        vertex(bufferBuilder, matrix, first, color, alpha);
        vertex(bufferBuilder, matrix, second, color, alpha);
        vertex(bufferBuilder, matrix, third, color, alpha);
    }

    private static void vertex(BufferBuilder bufferBuilder, Matrix4f matrix, Vec3 point, int color, int alpha) {
        bufferBuilder.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha)
                .endVertex();
    }
}
