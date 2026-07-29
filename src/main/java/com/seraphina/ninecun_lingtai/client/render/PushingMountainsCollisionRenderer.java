package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsCollisionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class PushingMountainsCollisionRenderer extends EntityRenderer<PushingMountainsCollisionEntity> {
    public PushingMountainsCollisionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            PushingMountainsCollisionEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
    }

    @Override
    public ResourceLocation getTextureLocation(PushingMountainsCollisionEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
