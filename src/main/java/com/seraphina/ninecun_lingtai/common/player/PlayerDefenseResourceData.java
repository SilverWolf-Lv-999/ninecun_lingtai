package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerDefenseResourceData {
    private static final String DEFENSE_RESOURCE_KEY = ModMain.MOD_ID + ":defense_resource";
    private static final String CURRENT_KEY = "current";

    private PlayerDefenseResourceData() {
    }

    public static DefenseResourceSnapshot capture(Player player) {
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        int elementMask = PlayerAptitudeData.getSpiritualRoot(player)
                .map(SpiritualRoot::getElementMask)
                .orElse(0);
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(stage);
        if (maximum <= 0) {
            clear(player);
            return DefenseResourceSnapshot.empty(elementMask);
        }
        return new DefenseResourceSnapshot(
                CultivationDefenseUtility.getDefenseResourceType(stage),
                getOrInitializeCurrent(player, maximum),
                maximum,
                elementMask
        );
    }

    public static void clampToStage(Player player) {
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(stage);
        if (maximum <= 0) {
            clear(player);
            return;
        }
        getOrInitializeCurrent(player, maximum);
    }

    public static int getCurrent(Player player) {
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(stage);
        if (maximum <= 0) {
            clear(player);
            return 0;
        }
        return getOrInitializeCurrent(player, maximum);
    }

    public static int repair(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(PlayerCultivationData.getStage(player));
        if (maximum <= 0) {
            clear(player);
            return 0;
        }
        int current = getOrInitializeCurrent(player, maximum);
        int repaired = (int) Math.min(
                (long) maximum - current,
                (long) amount
        );
        if (repaired <= 0) {
            return 0;
        }
        setCurrent(player, current + repaired);
        return repaired;
    }

    public static void refillToMaximum(Player player) {
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(PlayerCultivationData.getStage(player));
        if (maximum <= 0) {
            clear(player);
            return;
        }
        setCurrent(player, maximum);
    }

    public static boolean consume(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(PlayerCultivationData.getStage(player));
        if (maximum <= 0) {
            clear(player);
            return false;
        }
        int current = getOrInitializeCurrent(player, maximum);
        if (current < amount) {
            return false;
        }
        setCurrent(player, current - amount);
        return true;
    }

    public static void deplete(Player player) {
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(PlayerCultivationData.getStage(player));
        if (maximum <= 0) {
            clear(player);
            return;
        }
        setCurrent(player, 0);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(DEFENSE_RESOURCE_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    DEFENSE_RESOURCE_KEY,
                    sourceData.getCompound(DEFENSE_RESOURCE_KEY).copy()
            );
        } else {
            targetPlayer.getPersistentData().remove(DEFENSE_RESOURCE_KEY);
        }
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(DEFENSE_RESOURCE_KEY);
    }

    private static int getOrInitializeCurrent(Player player, int maximum) {
        CompoundTag resourceData = getOrCreateResourceData(player);
        if (!resourceData.contains(CURRENT_KEY, Tag.TAG_ANY_NUMERIC)) {
            resourceData.putInt(CURRENT_KEY, maximum);
            return maximum;
        }
        int current = Math.max(0, Math.min(resourceData.getInt(CURRENT_KEY), maximum));
        if (current != resourceData.getInt(CURRENT_KEY)) {
            resourceData.putInt(CURRENT_KEY, current);
        }
        return current;
    }

    private static void setCurrent(Player player, int current) {
        int maximum = CultivationDefenseUtility.getMaximumDefenseResource(PlayerCultivationData.getStage(player));
        if (maximum <= 0) {
            clear(player);
            return;
        }
        getOrCreateResourceData(player).putInt(CURRENT_KEY, Math.max(0, Math.min(current, maximum)));
    }

    private static CompoundTag getOrCreateResourceData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DEFENSE_RESOURCE_KEY, Tag.TAG_COMPOUND)) {
            persistentData.put(DEFENSE_RESOURCE_KEY, new CompoundTag());
        }
        return persistentData.getCompound(DEFENSE_RESOURCE_KEY);
    }
}
