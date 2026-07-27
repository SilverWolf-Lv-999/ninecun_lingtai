package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualEnergyUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerMeditationManager {
    private static final int ABSORPTION_INTERVAL_TICKS = 20;
    private static final int PROFICIENCY_INTERVAL_TICKS = 1200;
    private static final Map<UUID, MeditationSession> MEDITATING_PLAYERS = new HashMap<>();

    private PlayerMeditationManager() {
    }

    public static boolean startMeditating(ServerPlayer player) {
        return startMeditating(player, null);
    }

    public static boolean startMeditating(ServerPlayer player, @Nullable ResourceLocation techniqueId) {
        if (techniqueId != null && !canCultivateTechnique(player, techniqueId)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.technique_not_learned"),
                    true
            );
            syncSelfState(player, isMeditating(player));
            return false;
        }

        MeditationSession existingSession = MEDITATING_PLAYERS.get(player.getUUID());
        if (existingSession != null) {
            existingSession.setTechnique(techniqueId);
            syncStateToTrackingAndSelf(player, true);
            return true;
        }
        if (!canStartMeditating(player)) {
            syncSelfState(player, false);
            return false;
        }
        MEDITATING_PLAYERS.put(
                player.getUUID(),
                new MeditationSession(MeditationRotation.capture(player), techniqueId)
        );
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        if (techniqueId == null) {
            remindBreathingTechniqueRequired(player);
        }
        syncStateToTrackingAndSelf(player, true);
        return true;
    }

    public static void stopMeditating(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.remove(player.getUUID());
        if (session != null
                && session.techniqueId() != null
                && session.gainedProficiency() > 0L
                && !player.hasDisconnected()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.technique_training_summary",
                            getTechniqueName(session.techniqueId()),
                            session.gainedProficiency()
                    ),
                    false
            );
        }
        syncStateToTrackingAndSelf(player, false);
    }

    public static boolean isMeditating(ServerPlayer player) {
        return MEDITATING_PLAYERS.containsKey(player.getUUID());
    }

    public static Optional<ResourceLocation> getCultivatedTechnique(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        return session == null
                ? Optional.empty()
                : Optional.ofNullable(session.techniqueId());
    }

    public static void tickMeditation(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!canContinueMeditating(player)) {
            stopMeditating(player);
            return;
        }
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        if (session.techniqueId() == null) {
            // Realm cultivation is impossible without a breathing technique guiding the qi.
            if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
                remindBreathingTechniqueRequired(player);
                syncStateToTrackingAndSelf(player, true);
            }
            return;
        }
        if (session.incrementTechniqueTicks() >= PROFICIENCY_INTERVAL_TICKS) {
            session.resetTechniqueTicks();
            grantProficiency(player, session);
        }
        if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
            double breathingMultiplier = getBreathingAbsorptionMultiplier(player, session.techniqueId());
            if (breathingMultiplier > 0.0D) {
                absorbSpiritualEnergy(player, breathingMultiplier);
            }
            syncStateToTrackingAndSelf(player, true);
        }
    }

    public static void syncState(ServerPlayer recipient, ServerPlayer trackedPlayer) {
        boolean meditating = isMeditating(trackedPlayer);
        MeditationSyncData syncData = createMeditationSyncData(trackedPlayer, meditating);
        ModNetwork.sendMeditationState(
                recipient,
                trackedPlayer.getUUID(),
                meditating,
                syncData.environmentValues(),
                syncData.absorptionProfile().movementAffinities(),
                getRealmLevel(trackedPlayer)
        );
    }

    public static void clear() {
        MEDITATING_PLAYERS.clear();
    }

    public static long getProficiencyGainPerMinute(Player player) {
        return 1L << Math.min(getRealmLevel(player), 62);
    }

    /**
     * Layer requirements grow super-exponentially, so absorption must follow the requirement
     * curve — a per-realm constant would leave high realms absorbing a rounding error of what
     * their next layer needs. The exponent below 1 keeps higher layers slightly slower overall.
     */
    public static double getRealmAbsorptionMultiplier(Player player) {
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        BigInteger nextRequirement = CultivationUtility.getNextRequiredSpiritualEnergy(stage)
                .orElseGet(() -> CultivationUtility.getRequiredSpiritualEnergy(stage.orElseThrow()));
        double requirementScale = Math.max(
                1.0D,
                nextRequirement.doubleValue()
                        / CultivationUtility.getRequiredSpiritualEnergy(CultivationStage.first()).doubleValue()
        );
        return Math.pow(requirementScale, CommonConfig.MEDITATION_ABSORPTION_REQUIREMENT_EXPONENT.get());
    }

    public static int getRealmLevel(Player player) {
        return PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal())
                .orElse(0);
    }

    private static boolean canCultivateTechnique(ServerPlayer player, ResourceLocation techniqueId) {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId).isPresent()
                && PlayerTechniqueData.hasTechnique(player, techniqueId);
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
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        MeditationRotation rotation = session.rotation();
        player.setYRot(rotation.yRot());
        player.setXRot(rotation.xRot());
        player.setYHeadRot(rotation.yHeadRot());
        player.setYBodyRot(rotation.yBodyRot());
        player.yRotO = rotation.yRot();
        player.xRotO = rotation.xRot();
        player.yHeadRotO = rotation.yHeadRot();
        player.yBodyRotO = rotation.yBodyRot();
    }

    private static void absorbSpiritualEnergy(ServerPlayer player, double breathingMultiplier) {
        Optional<ItemStack> spiritualStone = getHeldUsableSpiritualStone(player);
        if (spiritualStone.isPresent()) {
            absorbHeldSpiritualStoneEnergy(player, spiritualStone.get(), breathingMultiplier);
            return;
        }

        MeditationAbsorptionProfile absorptionProfile = calculateAbsorptionProfile(player);
        if (absorptionProfile.absorptionRates().isEmpty()) {
            return;
        }
        double baseAbsorption = CommonConfig.MEDITATION_BASE_ABSORPTION_PER_SECOND.get()
                * breathingMultiplier
                * getRealmAbsorptionMultiplier(player);
        for (Map.Entry<ElementType, Double> entry : absorptionProfile.absorptionRates().entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), baseAbsorption * entry.getValue());
        }
        tryAdvanceLayer(player);
    }

    private static void absorbHeldSpiritualStoneEnergy(
            ServerPlayer player,
            ItemStack spiritualStone,
            double breathingMultiplier
    ) {
        Optional<SpiritualRoot> spiritualRootOptional = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRootOptional.isEmpty()) {
            return;
        }

        Map<ElementType, Double> absorptionRates = SpiritualEnergyUtility.createUniversalCultivationAbsorptionRates(
                spiritualRootOptional.get()
        );
        if (absorptionRates.isEmpty()) {
            return;
        }

        double baseAbsorption = CommonConfig.MEDITATION_BASE_ABSORPTION_PER_SECOND.get()
                * breathingMultiplier
                * getRealmAbsorptionMultiplier(player)
                * SpiritualStoneUtility.getAbsorptionMultiplier(spiritualStone);
        double requestedEnergy = absorptionRates.values().stream()
                .mapToDouble(rate -> baseAbsorption * rate)
                .sum();
        if (requestedEnergy <= 0.0D) {
            return;
        }

        double drainedEnergy = SpiritualStoneUtility.drainEnergy(spiritualStone, requestedEnergy);
        if (drainedEnergy <= 0.0D) {
            return;
        }

        double absorptionRatio = drainedEnergy / requestedEnergy;
        for (Map.Entry<ElementType, Double> entry : absorptionRates.entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(
                    player,
                    entry.getKey(),
                    baseAbsorption * entry.getValue() * absorptionRatio
            );
        }
        tryAdvanceLayer(player);
    }

    /**
     * Absorption multiplier of the breathing technique the player is currently cultivating with,
     * scaled by how far the technique has been learned. Returns {@code 0} when the technique is
     * not a breathing technique, so plain technique training draws in no spiritual energy.
     */
    private static double getBreathingAbsorptionMultiplier(ServerPlayer player, ResourceLocation techniqueId) {
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(techniqueId);
        if (technique.isEmpty() || technique.get().breathing().isEmpty()) {
            return 0.0D;
        }
        double masteryEfficiency = PlayerTechniqueData.getTechnique(player, techniqueId)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY)
                .getEfficiency();
        return technique.get().breathing().get().absorptionMultiplier() * masteryEfficiency;
    }

    private static void remindBreathingTechniqueRequired(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.breathing_technique_required"),
                true
        );
    }

    private static void tryAdvanceLayer(ServerPlayer player) {
        BreakthroughResult result = PlayerCultivationManager.tryAdvanceLayer(player);
        if (result != BreakthroughResult.SUCCESS) {
            return;
        }
        CultivationStage stage = PlayerCultivationData.getStage(player).orElseThrow();
        player.displayClientMessage(
                Component.empty()
                        .append(player.getDisplayName())
                        .append(" 踏入" + CultivationUtility.getDisplayName(stage)),
                false
        );
        PlayerDefenseResourceData.refillToMaximum(player);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
    }

    private static void grantProficiency(ServerPlayer player, MeditationSession session) {
        ResourceLocation techniqueId = session.techniqueId();
        if (techniqueId == null) {
            return;
        }
        if (!canCultivateTechnique(player, techniqueId)) {
            stopMeditating(player);
            return;
        }
        long gain = getProficiencyGainPerMinute(player);
        long total = PlayerTechniqueManager.addProficiency(player, techniqueId, gain);
        session.addGainedProficiency(gain);
        player.displayClientMessage(
                Component.translatable(
                        "message.ninecun_lingtai.proficiency_gain",
                        getTechniqueName(techniqueId),
                        gain,
                        total
                ),
                true
        );
    }

    private static String getTechniqueName(ResourceLocation techniqueId) {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .map(TechniqueDefinition::name)
                .orElse(techniqueId.toString());
    }

    private static MeditationAbsorptionProfile calculateAbsorptionProfile(ServerPlayer player) {
        Optional<SpiritualRoot> spiritualRootOptional = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRootOptional.isEmpty()) {
            return MeditationAbsorptionProfile.EMPTY;
        }

        SpiritualRoot spiritualRoot = spiritualRootOptional.get();
        FiveElementValues environmentValues = ElementEnvironmentManager.INSTANCE.getFiveElementValues(player);
        return new MeditationAbsorptionProfile(
                SpiritualEnergyUtility.createCultivationAbsorptionRates(spiritualRoot, environmentValues),
                SpiritualEnergyUtility.createMovementAffinities(spiritualRoot)
        );
    }

    private static Optional<ItemStack> getHeldUsableSpiritualStone(ServerPlayer player) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (SpiritualStoneUtility.hasRemainingEnergy(mainHandItem)) {
            return Optional.of(mainHandItem);
        }
        ItemStack offhandItem = player.getOffhandItem();
        if (SpiritualStoneUtility.hasRemainingEnergy(offhandItem)) {
            return Optional.of(offhandItem);
        }
        return Optional.empty();
    }

    private static MeditationSyncData createMeditationSyncData(ServerPlayer player, boolean meditating) {
        if (!meditating || isAbsorbingFromHeldSpiritualStone(player)) {
            return MeditationSyncData.EMPTY;
        }
        return new MeditationSyncData(
                ElementEnvironmentManager.INSTANCE.getFiveElementValues(player),
                calculateAbsorptionProfile(player)
        );
    }

    private static boolean isAbsorbingFromHeldSpiritualStone(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        if (session == null || session.techniqueId() == null || getHeldUsableSpiritualStone(player).isEmpty()) {
            return false;
        }
        return getBreathingAbsorptionMultiplier(player, session.techniqueId()) > 0.0D;
    }

    private static void syncSelfState(ServerPlayer player, boolean meditating) {
        MeditationSyncData syncData = createMeditationSyncData(player, meditating);
        ModNetwork.sendMeditationState(
                player,
                player.getUUID(),
                meditating,
                syncData.environmentValues(),
                syncData.absorptionProfile().movementAffinities(),
                getRealmLevel(player)
        );
    }

    private static void syncStateToTrackingAndSelf(ServerPlayer player, boolean meditating) {
        MeditationSyncData syncData = createMeditationSyncData(player, meditating);
        ModNetwork.sendMeditationStateToTrackingAndSelf(
                player,
                meditating,
                syncData.environmentValues(),
                syncData.absorptionProfile().movementAffinities(),
                getRealmLevel(player)
        );
    }

    private static final class MeditationSession {
        private final MeditationRotation rotation;
        @Nullable
        private ResourceLocation techniqueId;
        private int techniqueTicks;
        private long gainedProficiency;

        private MeditationSession(MeditationRotation rotation, @Nullable ResourceLocation techniqueId) {
            this.rotation = rotation;
            this.techniqueId = techniqueId;
        }

        private MeditationRotation rotation() {
            return this.rotation;
        }

        @Nullable
        private ResourceLocation techniqueId() {
            return this.techniqueId;
        }

        private void setTechnique(@Nullable ResourceLocation updatedTechniqueId) {
            if (!java.util.Objects.equals(this.techniqueId, updatedTechniqueId)) {
                this.techniqueId = updatedTechniqueId;
                this.techniqueTicks = 0;
            }
        }

        private int incrementTechniqueTicks() {
            this.techniqueTicks++;
            return this.techniqueTicks;
        }

        private void resetTechniqueTicks() {
            this.techniqueTicks = 0;
        }

        private long gainedProficiency() {
            return this.gainedProficiency;
        }

        private void addGainedProficiency(long amount) {
            this.gainedProficiency += Math.max(0L, amount);
        }
    }

    private record MeditationSyncData(
            FiveElementValues environmentValues,
            MeditationAbsorptionProfile absorptionProfile
    ) {
        private static final MeditationSyncData EMPTY = new MeditationSyncData(
                FiveElementValues.EMPTY,
                MeditationAbsorptionProfile.EMPTY
        );
    }

    private record MeditationRotation(float yRot, float xRot, float yHeadRot, float yBodyRot) {
        private static MeditationRotation capture(ServerPlayer player) {
            return new MeditationRotation(player.getYRot(), player.getXRot(), player.yHeadRot, player.yBodyRot);
        }
    }

    private record MeditationAbsorptionProfile(
            Map<ElementType, Double> absorptionRates,
            FiveElementValues movementAffinities
    ) {
        private static final MeditationAbsorptionProfile EMPTY = new MeditationAbsorptionProfile(
                Map.of(),
                FiveElementValues.EMPTY
        );

        private MeditationAbsorptionProfile {
            absorptionRates = Map.copyOf(absorptionRates);
        }
    }
}
