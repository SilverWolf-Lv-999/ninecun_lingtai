package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record PlayerCultivationInfo(
        Optional<SpiritualRoot> spiritualRoot,
        Optional<SoulStrength> soulStrength,
        Map<ElementType, CultivationStage> cultivationStages,
        Map<ElementType, Double> spiritualEnergy
) {
    public PlayerCultivationInfo {
        spiritualRoot = spiritualRoot == null ? Optional.empty() : spiritualRoot;
        soulStrength = soulStrength == null ? Optional.empty() : soulStrength;
        cultivationStages = immutableEnumMap(cultivationStages);
        spiritualEnergy = immutableEnumMap(spiritualEnergy);
    }

    public static PlayerCultivationInfo capture(Player player) {
        return new PlayerCultivationInfo(
                PlayerAptitudeData.getSpiritualRoot(player),
                PlayerAptitudeData.getSoulStrength(player),
                PlayerCultivationData.getAllStages(player),
                PlayerSpiritualEnergyData.getAllEnergy(player)
        );
    }

    public static PlayerCultivationInfo empty() {
        return new PlayerCultivationInfo(Optional.empty(), Optional.empty(), Map.of(), Map.of());
    }

    public double getSpiritualEnergy(ElementType element) {
        return this.spiritualEnergy.getOrDefault(element, 0.0D);
    }

    private static <T> Map<ElementType, T> immutableEnumMap(Map<ElementType, T> values) {
        EnumMap<ElementType, T> copiedValues = new EnumMap<>(ElementType.class);
        if (values != null) {
            copiedValues.putAll(values);
        }
        return Collections.unmodifiableMap(copiedValues);
    }
}
