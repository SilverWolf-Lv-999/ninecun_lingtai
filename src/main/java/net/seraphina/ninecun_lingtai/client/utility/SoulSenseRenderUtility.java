package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.player.ClientSoulSenseManager;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulSensePacket;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseTargetCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

public final class SoulSenseRenderUtility {
    private static final double MAXIMUM_LINE_DISTANCE = 160.0D;
    private static final float MINIMUM_ALPHA = 0.25F;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !ClientSoulSenseManager.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.0F);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float partialTick = (float) event.getPartialTick();
        Vec3 lineOrigin = getLineOrigin(minecraft, partialTick);

        for (SyncSoulSensePacket.EntityEntry entry : ClientSoulSenseManager.getEntityEntries()) {
            Entity entity = minecraft.level.getEntity(entry.entityId());
            if (entity == null || entity == minecraft.player) {
                continue;
            }
            SoulSenseTargetCategory category = SoulSenseTargetCategory.fromOrdinal(entry.categoryOrdinal())
                    .orElse(null);
            if (category == null) {
                continue;
            }
            Vec3 position = entity.getPosition(partialTick);
            AABB box = entity.getBoundingBox()
                    .move(position.subtract(entity.position()))
                    .inflate(0.05D);
            float alpha = getDistanceAlpha(cameraPosition, position);
            drawBox(buffer, pose, box, category.getColor(), alpha);
            drawLine(buffer, pose, lineOrigin, box.getCenter(), category.getColor(), alpha * 0.7F);
        }

        for (Map.Entry<BlockPos, Byte> entry : ClientSoulSenseManager.getBlockEntries().entrySet()) {
            SoulSenseTargetCategory category = SoulSenseTargetCategory.fromOrdinal(entry.getValue())
                    .orElse(null);
            if (category == null) {
                continue;
            }
            BlockPos pos = entry.getKey();
            AABB box = new AABB(pos).inflate(-0.06D);
            float alpha = getDistanceAlpha(cameraPosition, box.getCenter());
            drawBox(buffer, pose, box, category.getColor(), alpha);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static Vec3 getLineOrigin(Minecraft minecraft, float partialTick) {
        Vec3 eyePosition = minecraft.player.getEyePosition(partialTick);
        Vec3 look = minecraft.player.getViewVector(partialTick);
        return eyePosition.add(look.scale(0.8D)).subtract(0.0D, 0.25D, 0.0D);
    }

    private static float getDistanceAlpha(Vec3 cameraPosition, Vec3 targetPosition) {
        double distance = cameraPosition.distanceTo(targetPosition);
        float fade = (float) (1.0D - distance / MAXIMUM_LINE_DISTANCE);
        return Mth.clamp(fade, MINIMUM_ALPHA, 1.0F);
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f pose, AABB box, int color, float alpha) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addEdge(buffer, pose, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addEdge(buffer, pose, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        addEdge(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addEdge(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        addEdge(buffer, pose, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addEdge(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addEdge(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void drawLine(
            BufferBuilder buffer,
            Matrix4f pose,
            Vec3 start,
            Vec3 end,
            int color,
            float alpha
    ) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        addEdge(
                buffer,
                pose,
                (float) start.x,
                (float) start.y,
                (float) start.z,
                (float) end.x,
                (float) end.y,
                (float) end.z,
                red,
                green,
                blue,
                alpha
        );
    }

    private static void addEdge(
            BufferBuilder buffer,
            Matrix4f pose,
            float startX,
            float startY,
            float startZ,
            float endX,
            float endY,
            float endZ,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        buffer.vertex(pose, startX, startY, startZ).color(red, green, blue, alpha).endVertex();
        buffer.vertex(pose, endX, endY, endZ).color(red, green, blue, alpha).endVertex();
    }
}
