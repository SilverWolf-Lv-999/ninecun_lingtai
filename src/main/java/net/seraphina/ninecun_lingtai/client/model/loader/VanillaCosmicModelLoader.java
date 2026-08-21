package net.seraphina.ninecun_lingtai.client.model.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.seraphina.ninecun_lingtai.client.model.VanillaCosmicBakedModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VanillaCosmicModelLoader implements IGeometryLoader<VanillaCosmicModelLoader.CosmicGeometry> {
    public static final VanillaCosmicModelLoader INSTANCE = new VanillaCosmicModelLoader();

    public VanillaCosmicModelLoader() {
    }

    public VanillaCosmicModelLoader.CosmicGeometry read(JsonObject modelContents, JsonDeserializationContext deserializationContext) throws JsonParseException {
        JsonObject cosmicObj = modelContents.getAsJsonObject("cosmic");
        if (cosmicObj == null) {
            throw new IllegalStateException("Missing 'cosmic' object.");
        } else {
            List<String> maskTexture = new ArrayList();
            if (cosmicObj.has("mask") && cosmicObj.get("mask").isJsonArray()) {
                JsonArray masks = cosmicObj.getAsJsonArray("mask");

                for (int i = 0; i < masks.size(); ++i) {
                    maskTexture.add(masks.get(i).getAsString());
                }
            } else {
                maskTexture.add(GsonHelper.getAsString(cosmicObj, "mask"));
            }

            JsonObject clean = modelContents.deepCopy();
            clean.remove("cosmic");
            clean.remove("loader");
            BlockModel baseModel = deserializationContext.deserialize(clean, BlockModel.class);
            return new VanillaCosmicModelLoader.CosmicGeometry(baseModel, maskTexture);
        }
    }

    public static class CosmicGeometry implements IUnbakedGeometry<VanillaCosmicModelLoader.CosmicGeometry> {
        private final BlockModel baseModel;
        private final List<String> maskTextures;

        public CosmicGeometry(BlockModel baseModel, List<String> maskTextures) {
            this.baseModel = baseModel;
            this.maskTextures = maskTextures;
        }

        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            BakedModel baseBakedModel = this.baseModel.bake(baker, this.baseModel, spriteGetter, modelState, modelLocation, true);
            List<ResourceLocation> textures = new ArrayList();
            this.maskTextures.forEach((mask) -> {
                textures.add(ResourceLocation.parse(mask));
            });
            return new VanillaCosmicBakedModel(baseBakedModel, textures);
        }

        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            this.baseModel.resolveParents(modelGetter);
        }
    }
}
