package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleColors;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.common.ai.DaoTitleColorService;
import net.seraphina.ninecun_lingtai.common.daotitle.DaoTitleManager;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

public final class PlayerDaoTitleData {
    private static final String DATA_KEY = ModMain.MOD_ID + ":dao_title";
    private static final String CUSTOM_NAME_KEY = "custom_name";
    private static final String IMMORTAL_SUFFIX_KEY = "immortal_suffix";
    private static final String FIRST_COLOR_KEY = "first_color";
    private static final String SECOND_COLOR_KEY = "second_color";

    private PlayerDaoTitleData() {
    }

    public static String getCustomName(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return "";
        }
        String customName = data.getCompound(DATA_KEY).getString(CUSTOM_NAME_KEY);
        return DaoTitleUtility.isValidCustomName(customName) ? customName : "";
    }

    public static DaoTitleSuffix getImmortalSuffix(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return DaoTitleSuffix.IMMORTAL;
        }
        return DaoTitleUtility.getSuffix(data.getCompound(DATA_KEY).getInt(IMMORTAL_SUFFIX_KEY));
    }

    public static DaoTitleColors getColors(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return DaoTitleColors.defaults();
        }
        CompoundTag titleData = data.getCompound(DATA_KEY);
        if (!titleData.contains(FIRST_COLOR_KEY, Tag.TAG_ANY_NUMERIC)
                || !titleData.contains(SECOND_COLOR_KEY, Tag.TAG_ANY_NUMERIC)) {
            return DaoTitleColors.defaults();
        }
        return new DaoTitleColors(titleData.getInt(FIRST_COLOR_KEY), titleData.getInt(SECOND_COLOR_KEY));
    }

    public static void set(Player player, String customName, DaoTitleSuffix immortalSuffix) {
        String value = customName == null ? "" : customName.trim();
        if (!value.isEmpty() && !DaoTitleUtility.isValidCustomName(value)) {
            return;
        }
        CompoundTag data = player.getPersistentData().getCompound(DATA_KEY);
        data.putString(CUSTOM_NAME_KEY, value);
        data.putInt(IMMORTAL_SUFFIX_KEY, (immortalSuffix == null ? DaoTitleSuffix.IMMORTAL : immortalSuffix).ordinal());
        data.remove(FIRST_COLOR_KEY);
        data.remove(SECOND_COLOR_KEY);
        player.getPersistentData().put(DATA_KEY, data);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.refreshDisplayName();
            serverPlayer.refreshTabListName();
            DaoTitleManager.syncToAll(serverPlayer);
            DaoTitleColorService.schedule(serverPlayer);
        }
    }

    public static void setColors(Player player, DaoTitleColors colors) {
        CompoundTag data = player.getPersistentData().getCompound(DATA_KEY);
        data.putInt(FIRST_COLOR_KEY, colors.firstColor());
        data.putInt(SECOND_COLOR_KEY, colors.secondColor());
        player.getPersistentData().put(DATA_KEY, data);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag data = sourcePlayer.getPersistentData();
        if (data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(DATA_KEY, data.getCompound(DATA_KEY).copy());
        }
    }
}
