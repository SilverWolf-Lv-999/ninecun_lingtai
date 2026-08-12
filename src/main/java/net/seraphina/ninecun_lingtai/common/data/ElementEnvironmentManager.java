package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ElementEnvironmentManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "element_environments";

    public static final ElementEnvironmentManager INSTANCE = new ElementEnvironmentManager();

    private volatile List<ElementEnvironmentRule> environmentRules = List.of();

    private ElementEnvironmentManager() {
        super(GSON, DIRECTORY);
    }

    public double getDamageMultiplier(LivingEntity target, ElementType element) {
        double multiplier = getEnvironmentMultiplier(target.level(), target.blockPosition(), element);
        return element == ElementType.CHAOS ? Math.max(1.0D, multiplier) : multiplier;
    }

    public double getEnvironmentMultiplier(LivingEntity entity, ElementType element) {
        return getEnvironmentMultiplier(entity.level(), entity.blockPosition(), element);
    }

    public double getEnvironmentMultiplier(Level level, BlockPos position, ElementType element) {
        ResourceLocation dimension = level.dimension().location();
        Holder<Biome> biome = level.getBiome(position);
        WeatherType weather = WeatherType.getCurrentWeather(level, position);
        double multiplier = 1.0D;
        for (ElementEnvironmentRule environmentRule : this.environmentRules) {
            if (environmentRule.matches(dimension, biome, weather)) {
                multiplier *= environmentRule.getMultiplier(element);
            }
        }
        return multiplier;
    }

    public FiveElementValues getFiveElementValues(LivingEntity entity) {
        return new FiveElementValues(
                (float) getEnvironmentMultiplier(entity, ElementType.METAL),
                (float) getEnvironmentMultiplier(entity, ElementType.WOOD),
                (float) getEnvironmentMultiplier(entity, ElementType.WATER),
                (float) getEnvironmentMultiplier(entity, ElementType.FIRE),
                (float) getEnvironmentMultiplier(entity, ElementType.EARTH)
        );
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        List<ElementEnvironmentRule> loadedRules = new ArrayList<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadRules(entry.getKey(), entry.getValue(), loadedRules));
        this.environmentRules = List.copyOf(loadedRules);
        LOGGER.info("Loaded {} elemental environment rules", this.environmentRules.size());
    }

    private static void loadRules(
            ResourceLocation fileId,
            JsonElement jsonElement,
            List<ElementEnvironmentRule> loadedRules
    ) {
        try {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "element environment definition");
            if (jsonObject.has("rules")) {
                JsonArray rulesJson = GsonHelper.getAsJsonArray(jsonObject, "rules");
                if (rulesJson.isEmpty()) {
                    throw new JsonParseException("rules must not be empty");
                }
                for (int index = 0; index < rulesJson.size(); index++) {
                    loadedRules.add(parseRule(
                            GsonHelper.convertToJsonObject(rulesJson.get(index), "environment rule"),
                            fileId + "#" + index
                    ));
                }
            } else {
                loadedRules.add(parseRule(jsonObject, fileId.toString()));
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load element environment definition {}", fileId, exception);
        }
    }

    private static ElementEnvironmentRule parseRule(JsonObject jsonObject, String ruleId) {
        Set<ResourceLocation> dimensions = parseResourceLocations(jsonObject, "dimensions");
        Set<ResourceKey<Biome>> biomes = parseBiomeKeys(jsonObject);
        Set<TagKey<Biome>> biomeTags = parseBiomeTags(jsonObject);
        Set<WeatherType> weatherTypes = parseWeatherTypes(jsonObject, ruleId);
        if (dimensions.isEmpty() && biomes.isEmpty() && biomeTags.isEmpty() && weatherTypes.isEmpty()) {
            throw new JsonParseException(ruleId + " must define dimensions, biomes, biome_tags, or weather");
        }

        JsonObject multipliersJson = GsonHelper.getAsJsonObject(jsonObject, "multipliers");
        if (multipliersJson.size() == 0) {
            throw new JsonParseException(ruleId + " multipliers must not be empty");
        }
        Map<ElementType, Double> multipliers = new EnumMap<>(ElementType.class);
        for (Map.Entry<String, JsonElement> entry : multipliersJson.entrySet()) {
            ElementType element = ElementType.fromSerializedName(entry.getKey())
                    .orElseThrow(() -> new JsonParseException("Unknown element in " + ruleId + ": " + entry.getKey()));
            double multiplier = GsonHelper.convertToDouble(entry.getValue(), "multiplier for " + entry.getKey());
            if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
                throw new JsonParseException("Multiplier for " + entry.getKey() + " in " + ruleId
                        + " must be finite and non-negative");
            }
            multipliers.put(element, multiplier);
        }
        return new ElementEnvironmentRule(dimensions, biomes, biomeTags, weatherTypes, multipliers);
    }

    private static Set<ResourceLocation> parseResourceLocations(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return Set.of();
        }
        JsonArray valuesJson = GsonHelper.getAsJsonArray(jsonObject, fieldName);
        Set<ResourceLocation> values = new HashSet<>();
        for (JsonElement valueJson : valuesJson) {
            String valueName = GsonHelper.convertToString(valueJson, fieldName + " entry");
            ResourceLocation value = ResourceLocation.tryParse(valueName);
            if (value == null) {
                throw new JsonParseException("Invalid resource id in " + fieldName + ": " + valueName);
            }
            values.add(value);
        }
        return Set.copyOf(values);
    }

    private static Set<ResourceKey<Biome>> parseBiomeKeys(JsonObject jsonObject) {
        Set<ResourceKey<Biome>> biomes = new HashSet<>();
        for (ResourceLocation biome : parseResourceLocations(jsonObject, "biomes")) {
            biomes.add(ResourceKey.create(Registries.BIOME, biome));
        }
        return Set.copyOf(biomes);
    }

    private static Set<TagKey<Biome>> parseBiomeTags(JsonObject jsonObject) {
        Set<TagKey<Biome>> biomeTags = new HashSet<>();
        for (ResourceLocation biomeTag : parseResourceLocations(jsonObject, "biome_tags")) {
            biomeTags.add(TagKey.create(Registries.BIOME, biomeTag));
        }
        return Set.copyOf(biomeTags);
    }

    private static Set<WeatherType> parseWeatherTypes(JsonObject jsonObject, String ruleId) {
        if (!jsonObject.has("weather")) {
            return Set.of();
        }
        JsonArray weatherJson = GsonHelper.getAsJsonArray(jsonObject, "weather");
        Set<WeatherType> weatherTypes = new HashSet<>();
        for (JsonElement weatherEntryJson : weatherJson) {
            String weatherName = GsonHelper.convertToString(weatherEntryJson, "weather entry");
            WeatherType weatherType = WeatherType.fromSerializedName(weatherName);
            if (weatherType == null) {
                throw new JsonParseException("Unknown weather in " + ruleId + ": " + weatherName);
            }
            weatherTypes.add(weatherType);
        }
        return Set.copyOf(weatherTypes);
    }

    private record ElementEnvironmentRule(
            Set<ResourceLocation> dimensions,
            Set<ResourceKey<Biome>> biomes,
            Set<TagKey<Biome>> biomeTags,
            Set<WeatherType> weatherTypes,
            Map<ElementType, Double> multipliers
    ) {
        private ElementEnvironmentRule {
            dimensions = Set.copyOf(dimensions);
            biomes = Set.copyOf(biomes);
            biomeTags = Set.copyOf(biomeTags);
            weatherTypes = Set.copyOf(weatherTypes);
            multipliers = Map.copyOf(multipliers);
        }

        private boolean matches(ResourceLocation dimension, Holder<Biome> biome, WeatherType weatherType) {
            boolean matchesDimension = this.dimensions.isEmpty() || this.dimensions.contains(dimension);
            boolean matchesBiome = this.biomes.isEmpty() && this.biomeTags.isEmpty()
                    || this.biomes.stream().anyMatch(biome::is)
                    || this.biomeTags.stream().anyMatch(biome::is);
            boolean matchesWeather = this.weatherTypes.isEmpty() || this.weatherTypes.contains(weatherType);
            return matchesDimension && matchesBiome && matchesWeather;
        }

        private double getMultiplier(ElementType element) {
            return this.multipliers.getOrDefault(element, 1.0D);
        }
    }

    private enum WeatherType {
        CLEAR("clear"),
        RAIN("rain"),
        SNOW("snow"),
        THUNDER("thunder"),
        SHELTERED("sheltered");

        private final String serializedName;

        WeatherType(String serializedName) {
            this.serializedName = serializedName;
        }

        private static WeatherType getCurrentWeather(Level level, BlockPos position) {
            if (!level.isRaining()) {
                return CLEAR;
            }

            BlockPos precipitationPosition = position.above();
            if (!level.canSeeSky(precipitationPosition)) {
                return SHELTERED;
            }

            Biome.Precipitation precipitation = level.getBiome(precipitationPosition)
                    .value()
                    .getPrecipitationAt(precipitationPosition);
            if (precipitation == Biome.Precipitation.NONE) {
                return SHELTERED;
            }
            if (level.isThundering()) {
                return THUNDER;
            }
            return precipitation == Biome.Precipitation.SNOW ? SNOW : RAIN;
        }

        private static WeatherType fromSerializedName(String serializedName) {
            for (WeatherType weatherType : values()) {
                if (weatherType.serializedName.equals(serializedName)) {
                    return weatherType;
                }
            }
            return null;
        }
    }
}
