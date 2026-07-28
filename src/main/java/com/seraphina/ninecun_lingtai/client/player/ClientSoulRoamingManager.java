package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SoulInteractionPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SoulMovementPacket;
import com.seraphina.ninecun_lingtai.common.player.SoulRoamingSyncData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.joml.Matrix4f;

public final class ClientSoulRoamingManager {
    private static final int ARROW_COLOR = 0xDDEAF8FF;
    private static final int ARROW_HIGHLIGHT_COLOR = 0xF8FFFFFF;
    private static final int ARROW_SHADOW_COLOR = 0x90050A12;
    private static final float ARROW_HEAD_LENGTH = 14.0F;
    private static final float ARROW_HEAD_WIDTH = 8.0F;
    private static final float ARROW_TAIL_LENGTH = 13.0F;
    private static final float ARROW_TAIL_HALF_WIDTH = 2.5F;
    private static final float MOUSE_ROTATION_SCALE = 0.15F;

    private static boolean active;
    private static ResourceKey<Level> bodyDimension = Level.OVERWORLD;
    private static Vec3 bodyPosition = Vec3.ZERO;
    private static int soulEntityId = -1;
    private static boolean bodyExists;
    private static float soulYRot;
    private static float soulXRot;
    private static boolean soulRotationInitialized;
    private static float leftImpulse;
    private static float forwardImpulse;
    private static boolean jumping;
    private static boolean shiftKeyDown;

    private ClientSoulRoamingManager() {
    }

    public static void accept(SoulRoamingSyncData syncData) {
        if (!syncData.active()) {
            reset();
            return;
        }
        boolean wasActive = active;
        active = true;
        bodyDimension = syncData.bodyDimension();
        bodyPosition = syncData.bodyPosition();
        soulEntityId = syncData.soulEntityId();
        bodyExists = syncData.bodyExists();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            if (!wasActive) {
                soulYRot = player.getYRot();
                soulXRot = player.getXRot();
                soulRotationInitialized = true;
            }
            ClientSpellManager.reset();
            ClientTechniqueManager.reset();
            ClientPlayerValueControlManager.reset();
            ClientLightFlightManager.removePlayer(player.getUUID());
            ClientShrinkEarthManager.setActive(player.getUUID(), false, 1);
            applySoulRotation(findSoulEntity(minecraft), soulYRot, soulXRot);
            updateCameraEntity(minecraft);
        }
    }

    public static void tick() {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            reset();
            return;
        }
        SoulEntity soul = findSoulEntity(minecraft);
        if (soul != null) {
            if (!soulRotationInitialized) {
                soulYRot = soul.getYRot();
                soulXRot = soul.getXRot();
                soulRotationInitialized = true;
            }
            soul.moveSoul(
                    leftImpulse,
                    forwardImpulse,
                    (jumping ? 1.0F : 0.0F) + (shiftKeyDown ? -1.0F : 0.0F),
                    soulYRot,
                    soulXRot
            );
            applySoulRotation(soul, soulYRot, soulXRot);
            updateCameraEntity(minecraft);
        }
    }

    public static void sendMovement(Input input) {
        if (!active) {
            return;
        }
        float leftImpulse = Mth.clamp(input.leftImpulse, -1.0F, 1.0F);
        float forwardImpulse = Mth.clamp(input.forwardImpulse, -1.0F, 1.0F);
        ClientSoulRoamingManager.leftImpulse = leftImpulse;
        ClientSoulRoamingManager.forwardImpulse = forwardImpulse;
        ClientSoulRoamingManager.jumping = input.jumping;
        ClientSoulRoamingManager.shiftKeyDown = input.shiftKeyDown;
        ModNetwork.sendToServer(new SoulMovementPacket(
            leftImpulse,
            forwardImpulse,
                input.jumping,
                input.shiftKeyDown,
                soulYRot,
                soulXRot
        ));
    }

    public static boolean rotateSoul(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (!active) {
            return false;
        }
        soulYRot = Mth.wrapDegrees(soulYRot + (float) yRotDelta * MOUSE_ROTATION_SCALE);
        soulXRot = Mth.clamp(
                soulXRot + (float) xRotDelta * MOUSE_ROTATION_SCALE,
                -90.0F,
                90.0F
        );
        applySoulRotation(findSoulEntity(Minecraft.getInstance()), soulYRot, soulXRot);
        return true;
    }

    public static void sendInteraction(int targetEntityId) {
        if (active) {
            ModNetwork.sendToServer(new SoulInteractionPacket(targetEntityId));
        }
    }

    public static boolean isLocalPlayerSoulRoaming() {
        return active && Minecraft.getInstance().player != null;
    }

    public static float getCameraYRot(Entity cameraEntity, float partialTick) {
        float baseRotation = isSoulCameraEntity(cameraEntity)
                ? soulYRot
                : ClientMeditationCameraManager.getCameraYRot(cameraEntity, partialTick);
        return ClientTribulationManager.applyCameraYRot(cameraEntity, baseRotation, partialTick);
    }

    public static float getCameraXRot(Entity cameraEntity, float partialTick) {
        float baseRotation = isSoulCameraEntity(cameraEntity)
                ? soulXRot
                : ClientMeditationCameraManager.getCameraXRot(cameraEntity, partialTick);
        return ClientTribulationManager.applyCameraXRot(cameraEntity, baseRotation, partialTick);
    }

    public static Vec3 getCameraViewVector(Entity cameraEntity, float partialTick) {
        Vec3 baseVector = isSoulCameraEntity(cameraEntity)
                ? Vec3.directionFromRotation(soulXRot, soulYRot)
                : ClientMeditationCameraManager.getCameraViewVector(cameraEntity, partialTick);
        return ClientTribulationManager.applyCameraViewVector(cameraEntity, baseVector, partialTick);
    }

    public static HitResult pick(Entity cameraEntity, double hitDistance, float partialTick, boolean hitFluids) {
        if (!isSoulCameraEntity(cameraEntity)) {
            return ClientMeditationCameraManager.pick(cameraEntity, hitDistance, partialTick, hitFluids);
        }
        Vec3 eyePosition = cameraEntity.getEyePosition(partialTick);
        Vec3 viewVector = getCameraViewVector(cameraEntity, partialTick);
        Vec3 targetPosition = eyePosition.add(viewVector.scale(hitDistance));
        return cameraEntity.level().clip(new ClipContext(
                eyePosition,
                targetPosition,
                ClipContext.Block.OUTLINE,
                hitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                cameraEntity
        ));
    }

    public static void renderBodyArrow(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!active || !bodyExists || minecraft.options.hideGui || player == null || minecraft.level == null) {
            return;
        }
        if (!minecraft.level.dimension().equals(bodyDimension)) {
            return;
        }
        SoulEntity soul = findSoulEntity(minecraft);
        if (soul == null) {
            return;
        }

        double soulX = Mth.lerp(event.getPartialTick(), soul.xo, soul.getX());
        double soulZ = Mth.lerp(event.getPartialTick(), soul.zo, soul.getZ());
        double deltaX = bodyPosition.x - soulX;
        double deltaZ = bodyPosition.z - soulZ;
        if (deltaX * deltaX + deltaZ * deltaZ < 0.0001D) {
            return;
        }

        double bodyAngle = Math.atan2(deltaZ, deltaX);
        float cameraYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        double yawRadians = Math.toRadians(cameraYaw);
        double forwardAngle = Math.atan2(Math.cos(yawRadians), -Math.sin(yawRadians));
        float screenAngle = (float) (forwardAngle - bodyAngle);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0F;
        float centerY = Math.max(28.0F, screenHeight / 2.0F - 50.0F);
        float pulse = 1.0F + Mth.sin((player.tickCount + event.getPartialTick()) * 0.18F) * 0.05F;

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        appendArrow(bufferBuilder, matrix, centerX + 1.0F, centerY + 1.0F, screenAngle, pulse, ARROW_SHADOW_COLOR);
        appendArrow(bufferBuilder, matrix, centerX, centerY, screenAngle, pulse, ARROW_COLOR);
        appendArrowHighlight(bufferBuilder, matrix, centerX, centerY, screenAngle, pulse, ARROW_HIGHLIGHT_COLOR);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void reset() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getCameraEntity() != minecraft.player) {
            minecraft.setCameraEntity(minecraft.player);
        }
        active = false;
        bodyDimension = Level.OVERWORLD;
        bodyPosition = Vec3.ZERO;
        soulEntityId = -1;
        bodyExists = false;
        soulYRot = 0.0F;
        soulXRot = 0.0F;
        soulRotationInitialized = false;
        leftImpulse = 0.0F;
        forwardImpulse = 0.0F;
        jumping = false;
        shiftKeyDown = false;
    }

    private static void updateCameraEntity(Minecraft minecraft) {
        SoulEntity soul = findSoulEntity(minecraft);
        if (soul != null && minecraft.getCameraEntity() != soul) {
            minecraft.setCameraEntity(soul);
        }
    }

    private static SoulEntity findSoulEntity(Minecraft minecraft) {
        if (soulEntityId < 0 || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(soulEntityId);
        return entity instanceof SoulEntity soul ? soul : null;
    }

    private static boolean isSoulCameraEntity(Entity entity) {
        return active && entity != null && entity.getId() == soulEntityId;
    }

    private static void applySoulRotation(Entity entity, float yRot, float xRot) {
        if (!(entity instanceof SoulEntity soul)) {
            return;
        }
        soul.setYRot(yRot);
        soul.setXRot(xRot);
        soul.setYHeadRot(yRot);
        soul.setYBodyRot(yRot);
        soul.yRotO = yRot;
        soul.xRotO = xRot;
        soul.yHeadRotO = yRot;
        soul.yBodyRotO = yRot;
    }

    private static void appendArrow(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float centerX,
            float centerY,
            float angle,
            float scale,
            int color
    ) {
        ScreenPoint tip = rotate(centerX, centerY, 0.0F, -ARROW_HEAD_LENGTH * scale, angle);
        ScreenPoint left = rotate(centerX, centerY, -ARROW_HEAD_WIDTH * scale, 2.0F * scale, angle);
        ScreenPoint right = rotate(centerX, centerY, ARROW_HEAD_WIDTH * scale, 2.0F * scale, angle);
        ScreenPoint tailLeft = rotate(centerX, centerY, -ARROW_TAIL_HALF_WIDTH * scale, 1.0F * scale, angle);
        ScreenPoint tailRight = rotate(centerX, centerY, ARROW_TAIL_HALF_WIDTH * scale, 1.0F * scale, angle);
        ScreenPoint tailEndLeft = rotate(centerX, centerY, -ARROW_TAIL_HALF_WIDTH * scale, ARROW_TAIL_LENGTH * scale, angle);
        ScreenPoint tailEndRight = rotate(centerX, centerY, ARROW_TAIL_HALF_WIDTH * scale, ARROW_TAIL_LENGTH * scale, angle);

        appendTriangle(bufferBuilder, matrix, tip, left, right, color);
        appendTriangle(bufferBuilder, matrix, tailLeft, tailEndLeft, tailEndRight, color);
        appendTriangle(bufferBuilder, matrix, tailLeft, tailEndRight, tailRight, color);
    }

    private static void appendArrowHighlight(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float centerX,
            float centerY,
            float angle,
            float scale,
            int color
    ) {
        ScreenPoint innerTip = rotate(centerX, centerY, 0.0F, -ARROW_HEAD_LENGTH * 0.58F * scale, angle);
        ScreenPoint innerLeft = rotate(centerX, centerY, -ARROW_TAIL_HALF_WIDTH * 0.9F * scale, -0.5F * scale, angle);
        ScreenPoint innerRight = rotate(centerX, centerY, ARROW_TAIL_HALF_WIDTH * 0.9F * scale, -0.5F * scale, angle);
        appendTriangle(bufferBuilder, matrix, innerTip, innerLeft, innerRight, color);
    }

    private static void appendTriangle(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            ScreenPoint first,
            ScreenPoint second,
            ScreenPoint third,
            int color
    ) {
        vertex(bufferBuilder, matrix, first, color);
        vertex(bufferBuilder, matrix, second, color);
        vertex(bufferBuilder, matrix, third, color);
    }

    private static void vertex(BufferBuilder bufferBuilder, Matrix4f matrix, ScreenPoint point, int color) {
        bufferBuilder.vertex(matrix, point.x(), point.y(), 240.0F)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF)
                .endVertex();
    }

    private static ScreenPoint rotate(float centerX, float centerY, float localX, float localY, float angle) {
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        return new ScreenPoint(
                centerX + localX * cos - localY * sin,
                centerY + localX * sin + localY * cos
        );
    }

    private record ScreenPoint(float x, float y) {
    }
}
