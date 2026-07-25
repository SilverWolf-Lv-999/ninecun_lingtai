package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;

import java.util.Set;

public final class SpiritualEnergyUtility {
    private SpiritualEnergyUtility() {
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
