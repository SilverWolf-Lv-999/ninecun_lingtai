package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 引灵仪式会话的世界级持久化（模式照抄 RingStorageSavedData / TribulationSavedData：
 * {@code overworld().getDataStorage().computeIfAbsent}）。会话跨重启保留，
 * 重进后由 manager 按"中断轻档"结算——登出/重启不再是免罚出口（设计文档 §5.5、R5）。
 */
public final class BreakthroughRitualSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_breakthrough_ritual";
    private static final String SESSIONS_TAG = "Sessions";

    private final Map<UUID, BreakthroughRitualSession> sessions = new HashMap<>();

    public static BreakthroughRitualSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BreakthroughRitualSavedData::load,
                BreakthroughRitualSavedData::new,
                DATA_NAME
        );
    }

    public static BreakthroughRitualSavedData load(CompoundTag tag) {
        BreakthroughRitualSavedData savedData = new BreakthroughRitualSavedData();
        ListTag sessionTags = tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag sessionTag : sessionTags) {
            if (!(sessionTag instanceof CompoundTag sessionDataTag)) {
                continue;
            }
            BreakthroughRitualSession session = BreakthroughRitualSession.fromNbt(sessionDataTag);
            if (session != null) {
                savedData.sessions.put(session.playerId(), session);
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag sessionTags = new ListTag();
        for (BreakthroughRitualSession session : this.sessions.values()) {
            sessionTags.add(session.toNbt());
        }
        tag.put(SESSIONS_TAG, sessionTags);
        return tag;
    }

    public BreakthroughRitualSession getSession(UUID playerId) {
        return this.sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return this.sessions.containsKey(playerId);
    }

    public void putSession(BreakthroughRitualSession session) {
        this.sessions.put(session.playerId(), session);
        this.setDirty();
    }

    public void removeSession(UUID playerId) {
        if (this.sessions.remove(playerId) != null) {
            this.setDirty();
        }
    }
}
