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
            syncStateToTrackingAndSelf(player, true);
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
                absorptionProfile.movementAffinities()
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
        if (spiritualRoot.isChaos() || spiritualRoot.elements().contains(ElementType.YIN_YANG)) {
            for (ElementType element : ElementType.values()) {
                efficiencies.put(
                        element,
                        spiritualRoot.quality()
                                * ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(player, element)
                );
            }
            return new MeditationAbsorptionProfile(
                    efficiencies,
                    SpiritualEnergyUtility.createMovementAffinities(spiritualRoot)
            );
        }

        for (ElementType element : spiritualRoot.elements()) {
            double environmentMultiplier = ElementEnvironmentManager.INSTANCE.getEnvironmentMultiplier(
                    player,
                    element
            );
            double interactionMultiplier = SpiritualEnergyUtility.getMultiElementInteractionMultiplier(
                    element,
                    spiritualRoot.elements()
            );
            efficiencies.put(
                    element,
                    spiritualRoot.quality() * environmentMultiplier * interactionMultiplier
            );
        }
        return new MeditationAbsorptionProfile(
                efficiencies,
                SpiritualEnergyUtility.createMovementAffinities(spiritualRoot)
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
                absorptionProfile.movementAffinities()
        );
    }

    private record MeditationRotation(float yRot, float xRot, float yHeadRot, float yBodyRot) {
        private static MeditationRotation capture(ServerPlayer player) {
            return new MeditationRotation(player.getYRot(), player.getXRot(), player.yHeadRot, player.yBodyRot);
        }
    }

    private record MeditationAbsorptionProfile(
            Map<ElementType, Double> efficiencies,
            FiveElementValues movementAffinities
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
