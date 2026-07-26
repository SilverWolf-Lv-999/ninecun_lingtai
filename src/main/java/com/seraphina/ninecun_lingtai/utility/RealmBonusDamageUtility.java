package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.ElementalPower;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Set;

public final class RealmBonusDamageUtility {
    private RealmBonusDamageUtility() {
    }

    public static double getBonusDamage(CultivationStage stage) {
        return CommonConfig.REALM_BONUS_BASE_DAMAGE.get()
                * Math.pow(
                        CommonConfig.REALM_BONUS_LAYER_GROWTH.get(),
                        CultivationUtility.getProgressionIndex(stage)
                );
    }

    public static ElementType rollBonusElement(SpiritualRoot spiritualRoot, RandomSource random) {
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot);
        if (rootElements.size() == 1) {
            return rootElements.iterator().next();
        }
        List<ElementType> candidateElements = List.copyOf(rootElements);
        return candidateElements.get(random.nextInt(candidateElements.size()));
    }

    public static ElementalPower createBonusElementalPower(
            ElementType element,
            CultivationStage stage,
            SpiritualRoot spiritualRoot
    ) {
        return new ElementalPower(
                element,
                spiritualRoot.quality() * (stage.realm().ordinal() + 1)
        );
    }
}
