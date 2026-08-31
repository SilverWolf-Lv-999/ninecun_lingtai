package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.aura.AuraQuality;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record PlayerCultivationInfo(
        Optional<SpiritualRoot> spiritualRoot,
        Optional<SoulStrength> soulStrength,
        Optional<CultivationStage> cultivationStage,
        Map<ElementType, Double> spiritualEnergy,
        LifespanSnapshot lifespan,
        Optional<AuraQuality> auraQuality
) {
    public PlayerCultivationInfo {
        spiritualRoot = spiritualRoot == null ? Optional.empty() : spiritualRoot;
        soulStrength = soulStrength == null ? Optional.empty() : soulStrength;
        cultivationStage = cultivationStage == null ? Optional.empty() : cultivationStage;
        spiritualEnergy = immutableEnumMap(spiritualEnergy);
        lifespan = lifespan == null ? LifespanSnapshot.empty() : lifespan;
        auraQuality = auraQuality == null ? Optional.empty() : auraQuality;
    }

    public static PlayerCultivationInfo capture(Player player) {
        Optional<AuraQuality> auraQuality = player instanceof ServerPlayer serverPlayer
                ? Optional.of(SpiritualAuraManager.getQuality(
                        serverPlayer.serverLevel(),
                        serverPlayer.blockPosition()))
                : Optional.empty();
        return new PlayerCultivationInfo(
                PlayerAptitudeData.getSpiritualRoot(player),
                PlayerAptitudeData.getSoulStrength(player),
                PlayerCultivationData.getStage(player),
                PlayerSpiritualEnergyData.getAllEnergy(player),
                EntityLifespanData.capture(player),
                auraQuality
        );
    }

    public static PlayerCultivationInfo empty() {
        return new PlayerCultivationInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                LifespanSnapshot.empty(),
                Optional.empty()
        );
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
