package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerCultivationManager {

    public static BreakthroughResult tryBreakthrough(Player player) {
        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
        Optional<CultivationStage> targetStage = currentStage.isPresent()
                ? currentStage.get().next()
                : Optional.of(CultivationStage.first());
        if (targetStage.isEmpty()) {
            return BreakthroughResult.MAXIMUM_REALM;
        }
        return tryReachStage(player, targetStage.get());
    }

    /**
     * Advances one minor layer inside the current major realm. Mortals step onto the first
     * stage automatically, but reaching the realm peak never crosses into the next major realm;
     * that requires an explicit realm breakthrough.
     */
    public static BreakthroughResult tryAdvanceLayer(Player player) {
        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
        if (currentStage.isEmpty()) {
            return tryReachStage(player, CultivationStage.first());
        }
        CultivationStage stage = currentStage.get();
        if (stage.layer() >= CultivationStage.MAXIMUM_LAYER) {
            return BreakthroughResult.REALM_PEAK_REACHED;
        }
        return tryReachStage(player, new CultivationStage(stage.realm(), stage.layer() + 1));
    }

    /**
     * Breaks through into the next major realm. Only available at the current realm's peak,
     * e.g. Qi Refining peak -> Foundation Establishment layer one.
     */
    public static BreakthroughResult tryBreakthroughRealm(Player player) {
        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
        if (currentStage.isEmpty() || currentStage.get().layer() < CultivationStage.MAXIMUM_LAYER) {
            return BreakthroughResult.NOT_AT_REALM_PEAK;
        }
        Optional<CultivationRealm> nextRealm = currentStage.get().realm().next();
        if (nextRealm.isEmpty()) {
            return BreakthroughResult.MAXIMUM_REALM;
        }
        return tryReachStage(
                player,
                new CultivationStage(nextRealm.get(), CultivationStage.MINIMUM_LAYER)
        );
    }

    public static boolean hasEnoughEnergyForTarget(Player player, CultivationStage targetStage) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty()) {
            return false;
        }
        return CultivationUtility.areAllSpiritualRootsComplete(
                targetStage,
                spiritualRoot.get(),
                PlayerSpiritualEnergyData.getAllEnergy(player)
        );
    }

    public static boolean consumeEnergyForTarget(Player player, CultivationStage targetStage) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty() || !hasEnoughEnergyForTarget(player, targetStage)) {
            return false;
        }
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get());
        return PlayerSpiritualEnergyData.consumeEnergy(
                player,
                rootElements,
                CultivationUtility.getRequiredSpiritualEnergy(targetStage).doubleValue()
        );
    }

    private static BreakthroughResult tryReachStage(Player player, CultivationStage targetStage) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty()) {
            return BreakthroughResult.NO_SPIRITUAL_ROOT;
        }

        if (!consumeEnergyForTarget(player, targetStage)) {
            return BreakthroughResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        PlayerCultivationData.setStage(player, targetStage);
        return BreakthroughResult.SUCCESS;
    }
}
