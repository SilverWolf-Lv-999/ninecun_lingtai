package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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
        if (!spiritualEnergyData.contains(elementKey, Tag.TAG_ANY_NUMERIC)) {
            return 0.0D;
        }
        double spiritualEnergy = spiritualEnergyData.getDouble(elementKey);
        return Double.isFinite(spiritualEnergy) && spiritualEnergy > 0.0D
                ? spiritualEnergy
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
        double updatedEnergy = getEnergy(player, element) + amount;
        setEnergy(player, element, Double.isFinite(updatedEnergy) ? updatedEnergy : Double.MAX_VALUE);
    }

    public static void setEnergy(Player player, ElementType element, double amount) {
        if (!Double.isFinite(amount)) {
            return;
        }
        if (amount <= 0.0D) {
            clearEnergy(player, element);
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag spiritualEnergyData = persistentData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SPIRITUAL_ENERGY_KEY)
                : new CompoundTag();
        spiritualEnergyData.putDouble(element.getSerializedName(), amount);
        persistentData.put(SPIRITUAL_ENERGY_KEY, spiritualEnergyData);
    }

    public static void removeEnergy(Player player, ElementType element, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        setEnergy(player, element, Math.max(0.0D, getEnergy(player, element) - amount));
    }

    public static void clearEnergy(Player player, ElementType element) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag spiritualEnergyData = persistentData.getCompound(SPIRITUAL_ENERGY_KEY);
        spiritualEnergyData.remove(element.getSerializedName());
        if (spiritualEnergyData.isEmpty()) {
            persistentData.remove(SPIRITUAL_ENERGY_KEY);
        } else {
            persistentData.put(SPIRITUAL_ENERGY_KEY, spiritualEnergyData);
        }
    }

    public static void clearAllEnergy(Player player) {
        player.getPersistentData().remove(SPIRITUAL_ENERGY_KEY);
    }

    public static boolean consumeEnergy(Player player, ElementType element, double amount) {
        return consumeEnergy(player, Set.of(element), amount);
    }

    public static boolean consumeEnergy(Player player, Set<ElementType> elements, double amount) {
        if (elements == null || elements.isEmpty() || !Double.isFinite(amount) || amount <= 0.0D) {
            return false;
        }
        EnumMap<ElementType, Double> currentEnergy = new EnumMap<>(ElementType.class);
        for (ElementType element : elements) {
            double elementEnergy = getEnergy(player, element);
            if (elementEnergy < amount) {
                return false;
            }
            currentEnergy.put(element, elementEnergy);
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag spiritualEnergyData = persistentData.getCompound(SPIRITUAL_ENERGY_KEY);
        for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
            spiritualEnergyData.putDouble(
                    entry.getKey().getSerializedName(),
                    entry.getValue() - amount
            );
        }
        persistentData.put(SPIRITUAL_ENERGY_KEY, spiritualEnergyData);
        return true;
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
