package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerSoulTemperingData {
    private static final String SOUL_TEMPERING_KEY = ModMain.MOD_ID + ":soul_tempering";
    private static final String PROGRESS_KEY = "progress";

    public static double getProgress(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SOUL_TEMPERING_KEY, Tag.TAG_COMPOUND)) {
            return 0.0D;
        }
        double progress = persistentData.getCompound(SOUL_TEMPERING_KEY).getDouble(PROGRESS_KEY);
        return Double.isFinite(progress) && progress > 0.0D ? progress : 0.0D;
    }

    public static void setProgress(Player player, double progress) {
        if (!Double.isFinite(progress) || progress < 0.0D) {
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag soulTemperingData = persistentData.contains(SOUL_TEMPERING_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SOUL_TEMPERING_KEY)
                : new CompoundTag();
        soulTemperingData.putDouble(PROGRESS_KEY, progress);
        persistentData.put(SOUL_TEMPERING_KEY, soulTemperingData);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SOUL_TEMPERING_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SOUL_TEMPERING_KEY,
                    sourceData.getCompound(SOUL_TEMPERING_KEY).copy()
            );
        }
    }
}
