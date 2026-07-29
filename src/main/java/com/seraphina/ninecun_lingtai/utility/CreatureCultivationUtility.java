package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class CreatureCultivationUtility {
    /** Creatures are born at most at Foundation Establishment layer three. */
    public static final CultivationStage MAXIMUM_SPAWN_STAGE = new CultivationStage(
            CultivationRealm.FOUNDATION_ESTABLISHMENT,
            3
    );
    private static final int MISSING_PROGRESSION_INDEX = -1;
    private static final int LENGTH_TENTHS_RANGE = Math.round(
            (SpiritualRoot.MAXIMUM_LENGTH_INCHES - SpiritualRoot.MINIMUM_LENGTH_INCHES) * 10.0F
    ) + 1;

    private CreatureCultivationUtility() {
    }

    public static int getProgressionIndex(LivingEntity entity) {
        return CultivationApi.getStage(entity)
                .map(CultivationUtility::getProgressionIndex)
                .orElse(MISSING_PROGRESSION_INDEX);
    }

    /** Whether {@code entity} sits strictly below {@code reference} on the cultivation ladder. */
    public static boolean isStrictlyWeaker(LivingEntity entity, LivingEntity reference) {
        return getProgressionIndex(entity) < getProgressionIndex(reference);
    }

    /** A viewer perceives a creature's cultivation when their major realm is not below the target's. */
    public static boolean canInspect(Player viewer, CultivationStage targetStage) {
        Optional<CultivationStage> viewerStage = CultivationApi.getStage(viewer);
        return viewerStage.isPresent()
                && viewerStage.get().realm().ordinal() >= targetStage.realm().ordinal();
    }

    public static CultivationStage rollSpawnStage(RandomSource random) {
        int maximumIndex = CultivationUtility.getProgressionIndex(MAXIMUM_SPAWN_STAGE);
        int progressionIndex = random.nextInt(maximumIndex + 1);
        CultivationRealm realm = CultivationRealm.values()[progressionIndex / CultivationStage.MAXIMUM_LAYER];
        int layer = progressionIndex % CultivationStage.MAXIMUM_LAYER + 1;
        return new CultivationStage(realm, layer);
    }

    public static SpiritualRoot rollSpawnSpiritualRoot(RandomSource random) {
        List<ElementType> candidateElements = new ArrayList<>(SpiritualRoot.SELECTABLE_ELEMENTS);
        int elementCount = 1 + random.nextInt(candidateElements.size());
        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        for (int pickIndex = 0; pickIndex < elementCount; pickIndex++) {
            elements.add(candidateElements.remove(random.nextInt(candidateElements.size())));
        }
        float lengthInches = SpiritualRoot.MINIMUM_LENGTH_INCHES
                + random.nextInt(LENGTH_TENTHS_RANGE) / 10.0F;
        return new SpiritualRoot(elements, lengthInches);
    }
}
