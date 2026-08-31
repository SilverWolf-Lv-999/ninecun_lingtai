package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 魂游会话的世界级持久化账本（权威数据源；{@link PlayerSoulRoamingManager}
 * 的静态 map 只是运行期缓存）。SoulEntity 本身随区块落盘
 * （requiresCustomPersistence），这里只记会话归属，重启后按 soulUuid 重绑；
 * 重绑失败则安全返还本体。
 */
public final class SoulRoamingSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_soul_roaming";
    private static final String SESSIONS_TAG = "Sessions";

    private final Map<UUID, SoulRoamingRecord> records = new HashMap<>();

    public static SoulRoamingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SoulRoamingSavedData::load,
                SoulRoamingSavedData::new,
                DATA_NAME
        );
    }

    public static SoulRoamingSavedData load(CompoundTag tag) {
        SoulRoamingSavedData savedData = new SoulRoamingSavedData();
        ListTag recordTags = tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag recordTag : recordTags) {
            if (!(recordTag instanceof CompoundTag recordDataTag)) {
                continue;
            }
            SoulRoamingRecord record = SoulRoamingRecord.fromNbt(recordDataTag);
            if (record != null) {
                savedData.records.put(record.playerUuid(), record);
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recordTags = new ListTag();
        for (SoulRoamingRecord record : this.records.values()) {
            recordTags.add(record.toNbt());
        }
        tag.put(SESSIONS_TAG, recordTags);
        return tag;
    }

    @Nullable
    public SoulRoamingRecord getRecord(UUID playerUuid) {
        return this.records.get(playerUuid);
    }

    public boolean hasRecord(UUID playerUuid) {
        return this.records.containsKey(playerUuid);
    }

    public void putRecord(SoulRoamingRecord record) {
        this.records.put(record.playerUuid(), record);
        this.setDirty();
    }

    public void removeRecord(UUID playerUuid) {
        if (this.records.remove(playerUuid) != null) {
            this.setDirty();
        }
    }

    /** 持久化的魂游会话（运行期字段 soulEntityId、movementInput 重绑时重建）。 */
    public record SoulRoamingRecord(
            UUID playerUuid,
            ResourceKey<Level> soulDimension,
            Vec3 bodyPosition,
            UUID soulUuid
    ) {
        private static final String PLAYER_UUID_TAG = "Player";
        private static final String DIMENSION_TAG = "Dimension";
        private static final String BODY_X_TAG = "BodyX";
        private static final String BODY_Y_TAG = "BodyY";
        private static final String BODY_Z_TAG = "BodyZ";
        private static final String SOUL_UUID_TAG = "Soul";

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(PLAYER_UUID_TAG, this.playerUuid);
            tag.putString(DIMENSION_TAG, this.soulDimension.location().toString());
            tag.putDouble(BODY_X_TAG, this.bodyPosition.x);
            tag.putDouble(BODY_Y_TAG, this.bodyPosition.y);
            tag.putDouble(BODY_Z_TAG, this.bodyPosition.z);
            tag.putUUID(SOUL_UUID_TAG, this.soulUuid);
            return tag;
        }

        @Nullable
        public static SoulRoamingRecord fromNbt(CompoundTag tag) {
            if (!tag.hasUUID(PLAYER_UUID_TAG) || !tag.hasUUID(SOUL_UUID_TAG)) {
                return null;
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_TAG));
            if (dimensionId == null) {
                return null;
            }
            return new SoulRoamingRecord(
                    tag.getUUID(PLAYER_UUID_TAG),
                    ResourceKey.create(Registries.DIMENSION, dimensionId),
                    new Vec3(
                            tag.getDouble(BODY_X_TAG),
                            tag.getDouble(BODY_Y_TAG),
                            tag.getDouble(BODY_Z_TAG)
                    ),
                    tag.getUUID(SOUL_UUID_TAG)
            );
        }
    }
}
