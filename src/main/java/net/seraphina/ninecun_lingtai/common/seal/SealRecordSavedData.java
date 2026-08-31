package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.common.data.migration.SchemaVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 禁制权威存储（全局单例，跨维度；照抄 RingStorageSavedData 的 computeIfAbsent 范式）。
 *
 * <p>OwnerIndex（owner → sealIds）加载时重建、不落盘——离线玩家的刻印也要能被遍历
 * （死契扫描、锚位休眠、衰减期都要求这一点）。层的锚位排序依据是全局单调的
 * carveSequence（从新到旧休眠）。</p>
 */
public final class SealRecordSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_seal_records";
    private static final String SEALS_TAG = "Seals";
    private static final String CARVE_SEQUENCE_TAG = "NextCarveSequence";

    private final Map<UUID, SealRecord> records = new HashMap<>();
    private final Map<UUID, Set<UUID>> ownerIndex = new HashMap<>();
    /** 每主人的休眠层数缓存（钝痛提示的变化检测用，落盘无意义但存着省一轮首查抖动）。 */
    private final Map<UUID, Integer> dormantCountCache = new HashMap<>();
    private long nextCarveSequence = 1L;

    public static SealRecordSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SealRecordSavedData::load,
                SealRecordSavedData::new,
                DATA_NAME
        );
    }

    public static SealRecordSavedData load(CompoundTag tag) {
        SealRecordSavedData savedData = new SealRecordSavedData();
        savedData.nextCarveSequence = Math.max(1L, tag.getLong(CARVE_SEQUENCE_TAG));
        ListTag recordTags = tag.getList(SEALS_TAG, Tag.TAG_COMPOUND);
        for (Tag recordTag : recordTags) {
            if (!(recordTag instanceof CompoundTag recordDataTag)) {
                continue;
            }
            SealRecord record = SealRecord.fromNbt(recordDataTag);
            if (record != null) {
                savedData.records.put(record.sealId(), record);
                savedData.ownerIndex
                        .computeIfAbsent(record.ownerUuid(), unused -> new LinkedHashSet<>())
                        .add(record.sealId());
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        SchemaVersions.setVersion(tag, SchemaVersions.SEAL_RECORD_CURRENT);
        tag.putLong(CARVE_SEQUENCE_TAG, this.nextCarveSequence);
        ListTag recordTags = new ListTag();
        for (SealRecord record : this.records.values()) {
            recordTags.add(record.toNbt());
        }
        tag.put(SEALS_TAG, recordTags);
        return tag;
    }

    @Nullable
    public SealRecord getRecord(UUID sealId) {
        return this.records.get(sealId);
    }

    public SealRecord getOrCreateRecord(UUID sealId, UUID ownerUuid) {
        SealRecord existing = this.records.get(sealId);
        if (existing != null) {
            return existing;
        }
        SealRecord record = new SealRecord(sealId, ownerUuid);
        this.records.put(sealId, record);
        this.ownerIndex.computeIfAbsent(ownerUuid, unused -> new LinkedHashSet<>()).add(sealId);
        this.setDirty();
        return record;
    }

    public void removeRecord(UUID sealId) {
        SealRecord removed = this.records.remove(sealId);
        if (removed != null) {
            Set<UUID> ownerSeals = this.ownerIndex.get(removed.ownerUuid());
            if (ownerSeals != null) {
                ownerSeals.remove(sealId);
            }
            this.setDirty();
        }
    }

    public Collection<SealRecord> getAllRecords() {
        return this.records.values();
    }

    /** 某主人的全部禁制记录。 */
    public List<SealRecord> getRecordsOf(UUID ownerUuid) {
        Set<UUID> sealIds = this.ownerIndex.get(ownerUuid);
        if (sealIds == null || sealIds.isEmpty()) {
            return List.of();
        }
        List<SealRecord> result = new ArrayList<>(sealIds.size());
        for (UUID sealId : sealIds) {
            SealRecord record = this.records.get(sealId);
            if (record != null) {
                result.add(record);
            }
        }
        return result;
    }

    /** 某主人当前占用的锚位数（= 存续层总数；残骸不占）。 */
    public int getUsedAnchors(UUID ownerUuid) {
        int used = 0;
        for (SealRecord record : getRecordsOf(ownerUuid)) {
            used += record.layers().size();
        }
        return used;
    }

    /**
     * 某主人的全部层按刻印时间从旧到新排序（锚位休眠"从新到旧"即列表尾部先休眠）。
     */
    public List<SealLayer> getLayersByAge(UUID ownerUuid) {
        List<SealLayer> layers = new ArrayList<>();
        for (SealRecord record : getRecordsOf(ownerUuid)) {
            layers.addAll(record.layers());
        }
        layers.sort(Comparator.comparingLong(SealLayer::carveSequence));
        return layers;
    }

    public long nextCarveSequence() {
        long sequence = this.nextCarveSequence++;
        this.setDirty();
        return sequence;
    }

    public int getCachedDormantCount(UUID ownerUuid) {
        return this.dormantCountCache.getOrDefault(ownerUuid, 0);
    }

    public void setCachedDormantCount(UUID ownerUuid, int dormantCount) {
        this.dormantCountCache.put(ownerUuid, dormantCount);
    }
}
