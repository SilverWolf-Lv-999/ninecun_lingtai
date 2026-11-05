package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.aura.AuraQuality;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.seraphina.ninecun_lingtai.utility.CultivationProgressUtility;
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
        double chaosQi,
        double heavenlyLaws,
        double immortalEssence,
        LifespanSnapshot lifespan,
        Optional<AuraQuality> auraQuality
) {
    public PlayerCultivationInfo(
            Optional<SpiritualRoot> spiritualRoot,
            Optional<SoulStrength> soulStrength,
            Optional<CultivationStage> cultivationStage,
            Map<ElementType, Double> spiritualEnergy,
            LifespanSnapshot lifespan,
            Optional<AuraQuality> auraQuality
    ) {
        this(
                spiritualRoot,
                soulStrength,
                cultivationStage,
                spiritualEnergy,
                0.0D,
                0.0D,
                0.0D,
                lifespan,
                auraQuality
        );
    }

    public PlayerCultivationInfo {
        spiritualRoot = spiritualRoot == null ? Optional.empty() : spiritualRoot;
        soulStrength = soulStrength == null ? Optional.empty() : soulStrength;
        cultivationStage = cultivationStage == null ? Optional.empty() : cultivationStage;
        spiritualEnergy = immutableEnumMap(spiritualEnergy);
        chaosQi = finiteOrZero(chaosQi);
        heavenlyLaws = finiteOrZero(heavenlyLaws);
        immortalEssence = finiteOrZero(immortalEssence);
        lifespan = lifespan == null ? LifespanSnapshot.empty() : lifespan;
        auraQuality = auraQuality == null ? Optional.empty() : auraQuality;
    }

    public static PlayerCultivationInfo capture(Player player) {
        Optional<AuraQuality> auraQuality = player instanceof ServerPlayer serverPlayer
                ? Optional.of(SpiritualAuraManager.getQuality(
                        serverPlayer.serverLevel(),
                        serverPlayer.blockPosition()))
                : Optional.empty();
        Optional<CultivationStage> cultivationStage = PlayerCultivationData.getStage(player);
        Map<ElementType, Double> spiritualEnergy = cultivationStage
                .map(CultivationProgressUtility::usesSpiritualEnergyEra)
                .orElse(true)
                ? PlayerSpiritualEnergyData.getAllEnergy(player)
                : Map.of();
        return new PlayerCultivationInfo(
                PlayerAptitudeData.getSpiritualRoot(player),
                PlayerAptitudeData.getSoulStrength(player),
                cultivationStage,
                spiritualEnergy,
                PlayerCultivationProgressData.getChaosQi(player),
                PlayerCultivationProgressData.getHeavenlyLaws(player),
                PlayerCultivationProgressData.getImmortalEssence(player),
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
                0.0D,
                0.0D,
                0.0D,
                LifespanSnapshot.empty(),
                Optional.empty()
        );
    }

    public double getSpiritualEnergy(ElementType element) {
        return this.spiritualEnergy.getOrDefault(element, 0.0D);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }

    private static <T> Map<ElementType, T> immutableEnumMap(Map<ElementType, T> values) {
        EnumMap<ElementType, T> copiedValues = new EnumMap<>(ElementType.class);
        if (values != null) {
            copiedValues.putAll(values);
        }
        return Collections.unmodifiableMap(copiedValues);
    }
}
