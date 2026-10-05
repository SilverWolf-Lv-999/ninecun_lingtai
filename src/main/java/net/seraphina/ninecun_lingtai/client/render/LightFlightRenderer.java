package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightFlightRenderer {
    private static final int LIGHT_COLOR = 0x92FFD0;
    private static final double MAXIMUM_RENDER_DISTANCE_SQUARED = 256.0D * 256.0D;
    private static final ResourceLocation FLOW_TEXTURE = new ResourceLocation(
            ModMain.MOD_ID, "textures/effect/light_flight/flow.png"
    );

    private LightFlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        LightFlightVisualState state = ClientLightFlightManager.getVisualState(event.getEntity().getUUID());
        if (state != null && state.style() == LightFlightStyle.LIGHT && state.isFlying()
                && state.isMoving() && LightFlightShader.getLightFlightShader() != null) {
            event.setCanceled(true);
        }
    }

    public static void renderPostWorld(float partialTick, PoseStack poseStack, Camera camera,
                                      Matrix4f projectionMatrix) {
        ShaderInstance shader = LightFlightShader.getLightFlightShader();
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null || minecraft.level == null
                || (ClientLightFlightManager.getVisualStates().isEmpty()
                && !ClientLightFlightManager.isStopImpactActive())) {
            return;
        }
        Vec3 cameraPosition = camera.getPosition();
        Vector3f rightVector = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.rotation());
        Vector3f upVector = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.rotation());
        Vec3 cameraRight = new Vec3(rightVector.x, rightVector.y, rightVector.z);
        Vec3 cameraUp = new Vec3(upVector.x, upVector.y, upVector.z);
        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, FLOW_TEXTURE);
            float time = (minecraft.level.getGameTime() % 24000L + partialTick) / 20.0F;
            shader.safeGetUniform("Time").set(time);
            Matrix4f matrix = poseStack.last().pose();
            for (Map.Entry<UUID, LightFlightVisualState> entry : ClientLightFlightManager.getVisualStates().entrySet()) {
                Player player = minecraft.level.getPlayerByUUID(entry.getKey());
                LightFlightVisualState state = entry.getValue();
                if (player == null || state.style() != LightFlightStyle.LIGHT
                        || player.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED
                        || state.visibility(partialTick) <= 0.0F) {
                    continue;
                }
                Vec3 position = new Vec3(
                        Mth.lerp(partialTick, player.xOld, player.getX()),
                        Mth.lerp(partialTick, player.yOld, player.getY()) + player.getBbHeight() * 0.52D,
                        Mth.lerp(partialTick, player.zOld, player.getZ())
                );
                float boost = state.boostVisibility(partialTick);
                shader.safeGetUniform("Boost").set(boost);
                float visibility = state.visibility(partialTick);
                boolean insideLocalLight = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
                if (insideLocalLight) {
                    visibility *= 0.12F;
                }
                List<Vec3> trail = createTrailPath(position, state, partialTick);
                if (trail.size() > 1) {
                    appendTail(matrix, shader, trail, cameraPosition, cameraRight, time,
                            boost, visibility * state.trailVisibility(partialTick));
                }
                if (state.isMoving() && !insideLocalLight) {
                    shader.safeGetUniform("Mode").set(0.0F);
                    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                    float radius = 1.3F + boost * 0.2F;
                    appendBillboard(buffer, matrix, position.subtract(cameraPosition), cameraRight.scale(radius),
                            cameraUp.scale(radius), visibility);
                    BufferUploader.drawWithShader(buffer.end());
                }
            }
            renderStopImpact(matrix, shader, partialTick, cameraPosition, cameraRight, cameraUp);
        } finally {
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.restoreProjectionMatrix();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static List<Vec3> createTrailPath(Vec3 position, LightFlightVisualState state, float partialTick) {
        List<Vec3> anchors = new ArrayList<>();
        anchors.add(position);
        List<Vec3> history = new ArrayList<>(state.trailPositions());
        float length = 0.0F;
        float maximumLength = 13.0F + 5.0F * state.boostVisibility(partialTick);
        for (int i = state.isMoving() && history.size() > 1 ? 1 : 0; i < history.size(); i++) {
            Vec3 last = anchors.get(anchors.size() - 1);
            Vec3 point = history.get(i);
            float distance = (float) last.distanceTo(point);
            if (distance < 0.025F) {
                continue;
            }
            if (distance > 32.0F) {
                break;
            }
            if (length + distance >= maximumLength) {
                anchors.add(last.lerp(point, (maximumLength - length) / distance));
                break;
            }
            anchors.add(point);
            length += distance;
        }
        List<Vec3> trail = new ArrayList<>();
        trail.add(position);
        for (int i = 0; i < anchors.size() - 1 && trail.size() < 160; i++) {
            Vec3 a = anchors.get(Math.max(0, i - 1));
            Vec3 b = anchors.get(i);
            Vec3 c = anchors.get(i + 1);
            Vec3 d = anchors.get(Math.min(anchors.size() - 1, i + 2));
            Vec3 segment = c.subtract(b);
            double distance = segment.length();
            Vec3 tangent0 = c.subtract(a).normalize().scale(distance);
            Vec3 tangent1 = d.subtract(b).normalize().scale(distance);
            int steps = Mth.clamp((int) Math.ceil(distance / 0.22D), 1, 12);
            for (int step = 1; step <= steps && trail.size() < 160; step++) {
                double t = step / (double) steps;
                double t2 = t * t;
                double t3 = t2 * t;
                trail.add(b.scale(2.0D * t3 - 3.0D * t2 + 1.0D)
                        .add(tangent0.scale(t3 - 2.0D * t2 + t))
                        .add(c.scale(-2.0D * t3 + 3.0D * t2))
                        .add(tangent1.scale(t3 - t2)));
            }
        }
        return trail;
    }

    private static void appendTail(Matrix4f matrix, ShaderInstance shader, List<Vec3> trail,
                                   Vec3 cameraPosition, Vec3 cameraRight, float time, float boost, float alpha) {
        if (alpha <= 0.001F) {
            return;
        }
        int count = trail.size();
        Vec3[] sides = new Vec3[count];
        Vec3[] ups = new Vec3[count];
        float[] distances = new float[count];
        for (int i = 0; i < count; i++) {
            Vec3 tangent = trail.get(Math.max(0, i - 1)).subtract(trail.get(Math.min(count - 1, i + 1))).normalize();
            Vec3 side = tangent.cross(cameraPosition.subtract(trail.get(i)).normalize());
            if (side.lengthSqr() < 0.0001D) {
                side = cameraRight.subtract(tangent.scale(cameraRight.dot(tangent)));
                if (side.lengthSqr() < 0.0001D) {
                    side = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
                }
            }
            side = side.normalize();
            if (i > 0 && side.dot(sides[i - 1]) < 0.0D) {
                side = side.scale(-1.0D);
            }
            sides[i] = side;
            ups[i] = tangent.cross(side).normalize();
            if (i > 0) {
                distances[i] = distances[i - 1] + (float) trail.get(i).distanceTo(trail.get(i - 1));
            }
        }
        float totalLength = Math.max(0.001F, distances[count - 1]);
        shader.safeGetUniform("TailLength").set(totalLength);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        for (int layer = 0; layer < 2; layer++) {
            shader.safeGetUniform("Mode").set(layer == 0 ? 1.0F : 3.0F);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            int strands = layer == 0 ? 1 : 8;
            for (int strand = 0; strand < strands; strand++) {
                Vec3 previousLeft = Vec3.ZERO;
                Vec3 previousRight = Vec3.ZERO;
                for (int i = 0; i < count; i++) {
                    float u = distances[i] / totalLength;
                    float taper = (float) Math.pow(1.0F - u, 0.62D);
                    float spread = (0.1F + 0.26F * Mth.sin(Math.min(1.0F, distances[i] / 2.5F) * Mth.HALF_PI)) * taper;
                    float phase = strand * 2.399963F + distances[i] * (0.6F + strand * 0.045F) - time * (1.2F + strand * 0.11F);
                    Vec3 center = trail.get(i).subtract(cameraPosition);
                    float width;
                    if (layer == 0) {
                        width = (0.38F + 0.14F * boost) * taper + 0.012F;
                    } else {
                        center = center.add(sides[i].scale(Mth.sin(phase) * spread))
                                .add(ups[i].scale(Mth.cos(phase * 0.83F) * spread * 0.7F));
                        width = (0.018F + (strand % 3) * 0.009F) * taper * (1.0F + boost * 0.25F);
                    }
                    Vec3 left = center.subtract(sides[i].scale(width));
                    Vec3 right = center.add(sides[i].scale(width));
                    if (i > 0) {
                        float previousU = distances[i - 1] / totalLength;
                        float opacity = alpha * (layer == 0 ? 0.68F : 0.42F + (strand % 3) * 0.1F);
                        appendVertex(buffer, matrix, previousLeft, opacity, previousU, 0.0F);
                        appendVertex(buffer, matrix, left, opacity, u, 0.0F);
                        appendVertex(buffer, matrix, right, opacity, u, 1.0F);
                        appendVertex(buffer, matrix, previousRight, opacity, previousU, 1.0F);
                    }
                    previousLeft = left;
                    previousRight = right;
                }
            }
            BufferUploader.drawWithShader(buffer.end());
        }
        shader.safeGetUniform("Mode").set(4.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int spark = 0; spark < 24; spark++) {
            float u = Mth.frac(spark * 0.618034F + time * (0.18F + (spark % 3) * 0.025F));
            float distance = u * totalLength;
            int segment = 1;
            while (segment < count - 1 && distances[segment] < distance) {
                segment++;
            }
            float fraction = (distance - distances[segment - 1])
                    / Math.max(0.001F, distances[segment] - distances[segment - 1]);
            Vec3 side = sides[segment - 1].lerp(sides[segment], fraction).normalize();
            Vec3 tangent = trail.get(segment - 1).subtract(trail.get(segment)).normalize();
            float spread = Mth.sin(u * Mth.PI) * (0.2F + (spark % 5) * 0.035F);
            Vec3 point = trail.get(segment - 1).lerp(trail.get(segment), fraction).subtract(cameraPosition)
                    .add(side.scale(Mth.sin(spark * 2.399963F + time * 0.6F) * spread))
                    .add(ups[segment].scale(Mth.cos(spark * 1.37F) * spread));
            float opacity = alpha * Mth.sin(u * Mth.PI) * (1.0F - u) * 0.7F;
            float width = 0.009F + (spark % 3) * 0.003F;
            float length = 0.035F + (spark % 4) * 0.016F;
            appendBillboard(buffer, matrix, point, tangent.scale(length), side.scale(width), opacity);
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void renderStopImpact(Matrix4f matrix, ShaderInstance shader, float partialTick,
                                         Vec3 cameraPosition, Vec3 cameraRight, Vec3 cameraUp) {
        if (!ClientLightFlightManager.isStopImpactActive()) {
            return;
        }
        Vec3 worldCenter = ClientLightFlightManager.getStopImpactPosition(partialTick);
        if (worldCenter.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
            return;
        }
        Vec3 center = worldCenter.subtract(cameraPosition);
        float progress = ClientLightFlightManager.getStopImpactProgress(partialTick);
        float expansion = 1.0F - (float) Math.exp(-progress * 16.0D);
        float fade = 1.0F - (float) Mth.smoothstep(progress);
        shader.safeGetUniform("ImpactProgress").set(progress);
        shader.safeGetUniform("Boost").set(0.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        shader.safeGetUniform("Mode").set(5.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int plume = 0; plume < 9; plume++) {
            float angle = plume * 2.399963F;
            float radius = expansion * (0.45F + (plume % 3) * 0.24F);
            Vec3 point = center.add(cameraRight.scale(Mth.cos(angle) * radius))
                    .add(0.0D, Mth.sin(angle) * radius + progress * 0.95F, 0.0D);
            float size = 0.3F + expansion * (0.42F + (plume % 3) * 0.14F);
            appendBillboard(buffer, matrix, point, cameraRight.scale(size), cameraUp.scale(size * 1.8F),
                    fade * 0.64F);
        }
        BufferUploader.drawWithShader(buffer.end());
        shader.safeGetUniform("Mode").set(2.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        float shellSize = 0.25F + expansion * 1.65F;
        appendBillboard(buffer, matrix, center.add(0.0D, progress * 0.45F, 0.0D),
                cameraRight.scale(shellSize), cameraUp.scale(shellSize * 1.25F), fade);
        BufferUploader.drawWithShader(buffer.end());
        shader.safeGetUniform("Mode").set(4.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int spark = 0; spark < 32; spark++) {
            double angle = spark * 2.399963D;
            double y = 1.0D - 2.0D * (spark + 0.5D) / 32.0D;
            double radial = Math.sqrt(1.0D - y * y);
            double radius = expansion * (0.75D + (spark % 7) * 0.21D);
            Vec3 point = center.add(Math.cos(angle) * radial * radius,
                    y * radius + progress * 0.4D, Math.sin(angle) * radial * radius);
            float size = 0.018F + (spark % 3) * 0.008F;
            appendBillboard(buffer, matrix, point, cameraRight.scale(size), cameraUp.scale(size * 1.6F), fade);
        }
        BufferUploader.drawWithShader(buffer.end());
        shader.safeGetUniform("Mode").set(0.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        float flash = 1.0F - (float) Mth.smoothstep(Mth.clamp(progress / 0.58F, 0.0F, 1.0F));
        float size = 1.3F + expansion * 0.7F;
        appendBillboard(buffer, matrix, center, cameraRight.scale(size), cameraUp.scale(size), flash);
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void appendBillboard(BufferBuilder buffer, Matrix4f matrix, Vec3 center,
                                        Vec3 right, Vec3 up, float alpha) {
        appendVertex(buffer, matrix, center.subtract(right).subtract(up), alpha, 0.0F, 1.0F);
        appendVertex(buffer, matrix, center.add(right).subtract(up), alpha, 1.0F, 1.0F);
        appendVertex(buffer, matrix, center.add(right).add(up), alpha, 1.0F, 0.0F);
        appendVertex(buffer, matrix, center.subtract(right).add(up), alpha, 0.0F, 0.0F);
    }

    private static void appendVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position,
                                     float alpha, float u, float v) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(LIGHT_COLOR >> 16 & 0xFF, LIGHT_COLOR >> 8 & 0xFF, LIGHT_COLOR & 0xFF,
                        Mth.clamp(Math.round(alpha * 255.0F), 0, 255))
                .uv(u, v).endVertex();
    }
}
