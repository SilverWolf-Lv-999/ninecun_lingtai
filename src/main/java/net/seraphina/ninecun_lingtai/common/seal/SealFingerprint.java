package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;

/**
 * 神念指纹（阵禁篇 5.4）：破解者魂强星数 + 大境界 + 主灵根快照。
 * <b>不存玩家名/UUID</b>——凶手是谁要靠特征在服务器里自己排查（信息敬畏）。
 *
 * @param soulStars    破解者魂强星数
 * @param realmOrdinal 破解者大境界序号（-1 = 无修为）
 * @param element      破解者主灵根
 * @param calendarTick 留痕修真历刻
 * @param successful   该次破解是否成功
 */
public record SealFingerprint(
        int soulStars,
        int realmOrdinal,
        ElementType element,
        long calendarTick,
        boolean successful
) {
    private static final String SOUL_TAG = "Soul";
    private static final String REALM_TAG = "Realm";
    private static final String ELEMENT_TAG = "Element";
    private static final String TICK_TAG = "Tick";
    private static final String SUCCESS_TAG = "Success";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SOUL_TAG, this.soulStars);
        tag.putInt(REALM_TAG, this.realmOrdinal);
        tag.putString(ELEMENT_TAG, this.element.getSerializedName());
        tag.putLong(TICK_TAG, this.calendarTick);
        tag.putBoolean(SUCCESS_TAG, this.successful);
        return tag;
    }

    public static SealFingerprint fromNbt(CompoundTag tag) {
        return new SealFingerprint(
                tag.getInt(SOUL_TAG),
                tag.getInt(REALM_TAG),
                ElementType.fromSerializedName(tag.getString(ELEMENT_TAG)).orElse(ElementType.CHAOS),
                tag.getLong(TICK_TAG),
                tag.getBoolean(SUCCESS_TAG)
        );
    }
}
