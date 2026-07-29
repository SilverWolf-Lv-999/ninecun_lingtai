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

    /**
     * Retains the requested fraction of every stored spiritual-energy pool.
     * A ratio of zero clears all energy, while a ratio of one leaves it unchanged.
     */
    public static void retainEnergyRatio(Player player, double retainedRatio) {
        if (!Double.isFinite(retainedRatio)) {
            return;
        }
        if (retainedRatio <= 0.0D) {
            clearAllEnergy(player);
            return;
        }
        if (retainedRatio >= 1.0D) {
            return;
        }
        for (ElementType element : ElementType.values()) {
            double energy = getEnergy(player, element);
            if (energy > 0.0D) {
                setEnergy(player, element, energy * retainedRatio);
            }
        }
    }

    public static boolean consumeEnergy(Player player, ElementType element, double amount) {
        return consumeEnergy(player, Set.of(element), amount);
    }

    /**
     * 不挑属性地扣取灵力：从存量最多的属性开始依次抽取，直到凑足 {@code amount}。
     * 返回实际扣取的分布（用于施法失败时退还）；灵力总量不足时不扣取任何灵力并返回 null。
     */
    public static Map<ElementType, Double> consumeEnergyAny(Player player, double amount) {
        return consumeEnergyAny(player, getAllEnergy(player).keySet(), amount);
    }

    public static Map<ElementType, Double> consumeEnergyAny(
            Player player,
            Set<ElementType> eligibleElements,
            double amount
    ) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return null;
        }
        Map<ElementType, Double> currentEnergy = getEligibleEnergy(player, eligibleElements);
        double totalEnergy = currentEnergy.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalEnergy < amount) {
            return null;
        }
        EnumMap<ElementType, Double> consumed = new EnumMap<>(ElementType.class);
        double remaining = amount;
        while (remaining > 1.0E-9D) {
            ElementType richestElement = null;
            double richestEnergy = 0.0D;
            for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
                double available = entry.getValue() - consumed.getOrDefault(entry.getKey(), 0.0D);
                if (available > richestEnergy) {
                    richestEnergy = available;
                    richestElement = entry.getKey();
                }
            }
            if (richestElement == null) {
                return null;
            }
            double drained = Math.min(richestEnergy, remaining);
            consumed.merge(richestElement, drained, Double::sum);
            remaining -= drained;
        }
        for (Map.Entry<ElementType, Double> entry : consumed.entrySet()) {
            setEnergy(player, entry.getKey(), currentEnergy.get(entry.getKey()) - entry.getValue());
        }
        return Collections.unmodifiableMap(consumed);
    }

    public static double consumeEnergyUpTo(
            Player player,
            Set<ElementType> eligibleElements,
            double maximumAmount
    ) {
        if (!Double.isFinite(maximumAmount) || maximumAmount <= 0.0D) {
            return 0.0D;
        }
        Map<ElementType, Double> currentEnergy = getEligibleEnergy(player, eligibleElements);
        double remaining = maximumAmount;
        double consumed = 0.0D;
        for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
            if (remaining <= 1.0E-9D) {
                break;
            }
            double drained = Math.min(entry.getValue(), remaining);
            setEnergy(player, entry.getKey(), entry.getValue() - drained);
            remaining -= drained;
            consumed += drained;
        }
        return consumed;
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

    private static Map<ElementType, Double> getEligibleEnergy(
            Player player,
            Set<ElementType> eligibleElements
    ) {
        EnumMap<ElementType, Double> energyValues = new EnumMap<>(ElementType.class);
        if (eligibleElements == null || eligibleElements.isEmpty()) {
            energyValues.putAll(getAllEnergy(player));
            return energyValues;
        }
        for (ElementType element : eligibleElements) {
            double energy = getEnergy(player, element);
            if (energy > 0.0D) {
                energyValues.put(element, energy);
            }
        }
        return energyValues;
    }
}
