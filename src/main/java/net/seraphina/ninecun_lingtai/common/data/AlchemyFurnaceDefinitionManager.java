package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.seraphina.ninecun_lingtai.api.alchemy.FurnaceTier;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 丹炉阶别定义加载器（炼丹 M4-a，照 {@link AlchemyRecipeManager} 模式）：
 * 加载 {@code data/<namespace>/alchemy_furnaces/*.json}，按炉主方块注册名索引。
 *
 * <p>未定义的炉方块回落 {@link #DEFAULT}（凡铁口径 600/1.0/0/无性/3 阶/1000 耐久），
 * 保证 /reload 热更失败或数据包缺失时正在炼制的炉不崩（验收要点 9）。
 */
public final class AlchemyFurnaceDefinitionManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "alchemy_furnaces";

    /** 凡铁兜底口径（与现状炉行为逐位一致：上限 600、衰减 1.0、无容差）。 */
    public static final FurnaceTier DEFAULT = new FurnaceTier(
            new ResourceLocation("ninecun_lingtai", "alchemy_furnace"),
            600,
            1.0D,
            0.0D,
            null,
            3,
            1000
    );

    public static final AlchemyFurnaceDefinitionManager INSTANCE = new AlchemyFurnaceDefinitionManager();

    private volatile Map<ResourceLocation, FurnaceTier> tiersByBlock = Map.of();

    private AlchemyFurnaceDefinitionManager() {
        super(GSON, DIRECTORY);
    }

    /** 按炉主方块查阶别定义，未定义回落凡铁兜底。 */
    public FurnaceTier getTierForBlock(Block block) {
        return this.tiersByBlock.getOrDefault(BuiltInRegistries.BLOCK.getKey(block), DEFAULT);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, FurnaceTier> loadedTiers = new LinkedHashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadTier(entry.getKey(), entry.getValue(), loadedTiers));
        this.tiersByBlock = Map.copyOf(loadedTiers);
        LOGGER.info("Loaded {} alchemy furnace tiers", this.tiersByBlock.size());
    }

    private static void loadTier(
            ResourceLocation definitionId,
            JsonElement jsonElement,
            Map<ResourceLocation, FurnaceTier> loadedTiers
    ) {
        try {
            JsonObject json = GsonHelper.convertToJsonObject(jsonElement, "furnace tier");
            ResourceLocation blockId = new ResourceLocation(GsonHelper.getAsString(json, "block"));
            ElementType element = null;
            if (json.has("element") && !json.get("element").isJsonNull()) {
                String elementName = GsonHelper.getAsString(json, "element");
                element = ElementType.valueOf(elementName.toUpperCase(Locale.ROOT));
            }
            FurnaceTier tier = new FurnaceTier(
                    blockId,
                    GsonHelper.getAsInt(json, "heat_cap"),
                    GsonHelper.getAsDouble(json, "heat_decay_rate", 1.0D),
                    GsonHelper.getAsDouble(json, "tolerance_bonus", 0.0D),
                    element,
                    GsonHelper.getAsInt(json, "max_recipe_tier"),
                    GsonHelper.getAsInt(json, "max_durability", 1000)
            );
            FurnaceTier conflictingTier = loadedTiers.putIfAbsent(blockId, tier);
            if (conflictingTier != null) {
                LOGGER.error(
                        "Ignoring furnace tier {} because block {} is already defined",
                        definitionId,
                        blockId
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load alchemy furnace tier {}", definitionId, exception);
        }
    }
}
