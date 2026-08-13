package net.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffectData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatBand;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AlchemyRecipeJsonUtility {
    private static final double DEFAULT_TOXIN_PER_TIER = 5.0D;

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
        ElementType primaryElement = parsePrimaryElement(recipeJson);
        double baseSuccessRate = recipeJson.has("base_success_rate")
                ? GsonHelper.getAsDouble(recipeJson, "base_success_rate")
                : getDefaultBaseSuccessRate(grade.tier());
        List<HeatStage> heatStages = parseHeatStages(recipeJson);
        List<AlchemyFeedStep> feedPlan = parseFeedPlan(recipeJson);
        double pillToxin = recipeJson.has("pill_toxin")
                ? GsonHelper.getAsDouble(recipeJson, "pill_toxin")
                : grade.tier() * DEFAULT_TOXIN_PER_TIER;
        double failureExplosionRadius = 1.0D + 0.5D * grade.tier();
        int failureDregCount = AlchemyRecipeDefinition.DEFAULT_DREG_COUNT;
        if (recipeJson.has("failure")) {
            JsonObject failureJson = GsonHelper.getAsJsonObject(recipeJson, "failure");
            failureExplosionRadius = GsonHelper.getAsDouble(
                    failureJson,
                    "explosion_radius",
                    failureExplosionRadius
            );
            failureDregCount = GsonHelper.getAsInt(failureJson, "dreg_count", failureDregCount);
        }
        List<AlchemyPillEffectData> effects = parseEffects(recipeJson);
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
                    primaryElement,
                    baseSuccessRate,
                    heatStages,
                    feedPlan,
                    pillToxin,
                    failureExplosionRadius,
                    failureDregCount,
                    effects
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    public static double getDefaultBaseSuccessRate(int tier) {
        return 0.95D - 0.05D * (tier - 1);
    }

    @Nullable
    private static ElementType parsePrimaryElement(JsonObject recipeJson) {
        if (!recipeJson.has("element_profile")) {
            return null;
        }
        JsonObject elementJson = GsonHelper.getAsJsonObject(recipeJson, "element_profile");
        String elementName = GsonHelper.getAsString(elementJson, "primary_element");
        return ElementType.fromSerializedName(elementName)
                .orElseThrow(() -> new JsonParseException("Unknown primary element: " + elementName));
    }

    private static List<HeatStage> parseHeatStages(JsonObject recipeJson) {
        if (!recipeJson.has("heat_stages")) {
            return List.of();
        }
        JsonArray stagesJson = GsonHelper.getAsJsonArray(recipeJson, "heat_stages");
        List<HeatStage> stages = new ArrayList<>(stagesJson.size());
        for (JsonElement stageElement : stagesJson) {
            JsonObject stageJson = GsonHelper.convertToJsonObject(stageElement, "heat stage");
            String bandName = GsonHelper.getAsString(stageJson, "band");
            HeatBand band = HeatBand.fromSerializedName(bandName)
                    .orElseThrow(() -> new JsonParseException("Unknown heat band: " + bandName));
            double portion = GsonHelper.getAsDouble(stageJson, "portion");
            try {
                stages.add(new HeatStage(band, portion));
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(exception.getMessage(), exception);
            }
        }
        return List.copyOf(stages);
    }

    private static List<AlchemyFeedStep> parseFeedPlan(JsonObject recipeJson) {
        if (!recipeJson.has("feed_plan")) {
            return List.of();
        }
        JsonArray planJson = GsonHelper.getAsJsonArray(recipeJson, "feed_plan");
        List<AlchemyFeedStep> feedPlan = new ArrayList<>(planJson.size());
        for (JsonElement stepElement : planJson) {
            JsonObject stepJson = GsonHelper.convertToJsonObject(stepElement, "feed plan step");
            String slotName = GsonHelper.getAsString(stepJson, "slot");
            AlchemyIngredientSlot slot;
            try {
                slot = AlchemyIngredientSlot.fromSerializedName(slotName);
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(exception.getMessage(), exception);
            }
            int stageNumber = GsonHelper.getAsInt(stepJson, "stage");
            try {
                feedPlan.add(new AlchemyFeedStep(
                        slot,
                        stageNumber - 1,
                        GsonHelper.getAsDouble(stepJson, "window_start"),
                        GsonHelper.getAsDouble(stepJson, "ideal"),
                        GsonHelper.getAsDouble(stepJson, "window_end")
                ));
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(exception.getMessage(), exception);
            }
        }
        return List.copyOf(feedPlan);
    }

    private static List<AlchemyPillEffectData> parseEffects(JsonObject recipeJson) {
        if (recipeJson.has("effects")) {
            JsonArray effectsJson = GsonHelper.getAsJsonArray(recipeJson, "effects");
            List<AlchemyPillEffectData> effects = new ArrayList<>(effectsJson.size());
            for (JsonElement effectElement : effectsJson) {
                effects.add(parseEffect(GsonHelper.convertToJsonObject(effectElement, "pill effect")));
            }
            return List.copyOf(effects);
        }
        if (recipeJson.has("pill_effect")) {
            JsonObject effectJson = GsonHelper.getAsJsonObject(recipeJson, "pill_effect");
            Map<String, Double> numbers = new HashMap<>();
            numbers.put("max_health_fraction", GsonHelper.getAsDouble(effectJson, "max_health_fraction"));
            numbers.put(
                    "flat_healing",
                    effectJson.has("flat_healing")
                            ? GsonHelper.getAsDouble(effectJson, "flat_healing")
                            : GsonHelper.getAsDouble(effectJson, "minimum_healing")
            );
            numbers.put(
                    "regeneration_duration",
                    (double) GsonHelper.getAsInt(effectJson, "regeneration_duration", 0)
            );
            numbers.put(
                    "regeneration_amplifier",
                    (double) GsonHelper.getAsInt(effectJson, "regeneration_amplifier", 0)
            );
            return List.of(new AlchemyPillEffectData("heal", numbers));
        }
        return List.of();
    }

    private static AlchemyPillEffectData parseEffect(JsonObject effectJson) {
        String type = getRequiredNonBlankString(effectJson, "type");
        Map<String, Double> numbers = new HashMap<>();
        for (String key : effectJson.keySet()) {
            if (key.equals("type")) {
                continue;
            }
            JsonElement value = effectJson.get(key);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                numbers.put(key, value.getAsDouble());
            }
        }
        try {
            return new AlchemyPillEffectData(type, numbers);
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
