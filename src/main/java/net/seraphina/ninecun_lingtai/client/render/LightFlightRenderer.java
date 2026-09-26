package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.seraphina.ninecun_lingtai.client.player.LightFlightVisualState;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightFlightRenderer {
    private static final float BASE_RADIUS = 0.88F;
    private static final float BOOST_RADIUS_BONUS = 0.24F;
    private static final float MAXIMUM_TAIL_LENGTH = 18.0F;
    private static final int TAIL_RING_SIDES = 10;
    private static final int IMPACT_LATITUDE_RINGS = 12;
    private static final int IMPACT_LONGITUDE_SIDES = 24;
    private static final double MAXIMUM_RENDER_DISTANCE_SQUARED = 256.0D * 256.0D;

    private LightFlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        LightFlightVisualState visualState = ClientLightFlightManager.getVisualState(player.getUUID());
        if (visualState == null
                || visualState.style() != LightFlightStyle.LIGHT
                || !visualState.isFlying()
                || !visualState.isMoving()
                || LightFlightShader.getLightFlightShader() == null) {
            return;
        }
        event.setCanceled(true);
    }

    public static void renderPostWorld(
            float partialTick,
            PoseStack poseStack,
            Camera camera,
            Matrix4f projectionMatrix
    ) {
        ShaderInstance shaderInstance = LightFlightShader.getLightFlightShader();
        Minecraft minecraft = Minecraft.getInstance();
        if (shaderInstance == null || minecraft.level == null) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        Quaternionf cameraRotation = new Quaternionf(camera.rotation());
        Vector3f cameraRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation);
        Vector3f cameraUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRotation);
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
        shaderInstance.MODEL_VIEW_MATRIX.upload();
        shaderInstance.PROJECTION_MATRIX.set(projectionMatrix);
        shaderInstance.PROJECTION_MATRIX.upload();
        shaderInstance.safeGetUniform("Time").set(
                (minecraft.level.getGameTime() + partialTick) / 20.0F
        );

        for (Map.Entry<UUID, LightFlightVisualState> entry : ClientLightFlightManager.getVisualStates().entrySet()) {
            Player player = minecraft.level.getPlayerByUUID(entry.getKey());
            LightFlightVisualState visualState = entry.getValue();
            if (player == null
                    || visualState.style() != LightFlightStyle.LIGHT
                    || player.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            float visibility = visualState.visibility(partialTick);
            if (visibility <= 0.0F) {
                continue;
            }
            renderLight(
                    poseStack.last().pose(),
                    shaderInstance,
                    player,
                    visualState,
                    partialTick,
                    visibility,
                    cameraRight,
                    cameraUp
            );
        }
        renderStopImpact(
                poseStack.last().pose(),
                shaderInstance,
                partialTick,
                cameraPosition
        );

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderLight(
            Matrix4f matrix,
            ShaderInstance shaderInstance,
            Player player,
            LightFlightVisualState visualState,
            float partialTick,
            float visibility,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        Vec3 position = new Vec3(
                Mth.lerp(partialTick, player.xOld, player.getX()),
                Mth.lerp(partialTick, player.yOld, player.getY()) + player.getBbHeight() * 0.52D,
                Mth.lerp(partialTick, player.zOld, player.getZ())
        );
        float motion = Mth.clamp((float) (visualState.motionSpeed() / 0.05D), 0.0F, 1.0F);
        float boost = visualState.boostVisibility(partialTick);
        float radius = BASE_RADIUS + boost * BOOST_RADIUS_BONUS;
        int color = visualState.color();

        shaderInstance.safeGetUniform("Mode").set(0.0F);
        shaderInstance.safeGetUniform("Boost").set(boost);
        shaderInstance.safeGetUniform("TailStrength").set(visibility * (0.22F + motion * 0.78F));
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        appendBillboard(
                bufferBuilder,
                matrix,
                position,
                radius * 2.15F,
                color,
                visibility,
                cameraRight,
                cameraUp
        );
        BufferUploader.drawWithShader(bufferBuilder.end());

        shaderInstance.safeGetUniform("Mode").set(1.0F);
        List<Vec3> trail = visualState.isMoving()
                ? createTrailPath(position, visualState)
                : List.of();
        if (trail.size() > 1) {
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            appendTail(bufferBuilder, matrix, trail, radius, color, visibility);
            BufferUploader.drawWithShader(bufferBuilder.end());
        }
    }

    private static void appendTail(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            List<Vec3> trail,
            float radius,
            int color,
            float visibility
    ) {
        float totalLength = 0.0F;
        for (int point = 0; point < trail.size() - 1; point++) {
            totalLength += (float) trail.get(point).distanceTo(trail.get(point + 1));
        }
        if (totalLength < 0.01F) {
            return;
        }
        float[] distances = new float[trail.size()];
        List<Vector3f> sides = new ArrayList<>(trail.size());
        List<Vector3f> tops = new ArrayList<>(trail.size());
        for (int point = 0; point < trail.size(); point++) {
            if (point > 0) {
                distances[point] = distances[point - 1]
                        + (float) trail.get(point - 1).distanceTo(trail.get(point));
            }
            Vec3 previous = trail.get(Math.max(0, point - 1));
            Vec3 next = trail.get(Math.min(trail.size() - 1, point + 1));
            Vector3f axis = new Vector3f(
                    (float) (next.x - previous.x),
                    (float) (next.y - previous.y),
                    (float) (next.z - previous.z)
            ).normalize();
            Vector3f side = new Vector3f(axis).cross(new Vector3f(0.0F, 1.0F, 0.0F));
            if (side.lengthSquared() < 0.001F) {
                side = new Vector3f(axis).cross(new Vector3f(1.0F, 0.0F, 0.0F));
            }
            side.normalize();
            sides.add(side);
            tops.add(new Vector3f(side).cross(axis).normalize());
        }
        for (int point = 0; point < trail.size() - 1; point++) {
            float start = distances[point] / totalLength;
            float end = distances[point + 1] / totalLength;
            float startWidth = radius * Mth.lerp(start, 1.18F, 0.08F);
            float endWidth = radius * Mth.lerp(end, 1.18F, 0.08F);
            for (int radial = 0; radial < TAIL_RING_SIDES; radial++) {
                int nextRadial = (radial + 1) % TAIL_RING_SIDES;
                float radialValue = radial / (float) TAIL_RING_SIDES;
                float nextRadialValue = nextRadial / (float) TAIL_RING_SIDES;
                appendVertex(
                        bufferBuilder,
                        matrix,
                        trail.get(point).add(tubeOffset(
                                sides.get(point),
                                tops.get(point),
                                radialValue * Mth.TWO_PI,
                                startWidth
                        )),
                        color,
                        visibility,
                        start,
                        radialValue
                );
                appendVertex(
                        bufferBuilder,
                        matrix,
                        trail.get(point + 1).add(tubeOffset(
                                sides.get(point + 1),
                                tops.get(point + 1),
                                radialValue * Mth.TWO_PI,
                                endWidth
                        )),
                        color,
                        visibility,
                        end,
                        radialValue
                );
                appendVertex(
                        bufferBuilder,
                        matrix,
                        trail.get(point + 1).add(tubeOffset(
                                sides.get(point + 1),
                                tops.get(point + 1),
                                nextRadialValue * Mth.TWO_PI,
                                endWidth
                        )),
                        color,
                        visibility,
                        end,
                        nextRadialValue
                );
                appendVertex(
                        bufferBuilder,
                        matrix,
                        trail.get(point).add(tubeOffset(
                                sides.get(point),
                                tops.get(point),
                                nextRadialValue * Mth.TWO_PI,
                                startWidth
                        )),
                        color,
                        visibility,
                        start,
                        nextRadialValue
                );
            }
        }
    }

    private static List<Vec3> createTrailPath(Vec3 position, LightFlightVisualState visualState) {
        List<Vec3> trail = new ArrayList<>();
        trail.add(position);
        float length = 0.0F;
        for (Vec3 point : visualState.trailPositions()) {
            if (point.distanceTo(position) < 0.01D) {
                continue;
            }
            float segmentLength = (float) trail.get(trail.size() - 1).distanceTo(point);
            if (segmentLength <= 0.01F) {
                continue;
            }
            if (length + segmentLength >= MAXIMUM_TAIL_LENGTH) {
                Vec3 last = trail.get(trail.size() - 1);
                trail.add(last.lerp(point, (MAXIMUM_TAIL_LENGTH - length) / segmentLength));
                break;
            }
            trail.add(point);
            length += segmentLength;
        }
        return trail;
    }

    private static Vec3 tubeOffset(Vector3f side, Vector3f top, float angle, float radius) {
        float sideOffset = Mth.cos(angle) * radius;
        float topOffset = Mth.sin(angle) * radius;
        return new Vec3(
                side.x() * sideOffset + top.x() * topOffset,
                side.y() * sideOffset + top.y() * topOffset,
                side.z() * sideOffset + top.z() * topOffset
        );
    }

    private static void appendBillboard(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float size,
            int color,
            float alpha,
            Vector3f right,
            Vector3f up
    ) {
        float halfSize = size * 0.5F;
        appendVertex(bufferBuilder, matrix, center, right, up, -halfSize, -halfSize, color, alpha, 0.0F, 1.0F);
        appendVertex(bufferBuilder, matrix, center, right, up, halfSize, -halfSize, color, alpha, 1.0F, 1.0F);
        appendVertex(bufferBuilder, matrix, center, right, up, halfSize, halfSize, color, alpha, 1.0F, 0.0F);
        appendVertex(bufferBuilder, matrix, center, right, up, -halfSize, halfSize, color, alpha, 0.0F, 0.0F);
    }

    private static void renderStopImpact(
            Matrix4f matrix,
            ShaderInstance shaderInstance,
            float partialTick,
            Vec3 cameraPosition
    ) {
        if (!ClientLightFlightManager.isStopImpactActive()) {
            return;
        }
        Vec3 center = ClientLightFlightManager.getStopImpactPosition(partialTick);
        if (center.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
            return;
        }
        float progress = ClientLightFlightManager.getStopImpactProgress(partialTick);
        int color = ClientLightFlightManager.getStopImpactColor();
        shaderInstance.safeGetUniform("Mode").set(2.0F);
        shaderInstance.safeGetUniform("Boost").set(1.0F);
        shaderInstance.safeGetUniform("TailStrength").set(progress);
        shaderInstance.safeGetUniform("ImpactProgress").set(progress);
        shaderInstance.safeGetUniform("ImpactLayer").set(0.0F);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        appendImpactVolume(
                bufferBuilder,
                matrix,
                center,
                Mth.lerp(progress, 0.35F, 4.8F),
                color,
                0.92F
        );
        BufferUploader.drawWithShader(bufferBuilder.end());

        shaderInstance.safeGetUniform("ImpactLayer").set(1.0F);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        appendImpactVolume(
                bufferBuilder,
                matrix,
                center,
                Mth.lerp(progress, 0.25F, 2.75F),
                color,
                1.0F
        );
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void appendImpactVolume(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float radius,
            int color,
            float alpha
    ) {
        for (int latitude = 0; latitude < IMPACT_LATITUDE_RINGS; latitude++) {
            float v0 = latitude / (float) IMPACT_LATITUDE_RINGS;
            float v1 = (latitude + 1.0F) / IMPACT_LATITUDE_RINGS;
            for (int longitude = 0; longitude < IMPACT_LONGITUDE_SIDES; longitude++) {
                float u0 = longitude / (float) IMPACT_LONGITUDE_SIDES;
                float u1 = (longitude + 1.0F) / IMPACT_LONGITUDE_SIDES;
                appendImpactVertex(bufferBuilder, matrix, center, radius, color, alpha, u0, v0);
                appendImpactVertex(bufferBuilder, matrix, center, radius, color, alpha, u0, v1);
                appendImpactVertex(bufferBuilder, matrix, center, radius, color, alpha, u1, v1);
                appendImpactVertex(bufferBuilder, matrix, center, radius, color, alpha, u1, v0);
            }
        }
    }

    private static void appendImpactVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float radius,
            int color,
            float alpha,
            float u,
            float v
    ) {
        float longitude = u * Mth.TWO_PI;
        float latitude = (v - 0.5F) * Mth.PI;
        float cosLatitude = Mth.cos(latitude);
        float lobe = 1.0F
                + 0.12F * Mth.sin(longitude * 3.0F + latitude * 5.0F)
                + 0.07F * Mth.cos(longitude * 7.0F - latitude * 4.0F);
        Vec3 position = center.add(
                Mth.cos(longitude) * cosLatitude * radius * lobe,
                Mth.sin(latitude) * radius * lobe,
                Mth.sin(longitude) * cosLatitude * radius * lobe
        );
        appendVertex(bufferBuilder, matrix, position, color, alpha, u, v);
    }

    private static void appendVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            Vector3f right,
            Vector3f up,
            float rightOffset,
            float upOffset,
            int color,
            float alpha,
            float u,
            float v
    ) {
        Vec3 position = center.add(
                right.x() * rightOffset + up.x() * upOffset,
                right.y() * rightOffset + up.y() * upOffset,
                right.z() * rightOffset + up.z() * upOffset
        );
        appendVertex(bufferBuilder, matrix, position, color, alpha, u, v);
    }

    private static void appendVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 position,
            int color,
            float alpha,
            float u,
            float v
    ) {
        bufferBuilder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, Mth.clamp(Math.round(alpha * 255.0F), 0, 255))
                .uv(u, v)
                .endVertex();
    }
}
