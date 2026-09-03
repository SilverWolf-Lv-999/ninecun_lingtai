package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.seraphina.ninecun_lingtai.api.alchemy.HerbDefinition;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 灵药属性加载器（炼丹 M4-c，照 {@link AlchemyRecipeManager} 模式）：
 * 加载 {@code data/<namespace>/alchemy_herbs/*.json}，按物品注册名索引。
 * 未定义的物品视为无属性凡药（{@link #getDefinitionForItem} 返回 empty）。
 */
public final class AlchemyHerbManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "alchemy_herbs";

    public static final AlchemyHerbManager INSTANCE = new AlchemyHerbManager();

    private volatile Map<ResourceLocation, HerbDefinition> herbsByItem = Map.of();

    private AlchemyHerbManager() {
        super(GSON, DIRECTORY);
    }

    public Optional<HerbDefinition> getDefinitionForItem(ResourceLocation itemId) {
        return Optional.ofNullable(this.herbsByItem.get(itemId));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, HerbDefinition> loadedHerbs = new LinkedHashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadHerb(entry.getKey(), entry.getValue(), loadedHerbs));
        this.herbsByItem = Map.copyOf(loadedHerbs);
        LOGGER.info("Loaded {} alchemy herb definitions", this.herbsByItem.size());
    }

    private static void loadHerb(
            ResourceLocation definitionId,
            JsonElement jsonElement,
            Map<ResourceLocation, HerbDefinition> loadedHerbs
    ) {
        try {
            JsonObject json = GsonHelper.convertToJsonObject(jsonElement, "herb definition");
            ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
            ElementType element = null;
            if (json.has("element") && !json.get("element").isJsonNull()) {
                element = ElementType.valueOf(
                        GsonHelper.getAsString(json, "element").toUpperCase(Locale.ROOT)
                );
            }
            HerbDefinition herb = new HerbDefinition(
                    definitionId,
                    itemId,
                    GsonHelper.getAsInt(json, "tier"),
                    element,
                    GsonHelper.getAsString(json, "nature", "neutral")
            );
            HerbDefinition conflictingHerb = loadedHerbs.putIfAbsent(itemId, herb);
            if (conflictingHerb != null) {
                LOGGER.error(
                        "Ignoring herb definition {} because item {} is already defined by {}",
                        definitionId,
                        itemId,
                        conflictingHerb.id()
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load alchemy herb definition {}", definitionId, exception);
        }
    }
}
