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
                        || player.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
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
                renderFlightMotes(matrix, shader, state, partialTick, cameraPosition, cameraRight,
                        insideLocalLight ? 0.12F : 1.0F);
                if (state.isMoving() && !insideLocalLight) {
                    Vec3 direction = trail.size() > 1 ? position.subtract(trail.get(1)).normalize()
                            : player.getLookAngle();
                    renderFlightEnvelope(matrix, shader, position, direction, cameraPosition,
                            time, boost, visibility);
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
        float maximumLength = 50.0F;
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
        for (int i = 0; i < anchors.size() - 1 && trail.size() < 640; i++) {
            Vec3 a = anchors.get(Math.max(0, i - 1));
            Vec3 b = anchors.get(i);
            Vec3 c = anchors.get(i + 1);
            Vec3 d = anchors.get(Math.min(anchors.size() - 1, i + 2));
            Vec3 segment = c.subtract(b);
            double distance = segment.length();
            Vec3 tangent0 = c.subtract(a).normalize().scale(distance);
            Vec3 tangent1 = d.subtract(b).normalize().scale(distance);
            int steps = Mth.clamp((int) Math.ceil(distance / 0.22D), 1, 12);
            for (int step = 1; step <= steps && trail.size() < 640; step++) {
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
        float[] bendWidths = new float[count];
        for (int i = 0; i < count; i++) {
            bendWidths[i] = 1.0F;
            if (i > 0 && i < count - 1) {
                Vec3 incoming = trail.get(i).subtract(trail.get(i - 1)).normalize();
                Vec3 outgoing = trail.get(i + 1).subtract(trail.get(i)).normalize();
                bendWidths[i] = Mth.clamp((float) Math.sqrt(Math.max(0.0D,
                        (1.0D + incoming.dot(outgoing)) * 0.5D)) * 1.3F, 0.08F, 1.0F);
            }
            Vec3 tangent = trail.get(Math.max(0, i - 1)).subtract(trail.get(Math.min(count - 1, i + 1)));
            if (tangent.lengthSqr() < 0.000001D) {
                tangent = i + 1 < count ? trail.get(i).subtract(trail.get(i + 1))
                        : trail.get(i - 1).subtract(trail.get(i));
            }
            tangent = tangent.normalize();
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
            int strands = layer == 0 ? 2 : 6;
            for (int strand = 0; strand < strands; strand++) {
                Vec3 previousLeft = Vec3.ZERO;
                Vec3 previousRight = Vec3.ZERO;
                for (int i = 0; i < count; i++) {
                    float u = distances[i] / totalLength;
                    float taper = (float) Math.pow(1.0F - u, 0.3D) * bendWidths[i];
                    float opening = (float) Math.sqrt(Math.min(1.0F, distances[i] / 3.0F));
                    float spread = (0.06F + 0.48F * opening) * taper * (1.0F + boost * 0.3F);
                    float phase = strand * 2.399963F + distances[i] * (0.5F + strand * 0.018F) - time * 0.85F;
                    Vec3 center = trail.get(i).subtract(cameraPosition);
                    float width;
                    if (layer == 0) {
                        width = spread * (strand == 0 ? 1.45F : 0.8F) + 0.008F;
                    } else {
                        float offset = (strand - 2.5F) / 3.6F + Mth.sin(phase) * 0.17F;
                        center = center.add(sides[i].scale(offset * spread))
                                .add(ups[i].scale(Mth.sin(phase * 0.61F) * spread * 0.18F));
                        width = (strand % 2 == 0 ? 0.075F : 0.026F) * taper * (0.4F + opening * 0.6F)
                                * (1.0F + boost * 0.25F);
                    }
                    Vec3 left = center.subtract(sides[i].scale(width));
                    Vec3 right = center.add(sides[i].scale(width));
                    if (i > 0) {
                        float previousU = distances[i - 1] / totalLength;
                        float opacity = alpha * (layer == 0 ? (strand == 0 ? 0.56F : 0.45F)
                                : (strand % 2 == 0 ? 0.42F : 0.48F));
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
    }

    private static void renderFlightMotes(Matrix4f matrix, ShaderInstance shader, LightFlightVisualState state,
                                          float partialTick, Vec3 cameraPosition, Vec3 cameraRight, float alpha) {
        var motes = state.flightMotes();
        if (motes.isEmpty()) {
            return;
        }
        shader.safeGetUniform("Mode").set(4.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (LightFlightVisualState.FlightMote mote : motes) {
            float age = state.visualAge(partialTick) - mote.birthTick();
            if (age <= 0.0F || age >= mote.lifetime()) {
                continue;
            }
            float life = age / mote.lifetime();
            double drift = (1.0D - Math.exp(-age * 0.065D)) / 0.065D;
            double flutter = (Math.sin(age * 0.24D + mote.phase()) - Math.sin(mote.phase())) * 0.09D;
            Vec3 center = mote.position().subtract(cameraPosition).add(mote.velocity().scale(drift))
                    .add(flutter * Mth.cos(mote.phase()), age * 0.004D,
                            flutter * Mth.sin(mote.phase()));
            Vec3 view = center.scale(-1.0D).normalize();
            Vec3 axis = mote.velocity().subtract(view.scale(mote.velocity().dot(view)));
            if (axis.lengthSqr() < 0.000001D) {
                axis = cameraRight;
            }
            axis = axis.normalize();
            Vec3 side = axis.cross(view).normalize();
            float fade = (float) Mth.smoothstep(Math.min(1.0F, age / 2.0F))
                    * (1.0F - (float) Mth.smoothstep(life));
            float twinkle = 0.82F + 0.18F * Mth.sin(mote.phase() + age * 0.33F);
            float nearFade = Mth.clamp((float) center.length() - 0.2F, 0.0F, 1.0F);
            appendBillboard(buffer, matrix, center, axis.scale(mote.size() * (2.8F - life)),
                    side.scale(mote.size()), alpha * fade * twinkle * nearFade);
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void renderFlightEnvelope(Matrix4f matrix, ShaderInstance shader, Vec3 position,
                                             Vec3 direction, Vec3 cameraPosition, float time,
                                             float boost, float alpha) {
        Vec3 forward = direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.0001D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        Vec3 center = position.subtract(cameraPosition);
        float nearFade = Mth.clamp(((float) center.length() - 0.8F) / 1.2F, 0.0F, 1.0F);
        shader.safeGetUniform("Mode").set(6.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int ring = 0; ring < 16; ring++) {
            for (int slice = 0; slice < 24; slice++) {
                for (int corner = 0; corner < 4; corner++) {
                    float u = (ring + (corner == 1 || corner == 2 ? 1 : 0)) / 16.0F;
                    float v = (slice + (corner >= 2 ? 1 : 0)) / 24.0F;
                    float angle = v * Mth.TWO_PI;
                    float latitude = u * Mth.PI;
                    Vec3 radial = side.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
                    float ripple = 1.0F + Mth.sin(angle * 3.0F + u * 8.0F - time * 3.2F) * 0.045F
                            + Mth.sin(angle * 5.0F - u * 11.0F + time * 2.1F) * 0.025F;
                    float radius = Mth.sin(latitude) * (0.43F + boost * 0.08F) * ripple;
                    Vec3 point = center.add(forward.scale(Mth.cos(latitude) * (0.82F + boost * 0.2F) - 0.22F))
                            .add(radial.scale(radius));
                    Vec3 normal = forward.scale(Mth.cos(latitude) / (0.82F + boost * 0.2F))
                            .add(radial.scale(Mth.sin(latitude) / (0.43F + boost * 0.08F))).normalize();
                    float facing = (float) normal.dot(point.scale(-1.0D).normalize());
                    float rim = (float) Math.pow(1.0F - Math.abs(facing), 1.5D)
                            * (float) Mth.smoothstep(Math.min(1.0F, Math.abs(facing) / 0.3F));
                    float opacity = alpha * nearFade * (0.08F + rim * 0.46F) * (facing >= 0.0F ? 1.0F : 0.3F);
                    appendVertex(buffer, matrix, point, opacity, u, v);
                }
            }
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
        float nearFade = (float) Mth.smoothstep(Mth.clamp((float) center.length() / 2.0F, 0.0F, 1.0F));
        nearFade *= nearFade;
        float fade = (1.0F - (float) Mth.smoothstep(progress)) * nearFade;
        shader.safeGetUniform("ImpactProgress").set(progress);
        shader.safeGetUniform("Boost").set(0.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        shader.safeGetUniform("Mode").set(5.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int plume = 0; plume < 9; plume++) {
            float angle = plume * 2.399963F;
            float radius = expansion * (0.45F + (plume % 3) * 0.24F);
            Vec3 point = center.add(Mth.cos(angle) * radius, 0.0D, Mth.sin(angle * 1.3F) * radius * 0.65D)
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
        Vec3 flashCenter = center.subtract(center.normalize().scale(Math.min(0.4D, center.length() * 0.25D)));
        appendBillboard(buffer, matrix, flashCenter, cameraRight.scale(size), cameraUp.scale(size), flash * nearFade);
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
