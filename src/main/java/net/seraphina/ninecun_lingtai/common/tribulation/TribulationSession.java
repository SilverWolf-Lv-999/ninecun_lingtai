package net.seraphina.ninecun_lingtai.common.tribulation;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TribulationSession {
    private static final String PLAYER_ID_TAG = "PlayerId";
    private static final String TYPE_TAG = "Type";
    private static final String CENTER_X_TAG = "CenterX";
    private static final String CENTER_Y_TAG = "CenterY";
    private static final String CENTER_Z_TAG = "CenterZ";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String APTITUDE_SCORE_TAG = "AptitudeScore";
    private static final String RADIUS_TAG = "Radius";
    private static final String TARGET_REALM_TAG = "TargetRealm";
    private static final String TARGET_LAYER_TAG = "TargetLayer";
    private static final String REALM_MULTIPLIER_TAG = "RealmMultiplier";
    private static final String PHASE_TAG = "Phase";
    private static final String PHASE_REMAINING_TICKS_TAG = "PhaseRemainingTicks";
    private static final String COMPLETED_STRIKES_TAG = "CompletedStrikes";
    private static final String INNER_DEMON_IDS_TAG = "InnerDemonIds";
    private static final String TRIAL_TOKEN_TAG = "TrialToken";
    private static final String IN_MIND_TRIAL_TAG = "InMindTrial";

    private final UUID playerId;
    private final TribulationType type;
    private final Vec3 center;
    private final ResourceKey<Level> dimension;
    private final float aptitudeScore;
    private final double radius;
    private final CultivationStage targetStage;
    private final double realmMultiplier;
    private final List<UUID> innerDemonIds = new ArrayList<>();
    private TribulationPhase phase;
    private int phaseRemainingTicks;
    private int completedStrikes;
    /** Transient marker, never serialized: true until the first tick after a disk restore. */
    private boolean restoredFromDisk;
    /**
     * 心魔即己：泛化试炼令牌，指向 {@code MindTrialSavedData} 中的 MindTrial 会话。
     * legacy Vex 路径不使用（沿用 {@link #innerDemonIds}，R7 兼容保留）。
     */
    private UUID trialToken;
    /**
     * R1：玩家本体在心境维度时置 true——短路 {@code TribulationManager.tick} 的
     * "维度不符即 fail" 与 "拽回 center" 两处，否则传送进心境即判失败。
     */
    private boolean inMindTrial;

    public TribulationSession(
            UUID playerId,
            TribulationType type,
            Vec3 center,
            ResourceKey<Level> dimension,
            float aptitudeScore,
            double radius,
            int preparationTicks,
            CultivationStage targetStage,
            double realmMultiplier
    ) {
        this(
                playerId,
                type,
                center,
                dimension,
                aptitudeScore,
                radius,
                targetStage,
                realmMultiplier,
                TribulationPhase.PREPARING,
                preparationTicks,
                0,
                false
        );
    }

    private TribulationSession(
            UUID playerId,
            TribulationType type,
            Vec3 center,
            ResourceKey<Level> dimension,
            float aptitudeScore,
            double radius,
            CultivationStage targetStage,
            double realmMultiplier,
            TribulationPhase phase,
            int phaseRemainingTicks,
            int completedStrikes,
            boolean restoredFromDisk
    ) {
        this.playerId = playerId;
        this.type = type;
        this.center = center;
        this.dimension = dimension;
        this.aptitudeScore = aptitudeScore;
        this.radius = radius;
        this.targetStage = targetStage;
        this.realmMultiplier = realmMultiplier;
        this.phase = phase;
        this.phaseRemainingTicks = phaseRemainingTicks;
        this.completedStrikes = completedStrikes;
        this.restoredFromDisk = restoredFromDisk;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public TribulationType type() {
        return this.type;
    }

    public Vec3 center() {
        return this.center;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public float aptitudeScore() {
        return this.aptitudeScore;
    }

    public double radius() {
        return this.radius;
    }

    public TribulationPhase phase() {
        return this.phase;
    }

    public int phaseRemainingTicks() {
        return this.phaseRemainingTicks;
    }

    public int completedStrikes() {
        return this.completedStrikes;
    }

    public void tickPhase() {
        this.phaseRemainingTicks = Math.max(0, this.phaseRemainingTicks - 1);
    }

    public CultivationStage targetStage() {
        return this.targetStage;
    }

    public double realmMultiplier() {
        return this.realmMultiplier;
    }

    public List<UUID> innerDemonIds() {
        return this.innerDemonIds;
    }

    public UUID trialToken() {
        return this.trialToken;
    }

    public void setTrialToken(UUID trialToken) {
        this.trialToken = trialToken;
    }

    public boolean isInMindTrial() {
        return this.inMindTrial;
    }

    public void setInMindTrial(boolean inMindTrial) {
        this.inMindTrial = inMindTrial;
    }

    public void startStriking(int intervalTicks) {
        this.phase = TribulationPhase.STRIKING;
        this.phaseRemainingTicks = intervalTicks;
    }

    public void startInnerDemon(int durationTicks) {
        this.phase = TribulationPhase.INNER_DEMON;
        this.phaseRemainingTicks = durationTicks;
    }

    public void startAftermath(int aftermathTicks) {
        this.phase = TribulationPhase.AFTERMATH;
        this.phaseRemainingTicks = aftermathTicks;
    }

    public void startDissipating(int dissipatingTicks) {
        this.phase = TribulationPhase.DISSIPATING;
        this.phaseRemainingTicks = dissipatingTicks;
    }

    public void completeStrike() {
        this.completedStrikes++;
    }

    /**
     * Returns true exactly once after this session was deserialized from disk, so the manager
     * can re-synchronize clients and re-summon inner demons before resuming phase logic.
     */
    public boolean consumeRestoredFlag() {
        boolean restored = this.restoredFromDisk;
        this.restoredFromDisk = false;
        return restored;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER_ID_TAG, this.playerId);
        tag.putString(TYPE_TAG, this.type.name());
        tag.putDouble(CENTER_X_TAG, this.center.x);
        tag.putDouble(CENTER_Y_TAG, this.center.y);
        tag.putDouble(CENTER_Z_TAG, this.center.z);
        tag.putString(DIMENSION_TAG, this.dimension.location().toString());
        tag.putFloat(APTITUDE_SCORE_TAG, this.aptitudeScore);
        tag.putDouble(RADIUS_TAG, this.radius);
        tag.putString(TARGET_REALM_TAG, this.targetStage.realm().getSerializedName());
        tag.putInt(TARGET_LAYER_TAG, this.targetStage.layer());
        tag.putDouble(REALM_MULTIPLIER_TAG, this.realmMultiplier);
        tag.putString(PHASE_TAG, this.phase.name());
        tag.putInt(PHASE_REMAINING_TICKS_TAG, this.phaseRemainingTicks);
        tag.putInt(COMPLETED_STRIKES_TAG, this.completedStrikes);
        ListTag innerDemonTags = new ListTag();
        for (UUID innerDemonId : this.innerDemonIds) {
            innerDemonTags.add(NbtUtils.createUUID(innerDemonId));
        }
        tag.put(INNER_DEMON_IDS_TAG, innerDemonTags);
        if (this.trialToken != null) {
            tag.putUUID(TRIAL_TOKEN_TAG, this.trialToken);
        }
        tag.putBoolean(IN_MIND_TRIAL_TAG, this.inMindTrial);
        return tag;
    }

    /**
     * Recreates a persisted session. Returns null when the tag is malformed (unknown enum
     * names, missing player id or dimension), in which case the session is dropped.
     */
    public static TribulationSession fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(PLAYER_ID_TAG)) {
            return null;
        }
        TribulationType type = parseEnum(TribulationType.values(), tag.getString(TYPE_TAG));
        TribulationPhase phase = parseEnum(TribulationPhase.values(), tag.getString(PHASE_TAG));
        CultivationRealm targetRealm = CultivationRealm
                .fromSerializedName(tag.getString(TARGET_REALM_TAG))
                .orElse(null);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_TAG));
        if (type == null || phase == null || targetRealm == null || dimensionId == null) {
            return null;
        }
        int targetLayer = Mth.clamp(
                tag.getInt(TARGET_LAYER_TAG),
                CultivationStage.MINIMUM_LAYER,
                CultivationStage.MAXIMUM_LAYER
        );
        TribulationSession session = new TribulationSession(
                tag.getUUID(PLAYER_ID_TAG),
                type,
                new Vec3(
                        tag.getDouble(CENTER_X_TAG),
                        tag.getDouble(CENTER_Y_TAG),
                        tag.getDouble(CENTER_Z_TAG)
                ),
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                tag.getFloat(APTITUDE_SCORE_TAG),
                tag.getDouble(RADIUS_TAG),
                new CultivationStage(targetRealm, targetLayer),
                tag.getDouble(REALM_MULTIPLIER_TAG),
                phase,
                Math.max(0, tag.getInt(PHASE_REMAINING_TICKS_TAG)),
                Math.max(0, tag.getInt(COMPLETED_STRIKES_TAG)),
                true
        );
        for (Tag innerDemonTag : tag.getList(INNER_DEMON_IDS_TAG, Tag.TAG_INT_ARRAY)) {
            session.innerDemonIds.add(NbtUtils.loadUUID(innerDemonTag));
        }
        // 旧存档没有这两个键：读出为 null/false，legacy 行为不变（R7）。
        if (tag.hasUUID(TRIAL_TOKEN_TAG)) {
            session.trialToken = tag.getUUID(TRIAL_TOKEN_TAG);
        }
        session.inMindTrial = tag.getBoolean(IN_MIND_TRIAL_TAG);
        return session;
    }

    private static <T extends Enum<T>> T parseEnum(T[] values, String name) {
        for (T value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
