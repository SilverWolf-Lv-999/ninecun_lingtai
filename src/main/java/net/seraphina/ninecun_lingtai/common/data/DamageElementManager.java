package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.seraphina.ninecun_lingtai.api.element.ElementApi;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.element.ElementalPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DamageElementManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "damage_elements";
    private static final String ATTACKER_SPIRITUAL_ROOT = "attacker_spiritual_root";

    public static final DamageElementManager INSTANCE = new DamageElementManager();

    private volatile Map<ResourceLocation, DamageElementDefinition> damageElements = Map.of();

    private DamageElementManager() {
        super(GSON, DIRECTORY);
    }

    public Optional<ElementalPower> getElement(DamageSource damageSource) {
        return damageSource.typeHolder().unwrapKey()
                .map(resourceKey -> this.damageElements.get(resourceKey.location()))
                .flatMap(definition -> definition.resolve(damageSource));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, DamageElementDefinition> loadedDamageElements = new HashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadDefinition(entry.getKey(), entry.getValue(), loadedDamageElements));
        this.damageElements = Map.copyOf(loadedDamageElements);
        LOGGER.info("Loaded {} elemental damage mappings", this.damageElements.size());
    }

    private static void loadDefinition(
            ResourceLocation fileId,
            JsonElement jsonElement,
            Map<ResourceLocation, DamageElementDefinition> loadedDamageElements
    ) {
        try {
            ParsedDefinition parsedDefinition = parseDefinition(jsonElement);
            for (ResourceLocation damageType : parsedDefinition.damageTypes()) {
                DamageElementDefinition replaced = loadedDamageElements.put(damageType, parsedDefinition.definition());
                if (replaced != null) {
                    LOGGER.warn("Damage element mapping for {} was replaced by {}", damageType, fileId);
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load damage element definition {}", fileId, exception);
        }
    }

    private static ParsedDefinition parseDefinition(JsonElement jsonElement) {
        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "damage element definition");
        JsonArray damageTypesJson = GsonHelper.getAsJsonArray(jsonObject, "damage_types");
        if (damageTypesJson.isEmpty()) {
            throw new JsonParseException("damage_types must not be empty");
        }

        List<ResourceLocation> damageTypes = new ArrayList<>();
        for (JsonElement damageTypeJson : damageTypesJson) {
            String damageTypeName = GsonHelper.convertToString(damageTypeJson, "damage type");
            ResourceLocation damageType = ResourceLocation.tryParse(damageTypeName);
            if (damageType == null) {
                throw new JsonParseException("Invalid damage type id: " + damageTypeName);
            }
            damageTypes.add(damageType);
        }

        String elementName = GsonHelper.getAsString(jsonObject, "element");
        double strength = getPositiveDouble(jsonObject, "strength", 1.0D);
        DamageElementDefinition definition;
        if (ATTACKER_SPIRITUAL_ROOT.equals(elementName)) {
            ElementalPower fallbackElement = parseFallbackElement(jsonObject);
            definition = DamageElementDefinition.attackerSpiritualRoot(strength, fallbackElement);
        } else {
            ElementType element = parseElement(elementName, "element");
            definition = DamageElementDefinition.fixed(new ElementalPower(element, strength));
        }
        return new ParsedDefinition(List.copyOf(damageTypes), definition);
    }

    private static ElementalPower parseFallbackElement(JsonObject jsonObject) {
        if (!jsonObject.has("fallback_element")) {
            return null;
        }
        ElementType fallbackElement = parseElement(
                GsonHelper.getAsString(jsonObject, "fallback_element"),
                "fallback_element"
        );
        double fallbackStrength = getPositiveDouble(jsonObject, "fallback_strength", 1.0D);
        return new ElementalPower(fallbackElement, fallbackStrength);
    }

    private static ElementType parseElement(String elementName, String fieldName) {
        return ElementType.fromSerializedName(elementName)
                .orElseThrow(() -> new JsonParseException("Unknown " + fieldName + ": " + elementName));
    }

    private static double getPositiveDouble(JsonObject jsonObject, String fieldName, double defaultValue) {
        double value = GsonHelper.getAsDouble(jsonObject, fieldName, defaultValue);
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new JsonParseException(fieldName + " must be finite and greater than zero");
        }
        return value;
    }

    private enum ElementSource {
        FIXED,
        ATTACKER_SPIRITUAL_ROOT
    }

    private record ParsedDefinition(
            List<ResourceLocation> damageTypes,
            DamageElementDefinition definition
    ) {
    }

    private record DamageElementDefinition(
            ElementSource source,
            ElementalPower fixedElement,
            double strengthMultiplier,
            ElementalPower fallbackElement
    ) {
        private static DamageElementDefinition fixed(ElementalPower elementalPower) {
            return new DamageElementDefinition(ElementSource.FIXED, elementalPower, 1.0D, null);
        }

        private static DamageElementDefinition attackerSpiritualRoot(
                double strengthMultiplier,
                ElementalPower fallbackElement
        ) {
            return new DamageElementDefinition(
                    ElementSource.ATTACKER_SPIRITUAL_ROOT,
                    null,
                    strengthMultiplier,
                    fallbackElement
            );
        }

        private Optional<ElementalPower> resolve(DamageSource damageSource) {
            if (this.source == ElementSource.FIXED) {
                return Optional.of(this.fixedElement);
            }
            if (damageSource.getEntity() instanceof LivingEntity attacker) {
                Optional<ElementalPower> attackerElement = ElementApi.getAttackElement(attacker);
                if (attackerElement.isPresent()) {
                    ElementalPower elementalPower = attackerElement.get();
                    return Optional.of(new ElementalPower(
                            elementalPower.element(),
                            elementalPower.strength() * this.strengthMultiplier
                    ));
                }
            }
            return Optional.ofNullable(this.fallbackElement);
        }
    }
}
