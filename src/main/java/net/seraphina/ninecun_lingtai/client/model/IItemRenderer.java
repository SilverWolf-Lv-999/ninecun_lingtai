package net.seraphina.ninecun_lingtai.client.model;

import net.seraphina.ninecun_lingtai.client.utility.TextureUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public interface IItemRenderer extends PerspectiveModel {
    void renderItem(ItemStack var1, ItemDisplayContext var2, PoseStack var3, MultiBufferSource var4, int var5, int var6);

    default @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand) {
        return Collections.emptyList();
    }

    default boolean isCustomRenderer() {
        return true;
    }

    default @NotNull TextureAtlasSprite getParticleIcon() {
        return TextureUtils.getMissingSprite();
    }

    default @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
