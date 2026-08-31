package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.common.entity.MortalBodyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 劫场肉身替身渲染（心魔即己 §4.3）：owner 皮肤的盘坐玩家模型。
 * 皮肤经 {@link OwnerSkinCache} 异步解析（R5：首帧可能为占位皮肤）。
 */
public final class MortalBodyRenderer
        extends LivingEntityRenderer<MortalBodyEntity, PlayerModel<MortalBodyEntity>> {
    private static final float SHADOW_RADIUS = 0.4F;
    private static final float SITTING_OFFSET = -0.55F;

    public MortalBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new MeditatingPlayerModel<>(context), SHADOW_RADIUS);
    }

    @Override
    public void render(
            MortalBodyEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, SITTING_OFFSET, 0.0F);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MortalBodyEntity entity) {
        return OwnerSkinCache.resolve(
                entity.getOwnerUuid().orElse(null),
                entity.getOwnerName()
        );
    }

    @Override
    protected boolean shouldShowName(MortalBodyEntity entity) {
        return false;
    }

    /** 盘坐姿态（参考打坐渲染现状的手工摆位）。 */
    private static final class MeditatingPlayerModel<T extends MortalBodyEntity> extends PlayerModel<T> {
        private MeditatingPlayerModel(EntityRendererProvider.Context context) {
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
            super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
            // 双腿盘起。
            this.rightLeg.xRot = -1.35F;
            this.rightLeg.yRot = 0.55F;
            this.rightLeg.zRot = -0.12F;
            this.leftLeg.xRot = -1.35F;
            this.leftLeg.yRot = -0.55F;
            this.leftLeg.zRot = 0.12F;
            // 双手结印于身前。
            this.rightArm.xRot = -0.85F;
            this.rightArm.yRot = -0.35F;
            this.leftArm.xRot = -0.85F;
            this.leftArm.yRot = 0.35F;
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftPants.copyFrom(this.leftLeg);
        }
    }
}
