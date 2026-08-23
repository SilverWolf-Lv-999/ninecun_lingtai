package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.client.utility.SwordQiRenderUtility;
import net.seraphina.ninecun_lingtai.common.entity.ability.SwordQiEntity;

public final class SwordQiEntityRenderer extends EntityRenderer<SwordQiEntity> {
    public SwordQiEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            SwordQiEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        SwordQiRenderUtility.render(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordQiEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
