package net.seraphina.ninecun_lingtai.common.formation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阵图数据包加载器（照抄 {@code ElementEnvironmentManager} 的
 * SimpleJsonResourceReloadListener 范式，目录 {@code formations}）。
 * 注册入 {@code AddReloadListenerEvent} 由 {@link FormationEventHandler} 完成。
 */
public final class FormationManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "formations";

    public static final FormationManager INSTANCE = new FormationManager();

    /** 按 id 字典序保持稳定迭代序（潜行右键循环选阵依赖该顺序）。 */
    private volatile Map<ResourceLocation, FormationDefinition> definitions = Map.of();

    private FormationManager() {
        super(GSON, DIRECTORY);
    }

    @Nullable
    public FormationDefinition getDefinition(@Nullable ResourceLocation id) {
        return id == null ? null : this.definitions.get(id);
    }

    public List<ResourceLocation> getSortedIds() {
        return List.copyOf(this.definitions.keySet());
    }

    /** 循环选阵：给出当前 id 的下一张（无当前或已到底则回到第一张）；空库返回 null。 */
    @Nullable
    public ResourceLocation cycleNext(@Nullable ResourceLocation current) {
        List<ResourceLocation> ids = getSortedIds();
        if (ids.isEmpty()) {
            return null;
        }
        int index = current == null ? -1 : ids.indexOf(current);
        return ids.get((index + 1) % ids.size());
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        List<Map.Entry<ResourceLocation, JsonElement>> sortedEntries =
                new ArrayList<>(jsonElements.entrySet());
        sortedEntries.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        Map<ResourceLocation, FormationDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : sortedEntries) {
            try {
                loaded.put(entry.getKey(), FormationDefinition.fromJson(
                        entry.getKey(),
                        GsonHelper.convertToJsonObject(entry.getValue(), "formation definition")
                ));
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to load formation definition {}", entry.getKey(), exception);
            }
        }
        this.definitions = java.util.Collections.unmodifiableMap(loaded);
        LOGGER.info("Loaded {} formation definitions", loaded.size());
    }
}
