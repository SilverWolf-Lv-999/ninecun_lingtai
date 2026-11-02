package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.common.daotitle.DaoTitleManager;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

public final class PlayerDaoTitleData {
    private static final String DATA_KEY = ModMain.MOD_ID + ":dao_title";
    private static final String CUSTOM_NAME_KEY = "custom_name";
    private static final String IMMORTAL_SUFFIX_KEY = "immortal_suffix";

    private PlayerDaoTitleData() {
    }

    public static String getCustomName(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(DATA_KEY, Tag.TAG_COMPOUND)
                ? data.getCompound(DATA_KEY).getString(CUSTOM_NAME_KEY)
                : "";
    }

    public static DaoTitleSuffix getImmortalSuffix(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return DaoTitleSuffix.IMMORTAL;
        }
        return DaoTitleUtility.getSuffix(data.getCompound(DATA_KEY).getInt(IMMORTAL_SUFFIX_KEY));
    }

    public static void set(Player player, String customName, DaoTitleSuffix immortalSuffix) {
        String value = customName == null ? "" : customName.trim();
        if (value.length() > 32) {
            value = value.substring(0, 32);
        }
        CompoundTag data = player.getPersistentData().getCompound(DATA_KEY);
        data.putString(CUSTOM_NAME_KEY, value);
        data.putInt(IMMORTAL_SUFFIX_KEY, (immortalSuffix == null ? DaoTitleSuffix.IMMORTAL : immortalSuffix).ordinal());
        player.getPersistentData().put(DATA_KEY, data);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.refreshDisplayName();
            serverPlayer.refreshTabListName();
            DaoTitleManager.syncToAll(serverPlayer);
        }
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag data = sourcePlayer.getPersistentData();
        if (data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(DATA_KEY, data.getCompound(DATA_KEY).copy());
        }
    }
}
