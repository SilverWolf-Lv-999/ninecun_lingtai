package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.world.entity.player.Player;

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
        double requiredEnergy = getDiscountedRequirement(player, targetStage);
        return SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get()).stream()
                .allMatch(element -> PlayerSpiritualEnergyData.getEnergy(player, element) >= requiredEnergy);
    }

    public static boolean consumeEnergyForTarget(Player player, CultivationStage targetStage) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty() || !hasEnoughEnergyForTarget(player, targetStage)) {
            return false;
        }
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get());
        boolean consumed = PlayerSpiritualEnergyData.consumeEnergy(
                player,
                rootElements,
                getDiscountedRequirement(player, targetStage)
        );
        if (consumed && isRealmBreakthrough(player, targetStage)) {
            PlayerBreakthroughDiscountData.clearDiscount(player);
        }
        return consumed;
    }

    /**
     * 折扣后的单池灵力需求（破境仪式启动时锁定需求快照用，§5.4 防仪式中吃丹改折扣）。
     */
    public static double getDiscountedRequirement(Player player, CultivationStage targetStage) {
        double requiredEnergy = CultivationUtility.getRequiredSpiritualEnergy(targetStage).doubleValue();
        if (!isRealmBreakthrough(player, targetStage)) {
            return requiredEnergy;
        }
        double discount = PlayerBreakthroughDiscountData.getDiscount(
                player,
                player.level().getGameTime()
        );
        return requiredEnergy * (1.0D - discount);
    }

    private static boolean isRealmBreakthrough(Player player, CultivationStage targetStage) {
        return PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm() != targetStage.realm())
                .orElse(false);
    }

    /**
     * 按仪式启动时锁定的需求快照消费灵力：所有独立灵根元素池各自 ≥ 快照值才消费、
     * 各扣一份。供破境仪式掷骰结算调用（消费额以快照为准，不重算折扣）。
     */
    public static boolean consumeSnapshotEnergyForTarget(Player player, double snapshotAmount) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty() || snapshotAmount < 0.0D) {
            return false;
        }
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get());
        boolean sufficient = rootElements.stream()
                .allMatch(element -> PlayerSpiritualEnergyData.getEnergy(player, element) >= snapshotAmount);
        if (!sufficient) {
            return false;
        }
        return PlayerSpiritualEnergyData.consumeEnergy(player, rootElements, snapshotAmount);
    }

    /**
     * 无校验落境：原 {@code tryReachStage} 去掉能量部分——只 setStage + 清大境界折扣。
     * 供破境仪式掷骰成功（及日后天劫 tickAftermath）在各自完成消费后调用。
     */
    public static void commitStageUnchecked(Player player, CultivationStage targetStage) {
        boolean realmBreakthrough = isRealmBreakthrough(player, targetStage);
        PlayerCultivationData.setStage(player, targetStage);
        if (realmBreakthrough) {
            PlayerBreakthroughDiscountData.clearDiscount(player);
        }
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
