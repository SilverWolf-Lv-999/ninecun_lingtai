package net.seraphina.ninecun_lingtai.common.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一件物品的完整禁制权威记录（服务端专有，物品 NBT 只存 sealId 指针）。
 *
 * <p>层列表：index 0 = 最外层；破解由外向内逐层剥。全部层破尽后记录进入
 * "残骸"态（layers 空、指纹留存），主人触碰读取指纹后回收。</p>
 */
public final class SealRecord {
    private static final String SEAL_ID_TAG = "SealId";
    private static final String OWNER_TAG = "Owner";
    private static final String LAYERS_TAG = "Layers";
    private static final String FINGERPRINTS_TAG = "Fingerprints";
    private static final String DEATH_PACT_UNTIL_TAG = "DeathPactUntil";
    private static final String DECAY_DEADLINE_TAG = "DecayDeadline";
    private static final String DISTURB_COUNT_TAG = "DisturbCount";
    private static final String DISTURB_LOCK_UNTIL_TAG = "DisturbLockUntil";
    private static final String LAST_SEEN_DIMENSION_TAG = "LastSeenDimension";
    private static final String LAST_SEEN_X_TAG = "LastSeenX";
    private static final String LAST_SEEN_Y_TAG = "LastSeenY";
    private static final String LAST_SEEN_Z_TAG = "LastSeenZ";

    private final UUID sealId;
    private final UUID ownerUuid;
    private final List<SealLayer> layers = new ArrayList<>();
    private final List<SealFingerprint> fingerprints = new ArrayList<>();
    /** 死契到期修真历刻，0 = 无死契。 */
    private long deathPactUntilCalendarTick;
    /** 魂飞魄散衰减期到期刻（本期只存不判）。 */
    private long decayDeadlineCalendarTick;
    /** 惊扰值（0-3；满 3 锁死巧破 1 修真日）。 */
    private int disturbCount;
    private long disturbLockUntilCalendarTick;
    /** 最后一次被感知的位置（死契方位感知用；由触碰/破解/掉落事件更新）。 */
    @Nullable
    private String lastSeenDimension;
    private int lastSeenX;
    private int lastSeenY;
    private int lastSeenZ;

    public SealRecord(UUID sealId, UUID ownerUuid) {
        this.sealId = sealId;
        this.ownerUuid = ownerUuid;
    }

    public UUID sealId() {
        return this.sealId;
    }

    public UUID ownerUuid() {
        return this.ownerUuid;
    }

    /** 可修改的层列表（index 0 = 最外层）。仅 SealManager 变更后须 setDirty。 */
    public List<SealLayer> layers() {
        return this.layers;
    }

    public List<SealFingerprint> fingerprints() {
        return this.fingerprints;
    }

    /** 层破尽但指纹尚未被主人读取的"残骸"态。 */
    public boolean isBrokenResidue() {
        return this.layers.isEmpty() && !this.fingerprints.isEmpty();
    }

    /** 全部层强度之和（力破碾压线的比较基数）。 */
    public double totalStrength(boolean deathPactActive) {
        double sum = 0.0D;
        for (SealLayer layer : this.layers) {
            sum += layer.strength();
        }
        return deathPactActive ? sum * 1.5D : sum;
    }

    public long deathPactUntilCalendarTick() {
        return this.deathPactUntilCalendarTick;
    }

    public void setDeathPactUntilCalendarTick(long calendarTick) {
        this.deathPactUntilCalendarTick = calendarTick;
    }

    public boolean isDeathPactActive(long nowCalendarTick) {
        return this.deathPactUntilCalendarTick > nowCalendarTick;
    }

    public long decayDeadlineCalendarTick() {
        return this.decayDeadlineCalendarTick;
    }

    public void setDecayDeadlineCalendarTick(long calendarTick) {
        this.decayDeadlineCalendarTick = calendarTick;
    }

    public int disturbCount() {
        return this.disturbCount;
    }

    public void setDisturbCount(int disturbCount) {
        this.disturbCount = disturbCount;
    }

    public long disturbLockUntilCalendarTick() {
        return this.disturbLockUntilCalendarTick;
    }

    public void setDisturbLockUntilCalendarTick(long calendarTick) {
        this.disturbLockUntilCalendarTick = calendarTick;
    }

    public boolean isDisturbLocked(long nowCalendarTick) {
        return this.disturbLockUntilCalendarTick > nowCalendarTick;
    }

    public void updateLastSeen(ResourceLocation dimension, BlockPos position) {
        this.lastSeenDimension = dimension.toString();
        this.lastSeenX = position.getX();
        this.lastSeenY = position.getY();
        this.lastSeenZ = position.getZ();
    }

    @Nullable
    public String lastSeenDimension() {
        return this.lastSeenDimension;
    }

    public BlockPos lastSeenPosition() {
        return new BlockPos(this.lastSeenX, this.lastSeenY, this.lastSeenZ);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(SEAL_ID_TAG, this.sealId);
        tag.putUUID(OWNER_TAG, this.ownerUuid);
        ListTag layerTags = new ListTag();
        for (SealLayer layer : this.layers) {
            layerTags.add(layer.toNbt());
        }
        tag.put(LAYERS_TAG, layerTags);
        ListTag fingerprintTags = new ListTag();
        for (SealFingerprint fingerprint : this.fingerprints) {
            fingerprintTags.add(fingerprint.toNbt());
        }
        tag.put(FINGERPRINTS_TAG, fingerprintTags);
        tag.putLong(DEATH_PACT_UNTIL_TAG, this.deathPactUntilCalendarTick);
        tag.putLong(DECAY_DEADLINE_TAG, this.decayDeadlineCalendarTick);
        tag.putInt(DISTURB_COUNT_TAG, this.disturbCount);
        tag.putLong(DISTURB_LOCK_UNTIL_TAG, this.disturbLockUntilCalendarTick);
        if (this.lastSeenDimension != null) {
            tag.putString(LAST_SEEN_DIMENSION_TAG, this.lastSeenDimension);
            tag.putInt(LAST_SEEN_X_TAG, this.lastSeenX);
            tag.putInt(LAST_SEEN_Y_TAG, this.lastSeenY);
            tag.putInt(LAST_SEEN_Z_TAG, this.lastSeenZ);
        }
        return tag;
    }

    @Nullable
    public static SealRecord fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(SEAL_ID_TAG) || !tag.hasUUID(OWNER_TAG)) {
            return null;
        }
        SealRecord record = new SealRecord(tag.getUUID(SEAL_ID_TAG), tag.getUUID(OWNER_TAG));
        ListTag layerTags = tag.getList(LAYERS_TAG, Tag.TAG_COMPOUND);
        for (Tag layerTag : layerTags) {
            if (layerTag instanceof CompoundTag layerDataTag) {
                SealLayer layer = SealLayer.fromNbt(layerDataTag);
                if (layer != null) {
                    record.layers.add(layer);
                }
            }
        }
        ListTag fingerprintTags = tag.getList(FINGERPRINTS_TAG, Tag.TAG_COMPOUND);
        for (Tag fingerprintTag : fingerprintTags) {
            if (fingerprintTag instanceof CompoundTag fingerprintDataTag) {
                record.fingerprints.add(SealFingerprint.fromNbt(fingerprintDataTag));
            }
        }
        record.deathPactUntilCalendarTick = tag.getLong(DEATH_PACT_UNTIL_TAG);
        record.decayDeadlineCalendarTick = tag.getLong(DECAY_DEADLINE_TAG);
        record.disturbCount = tag.getInt(DISTURB_COUNT_TAG);
        record.disturbLockUntilCalendarTick = tag.getLong(DISTURB_LOCK_UNTIL_TAG);
        if (tag.contains(LAST_SEEN_DIMENSION_TAG, Tag.TAG_STRING)) {
            record.lastSeenDimension = tag.getString(LAST_SEEN_DIMENSION_TAG);
            record.lastSeenX = tag.getInt(LAST_SEEN_X_TAG);
            record.lastSeenY = tag.getInt(LAST_SEEN_Y_TAG);
            record.lastSeenZ = tag.getInt(LAST_SEEN_Z_TAG);
        }
        return record;
    }
}
