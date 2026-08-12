package net.seraphina.ninecun_lingtai.common.debug;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DaoScrollSnapshot(
        int entityId,
        String entityName,
        boolean player,
        Optional<CultivationStage> cultivationStage,
        Optional<SpiritualRoot> spiritualRoot,
        Optional<SoulStrength> soulStrength,
        long ageTicks,
        int defenseCurrent,
        Map<ElementType, Double> spiritualEnergy,
        List<DaoScrollTechniqueData> techniques
) {
    public DaoScrollSnapshot {
        entityName = entityName == null || entityName.isBlank() ? "Unknown" : entityName;
        cultivationStage = cultivationStage == null ? Optional.empty() : cultivationStage;
        spiritualRoot = spiritualRoot == null ? Optional.empty() : spiritualRoot;
        soulStrength = soulStrength == null ? Optional.empty() : soulStrength;
        ageTicks = Math.max(0L, ageTicks);
        defenseCurrent = Math.max(0, defenseCurrent);
        spiritualEnergy = immutableEnergyMap(spiritualEnergy);
        techniques = techniques == null ? List.of() : List.copyOf(techniques);
    }

    public double getEnergy(ElementType element) {
        return this.spiritualEnergy.getOrDefault(element, 0.0D);
    }

    public Component getEntityNameComponent() {
        return Component.literal(this.entityName);
    }

    private static Map<ElementType, Double> immutableEnergyMap(Map<ElementType, Double> values) {
        EnumMap<ElementType, Double> copiedValues = new EnumMap<>(ElementType.class);
        if (values != null) {
            values.forEach((element, energy) -> {
                if (element != null && energy != null && Double.isFinite(energy) && energy > 0.0D) {
                    copiedValues.put(element, energy);
                }
            });
        }
        return Collections.unmodifiableMap(copiedValues);
    }
}
