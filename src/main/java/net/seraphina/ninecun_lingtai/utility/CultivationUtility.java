package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

public final class CultivationUtility {
    private static final long[] QI_REFINING_REQUIREMENTS = {
            25L,
            35L,
            50L,
            70L,
            130L,
            250L,
            490L,
            970L,
            1930L
    };
    private static final int MAJOR_REALM_BREAKTHROUGH_MULTIPLIER = 2;
    private static final int MINOR_REALM_REQUIREMENT_OFFSET = 10;
    private static final BigInteger[][] SPIRITUAL_ENERGY_REQUIREMENTS = createSpiritualEnergyRequirements();


    public static BigInteger getRequiredSpiritualEnergy(CultivationStage targetStage) {
        return SPIRITUAL_ENERGY_REQUIREMENTS[targetStage.realm().ordinal()][targetStage.layer() - 1];
    }

    public static BigInteger getCombinedRequiredSpiritualEnergy(
            CultivationStage targetStage,
            SpiritualRoot spiritualRoot
    ) {
        int independentRootCount = SpiritualRootUtility.getIndependentRootElements(spiritualRoot).size();
        return getRequiredSpiritualEnergy(targetStage).multiply(BigInteger.valueOf(independentRootCount));
    }

    public static Optional<BigInteger> getNextRequiredSpiritualEnergy(Optional<CultivationStage> currentStage) {
        Optional<CultivationStage> nextStage = currentStage.isPresent()
                ? currentStage.get().next()
                : Optional.of(CultivationStage.first());
        return nextStage.map(CultivationUtility::getRequiredSpiritualEnergy);
    }

    /**
     * Returns the spiritual-energy amount required by the next target stage. At the final stage
     * of a realm this is the manual realm-breakthrough requirement; at the final realm it is the
     * current stage requirement.
     */
    public static BigInteger getMaximumAbsorbableSpiritualEnergy(Optional<CultivationStage> currentStage) {
        return getNextRequiredSpiritualEnergy(currentStage)
                .orElseGet(() -> currentStage
                        .map(CultivationUtility::getRequiredSpiritualEnergy)
                        .orElse(BigInteger.ZERO));
    }

    public static boolean areAllSpiritualRootsComplete(
            CultivationStage targetStage,
            SpiritualRoot spiritualRoot,
            Map<ElementType, Double> spiritualEnergy
    ) {
        double requiredSpiritualEnergy = getRequiredSpiritualEnergy(targetStage).doubleValue();
        return SpiritualRootUtility.getIndependentRootElements(spiritualRoot).stream()
                .allMatch(element -> spiritualEnergy.getOrDefault(element, 0.0D) >= requiredSpiritualEnergy);
    }

    public static String getDisplayName(CultivationStage stage) {
        String layerUnit = stage.realm().usesHeavenlyLayerName() ? "重天" : "层";
        return stage.realm().getChineseName()
                + ChineseNumberUtility.toChineseInteger(stage.layer())
                + layerUnit;
    }

    public static int getProgressionIndex(CultivationStage stage) {
        return stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER + stage.layer() - 1;
    }

    private static BigInteger[][] createSpiritualEnergyRequirements() {
        CultivationRealm[] realms = CultivationRealm.values();
        BigInteger[][] requirements = new BigInteger[realms.length][CultivationStage.MAXIMUM_LAYER];
        for (int layerIndex = 0; layerIndex < QI_REFINING_REQUIREMENTS.length; layerIndex++) {
            requirements[CultivationRealm.QI_REFINING.ordinal()][layerIndex] = BigInteger.valueOf(
                    QI_REFINING_REQUIREMENTS[layerIndex]
            );
        }

        BigInteger requirementOffset = BigInteger.valueOf(MINOR_REALM_REQUIREMENT_OFFSET);
        for (int realmIndex = 1; realmIndex < realms.length; realmIndex++) {
            BigInteger previousRealmPeak = requirements[realmIndex - 1][CultivationStage.MAXIMUM_LAYER - 1];
            requirements[realmIndex][0] = previousRealmPeak.multiply(
                    BigInteger.valueOf(MAJOR_REALM_BREAKTHROUGH_MULTIPLIER)
            );

            BigInteger minorRealmMultiplier = BigInteger.valueOf(realmIndex + 2L);
            for (int layerIndex = 1; layerIndex < CultivationStage.MAXIMUM_LAYER; layerIndex++) {
                requirements[realmIndex][layerIndex] = requirements[realmIndex][layerIndex - 1]
                        .multiply(minorRealmMultiplier)
                        .subtract(requirementOffset);
            }
        }
        return requirements;
    }
}
