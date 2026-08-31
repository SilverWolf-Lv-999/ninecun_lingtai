package net.seraphina.ninecun_lingtai.common.inner_demon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 单场心魔试炼会话（心魔即己 §6.2），持久化在 {@link MindTrialSavedData}。
 * 含镜像快照、阶段、计时、实体引用与遥测计数。服务端权威。
 */
public final class MindTrialSession {
    private static final String TRIAL_ID_TAG = "TrialId";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String POCKET_INDEX_TAG = "PocketIndex";
    private static final String ARENA_CENTER_TAG = "ArenaCenter";
    private static final String PHASE_TAG = "Phase";
    private static final String PHASE_REMAINING_TICKS_TAG = "PhaseRemainingTicks";
    private static final String DEMON_ID_TAG = "DemonId";
    private static final String BODY_ID_TAG = "BodyId";
    private static final String RESOLVE_CHOICE_TAG = "ResolveChoice";
    private static final String SNAPSHOT_TAG = "Snapshot";
    private static final String MOVEMENT_LOCK_UNTIL_TAG = "MovementLockUntil";
    private static final String FORCED_SWAP_COUNT_TAG = "ForcedSwapCount";
    private static final String STARTED_GAME_TIME_TAG = "StartedGameTime";
    private static final String LAST_DEMON_HEALTH_PCT_TAG = "LastDemonHealthPct";
    private static final String USED_MOBILITY_SPELL_TAG = "UsedMobilitySpell";

    private final UUID trialId;
    private final UUID ownerUuid;
    private final int pocketIndex;
    private final BlockPos arenaCenter;
    private final MindDemonSnapshot snapshot;

    private MindTrialPhase phase;
    private int phaseRemainingTicks;
    private UUID demonId;
    private UUID bodyId;
    private ResolveChoice resolveChoice;
    private long movementLockUntilGameTime;
    private int forcedSwapCount;
    private long startedGameTime;
    private float lastDemonHealthPct = 1.0F;
    /** 本场是否施放过位移法术（遁逃之执心障化解判定）。 */
    private boolean usedMobilitySpell;

    /** REVEAL 阶段待播台词键（瞬态，不序列化——重启恢复时重抽）。 */
    private final List<String> pendingRevealLines = new ArrayList<>();

    public MindTrialSession(
            UUID trialId,
            UUID ownerUuid,
            int pocketIndex,
            BlockPos arenaCenter,
            MindDemonSnapshot snapshot,
            long startedGameTime
    ) {
        this.trialId = trialId;
        this.ownerUuid = ownerUuid;
        this.pocketIndex = pocketIndex;
        this.arenaCenter = arenaCenter;
        this.snapshot = snapshot;
        this.phase = MindTrialPhase.REVEAL;
        this.startedGameTime = startedGameTime;
    }

    // ------------------------------------------------------------------ 访问器

    public UUID trialId() {
        return this.trialId;
    }

    public UUID ownerUuid() {
        return this.ownerUuid;
    }

    public int pocketIndex() {
        return this.pocketIndex;
    }

    public BlockPos arenaCenter() {
        return this.arenaCenter;
    }

    public MindDemonSnapshot snapshot() {
        return this.snapshot;
    }

    public MindTrialPhase phase() {
        return this.phase;
    }

    public int phaseRemainingTicks() {
        return this.phaseRemainingTicks;
    }

    public void setPhase(MindTrialPhase phase, int durationTicks) {
        this.phase = phase;
        this.phaseRemainingTicks = durationTicks;
    }

    public void tickPhase() {
        this.phaseRemainingTicks = Math.max(0, this.phaseRemainingTicks - 1);
    }

    public UUID demonId() {
        return this.demonId;
    }

    public void setDemonId(UUID demonId) {
        this.demonId = demonId;
    }

    public UUID bodyId() {
        return this.bodyId;
    }

    public void setBodyId(UUID bodyId) {
        this.bodyId = bodyId;
    }

    public ResolveChoice resolveChoice() {
        return this.resolveChoice;
    }

    public void setResolveChoice(ResolveChoice resolveChoice) {
        this.resolveChoice = resolveChoice;
    }

    public long movementLockUntilGameTime() {
        return this.movementLockUntilGameTime;
    }

    public void setMovementLockUntilGameTime(long gameTime) {
        this.movementLockUntilGameTime = gameTime;
    }

    public int forcedSwapCount() {
        return this.forcedSwapCount;
    }

    /** 遥测：REFLECT 阶段玩家被迫换招（施放锁定快照之外的法术）计数。 */
    public void incrementForcedSwapCount() {
        this.forcedSwapCount++;
    }

    public long startedGameTime() {
        return this.startedGameTime;
    }

    public float lastDemonHealthPct() {
        return this.lastDemonHealthPct;
    }

    public void setLastDemonHealthPct(float healthPct) {
        this.lastDemonHealthPct = healthPct;
    }

    public boolean usedMobilitySpell() {
        return this.usedMobilitySpell;
    }

    public void setUsedMobilitySpell(boolean usedMobilitySpell) {
        this.usedMobilitySpell = usedMobilitySpell;
    }

    public List<String> pendingRevealLines() {
        return this.pendingRevealLines;
    }

    // ------------------------------------------------------------------ 序列化

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TRIAL_ID_TAG, this.trialId);
        tag.putUUID(OWNER_UUID_TAG, this.ownerUuid);
        tag.putInt(POCKET_INDEX_TAG, this.pocketIndex);
        tag.put(ARENA_CENTER_TAG, NbtUtils.writeBlockPos(this.arenaCenter));
        tag.put(SNAPSHOT_TAG, this.snapshot.toNbt());
        tag.putString(PHASE_TAG, this.phase.name());
        tag.putInt(PHASE_REMAINING_TICKS_TAG, this.phaseRemainingTicks);
        if (this.demonId != null) {
            tag.putUUID(DEMON_ID_TAG, this.demonId);
        }
        if (this.bodyId != null) {
            tag.putUUID(BODY_ID_TAG, this.bodyId);
        }
        if (this.resolveChoice != null) {
            tag.putString(RESOLVE_CHOICE_TAG, this.resolveChoice.name());
        }
        tag.putLong(MOVEMENT_LOCK_UNTIL_TAG, this.movementLockUntilGameTime);
        tag.putInt(FORCED_SWAP_COUNT_TAG, this.forcedSwapCount);
        tag.putLong(STARTED_GAME_TIME_TAG, this.startedGameTime);
        tag.putFloat(LAST_DEMON_HEALTH_PCT_TAG, this.lastDemonHealthPct);
        tag.putBoolean(USED_MOBILITY_SPELL_TAG, this.usedMobilitySpell);
        return tag;
    }

    /** 畸形数据返回 null（读取方按崩塌兜底结算）。 */
    public static MindTrialSession fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(TRIAL_ID_TAG)
                || !tag.hasUUID(OWNER_UUID_TAG)
                || !tag.contains(SNAPSHOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        MindDemonSnapshot snapshot = MindDemonSnapshot.fromNbt(tag.getCompound(SNAPSHOT_TAG));
        MindTrialPhase phase = MindTrialPhase.byName(tag.getString(PHASE_TAG));
        if (snapshot == null || phase == null) {
            return null;
        }
        MindTrialSession session = new MindTrialSession(
                tag.getUUID(TRIAL_ID_TAG),
                tag.getUUID(OWNER_UUID_TAG),
                tag.getInt(POCKET_INDEX_TAG),
                NbtUtils.readBlockPos(tag.getCompound(ARENA_CENTER_TAG)),
                snapshot,
                tag.getLong(STARTED_GAME_TIME_TAG)
        );
        session.phase = phase;
        session.phaseRemainingTicks = Math.max(0, tag.getInt(PHASE_REMAINING_TICKS_TAG));
        if (tag.hasUUID(DEMON_ID_TAG)) {
            session.demonId = tag.getUUID(DEMON_ID_TAG);
        }
        if (tag.hasUUID(BODY_ID_TAG)) {
            session.bodyId = tag.getUUID(BODY_ID_TAG);
        }
        if (tag.contains(RESOLVE_CHOICE_TAG, Tag.TAG_STRING)) {
            session.resolveChoice = ResolveChoice.byName(tag.getString(RESOLVE_CHOICE_TAG));
        }
        session.movementLockUntilGameTime = tag.getLong(MOVEMENT_LOCK_UNTIL_TAG);
        session.forcedSwapCount = tag.getInt(FORCED_SWAP_COUNT_TAG);
        session.lastDemonHealthPct = tag.getFloat(LAST_DEMON_HEALTH_PCT_TAG);
        session.usedMobilitySpell = tag.getBoolean(USED_MOBILITY_SPELL_TAG);
        return session;
    }
}
