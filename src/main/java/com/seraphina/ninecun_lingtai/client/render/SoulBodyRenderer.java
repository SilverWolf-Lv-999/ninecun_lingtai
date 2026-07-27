package com.seraphina.ninecun_lingtai.client.render;

import com.seraphina.ninecun_lingtai.common.entity.SoulBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class SoulBodyRenderer extends MobRenderer<SoulBodyEntity, PlayerModel<SoulBodyEntity>> {
    private static final float SHADOW_RADIUS = 0.5F;
    private static final UUID DEFAULT_SKIN_UUID = new UUID(0L, 0L);

    public SoulBodyRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                SHADOW_RADIUS
        );
    }

    @Override
    public ResourceLocation getTextureLocation(SoulBodyEntity entity) {
        Optional<UUID> ownerUuid = entity.getOwnerUuid();
        if (ownerUuid.isEmpty()) {
            return DefaultPlayerSkin.getDefaultSkin(DEFAULT_SKIN_UUID);
        }
        Player owner = Minecraft.getInstance().level == null
                ? null
                : Minecraft.getInstance().level.getPlayerByUUID(ownerUuid.get());
        if (owner instanceof AbstractClientPlayer clientPlayer) {
            return clientPlayer.getSkinTextureLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(ownerUuid.get());
    }
}
