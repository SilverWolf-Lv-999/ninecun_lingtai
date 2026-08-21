package net.seraphina.ninecun_lingtai.client.model;


import net.seraphina.ninecun_lingtai.client.render.cosmic.CosmicItemShaders;
import net.seraphina.ninecun_lingtai.common.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CosmicBakedModel extends WrappedItemModel implements IItemRenderer {
    private final List<ResourceLocation> maskSprite;

    public CosmicBakedModel(BakedModel wrapped, List<ResourceLocation> maskSprite) {
        super(wrapped);
        this.maskSprite = maskSprite;
    }

    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource source, int light, int overlay) {
        this.renderWrapped(stack, pStack, source, light, overlay, true, Function.identity());
        if (ClientConfig.ENABLE_COSMIC_RENDERER.get()) {
            if (source instanceof MultiBufferSource.BufferSource bs)
                bs.endBatch();
            this.renderWrappedCosmic(stack, pStack, source, light, overlay, true, Function.identity());
            Minecraft mc = Minecraft.getInstance();
            CosmicItemShaders.updateShaderData(transformType);
            VertexConsumer cons = source.getBuffer(CosmicItemShaders.COSMIC_RENDER_TYPE);
            List<TextureAtlasSprite> atlasSprite = new ArrayList<>();

            for (ResourceLocation res : this.maskSprite) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res);

                atlasSprite.add(sprite);
            }
            mc.getItemRenderer().renderQuadList(pStack, cons, bakeItem(atlasSprite), stack, light, overlay);
            if (source instanceof MultiBufferSource.BufferSource bs)
                bs.endBatch();
        }
    }

    public @Nullable PerspectiveModelState getModelState() {
        return (PerspectiveModelState) this.parentState;
    }

    public boolean isCosmic() {
        return true;
    }
}
