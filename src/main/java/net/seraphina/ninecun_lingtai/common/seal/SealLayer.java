package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * 单层禁制的服务端权威记录（阵禁系统日志 5.1）。
 * <b>任何字段不下发客户端</b>——自毁阈值、刻印者快照都是破解博弈的秘密。
 */
public final class SealLayer {
    private static final String KIND_TAG = "Kind";
    private static final String STRENGTH_TAG = "Strength";
    private static final String OWNER_SOUL_TAG = "OwnerSoul";
    private static final String OWNER_REALM_TAG = "OwnerRealm";
    private static final String ELEMENT_TAG = "Element";
    private static final String DETONATE_THRESHOLD_TAG = "DetonateThreshold";
    private static final String CARVE_SEQUENCE_TAG = "CarveSequence";
    private static final String LEGACY_MESSAGE_TAG = "LegacyMessage";

    private final SealKind kind;
    /** 刻印时快照：魂强 × (1 + 境界序号 × 0.5) × (1 + 0.1 × 已叠层数)。 */
    private final double strength;
    /** 刻印者魂强星数快照（锁纹难度差用）。 */
    private final int ownerSoulSnapshot;
    /** 刻印者境界序号快照。 */
    private final int ownerRealmOrdinal;
    /** 刻印者主灵根（后续破禁锥克制用；无五行灵根记 CHAOS）。 */
    private final ElementType element;
    /** 仅 DETONATE：0.5-0.9；其余层为 0。 */
    private final double detonateThreshold;
    /** 全局刻印序号（锚位休眠"从新到旧"的排序依据）。 */
    private final long carveSequence;
    /** 仅 LEGACY：主人预录的遗言（服务端专有，绝不进物品 NBT / 任何下发包）。 */
    @Nullable
    private final String legacyMessage;

    public SealLayer(
            SealKind kind,
            double strength,
            int ownerSoulSnapshot,
            int ownerRealmOrdinal,
            ElementType element,
            double detonateThreshold,
            long carveSequence
    ) {
        this(kind, strength, ownerSoulSnapshot, ownerRealmOrdinal, element,
                detonateThreshold, carveSequence, null);
    }

    public SealLayer(
            SealKind kind,
            double strength,
            int ownerSoulSnapshot,
            int ownerRealmOrdinal,
            ElementType element,
            double detonateThreshold,
            long carveSequence,
            @Nullable String legacyMessage
    ) {
        this.kind = kind;
        this.strength = strength;
        this.ownerSoulSnapshot = ownerSoulSnapshot;
        this.ownerRealmOrdinal = ownerRealmOrdinal;
        this.element = element;
        this.detonateThreshold = detonateThreshold;
        this.carveSequence = carveSequence;
        this.legacyMessage = legacyMessage;
    }

    public SealKind kind() {
        return this.kind;
    }

    public double strength() {
        return this.strength;
    }

    public int ownerSoulSnapshot() {
        return this.ownerSoulSnapshot;
    }

    public int ownerRealmOrdinal() {
        return this.ownerRealmOrdinal;
    }

    public ElementType element() {
        return this.element;
    }

    public double detonateThreshold() {
        return this.detonateThreshold;
    }

    public long carveSequence() {
        return this.carveSequence;
    }

    @Nullable
    public String legacyMessage() {
        return this.legacyMessage;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KIND_TAG, this.kind.getSerializedName());
        tag.putDouble(STRENGTH_TAG, this.strength);
        tag.putInt(OWNER_SOUL_TAG, this.ownerSoulSnapshot);
        tag.putInt(OWNER_REALM_TAG, this.ownerRealmOrdinal);
        tag.putString(ELEMENT_TAG, this.element.getSerializedName());
        tag.putDouble(DETONATE_THRESHOLD_TAG, this.detonateThreshold);
        tag.putLong(CARVE_SEQUENCE_TAG, this.carveSequence);
        if (this.legacyMessage != null) {
            tag.putString(LEGACY_MESSAGE_TAG, this.legacyMessage);
        }
        return tag;
    }

    @Nullable
    public static SealLayer fromNbt(CompoundTag tag) {
        SealKind kind = SealKind.fromSerializedName(tag.getString(KIND_TAG)).orElse(null);
        if (kind == null) {
            return null;
        }
        ElementType element = ElementType.fromSerializedName(tag.getString(ELEMENT_TAG))
                .orElse(ElementType.CHAOS);
        return new SealLayer(
                kind,
                tag.getDouble(STRENGTH_TAG),
                tag.getInt(OWNER_SOUL_TAG),
                tag.getInt(OWNER_REALM_TAG),
                element,
                tag.getDouble(DETONATE_THRESHOLD_TAG),
                tag.getLong(CARVE_SEQUENCE_TAG),
                tag.contains(LEGACY_MESSAGE_TAG) ? tag.getString(LEGACY_MESSAGE_TAG) : null
        );
    }
}
