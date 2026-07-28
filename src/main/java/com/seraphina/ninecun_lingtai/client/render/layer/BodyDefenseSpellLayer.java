package com.seraphina.ninecun_lingtai.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seraphina.ninecun_lingtai.client.model.BodyDefenseSpellModel;
import com.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import com.seraphina.ninecun_lingtai.client.utility.DefenseSpellRenderUtility;
import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import java.util.Optional;

public final class BodyDefenseSpellLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float BASE_ALPHA = 0.22F;
    private static final float FLOW_ALPHA = 0.12F;
    private final BodyDefenseSpellModel spellModel;

    public BodyDefenseSpellLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
            BodyDefenseSpellModel spellModel
    ) {
        super(renderer);
        this.spellModel = spellModel;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        Optional<DefenseSpellSnapshot> snapshotOptional = ClientDefenseSpellManager.getBodyShield(player.getId());
        if (snapshotOptional.isEmpty() || player.isInvisible()) {
            return;
        }
        DefenseSpellSnapshot snapshot = snapshotOptional.get();
        if (!DefenseSpellRenderUtility.usesPlayerModelLayer(snapshot.kind())) {
            return;
        }

        this.spellModel.copyFrom(getParentModel());
        this.spellModel.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
        this.spellModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int color = DefenseSpellUtility.getColor(snapshot.kind(), snapshot.elementMask());
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float integrity = Mth.clamp((float) snapshot.ratio(), 0.0F, 1.0F);
        float pulse = 0.5F + 0.5F * Mth.sin((player.tickCount + partialTick) * 0.22F);
        float alpha = (BASE_ALPHA + pulse * 0.08F) * (0.45F + integrity * 0.55F);

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(player.getSkinTextureLocation())
        );
        this.spellModel.renderToBuffer(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                red,
                green,
                blue,
                alpha
        );

        float flowTime = player.tickCount + partialTick;
        VertexConsumer flowConsumer = bufferSource.getBuffer(RenderType.energySwirl(
                player.getSkinTextureLocation(),
                flowTime * 0.006F % 1.0F,
                flowTime * 0.012F % 1.0F
        ));
        this.spellModel.renderToBuffer(
                poseStack,
                flowConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                red,
                green,
                blue,
                FLOW_ALPHA * (0.5F + integrity * 0.5F)
        );
    }
}
