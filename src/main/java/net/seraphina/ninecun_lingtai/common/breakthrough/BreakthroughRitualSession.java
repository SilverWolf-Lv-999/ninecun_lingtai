package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 单次引灵仪式的可变会话对象。服务端唯一权威，随
 * {@link BreakthroughRitualSavedData} 持久化（防登出/重启白嫖，设计文档 §5.5）。
 * 掷骰之后的中间态不落盘——掷骰与后续分派在同一 tick 内完成。
 */
public final class BreakthroughRitualSession {
    private static final String PLAYER_ID_TAG = "PlayerId";
    private static final String TARGET_REALM_TAG = "TargetRealm";
    private static final String TARGET_LAYER_TAG = "TargetLayer";
    private static final String PHASE_TAG = "Phase";
    private static final String REMAINING_TICKS_TAG = "RemainingTicks";
    private static final String TOTAL_TICKS_TAG = "TotalTicks";
    private static final String REQUIRED_SNAPSHOT_TAG = "RequiredSnapshot";
    private static final String FINAL_CHANCE_TAG = "FinalChance";
    private static final String GRASP_BUCKET_TAG = "GraspBucketKey";
    private static final String DRAGGING_ID_TAG = "DraggingModifierId";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String CENTER_X_TAG = "CenterX";
    private static final String CENTER_Y_TAG = "CenterY";
    private static final String CENTER_Z_TAG = "CenterZ";
    private static final String CHANCE_SEED_NONCE_TAG = "ChanceSeedNonce";
    private static final String PHASE_RITUAL = "ritual";

    private final UUID playerId;
    private final CultivationStage targetStage;
    private final int totalTicks;
    /** 仪式启动时锁定的折扣后灵力需求（防仪式中吃丹改折扣，§5.4）。 */
    private final double requiredSnapshot;
    /** 仪式启动时锁定的最终成功率（掷骰用，绝不下发客户端）。 */
    private final double finalChance;
    private final String graspBucketKey;
    /** 最拖后腿修正项 id，仅魂强 ≥ 5 星时非空（R6）。 */
    private final String draggingModifierId;
    private final ResourceKey<Level> dimension;
    private final Vec3 center;
    private final long chanceSeedNonce;

    private int remainingTicks;
    /** 从磁盘恢复的会话首个 tick 走中断轻档结算（服务器重启视为仪式被打断）。 */
    private boolean restoredFromDisk;

    public BreakthroughRitualSession(
            UUID playerId,
            CultivationStage targetStage,
            int totalTicks,
            double requiredSnapshot,
            double finalChance,
            String graspBucketKey,
            String draggingModifierId,
            ResourceKey<Level> dimension,
            Vec3 center,
            long chanceSeedNonce
    ) {
        this.playerId = playerId;
        this.targetStage = targetStage;
        this.totalTicks = totalTicks;
        this.remainingTicks = totalTicks;
        this.requiredSnapshot = requiredSnapshot;
        this.finalChance = finalChance;
        this.graspBucketKey = graspBucketKey;
        this.draggingModifierId = draggingModifierId == null ? "" : draggingModifierId;
        this.dimension = dimension;
        this.center = center;
        this.chanceSeedNonce = chanceSeedNonce;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public CultivationStage targetStage() {
        return this.targetStage;
    }

    public int totalTicks() {
        return this.totalTicks;
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public double requiredSnapshot() {
        return this.requiredSnapshot;
    }

    public double finalChance() {
        return this.finalChance;
    }

    public String graspBucketKey() {
        return this.graspBucketKey;
    }

    public String draggingModifierId() {
        return this.draggingModifierId;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public Vec3 center() {
        return this.center;
    }

    public long chanceSeedNonce() {
        return this.chanceSeedNonce;
    }

    /** 每 tick 递减，返回递减后的剩余刻。 */
    public int tickDown() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
        }
        return this.remainingTicks;
    }

    public boolean consumeRestoredFlag() {
        boolean restored = this.restoredFromDisk;
        this.restoredFromDisk = false;
        return restored;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER_ID_TAG, this.playerId);
        tag.putString(TARGET_REALM_TAG, this.targetStage.realm().getSerializedName());
        tag.putInt(TARGET_LAYER_TAG, this.targetStage.layer());
        tag.putString(PHASE_TAG, PHASE_RITUAL);
        tag.putInt(REMAINING_TICKS_TAG, this.remainingTicks);
        tag.putInt(TOTAL_TICKS_TAG, this.totalTicks);
        tag.putDouble(REQUIRED_SNAPSHOT_TAG, this.requiredSnapshot);
        tag.putDouble(FINAL_CHANCE_TAG, this.finalChance);
        tag.putString(GRASP_BUCKET_TAG, this.graspBucketKey);
        tag.putString(DRAGGING_ID_TAG, this.draggingModifierId);
        tag.putString(DIMENSION_TAG, this.dimension.location().toString());
        tag.putDouble(CENTER_X_TAG, this.center.x);
        tag.putDouble(CENTER_Y_TAG, this.center.y);
        tag.putDouble(CENTER_Z_TAG, this.center.z);
        tag.putLong(CHANCE_SEED_NONCE_TAG, this.chanceSeedNonce);
        return tag;
    }

    /** 结构损坏（缺玩家 id、未知境界、坏维度）时返回 null，读取方丢弃该条。 */
    @Nullable
    public static BreakthroughRitualSession fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(PLAYER_ID_TAG)) {
            return null;
        }
        CultivationRealm targetRealm = CultivationRealm
                .fromSerializedName(tag.getString(TARGET_REALM_TAG))
                .orElse(null);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_TAG));
        if (targetRealm == null || dimensionId == null) {
            return null;
        }
        int targetLayer = Mth.clamp(
                tag.getInt(TARGET_LAYER_TAG),
                CultivationStage.MINIMUM_LAYER,
                CultivationStage.MAXIMUM_LAYER
        );
        BreakthroughRitualSession session = new BreakthroughRitualSession(
                tag.getUUID(PLAYER_ID_TAG),
                new CultivationStage(targetRealm, targetLayer),
                Math.max(1, tag.getInt(TOTAL_TICKS_TAG)),
                tag.getDouble(REQUIRED_SNAPSHOT_TAG),
                tag.getDouble(FINAL_CHANCE_TAG),
                tag.getString(GRASP_BUCKET_TAG),
                tag.getString(DRAGGING_ID_TAG),
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                new Vec3(
                        tag.getDouble(CENTER_X_TAG),
                        tag.getDouble(CENTER_Y_TAG),
                        tag.getDouble(CENTER_Z_TAG)
                ),
                tag.getLong(CHANCE_SEED_NONCE_TAG)
        );
        session.remainingTicks = Mth.clamp(tag.getInt(REMAINING_TICKS_TAG), 0, session.totalTicks);
        session.restoredFromDisk = true;
        return session;
    }
}
