package net.seraphina.ninecun_lingtai.common.tribulation;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.insight.InsightPool;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.*;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.seraphina.ninecun_lingtai.common.risk.RiskContext;
import net.seraphina.ninecun_lingtai.common.risk.RiskFailureType;
import net.seraphina.ninecun_lingtai.common.risk.RiskSettlementService;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.seraphina.ninecun_lingtai.utility.TribulationUtility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;

import java.util.Optional;
import java.util.UUID;

public final class TribulationManager {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final double TRIBULATION_REMAINING_SPIRITUAL_ENERGY_RATIO = 0.01D;

    public static StartResult request(ServerPlayer player) {
        Optional<CultivationStage> tribulationTarget = getTribulationTarget(player);
        if (tribulationTarget.isEmpty()) {
            return StartResult.NOT_REQUIRED;
        }
        return startTribulation(player, tribulationTarget.get());
    }

    /**
     * 由引灵仪式掷骰成功后调用：跳过"是否需要渡劫"判定，直接对目标境界起劫。
     * 能量消费在劫内进行（仪式层不消费，防双扣）。
     */
    public static StartResult startForRitual(ServerPlayer player, CultivationStage targetStage) {
        return startTribulation(player, targetStage);
    }

    private static StartResult startTribulation(ServerPlayer player, CultivationStage targetStage) {
        if (isInTribulation(player)) {
            return StartResult.ALREADY_ACTIVE;
        }
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        Optional<SoulStrength> soulStrength = PlayerAptitudeData.getSoulStrength(player);
        if (spiritualRoot.isEmpty() || soulStrength.isEmpty()) {
            return StartResult.NO_APTITUDE;
        }
        if (!PlayerCultivationManager.hasEnoughEnergyForTarget(player, targetStage)) {
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        TribulationType type = TribulationUtility.selectTypeForTarget(
                targetStage.realm(),
                spiritualRoot.get(),
                soulStrength.get()
        );
        float aptitudeScore = TribulationUtility.getAptitudeScore(spiritualRoot.get(), soulStrength.get());
        TribulationSession session = new TribulationSession(
                player.getUUID(),
                type,
                player.position(),
                player.level().dimension(),
                aptitudeScore,
                TribulationUtility.getStrikeRadius(aptitudeScore),
                CommonConfig.TRIBULATION_PREPARATION_TICKS.get(),
                targetStage,
                TribulationUtility.getRealmStrengthMultiplier(targetStage.realm())
        );
        TribulationSavedData savedData = savedData(player);
        savedData.putSession(session);
        if (!PlayerMeditationManager.startTribulationMeditating(player)) {
            savedData.removeSession(player.getUUID());
            return StartResult.CANNOT_MEDITATE;
        }
        if (!PlayerCultivationManager.consumeEnergyForTarget(player, targetStage)) {
            savedData.removeSession(player.getUUID());
            PlayerMeditationManager.finishTribulationMeditating(player);
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }
        PlayerSpiritualEnergyData.retainEnergyRatio(
                player,
                TRIBULATION_REMAINING_SPIRITUAL_ENERGY_RATIO
        );
        // 心魔即己 §5.2：雷劫开始即锁定行为账本快照——天劫里临时换套路对心魔镜像无效。
        PlayerBehaviorLedgerData.lockSnapshot(player);
        NotificationUtility.send(
                player,
                Component.literal(type.chineseName()),
                Component.empty(),
                20 * 9
        );
        ModNetwork.sendTribulationState(player, session);
        return StartResult.STARTED;
    }

    public static Optional<CultivationStage> getTribulationTarget(ServerPlayer player) {
        // 排除化神目标：元婴9→化神走飞升劫（AscensionManager），修复普通九九劫抢占导致的飞升死路由。
        return PlayerCultivationData.getStage(player)
                .filter(stage -> stage.layer() == CultivationStage.MAXIMUM_LAYER)
                .flatMap(stage -> stage.realm().next())
                .filter(nextRealm -> nextRealm.ordinal() >= CultivationRealm.GOLDEN_CORE.ordinal()
                        && nextRealm != CultivationRealm.SPIRIT_TRANSFORMATION)
                .map(nextRealm -> new CultivationStage(nextRealm, 1));
    }

    public static boolean isInTribulation(ServerPlayer player) {
        return player != null && savedData(player).hasSession(player.getUUID());
    }

    public static boolean isInTribulation(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer serverPlayer && isInTribulation(serverPlayer);
    }

    public static Optional<TribulationSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(savedData(player).getSession(player.getUUID()));
    }

    public static void tick(ServerPlayer player) {
        TribulationSavedData savedData = savedData(player);
        TribulationSession session = savedData.getSession(player.getUUID());
        if (session == null) {
            return;
        }
        // R1：心魔试炼中玩家本体在心境维度——必须短路"维度不符即 fail"与"拽回 center"，
        // 否则 MindRealmManager.enter 的传送会当场判定渡劫失败。
        boolean inMindTrial = session.isInMindTrial();
        if (!player.isAlive()
                || player.isSpectator()
                || (!inMindTrial && !player.level().dimension().equals(session.dimension()))) {
            fail(player);
            return;
        }
        if (session.consumeRestoredFlag()) {
            resumeAfterLoad(player, session);
            savedData.setDirty();
            return;
        }
        if (!inMindTrial) {
            if (player.distanceToSqr(session.center()) > 1.0D) {
                player.teleportTo(session.center().x, session.center().y, session.center().z);
            }
            player.setDeltaMovement(Vec3.ZERO);
            if (!PlayerMeditationManager.isMeditating(player)) {
                PlayerMeditationManager.startTribulationMeditating(player);
            }
            spawnCloudParticles(player, session);
        }

        switch (session.phase()) {
            case PREPARING -> tickPreparing(player, session);
            case STRIKING -> tickStriking(player, session);
            case INNER_DEMON -> tickInnerDemon(player, session);
            case AFTERMATH -> tickAftermath(player, session);
            case DISSIPATING -> tickDissipating(player, session);
        }
        savedData.setDirty();
        if (player.tickCount % 10 == 0 && savedData.getSession(player.getUUID()) == session) {
            ModNetwork.sendTribulationState(player, session);
        }
    }

    /**
     * Unified failure entry point. Every failure path (death, logout, dimension change,
     * spectator switch, inner demon timeout) must funnel into {@link #fail(ServerPlayer,
     * TribulationSession, FailureReason)} so the penalty settlement happens in one place.
     */
    public static void fail(ServerPlayer player) {
        TribulationSavedData savedData = savedData(player);
        TribulationSession session = savedData.getSession(player.getUUID());
        if (session == null) {
            return;
        }
        if (player.hasDisconnected() && !player.server.isRunning()) {
            // Server shutdown disconnects every player; that must not count as a personal
            // failure, otherwise the persisted session could never be resumed after a restart.
            suspend(player, session);
            return;
        }
        fail(player, session, inferFailureReason(player, session));
    }

    public static void fail(ServerPlayer player, TribulationSession session, FailureReason reason) {
        savedData(player).removeSession(player.getUUID());
        // 心魔即己：若处于心魔试炼中，先做心境侧清理（送回本体、清替身/心魔、
        // 释放分格、心障落地）。abort 内部不会回调 fail，无递归。
        MindTrialManager.abort(player, session);
        clearInnerDemons(player, session);
        PlayerMeditationManager.finishTribulationMeditating(player);
        settleFailurePenalty(player, session, reason);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendTribulationState(player, session, false);
    }

    public static void syncToTracking(ServerPlayer recipient, ServerPlayer subject) {
        TribulationSession session = savedData(subject).getSession(subject.getUUID());
        if (session != null) {
            ModNetwork.sendTribulationStateTo(recipient, session);
        }
    }

    public static void clear() {
        // Sessions now live in TribulationSavedData, which is persisted with the world before
        // ServerStoppedEvent fires; there is no static state left to reset. The method is kept
        // because TribulationEventHandler still wires it to ServerStoppedEvent.
    }

    private static TribulationSavedData savedData(ServerPlayer player) {
        return TribulationSavedData.get(player.server);
    }

    private static FailureReason inferFailureReason(ServerPlayer player, TribulationSession session) {
        if (!player.isAlive()) {
            return FailureReason.DEATH;
        }
        if (player.hasDisconnected()) {
            return FailureReason.LOGOUT;
        }
        if (!player.level().dimension().equals(session.dimension())) {
            return FailureReason.DIMENSION_CHANGED;
        }
        return FailureReason.INTERRUPTED;
    }

    /**
     * Suspends (instead of fails) a session when the player is disconnected by a server
     * shutdown: the session stays in {@link TribulationSavedData} and is resumed by
     * {@link #resumeAfterLoad} on the player's first tick after re-login.
     */
    private static void suspend(ServerPlayer player, TribulationSession session) {
        // 心魔试炼一并挂起：清实体、保留试炼会话，重启后 resumeAfterLoad 恢复。
        MindTrialManager.suspend(player, session);
        clearInnerDemons(player, session);
        PlayerMeditationManager.finishTribulationMeditating(player);
        savedData(player).setDirty();
        LOGGER.info(
                "Suspended tribulation session of {} (type={}, phase={}) for server shutdown; it will resume after restart",
                player.getGameProfile().getName(),
                session.type(),
                session.phase()
        );
    }

    /**
     * The single penalty settlement point for every tribulation failure.
     *
     * <p>折寿即天劫失败的核心损失（按失败原因分档系数缩放）；随后交
     * {@link RiskSettlementService} 以 coreLossHandled 标记结算，只补
     * SOUL_INJURY 长期魂伤与 POST_TRIBULATION_WEAKNESS 短期劫后虚弱，
     * 不再叠加自带的灵力扣除（防与折寿双重扣减）。</p>
     */
    private static void settleFailurePenalty(
            ServerPlayer player,
            TribulationSession session,
            FailureReason reason
    ) {
        // 心魔即己 §5.2：天劫失败采样进行为账本 + 解锁快照。
        PlayerBehaviorLedgerData.recordTribulationFail(player, reason.name());
        PlayerBehaviorLedgerData.clearLockedSnapshot(player);
        // 折寿年数 = 配置基准年数 × 失败原因系数，四舍五入取整。
        int lifespanPenaltyYears = (int) Math.round(
                CommonConfig.TRIBULATION_FAILURE_LIFESPAN_YEARS.get()
                        * getFailureLifespanScale(reason)
        );
        if (lifespanPenaltyYears > 0) {
            EntityLifespanData.addAgeTicks(player, LifespanUtility.ticksForYears(lifespanPenaltyYears));
        }
        // 统一风险结算：折寿已作为核心损失结算，此处只抽长期魂伤 + 短期劫后虚弱。
        RiskSettlementService.settle(
                player,
                RiskContext.of(
                                RiskFailureType.TRIBULATION_FAILURE,
                                session.targetStage().realm()
                        )
                        .withCoreLossHandled()
                        .withDetail("reason=" + reason.name())
        );
        LOGGER.info(
                "Tribulation of {} failed (type={}, phase={}, strikes={}/{}, reason={}); lifespan penalty {} years (scale={})",
                player.getGameProfile().getName(),
                session.type(),
                session.phase(),
                session.completedStrikes(),
                session.type().strikeCount(),
                reason,
                lifespanPenaltyYears,
                getFailureLifespanScale(reason)
        );
        String penaltyText = lifespanPenaltyYears > 0
                ? "，寿元折损" + lifespanPenaltyYears + "载"
                : "，道行未失";
        NotificationUtility.send(
                player,
                Component.literal("渡劫失败"),
                Component.literal(reason.chineseDescription() + penaltyText),
                20 * 8
        );
    }

    /**
     * 失败原因 → 折寿系数（策划口径，tribulation 配置组）。临阵脱逃罚得最重，
     * 心魔缠身按基准，外因中断从轻。
     */
    private static double getFailureLifespanScale(FailureReason reason) {
        return switch (reason) {
            case DEATH -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_DEATH.get();
            case LOGOUT -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_LOGOUT.get();
            case DIMENSION_CHANGED -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_DIMENSION_CHANGED.get();
            case INNER_DEMON_TIMEOUT -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_INNER_DEMON_TIMEOUT.get();
            case INTERRUPTED -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_INTERRUPTED.get();
            // 策划未单列 BODY_SLAIN（心魔试炼中肉身被毁）：肉身已毁等同劫中身陨，按 DEATH 档。
            case BODY_SLAIN -> CommonConfig.TRIBULATION_FAILURE_LIFESPAN_SCALE_DEATH.get();
        };
    }

    /**
     * First tick after a session was restored from disk: re-synchronizes the client HUD and,
     * if the inner demon trial was interrupted by the restart, re-summons the demons so the
     * trial cannot be skipped by restarting the server.
     */
    private static void resumeAfterLoad(ServerPlayer player, TribulationSession session) {
        LOGGER.info(
                "Resuming persisted tribulation session of {} (type={}, phase={}, strikes={}/{})",
                player.getGameProfile().getName(),
                session.type(),
                session.phase(),
                session.completedStrikes(),
                session.type().strikeCount()
        );
        if (session.phase() == TribulationPhase.INNER_DEMON) {
            if (session.isInMindTrial()) {
                // 心魔即己：重建替身/心魔并把玩家送回其心境分格（断线重连恢复）。
                MindTrialManager.resume(player, session);
                ModNetwork.sendTribulationState(player, session);
                return;
            }
            ServerLevel level = player.serverLevel();
            boolean anyDemonAlive = session.innerDemonIds().stream()
                    .map(level::getEntity)
                    .anyMatch(entity -> entity != null && entity.isAlive());
            if (!anyDemonAlive) {
                session.innerDemonIds().clear();
                spawnInnerDemons(player, session);
            }
        }
        ModNetwork.sendTribulationState(player, session);
    }

    private static void tickPreparing(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 40 == 0) {
            level.playSound(
                    null,
                    session.center().x,
                    session.center().y,
                    session.center().z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    0.55F,
                    1.35F + level.random.nextFloat() * 0.25F
            );
        }
        session.tickPhase();
        if (session.phaseRemainingTicks() <= 0) {
            session.startStriking(1);
            spawnStrike(player, session);
        }
    }

    private static void spawnCloudParticles(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        float intensity = switch (session.phase()) {
            case PREPARING -> {
                float progress = 1.0F - Mth.clamp(
                        session.phaseRemainingTicks()
                                / (float) Math.max(1, CommonConfig.TRIBULATION_PREPARATION_TICKS.get()),
                        0.0F,
                        1.0F
                );
                yield 0.12F + progress * 0.88F;
            }
            case STRIKING -> 1.0F;
            case INNER_DEMON -> 0.95F;
            case AFTERMATH -> 0.90F;
            case DISSIPATING -> Mth.clamp(
                    session.phaseRemainingTicks()
                            / (float) Math.max(1, CommonConfig.TRIBULATION_DISSIPATION_TICKS.get()),
                    0.0F,
                    1.0F
            );
        };
        if (intensity <= 0.0F) {
            return;
        }
        double radius = session.radius();
        level.sendParticles(
                ParticleTypes.CLOUD,
                session.center().x,
                session.center().y + 11.0D,
                session.center().z,
                Math.max(3, Math.round(6.0F * intensity)),
                radius * 0.62D,
                1.6D,
                radius * 0.62D,
                0.015D
        );
        level.sendParticles(
                ParticleTypes.SMOKE,
                session.center().x,
                session.center().y + 12.0D,
                session.center().z,
                Math.max(2, Math.round(4.0F * intensity)),
                radius * 0.55D,
                1.2D,
                radius * 0.55D,
                0.008D
        );
    }

    private static void tickStriking(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        if (session.completedStrikes() >= session.type().strikeCount()) {
            advanceAfterStrikes(player, session);
            return;
        }
        spawnStrike(player, session);
    }

    private static void advanceAfterStrikes(ServerPlayer player, TribulationSession session) {
        if (TribulationUtility.requiresInnerDemonTrial(session.targetStage().realm())) {
            // 心魔即己（T17）：mindDemon.enabled 时走灵台心境试炼；
            // 关闭或开场失败（心境维度缺失等）时回退 legacy Vex 心魔。
            if (MindTrialManager.isEnabled()) {
                // 相位计时给足全部阶段上限 + 余量；实际推进由 MindTrialManager 驱动，
                // tickInnerDemon 的心魔试炼分支不消耗该计时。
                session.startInnerDemon(
                        MindTrialManager.revealTicks()
                                + MindTrialManager.sparMaxTicks()
                                + MindTrialManager.reflectMaxTicks()
                                + MindTrialManager.resolveChoiceTicks()
                                + Math.max(
                                MindTrialManager.severTimeLimitTicks(),
                                MindTrialManager.holdTimeLimitTicks())
                                + 20 * 30
                );
                if (MindTrialManager.begin(player, session)) {
                    return;
                }
            }
            // legacy Vex 心魔（配置回退路径）。
            session.startInnerDemon(CommonConfig.TRIBULATION_INNER_DEMON_DURATION_TICKS.get());
            spawnInnerDemons(player, session);
            NotificationUtility.send(
                    player,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_title", "心魔劫"),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_legacy_begin",
                            "雷霆已尽，心魔现世，守住灵台方寸"),
                    20 * 6
            );
            return;
        }
        session.startAftermath(
                CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                        + CommonConfig.TRIBULATION_PROMOTION_DELAY_TICKS.get()
        );
    }

    private static void tickInnerDemon(ServerPlayer player, TribulationSession session) {
        if (session.isInMindTrial()) {
            // 心魔即己：改为轮询 MindTrialManager 的五阶段状态机；
            // 成功时由其回写 session.startAftermath，失败时经统一 fail 入口。
            MindTrialManager.tick(player, session);
            return;
        }
        session.tickPhase();
        ServerLevel level = player.serverLevel();
        boolean anyDemonAlive = session.innerDemonIds().stream()
                .map(level::getEntity)
                .anyMatch(entity -> entity != null && entity.isAlive());
        if (!anyDemonAlive) {
            session.startAftermath(
                    CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                            + CommonConfig.TRIBULATION_PROMOTION_DELAY_TICKS.get()
            );
            NotificationUtility.send(
                    player,
                    Component.literal("心魔尽斩"),
                    Component.literal("灵台清明，天光将至"),
                    20 * 5
            );
            return;
        }
        if (player.tickCount % 20 == 0) {
            applyInnerDemonSoulDamage(player);
        }
        if (session.phaseRemainingTicks() <= 0) {
            fail(player, session, FailureReason.INNER_DEMON_TIMEOUT);
        }
    }

    private static void applyInnerDemonSoulDamage(ServerPlayer player) {
        int illuminatedStars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(SoulStrength.MINIMUM_ILLUMINATED_STARS);
        double soulResistance = Math.min(0.7D, 0.1D * (illuminatedStars - 1));
        double damage = CommonConfig.TRIBULATION_INNER_DEMON_SOUL_DAMAGE.get()
                * (1.0D - soulResistance);
        if (damage <= 0.0D) {
            return;
        }
        TribulationUtility.noHurtDelay(player);
        player.hurt(
                new DamageSource(
                        player.level()
                                .registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(ModDamageTypes.SOUL_PIERCE)
                ),
                (float) damage
        );
    }

    private static void spawnInnerDemons(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        int demonCount = CommonConfig.TRIBULATION_INNER_DEMON_COUNT.get();
        int lifetimeTicks = CommonConfig.TRIBULATION_INNER_DEMON_DURATION_TICKS.get() + 40;
        RandomSource random = level.random;
        for (int index = 0; index < demonCount; index++) {
            Vex demon = EntityType.VEX.create(level);
            if (demon == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / demonCount) * index + random.nextDouble() * 0.6D;
            double distance = 2.5D + random.nextDouble() * 1.5D;
            demon.moveTo(
                    session.center().x + Math.cos(angle) * distance,
                    session.center().y + 1.5D + random.nextDouble(),
                    session.center().z + Math.sin(angle) * distance,
                    random.nextFloat() * 360.0F,
                    0.0F
            );
            demon.setCustomName(Component.literal("心魔"));
            demon.setLimitedLife(lifetimeTicks);
            demon.setTarget(player);
            if (level.addFreshEntity(demon)) {
                session.innerDemonIds().add(demon.getUUID());
            }
        }
        level.playSound(
                null,
                session.center().x,
                session.center().y,
                session.center().z,
                SoundEvents.VEX_AMBIENT,
                SoundSource.HOSTILE,
                1.4F,
                0.6F
        );
    }

    private static void clearInnerDemons(ServerPlayer player, TribulationSession session) {
        // Look the demons up in the session's own dimension: after a dimension-change failure
        // the player is already in the destination level and the demons would leak otherwise.
        ServerLevel level = player.server.getLevel(session.dimension());
        if (level != null) {
            for (UUID demonId : session.innerDemonIds()) {
                Entity demon = level.getEntity(demonId);
                if (demon != null) {
                    demon.discard();
                }
            }
        }
        session.innerDemonIds().clear();
    }

    private static void tickAftermath(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        clearInnerDemons(player, session);
        // 渡劫成功：解锁行为账本快照（心魔即己 §5.2）。
        PlayerBehaviorLedgerData.clearLockedSnapshot(player);
        // 畏雷心障化解：本场冲关未借破境丹之力而渡劫功成（标记在掷骰清加成前写入）。
        if (!PlayerBreakthroughBonusData.consumeAttemptPillUse(
                player,
                player.level().getGameTime()
        )) {
            MindTrialManager.resolveMindScar(
                    player,
                    PlayerBehaviorLedgerData.MindScarIds.THUNDER_FEAR
            );
        }
        PlayerCultivationData.setStage(player, session.targetStage());
        // 破境概率化（P4）：渡劫成功即本次冲关圆满——清空瓶颈补偿计数，
        // 金丹/元婴目标掉落悟道印记三选一。
        PlayerBreakthroughFailureData.clearFailures(player, session.targetStage().realm());
        if (session.targetStage().realm() == CultivationRealm.GOLDEN_CORE
                || session.targetStage().realm() == CultivationRealm.NASCENT_SOUL) {
            InsightPool.offerCandidates(player, session.targetStage().realm());
        }
        PlayerDefenseResourceData.refillToMaximum(player);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
        session.startDissipating(CommonConfig.TRIBULATION_DISSIPATION_TICKS.get());
        PlayerMeditationManager.finishTribulationMeditating(player);
    }

    private static void tickDissipating(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        savedData(player).removeSession(player.getUUID());
        ModNetwork.sendTribulationState(player, session, false);
    }

    private static void spawnStrike(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        int strikeIndex = session.completedStrikes() + 1;
        RandomSource random = level.random;
        double x = session.center().x + (random.nextDouble() - 0.5D) * 3.0D;
        double z = session.center().z + (random.nextDouble() - 0.5D) * 3.0D;
        double y = session.center().y;
        ThunderTribulationEntity lightning = new ThunderTribulationEntity(
                ModEntityTypes.THUNDER_TRIBULATION.get(),
                level
        );
        lightning.initialize(
                player,
                strikeIndex,
                TribulationUtility.getStrikeDamage(
                        session.type(),
                        strikeIndex,
                        session.aptitudeScore(),
                        session.realmMultiplier()
                ),
                TribulationUtility.getVisualIntensity(session.type(), strikeIndex, session.aptitudeScore())
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
                    1.2F,
                    0.72F + random.nextFloat() * 0.18F
            );
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    x,
                    y + 1.0D,
                    z,
                    32,
                    1.6D,
                    1.8D,
                    1.6D,
                    0.18D
            );
        }
        if (session.completedStrikes() >= session.type().strikeCount()) {
            advanceAfterStrikes(player, session);
        } else {
            session.startStriking(CommonConfig.TRIBULATION_STRIKE_INTERVAL_TICKS.get());
        }
        ModNetwork.sendTribulationState(player, session);
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        NOT_REQUIRED,
        NO_APTITUDE,
        INSUFFICIENT_SPIRITUAL_ENERGY,
        CANNOT_MEDITATE
    }

    public enum FailureReason {
        DEATH("劫中身陨"),
        LOGOUT("临阵离场，天罚随身"),
        DIMENSION_CHANGED("遁走他界，劫云不散"),
        INNER_DEMON_TIMEOUT("心魔缠身，道行未失"),
        INTERRUPTED("渡劫中断"),
        /** 心魔即己：灵台空守之际，肉身为外敌所毁（替身被打死）。 */
        BODY_SLAIN("神游灵台，肉身已毁");

        private final String chineseDescription;

        FailureReason(String chineseDescription) {
            this.chineseDescription = chineseDescription;
        }

        public String chineseDescription() {
            return this.chineseDescription;
        }
    }
}
