package net.seraphina.ninecun_lingtai.client.render.cosmic;

import net.seraphina.ninecun_lingtai.client.render.cosmic.MegaRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

public class ModelCosmicRenderer {
    public Model model;
    public ResourceLocation texture;

    public ModelCosmicRenderer(Model model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }

    public static Material material(ResourceLocation resourceLocation) {
        return new Material(InventoryMenu.BLOCK_ATLAS, resourceLocation);
    }

    public void render(MultiBufferSource bufferSource, PoseStack pStack, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, boolean renderBase, boolean glow) {
        CosmicItemShaders.updateShaderData(ItemDisplayContext.GROUND);
        pStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(MegaRenderType.mask(texture));
        if (renderBase)
            model.renderToBuffer(pStack, bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture)), pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        if (glow)
            model.renderToBuffer(pStack, bufferSource.getBuffer(MegaRenderType.glow(texture)), pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        model.renderToBuffer(pStack, consumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        pStack.popPose();
    }
}
