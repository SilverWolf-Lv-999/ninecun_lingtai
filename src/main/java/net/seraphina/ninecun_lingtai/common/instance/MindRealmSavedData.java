package net.seraphina.ninecun_lingtai.common.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 心境分格占用表（心魔即己 §4.2）。模式照抄 {@code RingStorageSavedData}：
 * 全服一份、挂主世界数据存储，记录 {@code pocketIndex → (trialId, ownerUuid)}。
 *
 * <p>崩塌兜底（R4）：崩溃时 release 不执行会留下残留占用——服务器启动扫描
 * （{@code MindTrialEventHandler.onServerStarted}）会释放没有对应试炼会话的分格，
 * 分格复用前平台重建会先 fill air。</p>
 */
public final class MindRealmSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_mind_realm";
    private static final String POCKETS_TAG = "Pockets";
    private static final String POCKET_INDEX_TAG = "PocketIndex";
    private static final String TRIAL_ID_TAG = "TrialId";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String NEXT_POCKET_INDEX_TAG = "NextPocketIndex";

    private final Map<Integer, Occupant> pockets = new HashMap<>();
    private int nextPocketIndex;

    public static MindRealmSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MindRealmSavedData::load,
                MindRealmSavedData::new,
                DATA_NAME
        );
    }

    public static MindRealmSavedData load(CompoundTag tag) {
        MindRealmSavedData savedData = new MindRealmSavedData();
        savedData.nextPocketIndex = tag.getInt(NEXT_POCKET_INDEX_TAG);
        for (Tag pocketTag : tag.getList(POCKETS_TAG, Tag.TAG_COMPOUND)) {
            if (!(pocketTag instanceof CompoundTag pocketDataTag)
                    || !pocketDataTag.hasUUID(TRIAL_ID_TAG)
                    || !pocketDataTag.hasUUID(OWNER_UUID_TAG)) {
                continue;
            }
            savedData.pockets.put(
                    pocketDataTag.getInt(POCKET_INDEX_TAG),
                    new Occupant(
                            pocketDataTag.getUUID(TRIAL_ID_TAG),
                            pocketDataTag.getUUID(OWNER_UUID_TAG)
                    )
            );
        }
        return savedData;
    }

    /** 分配一个空闲分格（优先复用已释放的低位索引，防索引无界增长）。 */
    public int allocatePocket(UUID trialId, UUID ownerUuid) {
        for (int index = 0; index < this.nextPocketIndex; index++) {
            if (!this.pockets.containsKey(index)) {
                this.pockets.put(index, new Occupant(trialId, ownerUuid));
                this.setDirty();
                return index;
            }
        }
        int index = this.nextPocketIndex++;
        this.pockets.put(index, new Occupant(trialId, ownerUuid));
        this.setDirty();
        return index;
    }

    public Optional<Occupant> getOccupant(int pocketIndex) {
        return Optional.ofNullable(this.pockets.get(pocketIndex));
    }

    public Optional<Integer> findPocketByTrial(UUID trialId) {
        for (Map.Entry<Integer, Occupant> entry : this.pockets.entrySet()) {
            if (entry.getValue().trialId().equals(trialId)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void releasePocket(int pocketIndex) {
        if (this.pockets.remove(pocketIndex) != null) {
            this.setDirty();
        }
    }

    /** 启动扫描用：全部占用中的分格索引快照。 */
    public List<Integer> getOccupiedPocketIndices() {
        List<Integer> indices = new ArrayList<>(this.pockets.keySet());
        indices.sort(Integer::compareTo);
        return List.copyOf(indices);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(NEXT_POCKET_INDEX_TAG, this.nextPocketIndex);
        ListTag pocketTags = new ListTag();
        for (Map.Entry<Integer, Occupant> entry : this.pockets.entrySet()) {
            CompoundTag pocketTag = new CompoundTag();
            pocketTag.putInt(POCKET_INDEX_TAG, entry.getKey());
            pocketTag.putUUID(TRIAL_ID_TAG, entry.getValue().trialId());
            pocketTag.putUUID(OWNER_UUID_TAG, entry.getValue().ownerUuid());
            pocketTags.add(pocketTag);
        }
        tag.put(POCKETS_TAG, pocketTags);
        return tag;
    }

    public record Occupant(UUID trialId, UUID ownerUuid) {
    }
}
