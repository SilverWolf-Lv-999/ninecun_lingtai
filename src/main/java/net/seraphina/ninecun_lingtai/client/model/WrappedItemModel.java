package net.seraphina.ninecun_lingtai.client.model;

import com.google.common.collect.ImmutableMap;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.utility.CosmicItemRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.TransformUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class WrappedItemModel implements PerspectiveModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    protected BakedModel wrapped;
    protected ModelState parentState;
    @Nullable
    protected LivingEntity entity;
    @Nullable
    protected ClientLevel world;
    protected ItemOverrides overrideList = new ItemOverrides() {
        public BakedModel resolve(@NotNull BakedModel originalModel, @NotNull ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
            WrappedItemModel.this.entity = entity;
            WrappedItemModel.this.world = world == null ? (entity == null ? null : (ClientLevel) entity.level()) : world;
            return WrappedItemModel.this.isCosmic() ? WrappedItemModel.this.wrapped.getOverrides().resolve(originalModel, stack, world, entity, seed) : originalModel;
        }
    };

    public WrappedItemModel(BakedModel wrapped) {
        this.wrapped = wrapped;
        this.parentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
    }

    public static List<BakedQuad> bakeItem(List<TextureAtlasSprite> sprites) {
        LinkedList<BakedQuad> quads = new LinkedList<>();

        for (TextureAtlasSprite sprite : sprites) {
            List<BlockElement> unbaked = ITEM_MODEL_GENERATOR.processFrames(sprites.indexOf(sprite), "layer" + sprites.indexOf(sprite), sprite.contents());

            for (BlockElement element : unbaked) {

                for (Map.Entry<Direction, BlockElementFace> directionBlockElementFaceEntry : element.faces.entrySet()) {
                    quads.add(FACE_BAKERY.bakeQuad(element.from, element.to, directionBlockElementFaceEntry.getValue(), sprite, directionBlockElementFaceEntry.getKey(), new PerspectiveModelState(ImmutableMap.of()), element.rotation, element.shade, ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "dynamic")));
                }
            }
        }

        return quads;
    }

    public static <E> void checkArgument(E argument, Predicate<E> predicate) {
        if (predicate.test(argument)) {
            throw new RuntimeException("");
        }
    }

    public static <T> boolean isNullOrContainsNull(T[] input) {
        if (input != null) {
            int var2 = input.length;

            for (T t : input) {
                if (t == null) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
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

    protected void renderWrapped(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous) {
        this.renderWrapped(stack, pStack, buffers, packedLight, packedOverlay, fabulous, Function.identity());
    }

    protected void renderWrapped(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous, Function<VertexConsumer, VertexConsumer> consOverride) {
        BakedModel model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {

            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(bakedModel, stack, packedLight, packedOverlay, pStack, consOverride.apply(buffers.getBuffer(rendertype)));
            }
        }

    }

    protected void renderWrappedCosmic(ItemStack stack, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, boolean fabulous, Function<VertexConsumer, VertexConsumer> consOverride) {
        BakedModel model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {

            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                CosmicItemRenderUtility.renderModelList(bakedModel, stack, packedLight, packedOverlay, pStack, buffers);
            }
        }

    }
}
