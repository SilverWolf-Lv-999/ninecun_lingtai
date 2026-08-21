package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.client.render.cosmic.MegaRenderType;
import net.seraphina.ninecun_lingtai.common.config.ClientConfig;

import java.util.List;

public final class CosmicItemRenderUtility {
    public static void renderModelList(
            BakedModel model,
            ItemStack stack,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (!ClientConfig.ENABLE_COSMIC_RENDERER.get()) {
            return;
        }

        RandomSource randomSource = RandomSource.create();
        VertexConsumer cosmicConsumer = VertexMultiConsumer.create(
                bufferSource.getBuffer(MegaRenderType.END_COSMIC_ITEM(
                        new ResourceLocation(ClientConfig.WRAPPED_COSMIC_LOCATION.get())
                ))
        );
        randomSource.setSeed(42L);
        for (Direction direction : Direction.values()) {
            renderQuadList(
                    poseStack,
                    cosmicConsumer,
                    model.getQuads(null, direction, randomSource),
                    stack,
                    packedLight,
                    packedOverlay
            );
        }
        renderQuadList(
                poseStack,
                cosmicConsumer,
                model.getQuads(null, null, randomSource),
                stack,
                packedLight,
                packedOverlay
        );
    }

    private static void renderQuadList(
            PoseStack poseStack,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            ItemStack stack,
            int packedLight,
            int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 0.1F, packedLight, packedOverlay, true);
        }
    }
}
