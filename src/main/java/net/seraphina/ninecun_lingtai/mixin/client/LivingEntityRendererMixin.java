package net.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Unique
    private LivingEntity ninecunLingtai$renderingEntity;

    @Inject(method = "render", at = @At("HEAD"))
    private void ninecunLingtai$captureRenderingEntity(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        this.ninecunLingtai$renderingEntity = entity;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ninecunLingtai$clearRenderingEntity(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        this.ninecunLingtai$renderingEntity = null;
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 7
    )
    private float ninecunLingtai$useSoulRoamingAlpha(float alpha) {
        if (this.ninecunLingtai$renderingEntity instanceof SoulEntity
                && ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            return 0.05F;
        }
        return alpha;
    }
}
