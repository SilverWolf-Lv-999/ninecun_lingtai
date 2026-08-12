package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家御剑飞行状态（persistent NBT）：是否御剑中、脚下之剑锁定的背包槽位与剑的快照。
 * 死亡时不随 clone 复制——人亡剑落，御剑状态自然消散。
 */
public final class PlayerSwordFlightData {
    private static final String SWORD_FLIGHT_KEY = ModMain.MOD_ID + ":sword_flight";
    private static final String SLOT_KEY = "slot";
    private static final String SWORD_KEY = "sword";

    private PlayerSwordFlightData() {
    }

    public static boolean isFlying(Player player) {
        return player.getPersistentData().contains(SWORD_FLIGHT_KEY, Tag.TAG_COMPOUND);
    }

    public static void start(Player player, int slot, ItemStack sword) {
        CompoundTag flightData = new CompoundTag();
        flightData.putInt(SLOT_KEY, slot);
        flightData.put(SWORD_KEY, sword.copyWithCount(1).save(new CompoundTag()));
        player.getPersistentData().put(SWORD_FLIGHT_KEY, flightData);
    }

    public static void stop(Player player) {
        player.getPersistentData().remove(SWORD_FLIGHT_KEY);
    }

    public static int getLockedSlot(Player player) {
        if (!isFlying(player)) {
            return -1;
        }
        return player.getPersistentData().getCompound(SWORD_FLIGHT_KEY).getInt(SLOT_KEY);
    }

    /** 登剑时的剑快照，仅用于展示与匹配剑的种类；耐久变化不影响锁定。 */
    public static ItemStack getSword(Player player) {
        if (!isFlying(player)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(player.getPersistentData().getCompound(SWORD_FLIGHT_KEY).getCompound(SWORD_KEY));
    }
}
