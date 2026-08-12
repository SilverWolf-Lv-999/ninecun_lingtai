package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class PushingMountainsEntityRenderer extends EntityRenderer<PushingMountainsEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;

    public PushingMountainsEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            PushingMountainsEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        PushingMountainsRenderMeshManager.render(
                entity,
                this.blockRenderDispatcher,
                poseStack,
                packedLight
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PushingMountainsEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
