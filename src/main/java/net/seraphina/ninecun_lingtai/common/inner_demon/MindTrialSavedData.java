package net.seraphina.ninecun_lingtai.common.inner_demon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 心魔试炼会话持久化（心魔即己 §6.2 会话 SavedData）。全服一份，挂主世界数据
 * 存储，模式与 {@code TribulationSavedData} 一致。每个玩家至多一场进行中的试炼。
 *
 * <p>另存一份"补偿通知"名单：启动扫描发现的孤儿会话按外劫横死兜底结算后，
 * 其 owner 登录时收到信箱式说明（§4.2 崩塌兜底）。</p>
 */
public final class MindTrialSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_mind_trials";
    private static final String SESSIONS_TAG = "Sessions";
    private static final String PENDING_COMPENSATION_TAG = "PendingCompensation";

    private final Map<UUID, MindTrialSession> sessionsByOwner = new HashMap<>();
    private final List<UUID> pendingCompensation = new ArrayList<>();

    public static MindTrialSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MindTrialSavedData::load,
                MindTrialSavedData::new,
                DATA_NAME
        );
    }

    public static MindTrialSavedData load(CompoundTag tag) {
        MindTrialSavedData savedData = new MindTrialSavedData();
        for (Tag sessionTag : tag.getList(SESSIONS_TAG, Tag.TAG_COMPOUND)) {
            if (!(sessionTag instanceof CompoundTag sessionDataTag)) {
                continue;
            }
            MindTrialSession session = MindTrialSession.fromNbt(sessionDataTag);
            if (session != null) {
                savedData.sessionsByOwner.put(session.ownerUuid(), session);
            }
        }
        for (Tag uuidTag : tag.getList(PENDING_COMPENSATION_TAG, Tag.TAG_INT_ARRAY)) {
            savedData.pendingCompensation.add(NbtUtils.loadUUID(uuidTag));
        }
        return savedData;
    }

    public MindTrialSession getByOwner(UUID ownerUuid) {
        return this.sessionsByOwner.get(ownerUuid);
    }

    public MindTrialSession getByTrialId(UUID trialId) {
        for (MindTrialSession session : this.sessionsByOwner.values()) {
            if (session.trialId().equals(trialId)) {
                return session;
            }
        }
        return null;
    }

    public void putSession(MindTrialSession session) {
        this.sessionsByOwner.put(session.ownerUuid(), session);
        this.setDirty();
    }

    public boolean removeSession(UUID ownerUuid) {
        boolean removed = this.sessionsByOwner.remove(ownerUuid) != null;
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    public List<MindTrialSession> getAllSessions() {
        return List.copyOf(this.sessionsByOwner.values());
    }

    public void addPendingCompensation(UUID ownerUuid) {
        if (!this.pendingCompensation.contains(ownerUuid)) {
            this.pendingCompensation.add(ownerUuid);
            this.setDirty();
        }
    }

    public boolean consumePendingCompensation(UUID ownerUuid) {
        boolean removed = this.pendingCompensation.remove(ownerUuid);
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag sessionTags = new ListTag();
        for (MindTrialSession session : this.sessionsByOwner.values()) {
            sessionTags.add(session.toNbt());
        }
        tag.put(SESSIONS_TAG, sessionTags);
        ListTag compensationTags = new ListTag();
        for (UUID ownerUuid : this.pendingCompensation) {
            compensationTags.add(NbtUtils.createUUID(ownerUuid));
        }
        tag.put(PENDING_COMPENSATION_TAG, compensationTags);
        return tag;
    }
}
