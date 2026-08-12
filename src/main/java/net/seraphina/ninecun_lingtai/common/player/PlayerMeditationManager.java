package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualEnergyUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerMeditationManager {
    private static final int ABSORPTION_INTERVAL_TICKS = 20;
    private static final int PROFICIENCY_INTERVAL_TICKS = 1200;
    private static final Map<UUID, MeditationSession> MEDITATING_PLAYERS = new HashMap<>();

    public static boolean startMeditating(ServerPlayer player) {
        return startMeditating(player, null, false);
    }

    public static boolean startMeditating(ServerPlayer player, @Nullable ResourceLocation techniqueId) {
        return startMeditating(player, techniqueId, false);
    }

    public static boolean startMeditating(
            ServerPlayer player,
            @Nullable ResourceLocation techniqueId,
            boolean cultivatingRealm
    ) {
        if (TribulationManager.isInTribulation(player)) {
            return startTribulationMeditating(player);
        }
        if (techniqueId != null && !canCultivateTechnique(player, techniqueId)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.technique_not_learned"),
                    true
            );
            syncSelfState(player, isMeditating(player));
            return false;
        }
        if (cultivatingRealm && !isBreathingTechnique(techniqueId)) {
            remindBreathingTechniqueRequired(player);
            syncSelfState(player, isMeditating(player));
            return false;
        }

        MeditationSession existingSession = MEDITATING_PLAYERS.get(player.getUUID());
        if (existingSession != null) {
            existingSession.setCultivation(techniqueId, cultivatingRealm);
            syncStateToTrackingAndSelf(player, true);
            return true;
        }
        if (!canStartMeditating(player)) {
            syncSelfState(player, false);
            return false;
        }
        MEDITATING_PLAYERS.put(
                player.getUUID(),
                new MeditationSession(MeditationRotation.capture(player), techniqueId, cultivatingRealm)
        );
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        syncStateToTrackingAndSelf(player, true);
        return true;
    }

    public static boolean startTribulationMeditating(ServerPlayer player) {
        MeditationSession existingSession = MEDITATING_PLAYERS.get(player.getUUID());
        if (existingSession != null) {
            existingSession.setCultivation(null, false);
            syncStateToTrackingAndSelf(player, true);
            return true;
        }
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return false;
        }
        MEDITATING_PLAYERS.put(
                player.getUUID(),
                new MeditationSession(MeditationRotation.capture(player), null, false)
        );
        player.stopUsingItem();
        stopMovement(player);
        lockRotation(player);
        syncStateToTrackingAndSelf(player, true);
        return true;
    }

    public static void stopMeditating(ServerPlayer player) {
        if (TribulationManager.isInTribulation(player)) {
            syncStateToTrackingAndSelf(player, true);
            return;
        }
        stopMeditatingInternal(player);
    }

    public static void finishTribulationMeditating(ServerPlayer player) {
        stopMeditatingInternal(player);
    }

    private static void stopMeditatingInternal(ServerPlayer player) {
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

    public static boolean isCultivatingRealm(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        return session != null && session.cultivatingRealm();
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
        if (TribulationManager.isInTribulation(player)) {
            if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
                syncStateToTrackingAndSelf(player, true);
            }
            return;
        }
        if (session.techniqueId() == null) {
            // Plain meditation has no technique training or spiritual-energy absorption.
            if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
                repairDefenseResource(player);
                syncStateToTrackingAndSelf(player, true);
            }
            return;
        }
        if (session.incrementTechniqueTicks() >= PROFICIENCY_INTERVAL_TICKS) {
            session.resetTechniqueTicks();
            grantProficiency(player, session);
        }
        if (player.tickCount % ABSORPTION_INTERVAL_TICKS == 0) {
            if (session.cultivatingRealm()) {
                double breathingMultiplier = getBreathingAbsorptionMultiplier(player, session.techniqueId());
                if (breathingMultiplier > 0.0D) {
                    absorbSpiritualEnergy(player, breathingMultiplier);
                    ModNetwork.sendCultivationInfo(player);
                }
            }
            repairDefenseResource(player);
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
        BigInteger nextRequirement = CultivationUtility.getMaximumAbsorbableSpiritualEnergy(stage);
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

    private static boolean isBreathingTechnique(@Nullable ResourceLocation techniqueId) {
        return techniqueId != null
                && TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .map(technique -> technique.breathing().isPresent())
                .orElse(false);
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
        Map<ElementType, Double> absorbableEnergy = calculateAbsorbableEnergy(
                player,
                absorptionProfile.absorptionRates(),
                baseAbsorption
        );
        for (Map.Entry<ElementType, Double> entry : absorbableEnergy.entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), entry.getValue());
        }
        if (!absorbableEnergy.isEmpty()) {
            tryAdvanceLayer(player);
        }
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
        Map<ElementType, Double> absorbableEnergy = calculateAbsorbableEnergy(
                player,
                absorptionRates,
                baseAbsorption
        );
        double requestedEnergy = absorbableEnergy.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        if (requestedEnergy <= 0.0D) {
            return;
        }

        double drainedEnergy = SpiritualStoneUtility.drainEnergy(spiritualStone, requestedEnergy);
        if (drainedEnergy <= 0.0D) {
            return;
        }

        double absorptionRatio = Math.min(1.0D, drainedEnergy / requestedEnergy);
        for (Map.Entry<ElementType, Double> entry : absorbableEnergy.entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(
                    player,
                    entry.getKey(),
                    entry.getValue() * absorptionRatio
            );
        }
        tryAdvanceLayer(player);
    }

    private static Map<ElementType, Double> calculateAbsorbableEnergy(
            ServerPlayer player,
            Map<ElementType, Double> absorptionRates,
            double baseAbsorption
    ) {
        double maximumEnergy = CultivationUtility.getMaximumAbsorbableSpiritualEnergy(
                PlayerCultivationData.getStage(player)
        ).doubleValue();
        if (!Double.isFinite(maximumEnergy) || maximumEnergy <= 0.0D
                || !Double.isFinite(baseAbsorption) || baseAbsorption <= 0.0D) {
            return Map.of();
        }

        EnumMap<ElementType, Double> absorbableEnergy = new EnumMap<>(ElementType.class);
        for (Map.Entry<ElementType, Double> entry : absorptionRates.entrySet()) {
            double requestedEnergy = baseAbsorption * entry.getValue();
            if (!Double.isFinite(requestedEnergy) || requestedEnergy <= 0.0D) {
                continue;
            }
            double remainingCapacity = maximumEnergy
                    - PlayerSpiritualEnergyData.getEnergy(player, entry.getKey());
            if (remainingCapacity > 0.0D) {
                absorbableEnergy.put(entry.getKey(), Math.min(requestedEnergy, remainingCapacity));
            }
        }
        return Map.copyOf(absorbableEnergy);
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

    private static void repairDefenseResource(ServerPlayer player) {
        int repairAmount = CultivationDefenseUtility.getDefenseResourceRepairAmount(
                PlayerCultivationData.getStage(player),
                CommonConfig.MEDITATION_DEFENSE_RESOURCE_REPAIR_PERCENT_PER_SECOND.get()
        );
        if (PlayerDefenseResourceData.repair(player, repairAmount) > 0) {
            ModNetwork.sendDefenseResource(player);
        }
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
        if (SpiritualStoneUtility.isUsableForCultivation(mainHandItem)) {
            return Optional.of(mainHandItem);
        }
        ItemStack offhandItem = player.getOffhandItem();
        if (SpiritualStoneUtility.isUsableForCultivation(offhandItem)) {
            return Optional.of(offhandItem);
        }
        return Optional.empty();
    }

    private static MeditationSyncData createMeditationSyncData(ServerPlayer player, boolean meditating) {
        if (!meditating
                || TribulationManager.isInTribulation(player)
                || !isCultivatingRealm(player)
                || isAbsorbingFromHeldSpiritualStone(player)) {
            return MeditationSyncData.EMPTY;
        }
        return new MeditationSyncData(
                ElementEnvironmentManager.INSTANCE.getFiveElementValues(player),
                calculateAbsorptionProfile(player)
        );
    }

    private static boolean isAbsorbingFromHeldSpiritualStone(ServerPlayer player) {
        MeditationSession session = MEDITATING_PLAYERS.get(player.getUUID());
        if (session == null
                || !session.cultivatingRealm()
                || session.techniqueId() == null
                || getHeldUsableSpiritualStone(player).isEmpty()) {
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
        private boolean cultivatingRealm;
        private int techniqueTicks;
        private long gainedProficiency;

        private MeditationSession(
                MeditationRotation rotation,
                @Nullable ResourceLocation techniqueId,
                boolean cultivatingRealm
        ) {
            this.rotation = rotation;
            this.techniqueId = techniqueId;
            this.cultivatingRealm = cultivatingRealm;
        }

        private MeditationRotation rotation() {
            return this.rotation;
        }

        @Nullable
        private ResourceLocation techniqueId() {
            return this.techniqueId;
        }

        private boolean cultivatingRealm() {
            return this.cultivatingRealm;
        }

        private void setCultivation(
                @Nullable ResourceLocation updatedTechniqueId,
                boolean updatedCultivatingRealm
        ) {
            if (!java.util.Objects.equals(this.techniqueId, updatedTechniqueId)
                    || this.cultivatingRealm != updatedCultivatingRealm) {
                this.techniqueId = updatedTechniqueId;
                this.cultivatingRealm = updatedCultivatingRealm;
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
