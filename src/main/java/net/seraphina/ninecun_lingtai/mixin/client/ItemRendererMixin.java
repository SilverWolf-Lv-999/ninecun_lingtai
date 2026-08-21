package net.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.seraphina.ninecun_lingtai.client.utility.LuminousItemRenderUtility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.seraphina.ninecun_lingtai.client.model.IItemRenderer;
import net.seraphina.ninecun_lingtai.client.model.separate.SeparateTransformsCosmicModel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin implements ResourceManagerReloadListener {
    @Shadow
    @Final
    private ItemColors itemColors;

    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

    @Shadow
    public static VertexConsumer getFoilBufferDirect(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean noEntityGlint,
            boolean hasGlint
    ) {
        return null;
    }

    @Shadow
    public abstract void renderModelLists(
            BakedModel model,
            ItemStack stack,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            VertexConsumer consumer
    );

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$renderCustomModel(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo callbackInfo
    ) {
        if (model instanceof IItemRenderer itemRenderer) {
            callbackInfo.cancel();
            poseStack.pushPose();
            IItemRenderer transformedRenderer = (IItemRenderer) ForgeHooksClient.handleCameraTransforms(
                    poseStack,
                    itemRenderer,
                    displayContext,
                    leftHand
            );
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            transformedRenderer.renderItem(
                    stack,
                    displayContext,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
            poseStack.popPose();
            return;
        }

        if (model instanceof SeparateTransformsCosmicModel separateTransformsModel) {
            callbackInfo.cancel();
            poseStack.pushPose();
            BakedModel transformedModel = ForgeHooksClient.handleCameraTransforms(
                    poseStack,
                    separateTransformsModel,
                    displayContext,
                    leftHand
            );
            if (transformedModel instanceof SeparateTransformsCosmicModel transformedCosmicModel) {
                poseStack.translate(-0.5D, -0.5D, -0.5D);
                transformedCosmicModel.renderItem(
                        stack,
                        displayContext,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
                poseStack.popPose();
            } else if (separateTransformsModel.perspectives.get(displayContext) != null) {
                poseStack.popPose();
                ninecunLingtai$renderFallback(
                        stack,
                        displayContext,
                        leftHand,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        separateTransformsModel.perspectives.get(displayContext)
                );
            } else {
                poseStack.popPose();
            }
        }
    }

    @Unique
    private void ninecunLingtai$renderFallback(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BakedModel model
    ) {
        poseStack.pushPose();
        BakedModel transformedModel = ForgeHooksClient.handleCameraTransforms(
                poseStack,
                model,
                displayContext,
                leftHand
        );
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        for (BakedModel renderPass : transformedModel.getRenderPasses(stack, true)) {
            for (RenderType renderType : renderPass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = getFoilBufferDirect(
                        bufferSource,
                        renderType,
                        true,
                        stack.hasFoil()
                );
                this.renderModelLists(
                        renderPass,
                        stack,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        consumer
                );
            }
        }
        poseStack.popPose();
    }

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
