package net.seraphina.ninecun_lingtai.client.model.loader;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.seraphina.ninecun_lingtai.client.model.separate.SeparateTransformsCosmicModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SeparateTransformsCosmicModelLoader implements IGeometryLoader<SeparateTransformsCosmicModelLoader.CosmicGeometry> {
    public static final SeparateTransformsCosmicModelLoader INSTANCE = new SeparateTransformsCosmicModelLoader();

    private SeparateTransformsCosmicModelLoader() {
    }

    @Override
    public CosmicGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        BlockModel baseModel = deserializationContext.deserialize(GsonHelper.getAsJsonObject(jsonObject, "base"), BlockModel.class);

        JsonObject perspectiveData = GsonHelper.getAsJsonObject(jsonObject, "perspectives");

        Map<ItemDisplayContext, BlockModel> perspectives = new HashMap<>();
        for (ItemDisplayContext transform : ItemDisplayContext.values()) {
            if (perspectiveData.has(transform.getSerializedName())) {
                BlockModel perspectiveModel = deserializationContext.deserialize(GsonHelper.getAsJsonObject(perspectiveData, transform.getSerializedName()), BlockModel.class);
                perspectives.put(transform, perspectiveModel);
            }
        }
        boolean cosmic = GsonHelper.getAsBoolean(jsonObject, "isCosmic", true);

        return new CosmicGeometry(baseModel, ImmutableMap.copyOf(perspectives), cosmic);
    }

    public static class CosmicGeometry implements IUnbakedGeometry<SeparateTransformsCosmicModelLoader.CosmicGeometry> {
        private final BlockModel baseModel;
        private final ImmutableMap<ItemDisplayContext, BlockModel> perspectives;
        private final boolean isCosmic;
        public CosmicGeometry(BlockModel baseModel, ImmutableMap<ItemDisplayContext, BlockModel> perspectives, boolean isCosmic) {
            this.baseModel = baseModel;
            this.perspectives = perspectives;
            this.isCosmic = isCosmic;
        }

        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

            return new SeparateTransformsCosmicModel(
                    baseModel.bake(baker, baseModel, spriteGetter, modelState, modelLocation, true), ImmutableMap.copyOf(Maps.transformValues(perspectives, value -> {
                return value.bake(baker, value, spriteGetter, modelState, modelLocation, context.useBlockLight());
            }))
            , isCosmic);
        }

        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            baseModel.resolveParents(modelGetter);
            perspectives.values().forEach(model -> model.resolveParents(modelGetter));
        }
    }
}