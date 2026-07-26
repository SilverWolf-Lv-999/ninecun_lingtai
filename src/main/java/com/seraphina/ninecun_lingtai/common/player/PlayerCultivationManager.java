package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;
import java.util.Optional;

public final class PlayerCultivationManager {
    private PlayerCultivationManager() {
    }

    public static BreakthroughResult tryBreakthrough(Player player, ElementType spiritualRootElement) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty()) {
            return BreakthroughResult.NO_SPIRITUAL_ROOT;
        }
        if (!SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get()).contains(spiritualRootElement)) {
            return BreakthroughResult.NOT_A_SPIRITUAL_ROOT;
        }

        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player, spiritualRootElement);
        Optional<CultivationStage> targetStage = currentStage.isPresent()
                ? currentStage.get().next()
                : Optional.of(CultivationStage.first());
        if (targetStage.isEmpty()) {
            return BreakthroughResult.MAXIMUM_REALM;
        }

        BigInteger requiredSpiritualEnergy = CultivationUtility.getRequiredSpiritualEnergy(targetStage.get());
        if (!PlayerSpiritualEnergyData.consumeEnergy(
                player,
                spiritualRootElement,
                requiredSpiritualEnergy.doubleValue()
        )) {
            return BreakthroughResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        PlayerCultivationData.setStage(player, spiritualRootElement, targetStage.get());
        return BreakthroughResult.SUCCESS;
    }
}
