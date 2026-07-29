package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class ThunderTribulationRenderer extends EntityRenderer<ThunderTribulationEntity> {
    public ThunderTribulationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            ThunderTribulationEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float lifeFactor = Math.min(1.0F, entity.getRemainingTicks() / 8.0F);
        float height = 22.0F + entity.getIntensity() * 8.0F;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        long seed = entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 13)
                ^ entity.getStrikeIndex() * 0x9E3779B97F4A7C15L;
        appendBolt(consumer, matrix, height, entity.getIntensity(), lifeFactor, seed);
        appendBolt(
                consumer,
                matrix,
                height * 0.78F,
                entity.getIntensity() * 0.72F,
                lifeFactor * 0.75F,
                seed ^ 0xD1B54A32D192ED03L
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderTribulationEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static void appendBolt(
            VertexConsumer consumer,
            Matrix4f matrix,
            float height,
            float intensity,
            float lifeFactor,
            long seed
    ) {
        int segmentCount = 16;
        float width = 0.42F + intensity * 0.10F;
        float previousX = 0.0F;
        float previousZ = 0.0F;
        for (int segment = 1; segment <= segmentCount; segment++) {
            float progress = segment / (float) segmentCount;
            float nextX = (random(seed + segment * 0x632BE59BD9B4E019L) - 0.5F)
                    * (1.25F + intensity * 0.28F);
            float nextZ = (random(seed + segment * 0xC6BC279692B5CC83L) - 0.5F)
                    * (1.25F + intensity * 0.28F);
            float previousY = (segment - 1) / (float) segmentCount * height;
            float nextY = progress * height;
            appendSegment(
                    consumer,
                    matrix,
                    previousX,
                    previousY,
                    previousZ,
                    nextX,
                    nextY,
                    nextZ,
                    width,
                    lifeFactor
            );
            previousX = nextX;
            previousZ = nextZ;
        }
    }

    private static void appendSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            float firstX,
            float firstY,
            float firstZ,
            float secondX,
            float secondY,
            float secondZ,
            float width,
            float alpha
    ) {
        quad(consumer, matrix, firstX - width, firstY, firstZ, secondX - width, secondY, secondZ, width, alpha);
        quad(consumer, matrix, firstX, firstY, firstZ - width, secondX, secondY, secondZ - width, width, alpha);
    }

    private static void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float firstX,
            float firstY,
            float firstZ,
            float secondX,
            float secondY,
            float secondZ,
            float width,
            float alpha
    ) {
        int alphaChannel = Math.max(20, Math.min(255, Math.round(alpha * 255.0F)));
        vertex(consumer, matrix, firstX, firstY, firstZ, alphaChannel);
        vertex(consumer, matrix, secondX, secondY, secondZ, alphaChannel);
        float flare = width * 1.45F;
        vertex(consumer, matrix, secondX + flare, secondY, secondZ + flare, alphaChannel);
        vertex(consumer, matrix, firstX + flare, firstY, firstZ + flare, alphaChannel);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(215, 225, 255, alpha)
                .endVertex();
    }

    private static float random(long seed) {
        seed ^= seed >>> 30;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 27;
        seed *= 0x94D049BB133111EBL;
        seed ^= seed >>> 31;
        return (seed >>> 40) / (float) (1L << 24);
    }
}
