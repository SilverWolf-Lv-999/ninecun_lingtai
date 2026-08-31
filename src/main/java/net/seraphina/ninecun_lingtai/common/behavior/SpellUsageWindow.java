package net.seraphina.ninecun_lingtai.common.behavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 施法滑动窗口统计：按修真日（游戏日，24000 tick）分桶，保留最近
 * {@value #MAX_BUCKETS} 桶，每桶至多 {@value #MAX_ENTRIES_PER_BUCKET} 个法术条目，
 * 超限时淘汰使用次数最少的条目（设计文档 R6 账本膨胀约束）。
 *
 * <p>本类是 {@link PlayerBehaviorLedgerData} 的内部结构包装，持有一份 ListTag 的
 * 解析视图；调用 {@link #save()} 写回 ListTag。桶结构：</p>
 * <pre>
 * { "Day": long, "Spells": { "&lt;spellId&gt;": int, ... } }
 * </pre>
 */
public final class SpellUsageWindow {
    public static final int MAX_BUCKETS = 7;
    public static final int MAX_ENTRIES_PER_BUCKET = 16;
    public static final long TICKS_PER_DAY = 24000L;

    private static final String DAY_TAG = "Day";
    private static final String SPELLS_TAG = "Spells";

    private final List<Bucket> buckets = new ArrayList<>();

    private SpellUsageWindow() {
    }

    public static SpellUsageWindow fromNbt(ListTag listTag) {
        SpellUsageWindow window = new SpellUsageWindow();
        if (listTag == null) {
            return window;
        }
        for (Tag tag : listTag) {
            if (!(tag instanceof CompoundTag bucketTag)) {
                continue;
            }
            Bucket bucket = new Bucket(bucketTag.getLong(DAY_TAG));
            CompoundTag spellsTag = bucketTag.getCompound(SPELLS_TAG);
            for (String spellId : spellsTag.getAllKeys()) {
                int uses = spellsTag.getInt(spellId);
                if (uses > 0) {
                    bucket.uses.put(spellId, uses);
                }
            }
            window.buckets.add(bucket);
        }
        window.buckets.sort(Comparator.comparingLong(bucket -> bucket.day));
        return window;
    }

    public ListTag save() {
        ListTag listTag = new ListTag();
        for (Bucket bucket : this.buckets) {
            CompoundTag bucketTag = new CompoundTag();
            bucketTag.putLong(DAY_TAG, bucket.day);
            CompoundTag spellsTag = new CompoundTag();
            bucket.uses.forEach(spellsTag::putInt);
            bucketTag.put(SPELLS_TAG, spellsTag);
            listTag.add(bucketTag);
        }
        return listTag;
    }

    /** 记录一次施法；{@code gameTime} 用于计算当前修真日并滚动淘汰过期桶。 */
    public void record(long gameTime, ResourceLocation spellId) {
        long day = Math.floorDiv(gameTime, TICKS_PER_DAY);
        Bucket current = this.buckets.isEmpty()
                ? null
                : this.buckets.get(this.buckets.size() - 1);
        if (current == null || current.day != day) {
            current = new Bucket(day);
            this.buckets.add(current);
            while (this.buckets.size() > MAX_BUCKETS) {
                this.buckets.remove(0);
            }
        }
        String key = spellId.toString();
        Integer existing = current.uses.get(key);
        if (existing == null && current.uses.size() >= MAX_ENTRIES_PER_BUCKET) {
            evictLeastUsed(current);
        }
        current.uses.merge(key, 1, Integer::sum);
    }

    public int totalUses() {
        int total = 0;
        for (Bucket bucket : this.buckets) {
            for (int uses : bucket.uses.values()) {
                total += uses;
            }
        }
        return total;
    }

    /** 匹配某类法术（按 id 判断）的总使用次数。 */
    public int usesMatching(Predicate<ResourceLocation> filter) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : aggregate().entrySet()) {
            ResourceLocation spellId = ResourceLocation.tryParse(entry.getKey());
            if (spellId != null && filter.test(spellId)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    /** 匹配某类法术的使用占比，窗口为空时返回 0。 */
    public double usageRatio(Predicate<ResourceLocation> filter) {
        int total = totalUses();
        return total <= 0 ? 0.0D : (double) usesMatching(filter) / total;
    }

    /** 使用次数最多的前 {@code count} 个法术 id，按次数降序。 */
    public List<ResourceLocation> topSpells(int count) {
        return aggregate().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(0, count))
                .map(entry -> ResourceLocation.tryParse(entry.getKey()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public boolean isEmpty() {
        return totalUses() <= 0;
    }

    private Map<String, Integer> aggregate() {
        Map<String, Integer> totals = new HashMap<>();
        for (Bucket bucket : this.buckets) {
            bucket.uses.forEach((spellId, uses) -> totals.merge(spellId, uses, Integer::sum));
        }
        return totals;
    }

    private static void evictLeastUsed(Bucket bucket) {
        bucket.uses.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .ifPresent(bucket.uses::remove);
    }

    private static final class Bucket {
        private final long day;
        private final Map<String, Integer> uses = new HashMap<>();

        private Bucket(long day) {
            this.day = day;
        }
    }
}
