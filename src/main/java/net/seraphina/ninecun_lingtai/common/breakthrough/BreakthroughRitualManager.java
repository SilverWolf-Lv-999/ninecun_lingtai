package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.common.ascension.AscensionManager;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.insight.InsightPool;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncBreakthroughRitualPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughBonusData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughFailureData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.risk.RiskContext;
import net.seraphina.ninecun_lingtai.common.risk.RiskFailureType;
import net.seraphina.ninecun_lingtai.common.risk.RiskSettlementService;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseManager;
import net.seraphina.ninecun_lingtai.common.status.ScarManager;
import net.seraphina.ninecun_lingtai.common.status.ScarType;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 引灵仪式状态机（服务端唯一权威，设计文档 §5.1）：
 * request 校验 → RITUAL 计时（30s）→ 掷骰 ROLL → 成/败/中断/取消四分支结算。
 *
 * <p>会话经 {@link BreakthroughRitualSavedData} 持久化，重启恢复的会话按
 * "中断轻档"结算（登出/重启不再是免罚出口）。内存无独立缓存——SavedData
 * 即唯一事实源，避免静态 HashMap 惯性（R5）。</p>
 *
 * <p>P2 阶段掷骰成功后<b>全境界直接 commitStageUnchecked</b>；接管天劫/飞升
 * 路由（startForRitual 分派）是 P4 的事，本类的 {@code settleSuccess} 留有
 * TODO 标注切换点。</p>
 */
public final class BreakthroughRitualManager {

    // 仪式时长/散逸/瓶颈等数值全部读 CommonConfig.breakthrough 组（运行时 get()）。
    private static final int TICKS_PER_DAY = 24000;
    private static final int SYNC_INTERVAL_TICKS = 20;
    /** 魂强达到此星数才下发"最拖后腿修正项"提示（R6）。 */
    private static final int DRAGGING_HINT_MINIMUM_STARS = 5;
    /** 中断轻档的倒地演出：3 秒大幅减速。 */
    private static final int INTERRUPT_STAGGER_TICKS = 60;
    private static final int INTERRUPT_STAGGER_AMPLIFIER = 3;

    private BreakthroughRitualManager() {
    }

    public enum RequestResult {
        STARTED,
        /** 当前不存在可掷骰的大境界目标（未到巅峰/已至仙巅），调用方回落原有路径。 */
        NOT_MAJOR_TARGET,
        ALREADY_IN_RITUAL,
        BLOCKED_BY_TRIBULATION,
        NO_APTITUDE,
        IN_BOTTLENECK,
        INSUFFICIENT_SPIRITUAL_ENERGY,
        CANNOT_MEDITATE
    }

    /**
     * 仪式发起入口（BreakthroughRequestPacket 的大境界分支调用）。
     * 只校验不消费：锁定折扣后需求快照进会话，掷骰时按快照结算（§5.4 防白嫖/防双扣）。
     */
    public static RequestResult request(ServerPlayer player) {
        if (isInRitual(player)) {
            return RequestResult.ALREADY_IN_RITUAL;
        }
        if (TribulationManager.isInTribulation(player) || AscensionManager.isInAscension(player)) {
            return RequestResult.BLOCKED_BY_TRIBULATION;
        }
        Optional<CultivationStage> target = getRitualTarget(player);
        if (target.isEmpty()) {
            return RequestResult.NOT_MAJOR_TARGET;
        }
        CultivationStage targetStage = target.get();
        if (PlayerAptitudeData.getSpiritualRoot(player).isEmpty()
                || PlayerAptitudeData.getSoulStrength(player).isEmpty()) {
            return RequestResult.NO_APTITUDE;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (PlayerBreakthroughFailureData.isInBottleneck(player, gameTime)
                || ScarManager.hasScar(player, ScarType.BOTTLENECK)) {
            return RequestResult.IN_BOTTLENECK;
        }
        if (!PlayerCultivationManager.hasEnoughEnergyForTarget(player, targetStage)) {
            return RequestResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        BreakthroughChanceCalculator.ChanceBreakdown breakdown =
                BreakthroughChanceCalculator.calculate(player, targetStage);
        int soulStars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(0);
        String draggingId = soulStars >= DRAGGING_HINT_MINIMUM_STARS
                ? breakdown.worstDraggingModifierId().orElse("")
                : "";
        BreakthroughRitualSession session = new BreakthroughRitualSession(
                player.getUUID(),
                targetStage,
                CommonConfig.BREAKTHROUGH_RITUAL_SECONDS.get() * 20,
                PlayerCultivationManager.getDiscountedRequirement(player, targetStage),
                breakdown.finalChance(),
                breakdown.graspBucketKey(),
                draggingId,
                player.level().dimension(),
                player.position(),
                player.serverLevel().random.nextLong()
        );
        BreakthroughRitualSavedData savedData = savedData(player);
        savedData.putSession(session);
        // R4：仪式与神识互斥；魂游拦截在 BreakthroughRequestPacket 入口已做。
        SoulSenseManager.deactivate(player, null);
        if (!PlayerMeditationManager.startRitualMeditating(player)) {
            savedData.removeSession(player.getUUID());
            return RequestResult.CANNOT_MEDITATE;
        }
        ModMain.LOGGER.info(
                "[breakthrough] ritual started for {} target={} {}",
                player.getName().getString(),
                targetStage.realm().getSerializedName(),
                breakdown.describe()
        );
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_ritual_start",
                        "引灵入体，天地灵气正在汇聚……"
                ),
                false
        );
        player.displayClientMessage(getGraspMessage(breakdown.graspBucketKey()), true);
        syncToClient(player, session);
        return RequestResult.STARTED;
    }

    /** 大境界掷骰目标：当前境界九层巅峰且存在下一大境界。 */
    public static Optional<CultivationStage> getRitualTarget(ServerPlayer player) {
        return PlayerCultivationData.getStage(player)
                .filter(stage -> stage.layer() >= CultivationStage.MAXIMUM_LAYER)
                .flatMap(stage -> stage.realm().next())
                .map(nextRealm -> new CultivationStage(nextRealm, CultivationStage.MINIMUM_LAYER));
    }

    public static boolean isInRitual(ServerPlayer player) {
        return player != null && savedData(player).hasSession(player.getUUID());
    }

    public static boolean isInRitual(Player player) {
        return player instanceof ServerPlayer serverPlayer && isInRitual(serverPlayer);
    }

    public static Optional<BreakthroughRitualSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(savedData(player).getSession(player.getUUID()));
    }

    /** 每玩家 tick 驱动（BreakthroughRitualEventHandler.onPlayerTick）。 */
    public static void tick(ServerPlayer player) {
        BreakthroughRitualSavedData savedData = savedData(player);
        BreakthroughRitualSession session = savedData.getSession(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.consumeRestoredFlag()) {
            // 服务器重启视为仪式被天地打断（§5.5），按中断轻档结算。
            settleInterrupted(player, session, "server_restart");
            return;
        }
        if (!player.isAlive()
                || player.isSpectator()
                || !player.level().dimension().equals(session.dimension())
                || !PlayerMeditationManager.isMeditating(player)) {
            settleInterrupted(player, session, "posture_broken");
            return;
        }
        int remaining = session.tickDown();
        savedData.setDirty();
        if (remaining % SYNC_INTERVAL_TICKS == 0) {
            playRitualAmbience(player, session);
            syncToClient(player, session);
        }
        if (remaining <= 0) {
            roll(player, session);
        }
    }

    /** 受击中断入口（LivingHurtEvent，实伤 > 0 时调用）。R3：环境/DoT 不打断。 */
    public static void onHurt(ServerPlayer player, DamageSource source) {
        BreakthroughRitualSession session = savedData(player).getSession(player.getUUID());
        if (session == null) {
            return;
        }
        if (CommonConfig.BREAKTHROUGH_INTERRUPT_ON_ENTITY_DAMAGE_ONLY.get()
                && (source.getEntity() == null || source.getEntity() == player)) {
            return;
        }
        settleInterrupted(player, session, "hurt_by_" + source.getMsgId());
    }

    /** 登出/换维度/死亡的中断结算入口（登出不再是免罚出口，§5.5）。 */
    public static void interrupt(ServerPlayer player, String reason) {
        BreakthroughRitualSession session = savedData(player).getSession(player.getUUID());
        if (session != null) {
            settleInterrupted(player, session, reason);
        }
    }

    /** 主动取消（CancelBreakthroughRitualPacket）：分文不动，仅散场（§5.1 CANCELLED）。 */
    public static void cancel(ServerPlayer player) {
        BreakthroughRitualSavedData savedData = savedData(player);
        BreakthroughRitualSession session = savedData.getSession(player.getUUID());
        if (session == null) {
            return;
        }
        savedData.removeSession(player.getUUID());
        PlayerMeditationManager.finishRitualMeditating(player);
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_ritual_cancelled",
                        "你散去灵机，止步于此。"
                ),
                false
        );
        syncInactive(player);
    }

    /** 计时归零：服务端掷骰并在同一 tick 内完成成/败分派（掷骰结果绝不下发，R6）。 */
    private static void roll(ServerPlayer player, BreakthroughRitualSession session) {
        savedData(player).removeSession(player.getUUID());
        boolean success = player.serverLevel().random.nextDouble() < session.finalChance();
        ModMain.LOGGER.info(
                "[breakthrough] roll for {} target={} chance={} result={}",
                player.getName().getString(),
                session.targetStage().realm().getSerializedName(),
                String.format("%.4f", session.finalChance()),
                success ? "success" : "fail"
        );
        // 一颗破境丹只保一次冲关：无论成败都清除加成。清除前记录"本次是否借丹"
        // （畏雷心障化解校验在渡劫成功结算处读取该标记）。
        PlayerBreakthroughBonusData.recordAttemptPillUse(
                player,
                PlayerBreakthroughBonusData.getSuccessBonus(
                        player,
                        player.serverLevel().getGameTime()
                ) > 0.0D
        );
        PlayerBreakthroughBonusData.clearBonus(player);
        if (success) {
            settleSuccess(player, session);
        } else {
            settleRollFailure(player, session);
        }
        PlayerMeditationManager.finishRitualMeditating(player);
        syncInactive(player);
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
    }

    /**
     * 掷骰成功（P4 路由表，§5.2）：
     * 目标 = 化神（元婴9）→ 飞升劫（问心）；目标 ∈ [金丹, ∞) → 天劫（劫内自带消费，
     * 仪式层不消费防双扣，R2）；目标 = 筑基 → 直接消费快照 + commitStageUnchecked。
     */
    private static void settleSuccess(ServerPlayer player, BreakthroughRitualSession session) {
        CultivationRealm targetRealm = session.targetStage().realm();
        if (targetRealm == CultivationRealm.SPIRIT_TRANSFORMATION) {
            AscensionManager.StartResult result = AscensionManager.startForRitual(player);
            ModMain.LOGGER.info(
                    "[breakthrough] ritual roll success for {} -> ascension tribulation ({})",
                    player.getName().getString(),
                    result
            );
            return;
        }
        if (targetRealm.ordinal() >= CultivationRealm.GOLDEN_CORE.ordinal()) {
            TribulationManager.StartResult result =
                    TribulationManager.startForRitual(player, session.targetStage());
            ModMain.LOGGER.info(
                    "[breakthrough] ritual roll success for {} -> tribulation ({})",
                    player.getName().getString(),
                    result
            );
            return;
        }
        commitWithoutTribulation(player, session);
    }

    /** 无劫路线（目标 = 筑基）：消费快照灵力后直接晋升。 */
    private static void commitWithoutTribulation(
            ServerPlayer player,
            BreakthroughRitualSession session
    ) {
        Optional<CultivationStage> previousStage = PlayerCultivationData.getStage(player);
        CultivationStage targetStage = session.targetStage();
        if (!PlayerCultivationManager.consumeSnapshotEnergyForTarget(
                player,
                session.requiredSnapshot()
        )) {
            // 仪式期间灵力被外力抽走（理论不可达：仪式姿态锁行为）。不奖不罚，软取消。
            ModMain.LOGGER.warn(
                    "[breakthrough] snapshot consume failed for {}, ritual dissolved without effect",
                    player.getName().getString()
            );
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_insufficient_energy")
            );
            return;
        }
        PlayerCultivationManager.commitStageUnchecked(player, targetStage);
        PlayerBreakthroughFailureData.clearFailures(player, targetStage.realm());
        PlayerDefenseResourceData.refillToMaximum(player);
        BreakthroughAnimationType.fromTransition(previousStage, targetStage).ifPresent(animationType ->
                ModNetwork.sendBreakthroughAnimation(
                        player,
                        animationType,
                        PlayerAptitudeData.getSpiritualRoot(player)
                                .map(root -> root.getElementMask())
                                .orElse(0)
                )
        );
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_success_broadcast",
                        "%s 踏入%s",
                        player.getDisplayName(),
                        CultivationUtility.getDisplayName(targetStage)
                ),
                false
        );
        ModNetwork.sendCultivationFeedback(
                player,
                true,
                Component.translatable(
                        "message.ninecun_lingtai.breakthrough_success",
                        CultivationUtility.getDisplayName(targetStage)
                )
        );
        // T14：金丹/元婴破境成功掉落悟道印记三选一候选。
        if (targetStage.realm() == CultivationRealm.GOLDEN_CORE
                || targetStage.realm() == CultivationRealm.NASCENT_SOUL) {
            InsightPool.offerCandidates(player, targetStage.realm());
        }
    }

    /**
     * 掷骰失败（§5.4）：本次灵力照烧 → 其余池按境界档散逸（即破境失败的核心损失）
     * → 统一风险结算（标记 coreLossHandled，只抽长期/短期伤痕）→ 瓶颈记账。
     */
    private static void settleRollFailure(ServerPlayer player, BreakthroughRitualSession session) {
        CultivationRealm targetRealm = session.targetStage().realm();
        long gameTime = player.serverLevel().getGameTime();
        int bottleneckDays = CommonConfig.BREAKTHROUGH_BOTTLENECK_DAYS.get();
        PlayerCultivationManager.consumeSnapshotEnergyForTarget(player, session.requiredSnapshot());
        PlayerSpiritualEnergyData.retainEnergyRatio(player, 1.0D - getFailDissipation(targetRealm));
        RiskSettlementService.settle(
                player,
                RiskContext.of(RiskFailureType.BREAKTHROUGH_FAILURE, targetRealm)
                        .withCoreLossHandled()
                        .withDetail("target=" + targetRealm.getSerializedName())
        );
        PlayerBehaviorLedgerData.recordBreakthroughFail(player, targetRealm.getSerializedName());
        PlayerBreakthroughFailureData.recordFailure(player, targetRealm, gameTime);
        PlayerBreakthroughFailureData.setBottleneckUntil(
                player,
                gameTime + (long) bottleneckDays * TICKS_PER_DAY
        );
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_roll_fail",
                        "气息一落千丈——破境失败"
                ),
                false
        );
        ModNetwork.sendCultivationFeedback(
                player,
                false,
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_bottleneck",
                        "道基未稳，%s 日后方可再度冲关",
                        bottleneckDays
                )
        );
    }

    /**
     * 中断轻档（受击/登出/换维度/死亡/重启）：不掷骰、不烧全额，只散逸 15%
     * + 3 秒倒地演出；记一次"未遂"（不计入 pity）。
     */
    private static void settleInterrupted(
            ServerPlayer player,
            BreakthroughRitualSession session,
            String reason
    ) {
        savedData(player).removeSession(player.getUUID());
        PlayerSpiritualEnergyData.retainEnergyRatio(
                player,
                1.0D - CommonConfig.BREAKTHROUGH_INTERRUPT_DISSIPATION.get()
        );
        PlayerBreakthroughFailureData.recordInterruption(player, session.targetStage().realm());
        ModMain.LOGGER.info(
                "[breakthrough] ritual interrupted for {} reason={}",
                player.getName().getString(),
                reason
        );
        if (!player.hasDisconnected()) {
            if (player.isAlive()) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        INTERRUPT_STAGGER_TICKS,
                        INTERRUPT_STAGGER_AMPLIFIER
                ));
            }
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.breakthrough_ritual_interrupted",
                            "仪式被打断，灵气逆冲而散！"
                    ),
                    false
            );
            ModNetwork.sendCultivationInfo(player);
        }
        PlayerMeditationManager.finishRitualMeditating(player);
        syncInactive(player);
    }

    /** 失败散逸三档（§5.4，读配置）：筑基/金丹低档，元婴/化神中档，合道及以上高档。 */
    public static double getFailDissipation(CultivationRealm targetRealm) {
        if (targetRealm.ordinal() <= CultivationRealm.GOLDEN_CORE.ordinal()) {
            return CommonConfig.BREAKTHROUGH_FAIL_DISSIPATION_LOW.get();
        }
        if (targetRealm.ordinal() <= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()) {
            return CommonConfig.BREAKTHROUGH_FAIL_DISSIPATION_MID.get();
        }
        return CommonConfig.BREAKTHROUGH_FAIL_DISSIPATION_HIGH.get();
    }

    /** 仪式演出：粒子随进度渐密 + 低频嗡鸣（BEACON_AMBIENT 变调）。M1 只做演出不接灵气存量。 */
    private static void playRitualAmbience(ServerPlayer player, BreakthroughRitualSession session) {
        double progress = 1.0D - (double) session.remainingTicks() / session.totalTicks();
        int particleCount = 4 + (int) (progress * 20.0D);
        player.serverLevel().sendParticles(
                ParticleTypes.ENCHANT,
                player.getX(),
                player.getY() + 1.2D,
                player.getZ(),
                particleCount,
                0.8D,
                0.6D,
                0.8D,
                0.05D
        );
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS,
                0.6F,
                0.6F + (float) progress * 0.5F
        );
    }

    private static Component getGraspMessage(String graspBucketKey) {
        return Component.translatableWithFallback(
                "message.ninecun_lingtai.breakthrough_" + graspBucketKey,
                "把握档位：" + graspBucketKey
        );
    }

    private static void syncToClient(ServerPlayer player, BreakthroughRitualSession session) {
        ModNetwork.sendBreakthroughRitual(player, new SyncBreakthroughRitualPacket(
                true,
                session.remainingTicks(),
                session.totalTicks(),
                session.graspBucketKey(),
                session.draggingModifierId()
        ));
    }

    private static void syncInactive(ServerPlayer player) {
        if (!player.hasDisconnected()) {
            ModNetwork.sendBreakthroughRitual(player, SyncBreakthroughRitualPacket.inactive());
        }
    }

    private static BreakthroughRitualSavedData savedData(ServerPlayer player) {
        return BreakthroughRitualSavedData.get(player.server);
    }
}
