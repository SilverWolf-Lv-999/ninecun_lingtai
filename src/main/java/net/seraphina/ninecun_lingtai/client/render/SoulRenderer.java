package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class SoulRenderer extends MobRenderer<SoulEntity, PlayerModel<SoulEntity>> {
    private static final float SHADOW_RADIUS = 0.0F;
    private static final UUID DEFAULT_SKIN_UUID = new UUID(0L, 0L);

    public SoulRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                SHADOW_RADIUS
        );
    }

    @Override
    public void render(
            SoulEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !entity.isOwnedBy(minecraft.player)) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected RenderType getRenderType(
            SoulEntity entity,
            boolean bodyVisible,
            boolean translucent,
            boolean glowing
    ) {
        return RenderType.entityTranslucent(this.getTextureLocation(entity));
    }

    @Override
    public ResourceLocation getTextureLocation(SoulEntity entity) {
        if (entity.getOwnerUuid().isEmpty()) {
            return DefaultPlayerSkin.getDefaultSkin(DEFAULT_SKIN_UUID);
        }
        AbstractClientPlayer owner = Minecraft.getInstance().level == null
                ? null
                : Minecraft.getInstance().level.getPlayerByUUID(entity.getOwnerUuid().get()) instanceof AbstractClientPlayer player
                ? player
                : null;
        return owner == null
                ? DefaultPlayerSkin.getDefaultSkin(entity.getOwnerUuid().get())
                : owner.getSkinTextureLocation();
    }

    @Override
    protected float getFlipDegrees(SoulEntity entity) {
        return 0.0F;
    }
}
