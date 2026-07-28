package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueBreathing;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueComprehension;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueEffect;
import com.seraphina.ninecun_lingtai.api.spell.SpellCategory;
import com.seraphina.ninecun_lingtai.api.spell.SpellGrade;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryThreshold;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueRequirements;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TechniqueJsonUtility {
    public static final ResourceLocation DEFAULT_CULTIVATION_PATH = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "spiritual_cultivation"
    );
    public static final ResourceLocation DEFAULT_INITIAL_MASTERY = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "initial_understanding"
    );
    public static final ResourceLocation DEFAULT_MINOR_MASTERY = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "minor_mastery"
    );

    private TechniqueJsonUtility() {
    }

    public static TechniqueDefinition parse(ResourceLocation techniqueId, JsonElement jsonElement) {
        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "technique definition");
        String name = getRequiredNonBlankString(jsonObject, "name");
        String description = GsonHelper.getAsString(jsonObject, "description", "");
        String content = getRequiredNonBlankString(jsonObject, "content");
        ResourceLocation cultivationPath = jsonObject.has("cultivation_path")
                ? parseResourceLocation(GsonHelper.getAsString(jsonObject, "cultivation_path"), "cultivation_path")
                : DEFAULT_CULTIVATION_PATH;
        Set<ElementType> compatibleElements = parseElements(jsonObject, "compatible_elements");
        TechniqueRequirements requirements = parseRequirements(jsonObject);
        TechniqueComprehension comprehension = parseComprehension(jsonObject);
        List<TechniqueEffect> effects = parseEffects(jsonObject);
        Optional<TechniqueBreathing> breathing = parseBreathing(jsonObject);
        Optional<TechniqueSpell> spell = parseSpell(jsonObject, compatibleElements);
        try {
            return new TechniqueDefinition(
                    techniqueId,
                    name,
                    description,
                    content,
                    cultivationPath,
                    compatibleElements,
                    requirements,
                    comprehension,
                    effects,
                    breathing,
                    spell
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static Optional<TechniqueSpell> parseSpell(
            JsonObject techniqueJson,
            Set<ElementType> compatibleElements
    ) {
        if (!techniqueJson.has("spell")) {
            return Optional.empty();
        }
        JsonObject spellJson = GsonHelper.getAsJsonObject(techniqueJson, "spell");
        ResourceLocation spellId = parseResourceLocation(
                GsonHelper.getAsString(spellJson, "id"),
                "spell id"
        );
        String categoryName = GsonHelper.getAsString(
                spellJson,
                "category",
                SpellCategory.UTILITY.getSerializedName()
        );
        SpellCategory category = SpellCategory.fromSerializedName(categoryName)
                .orElseThrow(() -> new JsonParseException("Unknown spell category: " + categoryName));
        int gradeRank = GsonHelper.getAsInt(spellJson, "grade");
        SpellGrade grade = SpellGrade.fromRank(gradeRank)
                .orElseThrow(() -> new JsonParseException("Spell grade must be between 1 and 9: " + gradeRank));
        double baseCost = GsonHelper.getAsDouble(spellJson, "base_cost", 1.0D);
        // 缺省沿用功法契合的灵根属性；显式给出空数组则表示不挑属性（消耗任意灵力）。
        Set<ElementType> costElements = spellJson.has("cost_elements")
                ? parseElements(spellJson, "cost_elements")
                : compatibleElements;
        try {
            return Optional.of(new TechniqueSpell(spellId, category, grade, baseCost, costElements));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static Optional<TechniqueBreathing> parseBreathing(JsonObject techniqueJson) {
        if (!techniqueJson.has("breathing")) {
            return Optional.empty();
        }
        JsonObject breathingJson = GsonHelper.getAsJsonObject(techniqueJson, "breathing");
        double absorptionMultiplier = GsonHelper.getAsDouble(breathingJson, "absorption_multiplier", 1.0D);
        try {
            return Optional.of(new TechniqueBreathing(absorptionMultiplier));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static TechniqueRequirements parseRequirements(JsonObject techniqueJson) {
        if (!techniqueJson.has("requirements")) {
            return TechniqueRequirements.none();
        }
        JsonObject requirementsJson = GsonHelper.getAsJsonObject(techniqueJson, "requirements");
        Optional<CultivationStage> minimumCultivation = parseCultivationStage(requirementsJson);
        int minimumSoulStrength = GsonHelper.getAsInt(requirementsJson, "minimum_soul_strength", 0);
        Set<ElementType> requiredElements = parseElements(requirementsJson, "required_elements");
        Set<ResourceLocation> requiredTechniques = parseResourceLocations(
                requirementsJson,
                "required_techniques"
        );
        int minimumSpiritualRootElements = GsonHelper.getAsInt(
                requirementsJson,
                "minimum_spiritual_root_elements",
                0
        );
        int maximumSpiritualRootElements = GsonHelper.getAsInt(
                requirementsJson,
                "maximum_spiritual_root_elements",
                0
        );
        try {
            return new TechniqueRequirements(
                    minimumCultivation,
                    minimumSoulStrength,
                    requiredElements,
                    requiredTechniques,
                    minimumSpiritualRootElements,
                    maximumSpiritualRootElements
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static Optional<CultivationStage> parseCultivationStage(JsonObject requirementsJson) {
        if (!requirementsJson.has("minimum_cultivation")) {
            return Optional.empty();
        }
        JsonObject stageJson = GsonHelper.getAsJsonObject(requirementsJson, "minimum_cultivation");
        String realmName = GsonHelper.getAsString(stageJson, "realm");
        CultivationRealm realm = CultivationRealm.fromSerializedName(realmName)
                .orElseThrow(() -> new JsonParseException("Unknown cultivation realm: " + realmName));
        int layer = GsonHelper.getAsInt(stageJson, "layer");
        try {
            return Optional.of(new CultivationStage(realm, layer));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static TechniqueComprehension parseComprehension(JsonObject techniqueJson) {
        JsonObject comprehensionJson = GsonHelper.getAsJsonObject(techniqueJson, "comprehension");
        int minimumLearningScore = GsonHelper.getAsInt(comprehensionJson, "minimum_learning_score", 60);
        ResourceLocation perfectScoreMastery = comprehensionJson.has("perfect_score_mastery")
                ? parseResourceLocation(
                        GsonHelper.getAsString(comprehensionJson, "perfect_score_mastery"),
                        "perfect_score_mastery"
                )
                : DEFAULT_MINOR_MASTERY;
        List<String> keyConcepts = parseStringList(comprehensionJson, "key_concepts");
        List<String> gradingCriteria = parseStringList(comprehensionJson, "grading_criteria");
        List<TechniqueMasteryThreshold> masteryThresholds = parseMasteryThresholds(
                comprehensionJson,
                minimumLearningScore
        );
        try {
            return new TechniqueComprehension(
                    minimumLearningScore,
                    perfectScoreMastery,
                    keyConcepts,
                    gradingCriteria,
                    masteryThresholds
            );
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static List<TechniqueMasteryThreshold> parseMasteryThresholds(
            JsonObject comprehensionJson,
            int minimumLearningScore
    ) {
        if (!comprehensionJson.has("mastery_levels")) {
            return List.of(new TechniqueMasteryThreshold(DEFAULT_INITIAL_MASTERY, minimumLearningScore));
        }
        JsonArray masteryLevelsJson = GsonHelper.getAsJsonArray(comprehensionJson, "mastery_levels");
        if (masteryLevelsJson.isEmpty()) {
            throw new JsonParseException("mastery_levels must not be empty");
        }
        List<TechniqueMasteryThreshold> masteryThresholds = new ArrayList<>();
        for (JsonElement masteryLevelJson : masteryLevelsJson) {
            JsonObject masteryLevelObject = GsonHelper.convertToJsonObject(masteryLevelJson, "mastery level");
            ResourceLocation mastery = parseResourceLocation(
                    GsonHelper.getAsString(masteryLevelObject, "id"),
                    "mastery level id"
            );
            int minimumScore = GsonHelper.getAsInt(masteryLevelObject, "minimum_score");
            try {
                masteryThresholds.add(new TechniqueMasteryThreshold(mastery, minimumScore));
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(exception.getMessage(), exception);
            }
        }
        return List.copyOf(masteryThresholds);
    }

    private static List<TechniqueEffect> parseEffects(JsonObject techniqueJson) {
        if (!techniqueJson.has("effects")) {
            return List.of();
        }
        JsonArray effectsJson = GsonHelper.getAsJsonArray(techniqueJson, "effects");
        List<TechniqueEffect> effects = new ArrayList<>();
        for (JsonElement effectJson : effectsJson) {
            JsonObject effectObject = GsonHelper.convertToJsonObject(effectJson, "technique effect");
            ResourceLocation type = parseResourceLocation(
                    GsonHelper.getAsString(effectObject, "type"),
                    "effect type"
            );
            JsonObject parameters = effectObject.has("parameters")
                    ? GsonHelper.getAsJsonObject(effectObject, "parameters")
                    : new JsonObject();
            effects.add(new TechniqueEffect(type, parameters));
        }
        return List.copyOf(effects);
    }

    private static Set<ElementType> parseElements(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return Set.of();
        }
        JsonArray elementsJson = GsonHelper.getAsJsonArray(jsonObject, fieldName);
        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        for (JsonElement elementJson : elementsJson) {
            String elementName = GsonHelper.convertToString(elementJson, fieldName + " entry");
            ElementType element = ElementType.fromSerializedName(elementName)
                    .orElseThrow(() -> new JsonParseException("Unknown element in " + fieldName + ": " + elementName));
            elements.add(element);
        }
        return Set.copyOf(elements);
    }

    private static Set<ResourceLocation> parseResourceLocations(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return Set.of();
        }
        JsonArray valuesJson = GsonHelper.getAsJsonArray(jsonObject, fieldName);
        Set<ResourceLocation> values = new HashSet<>();
        for (JsonElement valueJson : valuesJson) {
            values.add(parseResourceLocation(
                    GsonHelper.convertToString(valueJson, fieldName + " entry"),
                    fieldName
            ));
        }
        return Set.copyOf(values);
    }

    private static List<String> parseStringList(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return List.of();
        }
        JsonArray valuesJson = GsonHelper.getAsJsonArray(jsonObject, fieldName);
        List<String> values = new ArrayList<>();
        for (JsonElement valueJson : valuesJson) {
            String value = GsonHelper.convertToString(valueJson, fieldName + " entry");
            if (value.isBlank()) {
                throw new JsonParseException(fieldName + " entries must not be blank");
            }
            values.add(value);
        }
        return List.copyOf(values);
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
