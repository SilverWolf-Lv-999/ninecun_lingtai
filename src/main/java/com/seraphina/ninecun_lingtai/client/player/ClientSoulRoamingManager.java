package com.seraphina.ninecun_lingtai.client.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.common.player.SoulRoamingSyncData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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

    private static boolean active;
    private static ResourceKey<Level> bodyDimension = Level.OVERWORLD;
    private static Vec3 bodyPosition = Vec3.ZERO;
    private static int bodyEntityId = -1;
    private static boolean bodyExists;

    private ClientSoulRoamingManager() {
    }

    public static void accept(SoulRoamingSyncData syncData) {
        if (!syncData.active()) {
            reset();
            return;
        }
        active = true;
        bodyDimension = syncData.bodyDimension();
        bodyPosition = syncData.bodyPosition();
        bodyEntityId = syncData.bodyEntityId();
        bodyExists = syncData.bodyExists();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ClientMeditationManager.removePlayer(player.getUUID());
            ClientSpellManager.reset();
            ClientTechniqueManager.reset();
            ClientPlayerValueControlManager.reset();
            ClientLightFlightManager.removePlayer(player.getUUID());
            ClientShrinkEarthManager.setActive(player.getUUID(), false, 1);
        }
    }

    public static void tick() {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }
        player.noPhysics = true;
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        if (player.getAbilities().mayfly && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
        }
    }

    public static boolean isLocalPlayerSoulRoaming() {
        return active && Minecraft.getInstance().player != null;
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
        updateBodyPositionFromClientLevel(minecraft);

        double deltaX = bodyPosition.x - Mth.lerp(event.getPartialTick(), player.xo, player.getX());
        double deltaZ = bodyPosition.z - Mth.lerp(event.getPartialTick(), player.zo, player.getZ());
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
        active = false;
        bodyDimension = Level.OVERWORLD;
        bodyPosition = Vec3.ZERO;
        bodyEntityId = -1;
        bodyExists = false;
    }

    private static void updateBodyPositionFromClientLevel(Minecraft minecraft) {
        if (bodyEntityId < 0 || minecraft.level == null || !minecraft.level.dimension().equals(bodyDimension)) {
            return;
        }
        Entity body = minecraft.level.getEntity(bodyEntityId);
        if (body == null) {
            return;
        }
        bodyPosition = body.position();
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
