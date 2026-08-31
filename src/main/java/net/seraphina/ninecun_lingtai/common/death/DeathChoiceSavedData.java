package net.seraphina.ninecun_lingtai.common.death;

import net.seraphina.ninecun_lingtai.common.data.migration.SchemaVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡路线会话持久化（overworld 全局）。玩家在选择界面掉线/服务器重启后
 * 会话仍在，登录时恢复（灵气生态日志 §4 表定稿）。
 */
public final class DeathChoiceSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_death_choices";
    private static final String SESSIONS_TAG = "Sessions";

    private final Map<UUID, DeathChoiceSession> sessions = new HashMap<>();

    public static DeathChoiceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DeathChoiceSavedData::load,
                DeathChoiceSavedData::new,
                DATA_NAME
        );
    }

    public static DeathChoiceSavedData load(CompoundTag tag) {
        DeathChoiceSavedData savedData = new DeathChoiceSavedData();
        ListTag sessionTags = tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag sessionTag : sessionTags) {
            if (sessionTag instanceof CompoundTag sessionDataTag) {
                DeathChoiceSession session = DeathChoiceSession.fromNbt(sessionDataTag);
                if (session != null) {
                    savedData.sessions.put(session.playerUuid(), session);
                }
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        SchemaVersions.setVersion(tag, SchemaVersions.DEATH_CHOICE_CURRENT);
        ListTag sessionTags = new ListTag();
        for (DeathChoiceSession session : this.sessions.values()) {
            sessionTags.add(session.toNbt());
        }
        tag.put(SESSIONS_TAG, sessionTags);
        return tag;
    }

    @Nullable
    public DeathChoiceSession getSession(UUID playerUuid) {
        return this.sessions.get(playerUuid);
    }

    public void putSession(DeathChoiceSession session) {
        this.sessions.put(session.playerUuid(), session);
        this.setDirty();
    }

    public void removeSession(UUID playerUuid) {
        if (this.sessions.remove(playerUuid) != null) {
            this.setDirty();
        }
    }

    public Collection<DeathChoiceSession> getAllSessions() {
        return this.sessions.values();
    }
}
