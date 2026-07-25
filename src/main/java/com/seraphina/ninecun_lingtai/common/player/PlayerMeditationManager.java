package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.utility.SpiritualEnergyUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerMeditationManager {
    private static final int ABSORPTION_INTERVAL_TICKS = 20;
    private static final Map<UUID, MeditationRotation> MEDITATING_PLAYERS = new HashMap<>();

    private PlayerMeditationManager() {
    }

    public static boolean startMeditating(ServerPlayer player) {
        if (isMeditating(player)) {
            syncStateToTrackingAndSelf(player, true);
            return true;
        }
        if (!canStartMeditating(player)) {
            ModNetwork.sendMeditationState(
                    player,
                    player.getUUID(),
                    false,
                    FiveElementValues.EMPTY,
                    FiveElementValues.EMPTY
            );
            return false;
        }
        MEDITATING_PLAYERS.put(player.getUUID(), MeditationRotation.capture(player));
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        syncStateToTrackingAndSelf(player, true);
        return true;
    }

    public static void stopMeditating(ServerPlayer player) {
        MEDITATING_PLAYERS.remove(player.getUUID());
        syncStateToTrackingAndSelf(player, false);
    }

    public static boolean isMeditating(ServerPlayer player) {
        return MEDITATING_PLAYERS.containsKey(player.getUUID());
    }

    public static void tickMeditation(ServerPlayer player) {
        if (!isMeditating(player)) {
            return;
        }
        if (!canContinueMeditating(player)) {
            stopMeditating(player);
            return;
        }
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
            absorbSpiritualEnergy(player);
        }
    }

    public static void syncState(ServerPlayer recipient, ServerPlayer trackedPlayer) {
        boolean meditating = isMeditating(trackedPlayer);
        MeditationAbsorptionProfile absorptionProfile = meditating
                ? calculateAbsorptionProfile(trackedPlayer)
                : MeditationAbsorptionProfile.EMPTY;
        ModNetwork.sendMeditationState(
                recipient,
                trackedPlayer.getUUID(),
                meditating,
                meditating
                        ? ElementEnvironmentManager.INSTANCE.getFiveElementValues(trackedPlayer)
                        : FiveElementValues.EMPTY,
                absorptionProfile.visibleEfficiencies()
        );
    }

    public static void clear() {
        MEDITATING_PLAYERS.clear();
    }

    private static boolean canStartMeditating(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && player.onGround();
    }

    private static boolean canContinueMeditating(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping();
    }

    private static void stopMovement(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void lockRotation(ServerPlayer player) {
        MeditationRotation rotation = MEDITATING_PLAYERS.get(player.getUUID());
        if (rotation == null) {
            return;
        }
        player.setYRot(rotation.yRot());
        player.setXRot(rotation.xRot());
        player.setYHeadRot(rotation.yHeadRot());
        player.setYBodyRot(rotation.yBodyRot());
        player.yRotO = rotation.yRot();
        player.xRotO = rotation.xRot();
        player.yHeadRotO = rotation.yHeadRot();
        player.yBodyRotO = rotation.yBodyRot();
    }

    private static void absorbSpiritualEnergy(ServerPlayer player) {
        MeditationAbsorptionProfile absorptionProfile = calculateAbsorptionProfile(player);
        if (absorptionProfile.efficiencies().isEmpty()) {
            return;
        }
        double baseAbsorption = CommonConfig.MEDITATION_BASE_ABSORPTION_PER_SECOND.get()
                / absorptionProfile.efficiencies().size();
        for (Map.Entry<ElementType, Double> entry : absorptionProfile.efficiencies().entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), baseAbsorption * entry.getValue());
        }
    }

    private static MeditationAbsorptionProfile calculateAbsorptionProfile(ServerPlayer player) {
        Optional<SpiritualRoot> spiritualRootOptional = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRootOptional.isEmpty()) {
            return MeditationAbsorptionProfile.EMPTY;
        }

        SpiritualRoot spiritualRoot = spiritualRootOptional.get();
        EnumMap<ElementType, Double> efficiencies = new EnumMap<>(ElementType.class);
        if (spiritualRoot.isChaos()) {
            double chaosEfficiency = spiritualRoot.quality() * getChaosEnvironmentMultiplier(player);
            efficiencies.put(ElementType.CHAOS, chaosEfficiency);
            return new MeditationAbsorptionProfile(
                    efficiencies,
                    createChaosVisibleEfficiencies(player, spiritualRoot.quality())
            );
        }

        for (ElementType element : spiritualRoot.elements()) {
            double environmentMultiplier = element == ElementType.YIN_YANG
                    ? getYinYangAdjustedEnvironmentMultiplier(player)
                    : ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(player, element);
            double interactionMultiplier = SpiritualEnergyUtility.getMultiElementInteractionMultiplier(
                    element,
                    spiritualRoot.elements()
            );
            efficiencies.put(
                    element,
                    spiritualRoot.quality() * environmentMultiplier * interactionMultiplier
            );
        }
        FiveElementValues visibleEfficiencies = spiritualRoot.elements().contains(ElementType.YIN_YANG)
                ? createYinYangVisibleEfficiencies(player, spiritualRoot.quality(), efficiencies)
                : createVisibleEfficiencies(efficiencies);
        return new MeditationAbsorptionProfile(efficiencies, visibleEfficiencies);
    }

    private static double getYinYangAdjustedEnvironmentMultiplier(ServerPlayer player) {
        double strongestMultiplier = ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(
                player,
                ElementType.YIN_YANG
        );
        for (ElementType element : ElementType.FIVE_ELEMENTS) {
            strongestMultiplier = Math.max(
                    strongestMultiplier,
                    ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(player, element)
            );
        }
        return strongestMultiplier;
    }

    private static double getChaosEnvironmentMultiplier(ServerPlayer player) {
        double multiplierSum = 0.0D;
        for (ElementType element : ElementType.values()) {
            multiplierSum += ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(player, element);
        }
        return multiplierSum / ElementType.values().length;
    }

    private static FiveElementValues createChaosVisibleEfficiencies(ServerPlayer player, float rootQuality) {
        FiveElementValues environmentValues = ElementEnvironmentManager.INSTANCE.getFiveElementValues(player);
        return new FiveElementValues(
                environmentValues.metal() * rootQuality,
                environmentValues.wood() * rootQuality,
                environmentValues.water() * rootQuality,
                environmentValues.fire() * rootQuality,
                environmentValues.earth() * rootQuality
        );
    }

    private static FiveElementValues createVisibleEfficiencies(Map<ElementType, Double> efficiencies) {
        return new FiveElementValues(
                efficiencies.getOrDefault(ElementType.METAL, 0.0D).floatValue(),
                efficiencies.getOrDefault(ElementType.WOOD, 0.0D).floatValue(),
                efficiencies.getOrDefault(ElementType.WATER, 0.0D).floatValue(),
                efficiencies.getOrDefault(ElementType.FIRE, 0.0D).floatValue(),
                efficiencies.getOrDefault(ElementType.EARTH, 0.0D).floatValue()
        );
    }

    private static FiveElementValues createYinYangVisibleEfficiencies(
            ServerPlayer player,
            float rootQuality,
            Map<ElementType, Double> efficiencies
    ) {
        FiveElementValues environmentValues = ElementEnvironmentManager.INSTANCE.getFiveElementValues(player);
        return new FiveElementValues(
                Math.max(
                        efficiencies.getOrDefault(ElementType.METAL, 0.0D).floatValue(),
                        environmentValues.metal() * rootQuality
                ),
                Math.max(
                        efficiencies.getOrDefault(ElementType.WOOD, 0.0D).floatValue(),
                        environmentValues.wood() * rootQuality
                ),
                Math.max(
                        efficiencies.getOrDefault(ElementType.WATER, 0.0D).floatValue(),
                        environmentValues.water() * rootQuality
                ),
                Math.max(
                        efficiencies.getOrDefault(ElementType.FIRE, 0.0D).floatValue(),
                        environmentValues.fire() * rootQuality
                ),
                Math.max(
                        efficiencies.getOrDefault(ElementType.EARTH, 0.0D).floatValue(),
                        environmentValues.earth() * rootQuality
                )
        );
    }

    private static void syncStateToTrackingAndSelf(ServerPlayer player, boolean meditating) {
        MeditationAbsorptionProfile absorptionProfile = meditating
                ? calculateAbsorptionProfile(player)
                : MeditationAbsorptionProfile.EMPTY;
        ModNetwork.sendMeditationStateToTrackingAndSelf(
                player,
                meditating,
                meditating
                        ? ElementEnvironmentManager.INSTANCE.getFiveElementValues(player)
                        : FiveElementValues.EMPTY,
                absorptionProfile.visibleEfficiencies()
        );
    }

    private record MeditationRotation(float yRot, float xRot, float yHeadRot, float yBodyRot) {
        private static MeditationRotation capture(ServerPlayer player) {
            return new MeditationRotation(player.getYRot(), player.getXRot(), player.yHeadRot, player.yBodyRot);
        }
    }

    private record MeditationAbsorptionProfile(
            Map<ElementType, Double> efficiencies,
            FiveElementValues visibleEfficiencies
    ) {
        private static final MeditationAbsorptionProfile EMPTY = new MeditationAbsorptionProfile(
                Map.of(),
                FiveElementValues.EMPTY
        );

        private MeditationAbsorptionProfile {
            efficiencies = Map.copyOf(efficiencies);
        }
    }
}
