package net.seraphina.ninecun_lingtai.common.ascension;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.common.ai.AscensionComprehensionService;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.utility.AscensionPromptUtility;
import net.seraphina.ninecun_lingtai.utility.ChineseNumeralUtility;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.seraphina.ninecun_lingtai.utility.RealmSpawnUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRealmUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

public final class AscensionManager {
    public static final int TOTAL_STRIKES = 81;

    private static final int ASCENSION_STRIKE_INTERVAL_TICKS = 10;
    private static final int ASCENSION_PASSING_SCORE = 60;
    private static final double ASCENSION_REMAINING_SPIRITUAL_ENERGY_RATIO = 0.01D;
    private static final double ASCENSION_STRIKE_SPIRITUAL_ENERGY_RATIO = 0.84D;
    private static final double ASCENSION_DEFENSE_RESOURCE_RATIO = 0.12D;
    private static final Map<UUID, AscensionSession> SESSIONS = new HashMap<>();

    public static StartResult request(ServerPlayer player) {
        if (isInAscension(player)) {
            return StartResult.ALREADY_ACTIVE;
        }
        if (!isAscensionRequired(player)) {
            return StartResult.NOT_REQUIRED;
        }
        if (TribulationManager.isInTribulation(player)) {
            return StartResult.TRIBULATION_ACTIVE;
        }
        CultivationStage targetStage = new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1);
        if (!PlayerCultivationManager.hasEnoughEnergyForTarget(player, targetStage)) {
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        AscensionSession session = new AscensionSession(
                player.getUUID(),
                player.position(),
                player.level().dimension(),
                AscensionPromptUtility.createQuestions(player),
                CommonConfig.TRIBULATION_PREPARATION_TICKS.get()
        );
        SESSIONS.put(player.getUUID(), session);
        if (!PlayerMeditationManager.startAscensionMeditating(player)) {
            SESSIONS.remove(player.getUUID());
            return StartResult.CANNOT_MEDITATE;
        }
        if (!PlayerCultivationManager.consumeEnergyForTarget(player, targetStage)) {
            SESSIONS.remove(player.getUUID());
            PlayerMeditationManager.finishAscensionMeditating(player);
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }
        PlayerSpiritualEnergyData.retainEnergyRatio(player, ASCENSION_REMAINING_SPIRITUAL_ENERGY_RATIO);
        NotificationUtility.send(
                player,
                Component.literal("飞升劫将临"),
                Component.literal("九九雷劫之后，天道将问你所修之道。"),
                20 * 6
        );
        ModNetwork.sendAscensionState(player, session);
        return StartResult.STARTED;
    }

    public static boolean isAscensionRequired(ServerPlayer player) {
        return PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm() == CultivationRealm.NASCENT_SOUL
                        && stage.layer() == CultivationStage.MAXIMUM_LAYER)
                .orElse(false);
    }

    public static boolean isInAscension(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static boolean isInAscension(net.minecraft.world.entity.player.Player player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static Optional<AscensionSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(SESSIONS.get(player.getUUID()));
    }

    public static void tick(ServerPlayer player) {
        AscensionSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!player.isAlive()
                || player.isSpectator()
                || !player.level().dimension().equals(session.dimension())) {
            fail(player);
            return;
        }
        if (player.distanceToSqr(session.center()) > 1.0D) {
            player.teleportTo(session.center().x, session.center().y, session.center().z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (!PlayerMeditationManager.isMeditating(player)
                && !PlayerMeditationManager.startAscensionMeditating(player)) {
            fail(player);
            return;
        }
        spawnCloudParticles(player, session);

        switch (session.phase()) {
            case PREPARING -> tickPreparing(player, session);
            case STRIKING -> tickStriking(player, session);
            case QUESTIONS -> {
            }
        }
        if (player.tickCount % 10 == 0 && SESSIONS.get(player.getUUID()) == session) {
            ModNetwork.sendAscensionState(player, session);
        }
    }

    public static void submitAnswer(ServerPlayer player, int questionIndex, String answer) {
        AscensionSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.phase() != AscensionPhase.QUESTIONS) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("问心劫尚未开启。")
            );
            return;
        }
        if (questionIndex != session.questionIndex()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天道重现当前一问，请据实作答。")
            );
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }
        if (answer == null || answer.isBlank()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("此问不可留白，请重新作答。")
            );
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }
        if (session.isEvaluationPending()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天道正在衡量此答。")
            );
            return;
        }

        AscensionQuestion question = session.currentQuestion();
        String profileContext = AscensionPromptUtility.createProfileContext(player);
        session.setEvaluationPending(true);
        MinecraftServer server = player.server;
        UUID playerId = player.getUUID();
        int evaluatedQuestionIndex = session.questionIndex();
        AscensionComprehensionService.evaluate(question, profileContext, answer)
                .whenComplete((assessment, error) -> server.execute(() -> finishEvaluation(
                        server,
                        playerId,
                        evaluatedQuestionIndex,
                        assessment,
                        error
                )));
    }

    public static void fail(ServerPlayer player) {
        AscensionSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        PlayerMeditationManager.finishAscensionMeditating(player);
        ModNetwork.sendAscensionState(player, session, false);
    }

    public static void syncToTracking(ServerPlayer recipient, ServerPlayer subject) {
        AscensionSession session = SESSIONS.get(subject.getUUID());
        if (session != null) {
            ModNetwork.sendAscensionStateTo(recipient, session);
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static void tickPreparing(ServerPlayer player, AscensionSession session) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 40 == 0) {
            level.playSound(
                    null,
                    session.center().x,
                    session.center().y,
                    session.center().z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    0.70F,
                    0.78F + level.random.nextFloat() * 0.18F
            );
        }
        session.tickPhase();
        if (session.phaseRemainingTicks() <= 0) {
            session.startStriking(0);
            spawnStrike(player, session);
        }
    }

    private static void tickStriking(ServerPlayer player, AscensionSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        if (session.completedStrikes() >= TOTAL_STRIKES) {
            session.startQuestions();
            ModNetwork.sendAscensionState(player, session);
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }
        spawnStrike(player, session);
    }

    private static void spawnStrike(ServerPlayer player, AscensionSession session) {
        ServerLevel level = player.serverLevel();
        int strikeIndex = session.completedStrikes() + 1;
        RandomSource random = level.random;
        double x = session.center().x + (random.nextDouble() - 0.5D) * 3.0D;
        double z = session.center().z + (random.nextDouble() - 0.5D) * 3.0D;
        double y = session.center().y;

        PlayerSpiritualEnergyData.retainEnergyRatio(player, ASCENSION_STRIKE_SPIRITUAL_ENERGY_RATIO);
        drainOrdinaryDefenseResource(player);
        ThunderTribulationEntity lightning = new ThunderTribulationEntity(
                ModEntityTypes.THUNDER_TRIBULATION.get(),
                level
        );
        lightning.initialize(
                player,
                strikeIndex,
                getStrikeDamage(strikeIndex),
                1.20F + strikeIndex / (float) TOTAL_STRIKES * 0.65F
        );
        lightning.moveTo(x, y, z, 0.0F, 0.0F);
        if (level.addFreshEntity(lightning)) {
            session.completeStrike();
            level.playSound(
                    null,
                    x,
                    y,
                    z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.WEATHER,
                    1.35F,
                    0.62F + random.nextFloat() * 0.12F
            );
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    x,
                    y + 1.0D,
                    z,
                    40,
                    1.9D,
                    2.1D,
                    1.9D,
                    0.22D
            );
        }
        int nextDelay = session.completedStrikes() >= TOTAL_STRIKES
                ? CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                : ASCENSION_STRIKE_INTERVAL_TICKS;
        session.startStriking(nextDelay);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
        ModNetwork.sendAscensionState(player, session);
    }

    private static void drainOrdinaryDefenseResource(ServerPlayer player) {
        if (DefenseSpellUtility.getGuardian(player).isPresent()
                || PlayerDefenseSpellData.getBodyShield(player).isPresent()) {
            return;
        }
        int currentDefense = PlayerDefenseResourceData.getCurrent(player);
        if (currentDefense <= 0) {
            return;
        }
        int loss = Math.max(1, (int) Math.ceil(currentDefense * ASCENSION_DEFENSE_RESOURCE_RATIO));
        PlayerDefenseResourceData.consume(player, Math.min(currentDefense, loss));
    }

    private static float getStrikeDamage(int strikeIndex) {
        return 0.05F + Math.max(0, strikeIndex - 1) * 0.0015F;
    }

    private static void finishEvaluation(
            MinecraftServer server,
            UUID playerId,
            int evaluatedQuestionIndex,
            TechniqueAssessment assessment,
            Throwable error
    ) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || player.hasDisconnected()) {
            return;
        }
        AscensionSession session = SESSIONS.get(playerId);
        if (session == null
                || session.phase() != AscensionPhase.QUESTIONS
                || session.questionIndex() != evaluatedQuestionIndex
                || !session.isEvaluationPending()) {
            return;
        }
        session.setEvaluationPending(false);
        if (error != null || assessment == null) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天机未应，此问须重新作答。")
            );
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }
        if (assessment.score() < ASCENSION_PASSING_SCORE) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    createAssessmentFeedback(false, assessment)
            );
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }

        ModNetwork.sendCultivationFeedback(
                player,
                true,
                createAssessmentFeedback(true, assessment)
        );
        if (session.advanceQuestion()) {
            ModNetwork.sendOpenAscensionQuestion(player, session);
            return;
        }
        complete(player, session);
    }

    private static Component createAssessmentFeedback(
            boolean passed,
            TechniqueAssessment assessment
    ) {
        String verdict = passed ? "天道认可" : "天道不允";
        String score = ChineseNumeralUtility.toChinese(assessment.score());
        String feedback = ChineseNumeralUtility.replaceArabicNumerals(assessment.feedback());
        return Component.literal(verdict + "（" + score + "分）：" + feedback);
    }

    private static void complete(ServerPlayer player, AscensionSession session) {
        List<ServerPlayer> sourcePlayers = List.copyOf(player.serverLevel().players());
        PlayerCultivationData.setStage(
                player,
                new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)
        );
        PlayerDefenseResourceData.refillToMaximum(player);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);

        SESSIONS.remove(player.getUUID());
        PlayerMeditationManager.finishAscensionMeditating(player);
        ModNetwork.sendAscensionState(player, session, false);
        Component title = Component.literal("恭喜 " + player.getGameProfile().getName() + " 道友，得道飞升");
        for (ServerPlayer sourcePlayer : sourcePlayers) {
            NotificationUtility.send(sourcePlayer, title, Component.empty(), 20 * 3);
        }
        teleportToSpiritualRealm(player);
    }

    private static void teleportToSpiritualRealm(ServerPlayer player) {
        ResourceKey<Level> destinationKey = ResourceKey.create(
                Registries.DIMENSION,
                SpiritualRealmUtility.SPIRITUAL_REALM_ID
        );
        ServerLevel destination = player.server.getLevel(destinationKey);
        if (destination == null) {
            ModMain.LOGGER.error(
                    "Unable to find the spiritual realm while ascending player {}",
                    player.getGameProfile().getName()
            );
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("灵界尚未开启，暂留此界。")
            );
            return;
        }
        Vec3 destinationPosition = RealmSpawnUtility.findSafeSpawnPosition(
                destination,
                new Random(),
                CommonConfig.THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS.get()
        );
        player.changeDimension(
                destination,
                createTeleporter(destinationPosition, player.getYRot(), player.getXRot())
        );
    }

    private static ITeleporter createTeleporter(Vec3 destination, float yRot, float xRot) {
        return new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(
                    Entity entity,
                    ServerLevel destinationLevel,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo
            ) {
                return new PortalInfo(destination, Vec3.ZERO, yRot, xRot);
            }

            @Override
            public Entity placeEntity(
                    Entity entity,
                    ServerLevel currentLevel,
                    ServerLevel destinationLevel,
                    float yaw,
                    Function<Boolean, Entity> repositionEntity
            ) {
                return repositionEntity.apply(false);
            }

            @Override
            public boolean playTeleportSound(
                    ServerPlayer player,
                    ServerLevel sourceLevel,
                    ServerLevel destinationLevel
            ) {
                return false;
            }
        };
    }

    private static void spawnCloudParticles(ServerPlayer player, AscensionSession session) {
        ServerLevel level = player.serverLevel();
        float intensity = switch (session.phase()) {
            case PREPARING -> 0.20F + (1.0F - Mth.clamp(
                    session.phaseRemainingTicks()
                            / (float) Math.max(1, CommonConfig.TRIBULATION_PREPARATION_TICKS.get()),
                    0.0F,
                    1.0F
            )) * 0.80F;
            case STRIKING -> 1.0F;
            case QUESTIONS -> 0.75F;
        };
        level.sendParticles(
                ParticleTypes.CLOUD,
                session.center().x,
                session.center().y + 12.0D,
                session.center().z,
                Math.max(4, Math.round(8.0F * intensity)),
                24.0D,
                2.2D,
                24.0D,
                0.018D
        );
        level.sendParticles(
                ParticleTypes.SMOKE,
                session.center().x,
                session.center().y + 13.0D,
                session.center().z,
                Math.max(2, Math.round(5.0F * intensity)),
                20.0D,
                1.7D,
                20.0D,
                0.010D
        );
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        NOT_REQUIRED,
        TRIBULATION_ACTIVE,
        INSUFFICIENT_SPIRITUAL_ENERGY,
        CANNOT_MEDITATE
    }
}
