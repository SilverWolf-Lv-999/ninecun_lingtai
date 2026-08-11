package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffect;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AlchemyRecipeJsonUtility {

    public static AlchemyRecipeDefinition parse(ResourceLocation recipeId, JsonElement jsonElement) {
        JsonObject recipeJson = GsonHelper.convertToJsonObject(jsonElement, "alchemy recipe definition");
        String name = getRequiredNonBlankString(recipeJson, "name");
        String description = GsonHelper.getAsString(recipeJson, "description", "");
        String content = getRequiredNonBlankString(recipeJson, "content");
        AlchemyGrade grade = parseGrade(recipeJson);
        Set<CultivationRealm> allowedRealms = parseAllowedRealms(recipeJson);
        CultivationRealm minimumRefiningRealm = parseRealm(
                GsonHelper.getAsString(recipeJson, "minimum_refining_realm"),
                "minimum_refining_realm"
        );
        JsonObject ingredientsJson = GsonHelper.getAsJsonObject(recipeJson, "ingredients");
        AlchemyIngredient primaryIngredient = parseIngredient(
                GsonHelper.getAsJsonObject(ingredientsJson, "primary"),
                "ingredients.primary"
        );
        List<AlchemyIngredient> secondaryIngredients = parseIngredientList(
                GsonHelper.getAsJsonArray(ingredientsJson, "secondary"),
                "ingredients.secondary"
        );
        List<AlchemyIngredient> auxiliaryIngredients = parseIngredientList(
                GsonHelper.getAsJsonArray(ingredientsJson, "auxiliary"),
                "ingredients.auxiliary"
        );
        ResourceLocation outputItem = parseResourceLocation(
                GsonHelper.getAsString(recipeJson, "output_item"),
                "output_item"
        );
        int outputCount = GsonHelper.getAsInt(recipeJson, "output_count", 1);
        int brewingDuration = GsonHelper.getAsInt(recipeJson, "brewing_duration");
        AlchemyPillEffect pillEffect = parsePillEffect(recipeJson);
        try {
            return new AlchemyRecipeDefinition(
                    recipeId,
                    name,
                    description,
                    content,
                    grade,
                    allowedRealms,
                    minimumRefiningRealm,
                    primaryIngredient,
                    secondaryIngredients,
                    auxiliaryIngredients,
                    outputItem,
                    outputCount,
                    brewingDuration,
                    pillEffect
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static AlchemyGrade parseGrade(JsonObject recipeJson) {
        JsonObject gradeJson = GsonHelper.getAsJsonObject(recipeJson, "grade");
        try {
            return new AlchemyGrade(
                    GsonHelper.getAsInt(gradeJson, "tier"),
                    GsonHelper.getAsInt(gradeJson, "rank")
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static Set<CultivationRealm> parseAllowedRealms(JsonObject recipeJson) {
        JsonArray realmsJson = GsonHelper.getAsJsonArray(recipeJson, "allowed_realms");
        EnumSet<CultivationRealm> realms = EnumSet.noneOf(CultivationRealm.class);
        for (JsonElement realmJson : realmsJson) {
            String realmName = GsonHelper.convertToString(realmJson, "allowed realm");
            CultivationRealm realm = CultivationRealm.fromSerializedName(realmName)
                    .orElseThrow(() -> new JsonParseException("Unknown cultivation realm: " + realmName));
            realms.add(realm);
        }
        if (realms.isEmpty()) {
            throw new JsonParseException("allowed_realms must not be empty");
        }
        return Set.copyOf(realms);
    }

    private static CultivationRealm parseRealm(String serializedName, String fieldName) {
        return CultivationRealm.fromSerializedName(serializedName)
                .orElseThrow(() -> new JsonParseException(
                        "Unknown cultivation realm for " + fieldName + ": " + serializedName
                ));
    }

    private static AlchemyPillEffect parsePillEffect(JsonObject recipeJson) {
        JsonObject effectJson = GsonHelper.getAsJsonObject(recipeJson, "pill_effect");
        try {
            return new AlchemyPillEffect(
                    GsonHelper.getAsDouble(effectJson, "max_health_fraction"),
                    effectJson.has("flat_healing")
                            ? GsonHelper.getAsDouble(effectJson, "flat_healing")
                            : GsonHelper.getAsDouble(effectJson, "minimum_healing"),
                    GsonHelper.getAsInt(effectJson, "regeneration_duration", 0),
                    GsonHelper.getAsInt(effectJson, "regeneration_amplifier", 0)
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static AlchemyIngredient parseIngredient(JsonObject ingredientJson, String fieldName) {
        try {
            return new AlchemyIngredient(
                    parseResourceLocation(GsonHelper.getAsString(ingredientJson, "item"), fieldName + ".item"),
                    GsonHelper.getAsInt(ingredientJson, "count")
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static List<AlchemyIngredient> parseIngredientList(JsonArray ingredientJson, String fieldName) {
        if (ingredientJson.size() != 2) {
            throw new JsonParseException(fieldName + " must define exactly two ingredients");
        }
        return ingredientJson.asList().stream()
                .map(json -> parseIngredient(
                        GsonHelper.convertToJsonObject(json, fieldName + " ingredient"),
                        fieldName
                ))
                .toList();
    }

    private static ResourceLocation parseResourceLocation(String value, String fieldName) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(value);
        if (resourceLocation == null) {
            throw new JsonParseException("Invalid resource id for " + fieldName + ": " + value);
        }
        return resourceLocation;
    }

    private static String getRequiredNonBlankString(JsonObject jsonObject, String fieldName) {
        String value = GsonHelper.getAsString(jsonObject, fieldName);
        if (value.isBlank()) {
            throw new JsonParseException(fieldName + " must not be blank");
        }
        return value;
    }
}
