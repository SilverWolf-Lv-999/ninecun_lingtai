package net.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.client.utility.LuminousItemRenderUtility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
                    + "ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int ninecunLingtai$renderLuminousItemsAtFullBrightness(
            int packedLight,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
               MultiBufferSource bufferSource,
            int originalPackedLight,
            int packedOverlay,
            BakedModel bakedModel
    ) {
        return LuminousItemRenderUtility.resolvePackedLight(itemStack, packedLight);
    }
}
