package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class SpiritualEnergyUtility {
    public static final float ABSORPTION_AFFINITY = 1.0F;
    public static final float GENERATION_AFFINITY = 0.72F;
    public static final float REPULSION_AFFINITY = 0.0F;

    public static Map<ElementType, Double> createCultivationAbsorptionRates(
            SpiritualRoot spiritualRoot,
            FiveElementValues environmentValues
    ) {
        EnumMap<ElementType, Double> absorptionRates = new EnumMap<>(ElementType.class);
        double rootQuality = spiritualRoot.quality();
        if (spiritualRoot.isChaos()) {
            absorptionRates.put(ElementType.CHAOS, rootQuality * environmentValues.sum());
            return Map.copyOf(absorptionRates);
        }

        for (ElementType element : ElementType.FIVE_ELEMENTS) {
            if (!spiritualRoot.elements().contains(element)) {
                continue;
            }
            absorptionRates.put(
                    element,
                    rootQuality
                            * environmentValues.get(element)
                            * getMultiElementInteractionMultiplier(element, spiritualRoot.elements())
            );
        }
        if (spiritualRoot.elements().contains(ElementType.YIN_YANG)) {
            absorptionRates.put(
                    ElementType.YIN_YANG,
                    rootQuality * environmentValues.minimum() * ElementType.FIVE_ELEMENTS.size()
            );
        }
        return Map.copyOf(absorptionRates);
    }

    public static Map<ElementType, Double> createUniversalCultivationAbsorptionRates(SpiritualRoot spiritualRoot) {
        Set<ElementType> independentRootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot);
        double rate = spiritualRoot.quality() / independentRootElements.size();
        EnumMap<ElementType, Double> absorptionRates = new EnumMap<>(ElementType.class);
        for (ElementType element : independentRootElements) {
            absorptionRates.put(element, rate);
        }
        return Map.copyOf(absorptionRates);
    }

    public static double getMultiElementInteractionMultiplier(
            ElementType absorbedElement,
            Set<ElementType> spiritualRootElements
    ) {
        if (spiritualRootElements.size() <= 1) {
            return 1.0D;
        }
        double multiplierSum = 0.0D;
        int relatedElementCount = 0;
        for (ElementType otherElement : spiritualRootElements) {
            if (otherElement == absorbedElement) {
                continue;
            }
            multiplierSum += getRelationMultiplier(absorbedElement.getRelationTo(otherElement));
            relatedElementCount++;
        }
        return relatedElementCount == 0 ? 1.0D : multiplierSum / relatedElementCount;
    }

    public static FiveElementValues createMovementAffinities(SpiritualRoot spiritualRoot) {
        if (spiritualRoot.isChaos() || spiritualRoot.elements().contains(ElementType.YIN_YANG)) {
            return new FiveElementValues(
                    ABSORPTION_AFFINITY,
                    ABSORPTION_AFFINITY,
                    ABSORPTION_AFFINITY,
                    ABSORPTION_AFFINITY,
                    ABSORPTION_AFFINITY
            );
        }
        return new FiveElementValues(
                getMovementAffinity(ElementType.METAL, spiritualRoot.elements()),
                getMovementAffinity(ElementType.WOOD, spiritualRoot.elements()),
                getMovementAffinity(ElementType.WATER, spiritualRoot.elements()),
                getMovementAffinity(ElementType.FIRE, spiritualRoot.elements()),
                getMovementAffinity(ElementType.EARTH, spiritualRoot.elements())
        );
    }

    private static float getMovementAffinity(
            ElementType spiritualEnergyElement,
            Set<ElementType> spiritualRootElements
    ) {
        if (spiritualRootElements.contains(spiritualEnergyElement)) {
            return ABSORPTION_AFFINITY;
        }

        int generationRelationCount = 0;
        int restraintRelationCount = 0;
        for (ElementType spiritualRootElement : spiritualRootElements) {
            ElementRelation relation = spiritualEnergyElement.getRelationTo(spiritualRootElement);
            if (relation == ElementRelation.GENERATES || relation == ElementRelation.GENERATED_BY) {
                generationRelationCount++;
            } else if (relation == ElementRelation.OVERCOMES || relation == ElementRelation.OVERCOME_BY) {
                restraintRelationCount++;
            }
        }
        return generationRelationCount > restraintRelationCount
                ? GENERATION_AFFINITY
                : REPULSION_AFFINITY;
    }

    private static double getRelationMultiplier(ElementRelation relation) {
        return switch (relation) {
            case GENERATES -> CommonConfig.MEDITATION_GENERATES_MULTIPLIER.get();
            case GENERATED_BY -> CommonConfig.MEDITATION_GENERATED_BY_MULTIPLIER.get();
            case OVERCOMES -> CommonConfig.MEDITATION_OVERCOMES_MULTIPLIER.get();
            case OVERCOME_BY -> CommonConfig.MEDITATION_OVERCOME_BY_MULTIPLIER.get();
            case SAME, SUPPRESSES, SUPPRESSED_BY, NEUTRAL -> 1.0D;
        };
    }
}
