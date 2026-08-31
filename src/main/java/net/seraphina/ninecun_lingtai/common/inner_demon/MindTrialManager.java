package net.seraphina.ninecun_lingtai.common.inner_demon;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.behavior.SpellUsageWindow;
import net.seraphina.ninecun_lingtai.common.causality.CausalityHistoryManager;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.data.MindDemonLineManager;
import net.seraphina.ninecun_lingtai.common.entity.MindDemonEntity;
import net.seraphina.ninecun_lingtai.common.entity.MortalBodyEntity;
import net.seraphina.ninecun_lingtai.common.instance.MindRealmManager;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.seraphina.ninecun_lingtai.common.status.ScarManager;
import net.seraphina.ninecun_lingtai.common.status.ScarType;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncMindTrialPacket;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationSession;
import net.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 心魔试炼总状态机（心魔即己 §6.2，服务端权威）。由
 * {@code TribulationManager.tickInnerDemon} 每 tick 驱动（玩家离线自然暂停计时，
 * 即 mindDemon.pveOfflinePause 语义）。
 *
 * <pre>
 * begin → 生成 MortalBody → 玩家 enter 心境 → REVEAL → SPAR → REFLECT
 *       → RESOLVE_CHOICE →（SEVER | HOLD | ACCEPT）→ 留痕（回写 TribulationSession）
 * 崩塌路径：肉身被杀/玩家身陨/会话丢失 → TribulationManager.fail(...)
 * </pre>
 *
 * <p>配置值当前为类内常量，对应设计文档 §9 的 mindDemon.* 键（接线清单）。</p>
 */
public final class MindTrialManager {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final float SPAR_ADVANCE_HEALTH_PCT = 0.70F;
    private static final float REFLECT_ADVANCE_HEALTH_PCT = 0.35F;
    private static final int REVEAL_LINE_INTERVAL_TICKS = 50;
    private static final int BODY_MISSING_GRACE_TICKS = 60;
    private static final int CAUSALITY_GLIMPSE_RECORDS = 3;

    /** 肉身实体暂时找不到（区块异步加载等）的宽限计数，瞬态。 */
    private static final Map<UUID, Integer> BODY_MISSING_TICKS = new HashMap<>();

    private MindTrialManager() {
    }

    // ---------------------------------------------------- 配置读取（mindDemon 组）

    /** mindDemon.enabled：false 时 TribulationManager 回退 legacy Vex 心魔。 */
    public static boolean isEnabled() {
        return CommonConfig.MIND_DEMON_ENABLED.get();
    }

    /** mindDemon.revealSeconds（照见阶段时长，tick）。 */
    public static int revealTicks() {
        return CommonConfig.MIND_DEMON_REVEAL_SECONDS.get() * 20;
    }

    /** mindDemon.sparSeconds（试锋阶段上限，tick）。 */
    public static int sparMaxTicks() {
        return CommonConfig.MIND_DEMON_SPAR_SECONDS.get() * 20;
    }

    /** mindDemon.reflectSeconds（反照阶段上限，tick）。 */
    public static int reflectMaxTicks() {
        return CommonConfig.MIND_DEMON_REFLECT_SECONDS.get() * 20;
    }

    /** mindDemon.resolveChoiceSeconds（三选一窗口，超时默认守念，R8）。 */
    public static int resolveChoiceTicks() {
        return CommonConfig.MIND_DEMON_RESOLVE_CHOICE_SECONDS.get() * 20;
    }

    /** mindDemon.severTimeLimitSeconds（斩念限时，tick）。 */
    public static int severTimeLimitTicks() {
        return CommonConfig.MIND_DEMON_SEVER_TIME_LIMIT_SECONDS.get() * 20;
    }

    /** mindDemon.holdTimeLimitSeconds（守念存活时长，tick）。 */
    public static int holdTimeLimitTicks() {
        return CommonConfig.MIND_DEMON_HOLD_TIME_LIMIT_SECONDS.get() * 20;
    }

    // ------------------------------------------------------------------ 入口

    /**
     * 雷劫轮次结束后由 {@code TribulationManager.advanceAfterStrikes} 触发。
     * 返回 false 表示无法开场（如心境维度缺失），调用方应回退 legacy 路径。
     */
    public static boolean begin(ServerPlayer player, TribulationSession session) {
        MinecraftServer server = player.server;
        if (MindRealmManager.mindRealmLevel(server) == null) {
            LOGGER.error("[mind_trial] mind_realm dimension missing, falling back to legacy trial");
            return false;
        }
        ServerLevel fieldLevel = server.getLevel(session.dimension());
        if (fieldLevel == null) {
            return false;
        }
        UUID trialId = UUID.randomUUID();
        MindDemonSnapshot snapshot = MindDemonSnapshot.capture(player);
        MindRealmManager.MindArena arena = MindRealmManager.allocate(server, trialId, player.getUUID());
        MindTrialSession trial = new MindTrialSession(
                trialId,
                player.getUUID(),
                arena.pocketIndex(),
                arena.center(),
                snapshot,
                fieldLevel.getGameTime()
        );
        trial.setPhase(MindTrialPhase.REVEAL, revealTicks());

        // 劫场留替身（先生成，再送走本体）。
        MortalBodyEntity body = spawnBody(player, fieldLevel, session, trialId);
        if (body == null) {
            MindRealmManager.release(server, trialId);
            return false;
        }
        trial.setBodyId(body.getUUID());
        setFieldChunkForced(fieldLevel, session, true);

        session.setTrialToken(trialId);
        session.setInMindTrial(true);
        MindTrialSavedData.get(server).putSession(trial);

        if (!MindRealmManager.enter(player, arena)) {
            // 传送失败整体回滚，走 legacy。
            session.setInMindTrial(false);
            session.setTrialToken(null);
            body.discard();
            setFieldChunkForced(fieldLevel, session, false);
            MindTrialSavedData.get(server).removeSession(player.getUUID());
            MindRealmManager.release(server, trialId);
            return false;
        }

        prepareRevealLines(player, trial);
        NotificationUtility.send(
                player,
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.mind_trial_title", "心魔劫"),
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.mind_trial_begin", "雷霆已尽——身留劫场，魂入灵台"),
                20 * 6
        );
        LOGGER.info(
                "[mind_trial] begin: player={} trial={} pocket={} karma={} topSpells={} counterMoves={}",
                player.getGameProfile().getName(),
                trialId,
                arena.pocketIndex(),
                String.format("%.2f", snapshot.karmaScale()),
                snapshot.topSpells(),
                snapshot.counterMoves()
        );
        return true;
    }

    // ------------------------------------------------------------------ 每 tick 驱动

    /** 由 TribulationManager.tickInnerDemon 轮询。 */
    public static void tick(ServerPlayer player, TribulationSession session) {
        MinecraftServer server = player.server;
        MindTrialSavedData data = MindTrialSavedData.get(server);
        MindTrialSession trial = data.getByOwner(player.getUUID());
        if (trial == null
                || session.trialToken() == null
                || !trial.trialId().equals(session.trialToken())) {
            LOGGER.warn(
                    "[mind_trial] session lost for {}, settling as interrupted",
                    player.getGameProfile().getName()
            );
            TribulationManager.fail(player, session, TribulationManager.FailureReason.INTERRUPTED);
            return;
        }
        // 玩家必须待在心境维度（tp 逃走等价于遁走他界）。
        if (!MindRealmManager.isMindRealm(player.level().dimension())) {
            TribulationManager.fail(
                    player,
                    session,
                    TribulationManager.FailureReason.DIMENSION_CHANGED
            );
            return;
        }
        // 肉身检查：被杀/消失 → 心境崩塌，按外劫横死结算（≤1 tick，验收项）。
        ServerLevel fieldLevel = server.getLevel(session.dimension());
        MortalBodyEntity body = findBody(fieldLevel, trial);
        if (body == null || !body.isAlive()) {
            int missingTicks = BODY_MISSING_TICKS.merge(trial.trialId(), 1, Integer::sum);
            boolean confirmedDead = body != null && !body.isAlive();
            if (confirmedDead || missingTicks > BODY_MISSING_GRACE_TICKS) {
                NotificationUtility.send(
                        player,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_collapse_title", "心境崩塌"),
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_collapse", "肉身已陨，心境崩塌……"),
                        20 * 6
                );
                TribulationManager.fail(
                        player,
                        session,
                        TribulationManager.FailureReason.BODY_SLAIN
                );
                return;
            }
        } else {
            BODY_MISSING_TICKS.remove(trial.trialId());
            if (player.tickCount % 10 == 0) {
                body.mirrorHealth(player);
            }
        }

        MindDemonEntity demon = findDemon(server, trial);
        if (demon != null) {
            trial.setLastDemonHealthPct(demon.getHealthPercent());
        }

        trial.tickPhase();
        switch (trial.phase()) {
            case REVEAL -> tickReveal(player, trial);
            case SPAR -> tickSpar(player, session, trial, demon);
            case REFLECT -> tickReflect(player, session, trial, demon);
            case RESOLVE_CHOICE -> tickResolveChoice(player, session, trial, demon);
            case SEVER -> tickSever(player, session, trial, demon);
            case HOLD -> tickHold(player, session, trial, demon);
        }
        // 每 10 tick 心跳同步 Overlay（对齐 sendTribulationState 节流习惯）。
        if (player.tickCount % 10 == 0) {
            syncTrial(player, trial, false);
        }
        data.setDirty();
    }

    /** 向本人下发试炼状态帧（相位/剩余 tick/心魔血量/心潮红闪）。 */
    private static void syncTrial(ServerPlayer player, MindTrialSession trial, boolean bodyEchoFlash) {
        if (player.hasDisconnected()) {
            return;
        }
        ModNetwork.sendMindTrial(player, new SyncMindTrialPacket(
                (byte) trial.phase().ordinal(),
                trial.phaseRemainingTicks(),
                trial.lastDemonHealthPct(),
                List.of(),
                bodyEchoFlash
        ));
    }

    private static void tickReveal(ServerPlayer player, MindTrialSession trial) {
        if (trial.phaseRemainingTicks() % REVEAL_LINE_INTERVAL_TICKS == 0
                && !trial.pendingRevealLines().isEmpty()) {
            String line = trial.pendingRevealLines().remove(0);
            Component text = line.startsWith("message.")
                    ? Component.translatable(line)
                    : Component.literal(line);
            player.sendSystemMessage(
                    Component.translatableWithFallback(
                                    "message.ninecun_lingtai.mind_trial_demon_prefix", "『心魔』")
                            .withStyle(ChatFormatting.DARK_PURPLE)
                            .append(text.copy().withStyle(ChatFormatting.GRAY))
            );
        }
        if (trial.phaseRemainingTicks() <= 0) {
            spawnDemon(player, trial, MindDemonEntity.STAGE_SPAR, 1.0F);
            trial.setPhase(MindTrialPhase.SPAR, sparMaxTicks());
            sendPhaseTitle(
                    player,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_spar", "试锋"),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_spar_hint",
                            "心魔以汝之道法出手，见招拆招")
            );
        }
    }

    private static void tickSpar(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            MindDemonEntity demon
    ) {
        if (demon == null) {
            demon = spawnDemon(player, trial, MindDemonEntity.STAGE_SPAR, trial.lastDemonHealthPct());
            if (demon == null) {
                return;
            }
        }
        if (demon.getHealthPercent() <= SPAR_ADVANCE_HEALTH_PCT
                || trial.phaseRemainingTicks() <= 0) {
            demon.setAiStage(MindDemonEntity.STAGE_REFLECT);
            trial.setPhase(MindTrialPhase.REFLECT, reflectMaxTicks());
            sendPhaseTitle(
                    player,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_reflect", "反照"),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_reflect_hint",
                            "心魔窥破汝之倚仗，变招反制")
            );
        }
    }

    private static void tickReflect(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            MindDemonEntity demon
    ) {
        if (demon == null) {
            demon = spawnDemon(player, trial, MindDemonEntity.STAGE_REFLECT, trial.lastDemonHealthPct());
            if (demon == null) {
                return;
            }
        }
        if (demon.getHealthPercent() <= REFLECT_ADVANCE_HEALTH_PCT
                || trial.phaseRemainingTicks() <= 0) {
            int resolveTicks = resolveChoiceTicks();
            demon.setAiStage(MindDemonEntity.STAGE_PAUSED);
            trial.setPhase(MindTrialPhase.RESOLVE_CHOICE, resolveTicks);
            // 时停感演出：双方减速（§6.2 RESOLVE）。
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, resolveTicks, 2, false, false));
            demon.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, resolveTicks, 4, false, false));
            sendPhaseTitle(
                    player,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_resolve", "决念"),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_phase_resolve_hint",
                            "斩念？守念？认念？二十息内须下决断")
            );
            sendResolvePrompt(player);
        }
    }

    private static void tickResolveChoice(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            MindDemonEntity demon
    ) {
        if (trial.phaseRemainingTicks() % 100 == 0 && trial.phaseRemainingTicks() > 0) {
            sendResolvePrompt(player);
        }
        if (trial.phaseRemainingTicks() <= 0) {
            // R8：超时默认守念（认念会永久留 Scar，不能作为挂机默认值）。
            player.sendSystemMessage(
                    Component.translatableWithFallback(
                                    "message.ninecun_lingtai.mind_trial_resolve_timeout",
                                    "迟疑难决，唯有守念。")
                            .withStyle(ChatFormatting.GRAY)
            );
            applyResolveChoice(player, session, trial, ResolveChoice.HOLD);
        }
    }

    private static void tickSever(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            MindDemonEntity demon
    ) {
        if (demon == null || !demon.isAlive()) {
            finish(player, session, trial, TrialOutcome.SUCCESS);
            return;
        }
        if (trial.phaseRemainingTicks() <= 0) {
            TribulationManager.fail(
                    player,
                    session,
                    TribulationManager.FailureReason.INNER_DEMON_TIMEOUT
            );
        }
    }

    private static void tickHold(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            MindDemonEntity demon
    ) {
        if (demon == null || !demon.isAlive() || trial.phaseRemainingTicks() <= 0) {
            finish(player, session, trial, TrialOutcome.SUCCESS);
        }
        // 守念期间玩家死亡由死亡事件链（TribulationManager.fail → abort）处理为 FAIL。
    }

    // ------------------------------------------------------------------ 决念

    /** 决念三选一入口（命令 / SubmitResolveChoicePacket 接线后共用）。 */
    public static boolean submitResolve(ServerPlayer player, ResolveChoice choice) {
        MindTrialSavedData data = MindTrialSavedData.get(player.server);
        MindTrialSession trial = data.getByOwner(player.getUUID());
        if (trial == null || trial.phase() != MindTrialPhase.RESOLVE_CHOICE) {
            return false;
        }
        Optional<TribulationSession> session = TribulationManager.getSession(player);
        if (session.isEmpty() || !session.get().isInMindTrial()) {
            return false;
        }
        applyResolveChoice(player, session.get(), trial, choice);
        data.setDirty();
        return true;
    }

    private static void applyResolveChoice(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            ResolveChoice choice
    ) {
        trial.setResolveChoice(choice);
        MindDemonEntity demon = findDemon(player.server, trial);
        switch (choice) {
            case SEVER -> {
                trial.setPhase(MindTrialPhase.SEVER, severTimeLimitTicks());
                if (demon != null) {
                    demon.setAiStage(MindDemonEntity.STAGE_SEVER);
                }
                sendPhaseTitle(
                        player,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_sever", "斩念"),
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_sever_hint",
                                "一炷香内，亲手斩去此念！")
                );
            }
            case HOLD -> {
                trial.setPhase(MindTrialPhase.HOLD, holdTimeLimitTicks());
                if (demon != null) {
                    demon.setAiStage(MindDemonEntity.STAGE_FRENZY);
                }
                sendPhaseTitle(
                        player,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_hold", "守念"),
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_hold_hint",
                                "心魔狂化——守住灵台，静待其溃散")
                );
            }
            case ACCEPT -> {
                sendPhaseTitle(
                        player,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_accept", "认念"),
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_accept_hint",
                                "既认此念，与魔共存")
                );
                finish(player, session, trial, TrialOutcome.SUCCESS_WITH_SCAR);
            }
        }
    }

    // ------------------------------------------------------------------ 结算

    private enum TrialOutcome {
        SUCCESS,
        SUCCESS_WITH_SCAR
    }

    private static void finish(
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            TrialOutcome outcome
    ) {
        MinecraftServer server = player.server;
        if (outcome == TrialOutcome.SUCCESS_WITH_SCAR) {
            grantMindScar(player, trial, 1);
        }
        // 遁逃之执化解：一场不使用位移法术的心魔试炼通关（斩念/守念成功才算）。
        if (outcome == TrialOutcome.SUCCESS && !trial.usedMobilitySpell()) {
            resolveMindScar(player, PlayerBehaviorLedgerData.MindScarIds.FLIGHT_GRIP);
        }
        logTelemetry(player, trial, outcome == TrialOutcome.SUCCESS ? "sever_or_hold_success" : "accept");
        cleanup(server, player, session, trial, true);
        PlayerBehaviorLedgerData.clearLockedSnapshot(player);
        // 回写天劫会话：沿用现有晋升链（AFTERMATH → 晋升 → DISSIPATING）。
        session.startAftermath(
                CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                        + CommonConfig.TRIBULATION_PROMOTION_DELAY_TICKS.get()
        );
        NotificationUtility.send(
                player,
                outcome == TrialOutcome.SUCCESS
                        ? Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_success_title", "心魔尽斩")
                        : Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_accept_title", "与魔共存"),
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.mind_trial_success", "灵台清明，天光将至"),
                20 * 5
        );
    }

    /**
     * 天劫统一失败入口（{@code TribulationManager.fail}）的回调：无论失败原因，
     * 只要会话处于心魔试炼中就走这里做心境侧清理与心障落地。不得回调 fail（防递归）。
     */
    public static void abort(ServerPlayer player, TribulationSession session) {
        if (!session.isInMindTrial()) {
            return;
        }
        MinecraftServer server = player.server;
        MindTrialSavedData data = MindTrialSavedData.get(server);
        MindTrialSession trial = data.getByOwner(player.getUUID());
        session.setInMindTrial(false);
        session.setTrialToken(null);
        if (trial == null) {
            return;
        }
        // 天劫中败留心障（§8：severity 高于认念）。
        grantMindScar(player, trial, 2);
        logTelemetry(player, trial, "fail");
        cleanup(server, player, session, trial, player.isAlive());
    }

    /** 关服挂起：只清实体，保留试炼会话与分格，重启后 resume（对齐 suspend 语义）。 */
    public static void suspend(ServerPlayer player, TribulationSession session) {
        if (!session.isInMindTrial()) {
            return;
        }
        MinecraftServer server = player.server;
        MindTrialSession trial = MindTrialSavedData.get(server).getByOwner(player.getUUID());
        if (trial == null) {
            return;
        }
        discardEntity(findDemon(server, trial));
        ServerLevel fieldLevel = server.getLevel(session.dimension());
        discardEntity(findBody(fieldLevel, trial));
        trial.setDemonId(null);
        trial.setBodyId(null);
        MindTrialSavedData.get(server).setDirty();
        LOGGER.info(
                "[mind_trial] suspended trial {} of {} at phase {}",
                trial.trialId(),
                player.getGameProfile().getName(),
                trial.phase()
        );
    }

    /** 重启/重连恢复：重建替身与心魔到当前阶段（断线重连回到当前阶段，验收项）。 */
    public static void resume(ServerPlayer player, TribulationSession session) {
        MinecraftServer server = player.server;
        MindTrialSavedData data = MindTrialSavedData.get(server);
        MindTrialSession trial = data.getByOwner(player.getUUID());
        if (trial == null || session.trialToken() == null
                || !trial.trialId().equals(session.trialToken())) {
            LOGGER.warn(
                    "[mind_trial] resume failed for {}: trial session missing, settling as interrupted",
                    player.getGameProfile().getName()
            );
            TribulationManager.fail(player, session, TribulationManager.FailureReason.INTERRUPTED);
            return;
        }
        ServerLevel fieldLevel = server.getLevel(session.dimension());
        if (fieldLevel != null) {
            setFieldChunkForced(fieldLevel, session, true);
            if (findBody(fieldLevel, trial) == null) {
                MortalBodyEntity body = spawnBody(player, fieldLevel, session, trial.trialId());
                if (body != null) {
                    trial.setBodyId(body.getUUID());
                }
            }
        }
        if (!MindRealmManager.isMindRealm(player.level().dimension())) {
            MindRealmManager.enter(
                    player,
                    new MindRealmManager.MindArena(trial.pocketIndex(), trial.arenaCenter())
            );
        }
        if (trial.phase() != MindTrialPhase.REVEAL && findDemon(server, trial) == null) {
            spawnDemon(player, trial, stageForPhase(trial.phase()), trial.lastDemonHealthPct());
        }
        if (trial.phase() == MindTrialPhase.REVEAL && trial.pendingRevealLines().isEmpty()) {
            prepareRevealLines(player, trial);
        }
        data.setDirty();
        LOGGER.info(
                "[mind_trial] resumed trial {} of {} at phase {}",
                trial.trialId(),
                player.getGameProfile().getName(),
                trial.phase()
        );
    }

    private static void cleanup(
            MinecraftServer server,
            ServerPlayer player,
            TribulationSession session,
            MindTrialSession trial,
            boolean teleportBack
    ) {
        discardEntity(findDemon(server, trial));
        ServerLevel fieldLevel = server.getLevel(session.dimension());
        discardEntity(findBody(fieldLevel, trial));
        if (fieldLevel != null) {
            setFieldChunkForced(fieldLevel, session, false);
        }
        if (teleportBack && player.isAlive()
                && MindRealmManager.isMindRealm(player.level().dimension())) {
            MindRealmManager.exit(player, session);
        }
        session.setInMindTrial(false);
        session.setTrialToken(null);
        BODY_MISSING_TICKS.remove(trial.trialId());
        MindTrialSavedData.get(server).removeSession(trial.ownerUuid());
        MindRealmManager.release(server, trial.trialId());
    }

    // ------------------------------------------------------------------ 对外回调

    /** 替身受击转发（心潮震荡，§4.3）：玩家受同源真实伤害 × bodyEchoRatio，心魔小增益。 */
    public static void onBodyHurt(MortalBodyEntity body, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (body.getTrialId() == null || amount <= 0.0F) {
            return;
        }
        MinecraftServer server = body.level().getServer();
        if (server == null) {
            return;
        }
        MindTrialSession trial = MindTrialSavedData.get(server).getByTrialId(body.getTrialId());
        if (trial == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(trial.ownerUuid());
        if (owner == null || !owner.isAlive()) {
            return;
        }
        float echo = amount * CommonConfig.MIND_DEMON_BODY_ECHO_RATIO.get().floatValue();
        owner.hurt(source, echo);
        owner.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.mind_trial_body_hurt", "肉身受创，心潮震荡！"),
                true
        );
        syncTrial(owner, trial, true);
        MindDemonEntity demon = findDemon(server, trial);
        if (demon != null) {
            demon.addEchoRageStack();
        }
    }

    /** AIR_LOCK 反制招落点：禁位移法术（CastSpellPacket 校验用）。 */
    public static void applyAirLock(ServerPlayer owner) {
        MindTrialSession trial = MindTrialSavedData.get(owner.server).getByOwner(owner.getUUID());
        if (trial != null) {
            trial.setMovementLockUntilGameTime(
                    owner.level().getGameTime() + MindDemonCounterMove.AIR_LOCK_DURATION_TICKS
            );
            MindTrialSavedData.get(owner.server).setDirty();
        }
    }

    /** 位移法术是否被封空圈禁用（由 CastSpellPacket.handle 调用）。 */
    public static boolean isMovementSpellLocked(ServerPlayer player, net.minecraft.resources.ResourceLocation spellId) {
        if (!MindDemonSnapshot.isMobilitySpell(spellId)) {
            return false;
        }
        MindTrialSession trial = MindTrialSavedData.get(player.server).getByOwner(player.getUUID());
        return trial != null
                && player.level().getGameTime() < trial.movementLockUntilGameTime();
    }

    /** 施法遥测：REFLECT 阶段记录换招；全程记录是否用过位移法术（遁逃之执化解判定）。 */
    public static void onOwnerSpellCast(ServerPlayer player, net.minecraft.resources.ResourceLocation spellId) {
        MindTrialSession trial = MindTrialSavedData.get(player.server).getByOwner(player.getUUID());
        if (trial == null) {
            return;
        }
        boolean dirty = false;
        if (trial.phase() == MindTrialPhase.REFLECT
                && !trial.snapshot().topSpells().contains(spellId)) {
            trial.incrementForcedSwapCount();
            dirty = true;
        }
        if (!trial.usedMobilitySpell() && MindDemonSnapshot.isMobilitySpell(spellId)) {
            trial.setUsedMobilitySpell(true);
            dirty = true;
        }
        if (dirty) {
            MindTrialSavedData.get(player.server).setDirty();
        }
    }

    /** PILL_HAZE 药念幻象（§6.3，演出型 UI 干扰，不真扣冷却）。 */
    public static void onPillUse(ServerPlayer player) {
        MindTrialSession trial = MindTrialSavedData.get(player.server).getByOwner(player.getUUID());
        if (trial != null
                && trial.snapshot().counterMoves().contains(MindDemonCounterMove.PILL_HAZE)
                && player.getRandom().nextDouble() < MindDemonCounterMove.PILL_HAZE_TRIGGER_CHANCE) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                                    "message.ninecun_lingtai.mind_trial_pill_haze",
                                    "丹力凝滞难行……（药念幻象，认清节奏可无视）")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );
        }
    }

    public static Optional<MindTrialSession> getActiveTrial(ServerPlayer player) {
        return Optional.ofNullable(
                MindTrialSavedData.get(player.server).getByOwner(player.getUUID())
        );
    }

    // ------------------------------------------------------------------ 心障（P8）

    /**
     * 失败/认念留下命名心障（§8）：Scar 框架用 {@code ScarType.HEART_DEMON} 落地
     * （同类型至多一条，合并规则见 PlayerScarData.addScar），具体心障 id 存在
     * 行为账本镜像（{@code PlayerBehaviorLedgerData.addActiveScar}）中区分。
     * 建议后续扩展 ScarType 增加按 id 区分的心障子类（见开发日志）。
     */
    private static void grantMindScar(ServerPlayer player, MindTrialSession trial, int severity) {
        String scarId = pickScarId(player);
        ScarManager.applyScar(player, ScarType.HEART_DEMON, severity);
        PlayerBehaviorLedgerData.addActiveScar(player, scarId);
        player.sendSystemMessage(
                Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_scar_applied",
                                "心障留痕：%s（化解：%s）",
                                scarName(scarId),
                                scarResolveHint(scarId))
                        .withStyle(ChatFormatting.DARK_PURPLE)
        );
        // 嗜杀心障的"7 修真日零击杀"窗口从获得心障当日起算。
        if (PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST.equals(scarId)) {
            PlayerBehaviorLedgerData.setLastKillCalendarDay(
                    player,
                    net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility
                            .getCalendarDay(player.server)
            );
        }
        LOGGER.info(
                "[mind_trial] scar {} (severity {}) applied to {}",
                scarId,
                severity,
                player.getGameProfile().getName()
        );
    }

    /**
     * 心障化解统一入口（接线清单第 10 条）：账本 active → resolved、化解提示，
     * 全部命名心障清空后再移除 {@code ScarType.HEART_DEMON}。心障不存在时静默返回，
     * 各化解 hook 可无条件调用。
     */
    public static void resolveMindScar(ServerPlayer player, String scarId) {
        if (!PlayerBehaviorLedgerData.resolveActiveScar(player, scarId)) {
            return;
        }
        player.sendSystemMessage(
                Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_scar_resolved",
                                "心结既开：%s已然化解",
                                scarName(scarId))
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
        );
        if (!PlayerBehaviorLedgerData.hasAnyActiveMindScar(player)) {
            ScarManager.resolveScar(player, ScarType.HEART_DEMON);
        }
        LOGGER.info(
                "[mind_trial] scar {} resolved for {}",
                scarId,
                player.getGameProfile().getName()
        );
    }

    /** 按锁定账本的主导特质挑心障 id（§8 获得条件表）。 */
    private static String pickScarId(ServerPlayer player) {
        CompoundTag ledger = PlayerBehaviorLedgerData.getLockedSnapshotOrLive(player);
        SpellUsageWindow window = PlayerBehaviorLedgerData.spellWindowFrom(ledger);
        int totalKills = PlayerBehaviorLedgerData.totalKillsFrom(ledger);
        int soulPills = PlayerBehaviorLedgerData.soulPillUsesFrom(ledger);
        double mobilityRatio = window.usageRatio(MindDemonSnapshot::isMobilitySpell);
        if (totalKills >= 20) {
            return PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST;
        }
        if (soulPills > 5) {
            return PlayerBehaviorLedgerData.MindScarIds.PILL_BOND;
        }
        if (mobilityRatio > 0.30D) {
            return PlayerBehaviorLedgerData.MindScarIds.FLIGHT_GRIP;
        }
        return PlayerBehaviorLedgerData.MindScarIds.THUNDER_FEAR;
    }

    /** 心障名（scar.ninecun_lingtai.<id>.name），化解 hook 的提示文案共用。 */
    public static Component scarName(String scarId) {
        String fallback = switch (scarId) {
            case PlayerBehaviorLedgerData.MindScarIds.THUNDER_FEAR -> "畏雷";
            case PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST -> "嗜杀";
            case PlayerBehaviorLedgerData.MindScarIds.PILL_BOND -> "药执";
            case PlayerBehaviorLedgerData.MindScarIds.FLIGHT_GRIP -> "遁逃之执";
            default -> scarId;
        };
        return Component.translatableWithFallback(
                "scar.ninecun_lingtai." + scarId + ".name",
                fallback
        );
    }

    /** 心障化解方式提示（scar.ninecun_lingtai.<id>.resolve_hint）。 */
    public static Component scarResolveHint(String scarId) {
        String fallback = switch (scarId) {
            case PlayerBehaviorLedgerData.MindScarIds.THUNDER_FEAR -> "不服破境丹渡过一次天劫";
            case PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST -> "七个修真日内零击杀";
            case PlayerBehaviorLedgerData.MindScarIds.PILL_BOND -> "连续打坐三十分钟无服丹";
            case PlayerBehaviorLedgerData.MindScarIds.FLIGHT_GRIP -> "不用位移法术通关一次心魔试炼";
            default -> scarId;
        };
        return Component.translatableWithFallback(
                "scar.ninecun_lingtai." + scarId + ".resolve_hint",
                fallback
        );
    }

    // ------------------------------------------------------------------ 内部

    private static MortalBodyEntity spawnBody(
            ServerPlayer player,
            ServerLevel fieldLevel,
            TribulationSession session,
            UUID trialId
    ) {
        MortalBodyEntity body = ModEntityTypes.MORTAL_BODY.get().create(fieldLevel);
        if (body == null) {
            return null;
        }
        Vec3 center = session.center();
        body.moveTo(center.x, center.y, center.z, player.getYRot(), 0.0F);
        body.initialize(player, trialId);
        if (!fieldLevel.addFreshEntity(body)) {
            return null;
        }
        return body;
    }

    private static MindDemonEntity spawnDemon(
            ServerPlayer player,
            MindTrialSession trial,
            byte stage,
            float healthPct
    ) {
        ServerLevel mindLevel = MindRealmManager.mindRealmLevel(player.server);
        if (mindLevel == null) {
            return null;
        }
        MindDemonEntity demon = ModEntityTypes.MIND_DEMON.get().create(mindLevel);
        if (demon == null) {
            return null;
        }
        BlockPos center = trial.arenaCenter();
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        demon.moveTo(
                center.getX() + 0.5D + Math.cos(angle) * 6.0D,
                center.getY() + 1.0D,
                center.getZ() + 0.5D + Math.sin(angle) * 6.0D,
                player.getRandom().nextFloat() * 360.0F,
                0.0F
        );
        demon.initialize(player, trial.snapshot(), trial.trialId(), stage);
        demon.setHealth(Math.max(1.0F, demon.getMaxHealth() * Math.max(0.05F, healthPct)));
        if (!mindLevel.addFreshEntity(demon)) {
            return null;
        }
        trial.setDemonId(demon.getUUID());
        mindLevel.playSound(
                null,
                center,
                net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                net.minecraft.sounds.SoundSource.HOSTILE,
                1.4F,
                0.6F
        );
        return demon;
    }

    private static byte stageForPhase(MindTrialPhase phase) {
        return switch (phase) {
            case REVEAL, SPAR -> MindDemonEntity.STAGE_SPAR;
            case REFLECT -> MindDemonEntity.STAGE_REFLECT;
            case RESOLVE_CHOICE -> MindDemonEntity.STAGE_PAUSED;
            case SEVER -> MindDemonEntity.STAGE_SEVER;
            case HOLD -> MindDemonEntity.STAGE_FRENZY;
        };
    }

    private static MindDemonEntity findDemon(MinecraftServer server, MindTrialSession trial) {
        if (trial.demonId() == null) {
            return null;
        }
        ServerLevel mindLevel = MindRealmManager.mindRealmLevel(server);
        if (mindLevel == null) {
            return null;
        }
        Entity entity = mindLevel.getEntity(trial.demonId());
        return entity instanceof MindDemonEntity demon ? demon : null;
    }

    private static MortalBodyEntity findBody(ServerLevel fieldLevel, MindTrialSession trial) {
        if (fieldLevel == null || trial.bodyId() == null) {
            return null;
        }
        Entity entity = fieldLevel.getEntity(trial.bodyId());
        return entity instanceof MortalBodyEntity body ? body : null;
    }

    private static void discardEntity(Entity entity) {
        if (entity != null) {
            entity.discard();
        }
    }

    /** 劫场中心区块保持加载：肉身实体必须常驻可查（否则误判崩塌）。 */
    private static void setFieldChunkForced(
            ServerLevel fieldLevel,
            TribulationSession session,
            boolean add
    ) {
        BlockPos center = BlockPos.containing(session.center());
        ChunkPos chunkPos = new ChunkPos(center);
        ForgeChunkManager.forceChunk(
                fieldLevel,
                ModMain.MOD_ID,
                center,
                chunkPos.x,
                chunkPos.z,
                add,
                true
        );
    }

    /** REVEAL 台词：台词库按锁定账本条件选 3–5 条 + 因果账本近records剪影（§6.5/§5）。 */
    private static void prepareRevealLines(ServerPlayer player, MindTrialSession trial) {
        trial.pendingRevealLines().clear();
        CompoundTag ledger = PlayerBehaviorLedgerData.getLockedSnapshotOrLive(player);
        trial.pendingRevealLines().addAll(
                MindDemonLineManager.INSTANCE.selectLineKeys(ledger, "reveal", player.getRandom(), 3)
        );
        // 因果账本只读剪影（拼好的中文句子，直接可用）。
        List<net.seraphina.ninecun_lingtai.common.causality.CausalityRecord> records =
                CausalityHistoryManager.getRecords(player.server, player.getUUID(), 50);
        int taken = 0;
        for (int index = records.size() - 1; index >= 0 && taken < CAUSALITY_GLIMPSE_RECORDS; index--) {
            String description = records.get(index).description();
            if (description != null && !description.isBlank()) {
                trial.pendingRevealLines().add("犹记得——" + description);
                taken++;
            }
        }
    }

    private static void sendPhaseTitle(ServerPlayer player, Component title, Component subtitle) {
        NotificationUtility.send(player, title, subtitle, 20 * 5);
    }

    /** RESOLVE 三选一：聊天可点选项（SubmitResolveChoicePacket 注册前的可用输入路径）。 */
    private static void sendResolvePrompt(ServerPlayer player) {
        Component prompt = Component.translatableWithFallback(
                        "message.ninecun_lingtai.mind_trial_resolve_prompt", "决念：")
                .append(choiceButton(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_sever_button", "【斩念】"),
                        "sever",
                        ChatFormatting.RED,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_sever_hover",
                                "限时斩杀心魔，败则劫毁")))
                .append(Component.literal(" "))
                .append(choiceButton(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_hold_button", "【守念】"),
                        "hold",
                        ChatFormatting.AQUA,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_hold_hover",
                                "心魔狂化下存活，身陨则败")))
                .append(Component.literal(" "))
                .append(choiceButton(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_accept_button", "【认念】"),
                        "accept",
                        ChatFormatting.DARK_PURPLE,
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_resolve_accept_hover",
                                "立得心障，与魔共存")));
        player.sendSystemMessage(prompt);
    }

    private static Component choiceButton(
            Component label,
            String choiceArgument,
            ChatFormatting color,
            Component hoverText
    ) {
        return label.copy().withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/mindtrial resolve " + choiceArgument
                ))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        hoverText
                ))
        );
    }

    private static void logTelemetry(ServerPlayer player, MindTrialSession trial, String resolution) {
        long duration = player.level().getGameTime() - trial.startedGameTime();
        LOGGER.info(
                "[inner_demon_result] player={} resolution={} choice={} durationTicks={} forcedSwaps={} scars={}",
                player.getGameProfile().getName(),
                resolution,
                trial.resolveChoice(),
                duration,
                trial.forcedSwapCount(),
                trial.snapshot().oldScarIds()
        );
    }
}
