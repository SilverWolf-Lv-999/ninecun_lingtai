package net.seraphina.ninecun_lingtai.client.model.separate;

import com.google.common.collect.ImmutableMap;
import net.seraphina.ninecun_lingtai.client.model.PerspectiveModel;
import net.seraphina.ninecun_lingtai.client.utility.CosmicItemRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.TransformUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class STWrappedModel implements PerspectiveModel {
    public final ImmutableMap<ItemDisplayContext, BakedModel> perspectives;
    public BakedModel wrapped;
    public ModelState mainParentState;
    @Nullable
    public LivingEntity entity;
    @Nullable
    public ClientLevel world;
    public ItemOverrides overrideList = new ItemOverrides() {
        public BakedModel resolve(@NotNull BakedModel originalModel, @NotNull ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
            STWrappedModel.this.entity = entity;
            STWrappedModel.this.world = world == null ? (entity == null ? null : (ClientLevel) entity.level()) : world;
            return STWrappedModel.this.isCosmic() ? STWrappedModel.this.wrapped.getOverrides().resolve(originalModel, stack, world, entity, seed) : originalModel;
        }
    };
    public BakedModel model;

    public STWrappedModel(BakedModel wrapped, ImmutableMap<ItemDisplayContext, BakedModel> perspectives) {
        this.wrapped = wrapped;
        this.mainParentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
        this.perspectives = perspectives;
    }

    public boolean isCosmic() {
        return false;
    }

    public @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand) {
        return Collections.emptyList();
    }

    public @NotNull TextureAtlasSprite getParticleIcon() {
        return this.wrapped.getParticleIcon();
    }

    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return this.wrapped.getParticleIcon(data);
    }

    public @NotNull ItemOverrides getOverrides() {
        return this.overrideList;
    }

    public boolean useAmbientOcclusion() {
        return this.wrapped.useAmbientOcclusion();
    }

    public boolean isGui3d() {
        return this.wrapped.isGui3d();
    }

    public boolean usesBlockLight() {
        return this.wrapped.usesBlockLight();
    }

    public void renderWrapped(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous) {
        this.renderWrapped(stack, pStack, buffers, packedLight, packedOverlay, fabulous, Function.identity());
    }

    public void renderWrapped(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous, Function<VertexConsumer, VertexConsumer> consOverride) {
        if (model == null)
            model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {
            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(bakedModel, stack, packedLight, packedOverlay, pStack, consOverride.apply(buffers.getBuffer(rendertype)));
            }
        }

    }

    protected void renderWrappedCosmic(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous, Function<VertexConsumer, VertexConsumer> consOverride) {
        if (model == null)
            model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {

            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                CosmicItemRenderUtility.renderModelList(bakedModel, stack, packedLight, packedOverlay, pStack, buffers);
            }
        }

    }
}
