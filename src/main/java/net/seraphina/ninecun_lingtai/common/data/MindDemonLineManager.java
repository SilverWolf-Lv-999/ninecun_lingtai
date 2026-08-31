package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.behavior.SpellUsageWindow;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindDemonSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 心魔台词库 reload listener（心魔即己 §6.5，模式照抄
 * {@link ElementEnvironmentManager} 的 {@link SimpleJsonResourceReloadListener}）。
 *
 * <p>数据目录 {@code data/&lt;namespace&gt;/mind_demon_lines/*.json}：</p>
 * <pre>
 * { "conditions": { "min_kills_villager": 5, ... },
 *   "phase": "reveal",
 *   "lines": [ "message.ninecun_lingtai.demon_line.butcher_1", ... ] }
 * </pre>
 * lines 存语言键（文案进 lang，保持可本地化——lang 键接线清单见开发日志）。
 * 注册点：{@code MindTrialEventHandler.onAddReloadListener}。
 */
public final class MindDemonLineManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "mind_demon_lines";

    public static final MindDemonLineManager INSTANCE = new MindDemonLineManager();

    private volatile List<LineEntry> entries = List.of();

    private MindDemonLineManager() {
        super(GSON, DIRECTORY);
    }

    /** 按锁定账本匹配条件集，随机抽 {@code count} 条语言键；无匹配时返回兜底台词。 */
    public List<String> selectLineKeys(
            CompoundTag lockedLedger,
            String phase,
            RandomSource random,
            int count
    ) {
        List<String> candidates = new ArrayList<>();
        for (LineEntry entry : this.entries) {
            if (entry.phase().equalsIgnoreCase(phase) && entry.matches(lockedLedger)) {
                candidates.addAll(entry.lineKeys());
            }
        }
        if (candidates.isEmpty()) {
            // lang 文件归其它系统所有，本批数据文件直接存中文字面量；
            // 未来本地化时把 lines 换成 message.* 键即可（消费端两者都识别）。
            return List.of("汝之道心，吾已了然于胸");
        }
        List<String> selected = new ArrayList<>(Math.min(count, candidates.size()));
        while (selected.size() < count && !candidates.isEmpty()) {
            selected.add(candidates.remove(random.nextInt(candidates.size())));
        }
        return List.copyOf(selected);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonEntries,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        List<LineEntry> parsedEntries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> jsonEntry : jsonEntries.entrySet()) {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(
                        jsonEntry.getValue(),
                        "mind_demon_lines"
                );
                String phase = GsonHelper.getAsString(root, "phase", "reveal");
                JsonObject conditions = GsonHelper.getAsJsonObject(
                        root,
                        "conditions",
                        new JsonObject()
                );
                JsonArray linesArray = GsonHelper.getAsJsonArray(root, "lines");
                List<String> lineKeys = new ArrayList<>(linesArray.size());
                for (JsonElement lineElement : linesArray) {
                    lineKeys.add(GsonHelper.convertToString(lineElement, "line"));
                }
                if (!lineKeys.isEmpty()) {
                    parsedEntries.add(new LineEntry(
                            phase,
                            Conditions.fromJson(conditions),
                            List.copyOf(lineKeys)
                    ));
                }
            } catch (JsonParseException exception) {
                LOGGER.error(
                        "Malformed mind demon line file {}: {}",
                        jsonEntry.getKey(),
                        exception.getMessage()
                );
            }
        }
        this.entries = List.copyOf(parsedEntries);
        LOGGER.info("Loaded {} mind demon line entries", parsedEntries.size());
    }

    private record LineEntry(String phase, Conditions conditions, List<String> lineKeys) {
        private boolean matches(CompoundTag lockedLedger) {
            return this.conditions.matches(lockedLedger);
        }
    }

    /** 条件集：全部满足才匹配；缺省条件恒真。 */
    private record Conditions(
            int minKillsVillager,
            int minKillsCultivator,
            int minKillsAlly,
            int minKillsTotal,
            int minPossessCount,
            int minBreakthroughFails,
            int minTribulationFails,
            int minSoulPillUses,
            double minMobilityRatio
    ) {
        private static Conditions fromJson(JsonObject json) {
            return new Conditions(
                    GsonHelper.getAsInt(json, "min_kills_villager", 0),
                    GsonHelper.getAsInt(json, "min_kills_cultivator", 0),
                    GsonHelper.getAsInt(json, "min_kills_ally", 0),
                    GsonHelper.getAsInt(json, "min_kills_total", 0),
                    GsonHelper.getAsInt(json, "min_possess_count", 0),
                    GsonHelper.getAsInt(json, "min_breakthrough_fails", 0),
                    GsonHelper.getAsInt(json, "min_tribulation_fails", 0),
                    GsonHelper.getAsInt(json, "min_soul_pill_uses", 0),
                    GsonHelper.getAsDouble(json, "min_mobility_ratio", 0.0D)
            );
        }

        private boolean matches(CompoundTag ledger) {
            SpellUsageWindow window = PlayerBehaviorLedgerData.spellWindowFrom(ledger);
            return PlayerBehaviorLedgerData.killsFrom(
                    ledger, PlayerBehaviorLedgerData.KillCategory.VILLAGER) >= this.minKillsVillager
                    && PlayerBehaviorLedgerData.killsFrom(
                    ledger, PlayerBehaviorLedgerData.KillCategory.CULTIVATOR) >= this.minKillsCultivator
                    && PlayerBehaviorLedgerData.killsFrom(
                    ledger, PlayerBehaviorLedgerData.KillCategory.ALLY) >= this.minKillsAlly
                    && PlayerBehaviorLedgerData.totalKillsFrom(ledger) >= this.minKillsTotal
                    && PlayerBehaviorLedgerData.possessCountFrom(ledger) >= this.minPossessCount
                    && PlayerBehaviorLedgerData.breakthroughFailsFrom(ledger) >= this.minBreakthroughFails
                    && PlayerBehaviorLedgerData.tribulationFailsFrom(ledger) >= this.minTribulationFails
                    && PlayerBehaviorLedgerData.soulPillUsesFrom(ledger) >= this.minSoulPillUses
                    && window.usageRatio(MindDemonSnapshot::isMobilitySpell) >= this.minMobilityRatio;
        }
    }
}
