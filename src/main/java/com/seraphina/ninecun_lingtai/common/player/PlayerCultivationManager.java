package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
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
    private PlayerCultivationManager() {
    }

    public static BreakthroughResult tryBreakthrough(Player player) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty()) {
            return BreakthroughResult.NO_SPIRITUAL_ROOT;
        }

        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
        Optional<CultivationStage> targetStage = currentStage.isPresent()
                ? currentStage.get().next()
                : Optional.of(CultivationStage.first());
        if (targetStage.isEmpty()) {
            return BreakthroughResult.MAXIMUM_REALM;
        }

        BigInteger requiredSpiritualEnergy = CultivationUtility.getRequiredSpiritualEnergy(targetStage.get());
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get());
        Map<ElementType, Double> spiritualEnergy = PlayerSpiritualEnergyData.getAllEnergy(player);
        if (!CultivationUtility.areAllSpiritualRootsComplete(
                targetStage.get(),
                spiritualRoot.get(),
                spiritualEnergy
        )) {
            return BreakthroughResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }
        if (!PlayerSpiritualEnergyData.consumeEnergy(
                player,
                rootElements,
                requiredSpiritualEnergy.doubleValue()
        )) {
            return BreakthroughResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        PlayerCultivationData.setStage(player, targetStage.get());
        return BreakthroughResult.SUCCESS;
    }
}
