package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerSpiritInfusionData {
    public static final double INFUSION_CAP_FRACTION = 0.30D;

    private static final String INFUSION_KEY = ModMain.MOD_ID + ":spirit_infusion_used";
    private static final String STAGE_KEY = "Stage";
    private static final String USED_KEY = "Used";

    public static double getUsed(Player player, CultivationStage stage) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(INFUSION_KEY, Tag.TAG_COMPOUND)) {
            return 0.0D;
        }
        CompoundTag infusionData = persistentData.getCompound(INFUSION_KEY);
        if (!getStageKey(stage).equals(infusionData.getString(STAGE_KEY))) {
            return 0.0D;
        }
        return Math.max(0.0D, infusionData.getDouble(USED_KEY));
    }

    public static void addUsed(Player player, CultivationStage stage, double amount) {
        CompoundTag infusionData = new CompoundTag();
        infusionData.putString(STAGE_KEY, getStageKey(stage));
        infusionData.putDouble(USED_KEY, getUsed(player, stage) + Math.max(0.0D, amount));
        player.getPersistentData().put(INFUSION_KEY, infusionData);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(INFUSION_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    INFUSION_KEY,
                    sourceData.getCompound(INFUSION_KEY).copy()
            );
        }
    }

    private static String getStageKey(CultivationStage stage) {
        return stage.realm().getSerializedName() + ":" + stage.layer();
    }
}
