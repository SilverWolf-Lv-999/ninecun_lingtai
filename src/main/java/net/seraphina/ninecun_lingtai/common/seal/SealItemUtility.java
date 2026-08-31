package net.seraphina.ninecun_lingtai.common.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 物品侧的 sealId 指针与公开展示快照（复刻 RingStorageUtility.getOrCreateRingId 模式）。
 * 物品 NBT <b>只存这些</b>：sealId、公开品阶、层存在标记、死契标记——
 * 层类型/阈值/纹骨等全部秘密只在 {@link SealRecordSavedData}。
 */
public final class SealItemUtility {
    private static final String SEAL_ID_TAG = "NinecunSealId";
    private static final String DISPLAY_SEALED_TAG = "NinecunSealDisplaySealed";
    private static final String DISPLAY_GRADE_TAG = "NinecunSealDisplayGrade";
    private static final String DISPLAY_DEATH_PACT_TAG = "NinecunSealDisplayDeathPact";

    private SealItemUtility() {
    }

    public static UUID getOrCreateSealId(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.hasUUID(SEAL_ID_TAG)) {
            return tag.getUUID(SEAL_ID_TAG);
        }
        UUID sealId = UUID.randomUUID();
        tag.putUUID(SEAL_ID_TAG, sealId);
        return sealId;
    }

    public static Optional<UUID> getSealId(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.hasUUID(SEAL_ID_TAG)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(SEAL_ID_TAG));
    }

    /** 客户端 tooltip 仅凭此快照渲染（层数是否存在 + 公开品阶 + 死契标记，无层内容）。 */
    public static void writeDisplaySnapshot(
            ItemStack itemStack,
            boolean sealed,
            int publicGrade,
            boolean deathPact
    ) {
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putBoolean(DISPLAY_SEALED_TAG, sealed);
        tag.putInt(DISPLAY_GRADE_TAG, publicGrade);
        tag.putBoolean(DISPLAY_DEATH_PACT_TAG, deathPact);
    }

    public static boolean isDisplaySealed(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && tag.getBoolean(DISPLAY_SEALED_TAG);
    }

    public static int getDisplayGrade(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag == null ? 0 : tag.getInt(DISPLAY_GRADE_TAG);
    }

    public static boolean isDisplayDeathPact(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && tag.getBoolean(DISPLAY_DEATH_PACT_TAG);
    }

    /** 残骸读取完毕/记录回收后清掉物品上的指针与展示快照。 */
    public static void clearSealTags(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(SEAL_ID_TAG);
        tag.remove(DISPLAY_SEALED_TAG);
        tag.remove(DISPLAY_GRADE_TAG);
        tag.remove(DISPLAY_DEATH_PACT_TAG);
    }
}
