package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class PlayerTechniqueProficiencyData {
    private static final String PROFICIENCY_KEY = ModMain.MOD_ID + ":technique_proficiency";

    public static Map<ResourceLocation, Long> getAll(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(PROFICIENCY_KEY, Tag.TAG_COMPOUND)) {
            return Map.of();
        }
        CompoundTag proficiencyData = persistentData.getCompound(PROFICIENCY_KEY);
        Map<ResourceLocation, Long> proficiencies = new HashMap<>();
        for (String key : proficiencyData.getAllKeys()) {
            ResourceLocation techniqueId = ResourceLocation.tryParse(key);
            if (techniqueId == null || !proficiencyData.contains(key, Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            long proficiency = proficiencyData.getLong(key);
            if (proficiency > 0L) {
                proficiencies.put(techniqueId, proficiency);
            }
        }
        return Map.copyOf(proficiencies);
    }

    public static long get(Player player, ResourceLocation techniqueId) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(PROFICIENCY_KEY, Tag.TAG_COMPOUND)) {
            return 0L;
        }
        CompoundTag proficiencyData = persistentData.getCompound(PROFICIENCY_KEY);
        String key = techniqueId.toString();
        if (!proficiencyData.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return 0L;
        }
        return Math.max(0L, proficiencyData.getLong(key));
    }

    public static long add(Player player, ResourceLocation techniqueId, long amount) {
        long currentProficiency = get(player, techniqueId);
        if (amount <= 0L) {
            return currentProficiency;
        }
        long updatedProficiency = currentProficiency + amount;
        if (updatedProficiency < 0L) {
            updatedProficiency = Long.MAX_VALUE;
        }
        set(player, techniqueId, updatedProficiency);
        return updatedProficiency;
    }

    public static void set(Player player, ResourceLocation techniqueId, long amount) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag proficiencyData = persistentData.contains(PROFICIENCY_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PROFICIENCY_KEY)
                : new CompoundTag();
        if (amount <= 0L) {
            proficiencyData.remove(techniqueId.toString());
        } else {
            proficiencyData.putLong(techniqueId.toString(), amount);
        }
        if (proficiencyData.isEmpty()) {
            persistentData.remove(PROFICIENCY_KEY);
        } else {
            persistentData.put(PROFICIENCY_KEY, proficiencyData);
        }
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(PROFICIENCY_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(PROFICIENCY_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    PROFICIENCY_KEY,
                    sourceData.getCompound(PROFICIENCY_KEY).copy()
            );
        }
    }
}
