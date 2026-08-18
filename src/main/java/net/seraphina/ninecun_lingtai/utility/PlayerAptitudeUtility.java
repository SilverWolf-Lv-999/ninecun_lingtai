package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class PlayerAptitudeUtility {
    private static final double MINIMUM_ENVIRONMENT_CONCENTRATION = 0.05D;
    private static final double MAXIMUM_ENVIRONMENT_CONCENTRATION = 16.0D;
    private static final double MINIMUM_ENVIRONMENT_RICHNESS = 0.35D;
    private static final double MAXIMUM_ENVIRONMENT_RICHNESS = 2.5D;
    private static final double MINIMUM_ELEMENT_WEIGHT = 0.000001D;
    private static final double BASE_LENGTH_DECAY = 6.0D;
    private static final double BASE_SOUL_DECAY = 5.0D;
    private static final double BASE_YIN_YANG_CHANCE = 0.01D;
    private static final double BASE_CHAOS_CHANCE = 0.0005D;
    private static final double[] ROOT_COUNT_WEIGHTS = {
            1.0D,
            0.22D,
            0.06D,
            0.015D,
            0.003D
    };
    private static final int ROOT_LENGTH_TENTHS = Math.round(
            (SpiritualRoot.MAXIMUM_LENGTH_INCHES - SpiritualRoot.MINIMUM_LENGTH_INCHES) * 10.0F
    ) + 1;

    public static boolean assignRandomAptitude(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (PlayerAptitudeData.hasCompleteAptitude(player)) {
            return false;
        }

        RandomAptitude aptitude = rollRandomAptitude(player);
        PlayerAptitudeData.setAptitude(player, aptitude.spiritualRoot(), aptitude.soulStrength());
        return true;
    }

    public static RandomAptitude rollRandomAptitude(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        ElementEnvironmentManager environmentManager = ElementEnvironmentManager.INSTANCE;
        FiveElementValues fiveElementValues = environmentManager.getFiveElementValues(entity);
        double yinYangValue = environmentManager.getEnvironmentMultiplier(entity, ElementType.YIN_YANG);
        double chaosValue = environmentManager.getEnvironmentMultiplier(entity, ElementType.CHAOS);
        return rollRandomAptitude(entity.getRandom(), fiveElementValues, yinYangValue, chaosValue);
    }

    public static RandomAptitude rollRandomAptitude(
            RandomSource random,
            FiveElementValues fiveElementValues,
            double yinYangValue,
            double chaosValue
    ) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(fiveElementValues, "fiveElementValues");
        double environmentRichness = getEnvironmentRichness(fiveElementValues, yinYangValue, chaosValue);
        SpiritualRoot spiritualRoot = rollSpiritualRoot(
                random,
                fiveElementValues,
                yinYangValue,
                chaosValue,
                environmentRichness
        );
        SoulStrength soulStrength = rollSoulStrength(random, environmentRichness);
        return new RandomAptitude(spiritualRoot, soulStrength);
    }

    private static SpiritualRoot rollSpiritualRoot(
            RandomSource random,
            FiveElementValues fiveElementValues,
            double yinYangValue,
            double chaosValue,
            double environmentRichness
    ) {
        double chaosChance = Math.min(
                0.01D,
                BASE_CHAOS_CHANCE * getPositiveFiniteValue(chaosValue) * environmentRichness
        );
        double yinYangChance = Math.min(
                0.08D,
                BASE_YIN_YANG_CHANCE * getPositiveFiniteValue(yinYangValue) * environmentRichness
        );
        double specialRoll = random.nextDouble();
        if (specialRoll < chaosChance) {
            return new SpiritualRoot(EnumSet.copyOf(SpiritualRoot.SELECTABLE_ELEMENTS), rollRootLength(
                    random,
                    environmentRichness
            ));
        }
        if (specialRoll < chaosChance + yinYangChance) {
            return new SpiritualRoot(EnumSet.of(ElementType.YIN_YANG), rollRootLength(
                    random,
                    environmentRichness
            ));
        }

        int elementCount = 1 + rollWeightedIndex(random, ROOT_COUNT_WEIGHTS);
        List<ElementType> candidates = new ArrayList<>(ElementType.FIVE_ELEMENTS);
        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        for (int index = 0; index < elementCount; index++) {
            int selectedIndex = rollElementIndex(random, candidates, fiveElementValues);
            elements.add(candidates.remove(selectedIndex));
        }
        return new SpiritualRoot(elements, rollRootLength(random, environmentRichness));
    }

    private static int rollElementIndex(
            RandomSource random,
            List<ElementType> candidates,
            FiveElementValues fiveElementValues
    ) {
        double totalWeight = 0.0D;
        for (ElementType candidate : candidates) {
            totalWeight += getElementWeight(fiveElementValues.get(candidate));
        }
        double target = random.nextDouble() * totalWeight;
        for (int index = 0; index < candidates.size(); index++) {
            target -= getElementWeight(fiveElementValues.get(candidates.get(index)));
            if (target < 0.0D) {
                return index;
            }
        }
        return candidates.size() - 1;
    }

    private static float rollRootLength(RandomSource random, double environmentRichness) {
        double decay = BASE_LENGTH_DECAY / environmentRichness;
        int lengthIndex = rollDecayingIndex(random, ROOT_LENGTH_TENTHS, decay);
        return SpiritualRoot.MINIMUM_LENGTH_INCHES + lengthIndex / 10.0F;
    }

    private static SoulStrength rollSoulStrength(RandomSource random, double environmentRichness) {
        double decay = BASE_SOUL_DECAY / environmentRichness;
        int starIndex = rollDecayingIndex(
                random,
                SoulStrength.MAXIMUM_ILLUMINATED_STARS,
                decay
        );
        return new SoulStrength(SoulStrength.MINIMUM_ILLUMINATED_STARS + starIndex);
    }

    private static int rollDecayingIndex(RandomSource random, int valueCount, double decay) {
        double totalWeight = 0.0D;
        for (int index = 0; index < valueCount; index++) {
            totalWeight += Math.exp(-decay * index / (double) (valueCount - 1));
        }
        double target = random.nextDouble() * totalWeight;
        for (int index = 0; index < valueCount; index++) {
            target -= Math.exp(-decay * index / (double) (valueCount - 1));
            if (target < 0.0D) {
                return index;
            }
        }
        return valueCount - 1;
    }

    private static int rollWeightedIndex(RandomSource random, double[] weights) {
        double totalWeight = 0.0D;
        for (double weight : weights) {
            totalWeight += weight;
        }
        double target = random.nextDouble() * totalWeight;
        for (int index = 0; index < weights.length; index++) {
            target -= weights[index];
            if (target < 0.0D) {
                return index;
            }
        }
        return weights.length - 1;
    }

    private static double getEnvironmentRichness(
            FiveElementValues fiveElementValues,
            double yinYangValue,
            double chaosValue
    ) {
        double totalConcentration = fiveElementValues.sum()
                + getPositiveFiniteValue(yinYangValue)
                + getPositiveFiniteValue(chaosValue);
        double averageConcentration = totalConcentration / (ElementType.FIVE_ELEMENTS.size() + 2.0D);
        double boundedConcentration = clamp(
                averageConcentration,
                MINIMUM_ENVIRONMENT_CONCENTRATION,
                MAXIMUM_ENVIRONMENT_CONCENTRATION
        );
        return clamp(
                Math.sqrt(boundedConcentration),
                MINIMUM_ENVIRONMENT_RICHNESS,
                MAXIMUM_ENVIRONMENT_RICHNESS
        );
    }

    private static double getElementWeight(float value) {
        return Math.max(MINIMUM_ELEMENT_WEIGHT, getPositiveFiniteValue(value));
    }

    private static double getPositiveFiniteValue(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record RandomAptitude(SpiritualRoot spiritualRoot, SoulStrength soulStrength) {
    }
}
