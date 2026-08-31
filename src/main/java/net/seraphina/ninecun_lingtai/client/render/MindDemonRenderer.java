package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.seraphina.ninecun_lingtai.common.entity.MindDemonEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 心魔渲染（心魔即己 §6）：owner 皮肤的暗色剪影——半透明 RenderType +
 * 黑紫 tint（模型层整体乘色）。破盾蓄力时泛蓝白光提示前摇。
 */
public final class MindDemonRenderer extends MobRenderer<MindDemonEntity, PlayerModel<MindDemonEntity>> {
    private static final float SHADOW_RADIUS = 0.4F;

    public MindDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowedPlayerModel<>(context), SHADOW_RADIUS);
    }

    @Override
    public ResourceLocation getTextureLocation(MindDemonEntity entity) {
        return OwnerSkinCache.resolve(
                entity.getOwnerUuid().orElse(null),
                entity.getOwnerName()
        );
    }

    @Override
    protected RenderType getRenderType(
            MindDemonEntity entity,
            boolean bodyVisible,
            boolean translucent,
            boolean glowing
    ) {
        return RenderType.entityTranslucent(this.getTextureLocation(entity));
    }

    /** 黑紫剪影：整体乘 (0.35, 0.18, 0.5)，蓄力时抬亮为蓝白。 */
    private static final class ShadowedPlayerModel<T extends MindDemonEntity> extends PlayerModel<T> {
        private boolean charging;

        private ShadowedPlayerModel(EntityRendererProvider.Context context) {
            super(context.bakeLayer(ModelLayers.PLAYER), false);
        }

        @Override
        public void setupAnim(
                T entity,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.charging = entity.isCharging();
        }

        @Override
        public void renderToBuffer(
                PoseStack poseStack,
                VertexConsumer vertexConsumer,
                int packedLight,
                int packedOverlay,
                float red,
                float green,
                float blue,
                float alpha
        ) {
            if (this.charging) {
                super.renderToBuffer(
                        poseStack, vertexConsumer, packedLight, packedOverlay,
                        red * 0.55F, green * 0.65F, blue * 1.0F, alpha * 0.95F
                );
            } else {
                super.renderToBuffer(
                        poseStack, vertexConsumer, packedLight, packedOverlay,
                        red * 0.35F, green * 0.18F, blue * 0.50F, alpha * 0.88F
                );
            }
        }
    }
}
