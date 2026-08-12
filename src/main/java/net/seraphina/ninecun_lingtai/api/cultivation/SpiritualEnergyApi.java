package net.seraphina.ninecun_lingtai.api.cultivation;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class SpiritualEnergyApi {
    private SpiritualEnergyApi() {
    }

    public static double getEnergy(Player player, ElementType element) {
        return PlayerSpiritualEnergyData.getEnergy(player, element);
    }

    public static Map<ElementType, Double> getAllEnergy(Player player) {
        return PlayerSpiritualEnergyData.getAllEnergy(player);
    }
}
