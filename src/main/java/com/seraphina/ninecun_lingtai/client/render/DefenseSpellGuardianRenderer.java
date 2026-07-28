package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class DefenseSpellGuardianRenderer extends EntityRenderer<DefenseSpellGuardianEntity> {
    public DefenseSpellGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35F;
    }

    @Override
    public void render(
            DefenseSpellGuardianEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        DefenseSpellKind kind = entity.getSpellKind();
        int color = DefenseSpellUtility.getColor(kind, entity.getElementMask());
        float pulse = 0.96F + 0.04F * (float) Math.sin((entity.tickCount + partialTick) * 0.25D);
        float width = kind == DefenseSpellKind.EARTH_GUARDIAN ? 0.75F : 0.48F;
        float height = kind == DefenseSpellKind.FIRE_GUARDIAN ? 0.72F : 1.05F;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.52D, 0.0D);
        poseStack.scale(width * pulse, height * pulse, width * pulse);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        appendOctahedron(consumer, poseStack.last().pose(), color, 190);
        if (kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN) {
            appendOrbitingCrystals(consumer, poseStack.last().pose(), color, entity.tickCount + partialTick);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DefenseSpellGuardianEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static void appendOctahedron(VertexConsumer consumer, Matrix4f matrix, int color, int alpha) {
        Point top = new Point(0.0F, 1.0F, 0.0F);
        Point bottom = new Point(0.0F, -1.0F, 0.0F);
        Point east = new Point(1.0F, 0.0F, 0.0F);
        Point west = new Point(-1.0F, 0.0F, 0.0F);
        Point south = new Point(0.0F, 0.0F, 1.0F);
        Point north = new Point(0.0F, 0.0F, -1.0F);
        triangle(consumer, matrix, top, east, south, color, alpha);
        triangle(consumer, matrix, top, south, west, color, alpha);
        triangle(consumer, matrix, top, west, north, color, alpha);
        triangle(consumer, matrix, top, north, east, color, alpha);
        triangle(consumer, matrix, bottom, south, east, color, alpha);
        triangle(consumer, matrix, bottom, west, south, color, alpha);
        triangle(consumer, matrix, bottom, north, west, color, alpha);
        triangle(consumer, matrix, bottom, east, north, color, alpha);
    }

    private static void appendOrbitingCrystals(
            VertexConsumer consumer,
            Matrix4f matrix,
            int color,
            float time
    ) {
        for (int index = 0; index < 4; index++) {
            double angle = time * 0.08D + Math.PI * 0.5D * index;
            float x = (float) Math.cos(angle) * 1.55F;
            float z = (float) Math.sin(angle) * 1.55F;
            Point top = new Point(x, 0.38F, z);
            Point bottom = new Point(x, -0.38F, z);
            Point sideA = new Point(x + 0.18F, 0.0F, z);
            Point sideB = new Point(x, 0.0F, z + 0.18F);
            Point sideC = new Point(x - 0.18F, 0.0F, z);
            Point sideD = new Point(x, 0.0F, z - 0.18F);
            triangle(consumer, matrix, top, sideA, sideB, color, 220);
            triangle(consumer, matrix, top, sideB, sideC, color, 220);
            triangle(consumer, matrix, top, sideC, sideD, color, 220);
            triangle(consumer, matrix, top, sideD, sideA, color, 220);
            triangle(consumer, matrix, bottom, sideB, sideA, color, 180);
            triangle(consumer, matrix, bottom, sideC, sideB, color, 180);
            triangle(consumer, matrix, bottom, sideD, sideC, color, 180);
            triangle(consumer, matrix, bottom, sideA, sideD, color, 180);
        }
    }

    private static void triangle(
            VertexConsumer consumer,
            Matrix4f matrix,
            Point first,
            Point second,
            Point third,
            int color,
            int alpha
    ) {
        vertex(consumer, matrix, first, color, alpha);
        vertex(consumer, matrix, second, color, alpha);
        vertex(consumer, matrix, third, color, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Point point, int color, int alpha) {
        consumer.vertex(matrix, point.x, point.y, point.z)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha)
                .endVertex();
    }

    private record Point(float x, float y, float z) {
    }
}
