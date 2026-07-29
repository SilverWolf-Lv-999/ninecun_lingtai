package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.entity.EntityCultivationData;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;

public final class PlayerSpiritualEnergyData {
    private PlayerSpiritualEnergyData() {
    }

    public static double getEnergy(Player player, ElementType element) {
        return EntityCultivationData.getEnergy(player, element);
    }

    public static Map<ElementType, Double> getAllEnergy(Player player) {
        return EntityCultivationData.getAllEnergy(player);
    }

    public static void addEnergy(Player player, ElementType element, double amount) {
        EntityCultivationData.addEnergy(player, element, amount);
    }

    public static void setEnergy(Player player, ElementType element, double amount) {
        EntityCultivationData.setEnergy(player, element, amount);
    }

    public static void removeEnergy(Player player, ElementType element, double amount) {
        EntityCultivationData.removeEnergy(player, element, amount);
    }

    public static void clearEnergy(Player player, ElementType element) {
        EntityCultivationData.clearEnergy(player, element);
    }

    public static void clearAllEnergy(Player player) {
        EntityCultivationData.clearAllEnergy(player);
    }

    public static void retainEnergyRatio(Player player, double retainedRatio) {
        EntityCultivationData.retainEnergyRatio(player, retainedRatio);
    }

    public static boolean consumeEnergy(Player player, ElementType element, double amount) {
        return EntityCultivationData.consumeEnergy(player, element, amount);
    }

    public static Map<ElementType, Double> consumeEnergyAny(Player player, double amount) {
        return EntityCultivationData.consumeEnergyAny(player, amount);
    }

    public static Map<ElementType, Double> consumeEnergyAny(
            Player player,
            Set<ElementType> eligibleElements,
            double amount
    ) {
        return EntityCultivationData.consumeEnergyAny(player, eligibleElements, amount);
    }

    public static double consumeEnergyUpTo(
            Player player,
            Set<ElementType> eligibleElements,
            double maximumAmount
    ) {
        return EntityCultivationData.consumeEnergyUpTo(player, eligibleElements, maximumAmount);
    }

    public static boolean consumeEnergy(Player player, Set<ElementType> elements, double amount) {
        return EntityCultivationData.consumeEnergy(player, elements, amount);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        EntityCultivationData.clearAllEnergy(targetPlayer);
        EntityCultivationData.getAllEnergy(sourcePlayer).forEach(
                (element, energy) -> EntityCultivationData.setEnergy(targetPlayer, element, energy)
        );
    }
}
