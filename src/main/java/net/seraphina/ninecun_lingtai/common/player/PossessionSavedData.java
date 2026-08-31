package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 夺舍会话的世界级持久化账本（权威数据源；{@link PlayerPossessionManager}
 * 的静态 map 只是运行期缓存）。干净关服时进行中的会话保留在这里，重启后由
 * 登录流程恢复或安全解除，杜绝"重启即状态错乱"。
 */
public final class PossessionSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_possessions";
    private static final String SESSIONS_TAG = "Sessions";

    private final Map<UUID, PossessionRecord> records = new HashMap<>();

    public static PossessionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PossessionSavedData::load,
                PossessionSavedData::new,
                DATA_NAME
        );
    }

    public static PossessionSavedData load(CompoundTag tag) {
        PossessionSavedData savedData = new PossessionSavedData();
        ListTag recordTags = tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag recordTag : recordTags) {
            if (!(recordTag instanceof CompoundTag recordDataTag)) {
                continue;
            }
            PossessionRecord record = PossessionRecord.fromNbt(recordDataTag);
            if (record != null) {
                savedData.records.put(record.possessorUuid(), record);
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recordTags = new ListTag();
        for (PossessionRecord record : this.records.values()) {
            recordTags.add(record.toNbt());
        }
        tag.put(SESSIONS_TAG, recordTags);
        return tag;
    }

    @Nullable
    public PossessionRecord getRecord(UUID possessorUuid) {
        return this.records.get(possessorUuid);
    }

    @Nullable
    public PossessionRecord findRecordByTarget(UUID targetUuid) {
        return this.records.values().stream()
                .filter(record -> record.targetUuid().equals(targetUuid))
                .findFirst()
                .orElse(null);
    }

    public void putRecord(PossessionRecord record) {
        this.records.put(record.possessorUuid(), record);
        this.setDirty();
    }

    public void removeRecord(UUID possessorUuid) {
        if (this.records.remove(possessorUuid) != null) {
            this.setDirty();
        }
    }

    /**
     * 持久化的夺舍会话。皮肤等 GameProfile properties 不落盘，重启恢复后由
     * 客户端按 profile id 自行拉取；{@code targetProfileName} 仅用于播报显示。
     */
    public record PossessionRecord(
            UUID possessorUuid,
            UUID targetUuid,
            UUID targetProfileId,
            String targetProfileName,
            GameType targetGameType
    ) {
        private static final String POSSESSOR_UUID_TAG = "Possessor";
        private static final String TARGET_UUID_TAG = "Target";
        private static final String TARGET_PROFILE_ID_TAG = "TargetProfileId";
        private static final String TARGET_PROFILE_NAME_TAG = "TargetProfileName";
        private static final String TARGET_GAME_TYPE_TAG = "TargetGameType";

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(POSSESSOR_UUID_TAG, this.possessorUuid);
            tag.putUUID(TARGET_UUID_TAG, this.targetUuid);
            tag.putUUID(TARGET_PROFILE_ID_TAG, this.targetProfileId);
            tag.putString(TARGET_PROFILE_NAME_TAG, this.targetProfileName);
            tag.putInt(TARGET_GAME_TYPE_TAG, this.targetGameType.getId());
            return tag;
        }

        @Nullable
        public static PossessionRecord fromNbt(CompoundTag tag) {
            if (!tag.hasUUID(POSSESSOR_UUID_TAG) || !tag.hasUUID(TARGET_UUID_TAG)) {
                return null;
            }
            UUID targetUuid = tag.getUUID(TARGET_UUID_TAG);
            return new PossessionRecord(
                    tag.getUUID(POSSESSOR_UUID_TAG),
                    targetUuid,
                    tag.hasUUID(TARGET_PROFILE_ID_TAG) ? tag.getUUID(TARGET_PROFILE_ID_TAG) : targetUuid,
                    tag.getString(TARGET_PROFILE_NAME_TAG),
                    GameType.byId(tag.getInt(TARGET_GAME_TYPE_TAG))
            );
        }
    }
}
