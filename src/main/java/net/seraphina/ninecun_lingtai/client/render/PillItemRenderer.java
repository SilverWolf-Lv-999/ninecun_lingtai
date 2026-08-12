package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.BaseModel;
import net.seraphina.ninecun_lingtai.common.item.ColorItem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class PillItemRenderer extends BaseModel {
    private static final int LATITUDE_SEGMENTS = 12;
    private static final int LONGITUDE_SEGMENTS = 20;
    private static final float RADIUS = 0.18F;
    private static final float GUI_SCALE = 1.5F;
    private static final float GROUND_SCALE = 0.75F;
    private static final float FIXED_SCALE = 0.85F;
    private static final float HAND_SCALE = 0.7F;
    private static final RenderType PILL_RENDER_TYPE = RenderType.create(
            "pill_sphere",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            16 * 1024,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(
                            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader)
                    )
                    .createCompositeState(false)
    );

    public static final PillItemRenderer INSTANCE = new PillItemRenderer();

    @Override
    public void renderByItem(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!(itemStack.getItem() instanceof ColorItem colorItem)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        float scale = getScale(displayContext);
        poseStack.scale(scale, scale, scale);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(PILL_RENDER_TYPE);
        appendSphere(vertexConsumer, poseStack.last().pose(), colorItem.getColor());
        poseStack.popPose();
    }

    private static float getScale(ItemDisplayContext displayContext) {
        return switch (displayContext) {
            case GUI -> GUI_SCALE;
            case GROUND -> GROUND_SCALE;
            case FIXED -> FIXED_SCALE;
            case FIRST_PERSON_LEFT_HAND,
                 FIRST_PERSON_RIGHT_HAND,
                 THIRD_PERSON_LEFT_HAND,
                 THIRD_PERSON_RIGHT_HAND -> HAND_SCALE;
            default -> 1.0F;
        };
    }

    private static void appendSphere(VertexConsumer vertexConsumer, Matrix4f matrix, int color) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            float theta = (float) Math.PI * latitude / LATITUDE_SEGMENTS;
            float nextTheta = (float) Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                float phi = (float) (Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS);
                float nextPhi = (float) (Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS);
                SpherePoint topLeft = spherePoint(theta, phi);
                SpherePoint bottomLeft = spherePoint(nextTheta, phi);
                SpherePoint bottomRight = spherePoint(nextTheta, nextPhi);
                SpherePoint topRight = spherePoint(theta, nextPhi);
                appendTriangle(vertexConsumer, matrix, topLeft, bottomRight, bottomLeft, color);
                appendTriangle(vertexConsumer, matrix, topLeft, topRight, bottomRight, color);
            }
        }
    }

    private static SpherePoint spherePoint(float theta, float phi) {
        float sinTheta = Mth.sin(theta);
        return new SpherePoint(
                Mth.cos(phi) * sinTheta * RADIUS,
                Mth.cos(theta) * RADIUS,
                Mth.sin(phi) * sinTheta * RADIUS
        );
    }

    private static void appendTriangle(
            VertexConsumer vertexConsumer,
            Matrix4f matrix,
            SpherePoint first,
            SpherePoint second,
            SpherePoint third,
            int color
    ) {
        vertex(vertexConsumer, matrix, first, color);
        vertex(vertexConsumer, matrix, second, color);
        vertex(vertexConsumer, matrix, third, color);
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f matrix, SpherePoint point, int color) {
        int shadedColor = shadeColor(color, point);
        vertexConsumer.vertex(matrix, point.x(), point.y(), point.z())
                .color(
                        shadedColor >> 16 & 0xFF,
                        shadedColor >> 8 & 0xFF,
                        shadedColor & 0xFF,
                        255
                )
                .endVertex();
    }

    private static int shadeColor(int color, SpherePoint point) {
        float light = 0.62F + Math.max(0.0F, point.y() / RADIUS) * 0.3F
                + Math.max(0.0F, -point.z() / RADIUS) * 0.08F;
        return Math.round((color >> 16 & 0xFF) * light) << 16
                | Math.round((color >> 8 & 0xFF) * light) << 8
                | Math.round((color & 0xFF) * light);
    }

    private record SpherePoint(float x, float y, float z) {
    }
}
