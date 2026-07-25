package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerSpiritualEnergyData {
    private static final String SPIRITUAL_ENERGY_KEY = ModMain.MOD_ID + ":spiritual_energy";

    private PlayerSpiritualEnergyData() {
    }

    public static double getEnergy(Player player, ElementType element) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)) {
            return 0.0D;
        }
        CompoundTag spiritualEnergyData = persistentData.getCompound(SPIRITUAL_ENERGY_KEY);
        String elementKey = element.getSerializedName();
        return spiritualEnergyData.contains(elementKey, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0.0D, spiritualEnergyData.getDouble(elementKey))
                : 0.0D;
    }

    public static Map<ElementType, Double> getAllEnergy(Player player) {
        EnumMap<ElementType, Double> energyValues = new EnumMap<>(ElementType.class);
        for (ElementType element : ElementType.values()) {
            double energy = getEnergy(player, element);
            if (energy > 0.0D) {
                energyValues.put(element, energy);
            }
        }
        return Collections.unmodifiableMap(energyValues);
    }

    public static void addEnergy(Player player, ElementType element, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag spiritualEnergyData = persistentData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SPIRITUAL_ENERGY_KEY)
                : new CompoundTag();
        String elementKey = element.getSerializedName();
        double updatedEnergy = getEnergy(player, element) + amount;
        spiritualEnergyData.putDouble(elementKey, Double.isFinite(updatedEnergy) ? updatedEnergy : Double.MAX_VALUE);
        persistentData.put(SPIRITUAL_ENERGY_KEY, spiritualEnergyData);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SPIRITUAL_ENERGY_KEY,
                    sourceData.getCompound(SPIRITUAL_ENERGY_KEY).copy()
            );
        }
    }
}
