package net.seraphina.ninecun_lingtai.common.tribulation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-level persistence for in-progress tribulation sessions. Sessions survive server
 * restarts, so quitting or crashing the game can never be used to escape an ongoing
 * tribulation; the manager resumes them on the player's first tick after re-login.
 */
public final class TribulationSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_tribulations";
    private static final String SESSIONS_TAG = "Sessions";

    private final Map<UUID, TribulationSession> sessions = new HashMap<>();

    public static TribulationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TribulationSavedData::load,
                TribulationSavedData::new,
                DATA_NAME
        );
    }

    public static TribulationSavedData load(CompoundTag tag) {
        TribulationSavedData savedData = new TribulationSavedData();
        ListTag sessionTags = tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag sessionTag : sessionTags) {
            if (!(sessionTag instanceof CompoundTag sessionDataTag)) {
                continue;
            }
            TribulationSession session = TribulationSession.fromNbt(sessionDataTag);
            if (session != null) {
                savedData.sessions.put(session.playerId(), session);
            }
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag sessionTags = new ListTag();
        for (TribulationSession session : this.sessions.values()) {
            sessionTags.add(session.toNbt());
        }
        tag.put(SESSIONS_TAG, sessionTags);
        return tag;
    }

    public TribulationSession getSession(UUID playerId) {
        return this.sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return this.sessions.containsKey(playerId);
    }

    public void putSession(TribulationSession session) {
        this.sessions.put(session.playerId(), session);
        this.setDirty();
    }

    public void removeSession(UUID playerId) {
        if (this.sessions.remove(playerId) != null) {
            this.setDirty();
        }
    }
}
