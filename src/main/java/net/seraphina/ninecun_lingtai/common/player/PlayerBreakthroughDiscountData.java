package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerBreakthroughDiscountData {
    private static final String DISCOUNT_KEY = ModMain.MOD_ID + ":breakthrough_discount";
    private static final String DISCOUNT_VALUE_KEY = "Discount";
    private static final String EXPIRE_GAME_TIME_KEY = "ExpireGameTime";

    public static double getDiscount(Player player, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DISCOUNT_KEY, Tag.TAG_COMPOUND)) {
            return 0.0D;
        }
        CompoundTag discountData = persistentData.getCompound(DISCOUNT_KEY);
        if (discountData.getLong(EXPIRE_GAME_TIME_KEY) < gameTime) {
            persistentData.remove(DISCOUNT_KEY);
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(0.9D, discountData.getDouble(DISCOUNT_VALUE_KEY)));
    }

    public static void applyDiscount(Player player, double discount, long expireGameTime) {
        CompoundTag persistentData = player.getPersistentData();
        double currentDiscount = persistentData.contains(DISCOUNT_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(DISCOUNT_KEY).getDouble(DISCOUNT_VALUE_KEY)
                : 0.0D;
        CompoundTag discountData = new CompoundTag();
        discountData.putDouble(DISCOUNT_VALUE_KEY, Math.max(currentDiscount, discount));
        discountData.putLong(EXPIRE_GAME_TIME_KEY, expireGameTime);
        persistentData.put(DISCOUNT_KEY, discountData);
    }

    public static void clearDiscount(Player player) {
        player.getPersistentData().remove(DISCOUNT_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(DISCOUNT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    DISCOUNT_KEY,
                    sourceData.getCompound(DISCOUNT_KEY).copy()
            );
        }
    }
}
