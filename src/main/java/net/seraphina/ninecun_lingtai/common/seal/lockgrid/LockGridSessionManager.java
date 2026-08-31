package net.seraphina.ninecun_lingtai.common.seal.lockgrid;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridClickPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridExitPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridFlowPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridResultPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridResultPacket.Result;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenLockGridPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.seal.SealAnchorUtility;
import net.seraphina.ninecun_lingtai.common.seal.SealKind;
import net.seraphina.ninecun_lingtai.common.seal.SealLayer;
import net.seraphina.ninecun_lingtai.common.seal.SealManager;
import net.seraphina.ninecun_lingtai.common.seal.SealRecord;
import net.seraphina.ninecun_lingtai.common.seal.SealRecordSavedData;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 锁纹对弈服务端会话（阵禁系统日志 6.3 状态机原样实现）。
 *
 * <pre>
 * VIEWING（发流光帧，每秒扣灵力）──首次落子──> ACTING（流光停发）
 * ACTING ──Click──> 命中 progress+1（每 3 节稳固）／点错 tolerance−1 回退稳固点
 * tolerance&lt;0 → FAILED（魂伤 + 惊扰 +1 + 防御层触发）
 * 主径完成 → LAYER_BROKEN → 有里层 GAP 2s 重开 ／ 无里层 COMPLETE
 * 任意态 收手/灵力尽/受击/下线 → EXPELLED（惊扰 +1，无伤）
 * </pre>
 *
 * 主径与假流光只活在本类的 {@link Session} 里，绝不下发。
 */
public final class LockGridSessionManager {
    private static final int GAP_TICKS = 40;
    private static final int TREMOR_HOLD_MILLIS = 300;
    /** 会话硬超时（10 分钟），防挂机泄漏。 */
    private static final int SESSION_TIMEOUT_TICKS = 20 * 600;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Random SEED_RANDOM = new Random();

    private LockGridSessionManager() {
    }

    public static boolean hasSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------------
    // 开局
    // ------------------------------------------------------------------

    public static void startCrack(ServerPlayer breaker, InteractionHand hand, SealRecord record) {
        if (SESSIONS.containsKey(breaker.getUUID())) {
            return;
        }
        MinecraftServer server = breaker.server;
        long now = MonotonicCalendarUtility.getCalendarTick(server);
        if (record.isDisturbLocked(now)) {
            breaker.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.seal.crack_disturb_locked"), true);
            return;
        }
        SealManager.notifyTouchAlert(server, record, breaker.serverLevel(), breaker.position(),
                "message.ninecun_lingtai.seal.alert.crack_started");
        Session session = new Session(breaker.getUUID(), hand, record.sealId(),
                record.layers().size());
        SESSIONS.put(breaker.getUUID(), session);
        if (!openLayer(breaker, session, record)) {
            SESSIONS.remove(breaker.getUUID());
        }
    }

    /** 生成当前最外层盘面并下发开屏包。 */
    private static boolean openLayer(ServerPlayer breaker, Session session, SealRecord record) {
        MinecraftServer server = breaker.server;
        if (record.layers().isEmpty()) {
            return false;
        }
        SealLayer layer = record.layers().get(0);
        int breakerStars = PlayerAptitudeData.getSoulStrength(breaker)
                .map(SoulStrength::illuminatedStars).orElse(0);
        int soulDiff = breakerStars - layer.ownerSoulSnapshot();
        int depth = session.totalLayersAtStart - record.layers().size();
        boolean deathPact = record.isDeathPactActive(MonotonicCalendarUtility.getCalendarTick(server));
        long boneSeed = record.sealId().getLeastSignificantBits()
                ^ (layer.carveSequence() * 0x100000001B3L);
        session.nonce = SEED_RANDOM.nextLong();
        session.board = LockGridGenerator.generate(
                layer.kind().getGrade(), depth, soulDiff, deathPact, boneSeed, session.nonce);
        session.grade = layer.kind().getGrade();
        session.soulDiff = soulDiff;
        session.tolerance = Math.max(0, soulDiff + 2);
        session.progress = 0;
        session.stableProgress = 0;
        session.expectedClickSequence = 0;
        session.tremorAvailable = soulDiff >= 0;
        session.state = State.VIEWING;
        session.gapTicksRemaining = 0;
        session.ageTicks = 0;
        session.nextFlowTick = 0;

        List<OpenLockGridPacket.NodeEntry> nodeEntries = new ArrayList<>();
        for (LockGridBoard.Node node : session.board.nodes()) {
            nodeEntries.add(new OpenLockGridPacket.NodeEntry(
                    (short) node.x(),
                    (short) node.y(),
                    (byte) ElementType.FIVE_ELEMENTS.indexOf(node.element()),
                    node.fog()
            ));
        }
        ModNetwork.sendOpenLockGrid(breaker, new OpenLockGridPacket(
                session.nonce,
                session.grade,
                depth + 1,
                session.tolerance,
                session.board.startNode(),
                LockGridGenerator.cycleTicks(session.grade),
                deathPact,
                nodeEntries,
                session.board.edges()
        ));
        return true;
    }

    // ------------------------------------------------------------------
    // tick：灵力扣减 + 流光时序 + GAP 换层 + 超时
    // ------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer breaker = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (breaker == null || !breaker.isAlive()) {
                iterator.remove();
                addDisturb(server, session);
                continue;
            }
            session.ageTicks++;
            if (session.ageTicks > SESSION_TIMEOUT_TICKS) {
                iterator.remove();
                expel(breaker, session);
                continue;
            }
            if (session.state == State.GAP) {
                if (--session.gapTicksRemaining <= 0) {
                    SealRecord record = SealRecordSavedData.get(server).getRecord(session.sealId);
                    if (record == null || record.layers().isEmpty()
                            || !openLayer(breaker, session, record)) {
                        iterator.remove();
                        ModNetwork.sendLockGridResult(breaker,
                                LockGridResultPacket.of(Result.EXPELLED, -1, 0, 0));
                    }
                }
                continue;
            }
            // 界面实时不暂停：每秒扣灵力，扣不动即弹出
            if (session.ageTicks % 20 == 0) {
                double drain = CommonConfig.SEAL_LOCK_GRID_ENERGY_PER_SECOND_PER_GRADE.get()
                        * session.grade;
                if (PlayerSpiritualEnergyData.consumeEnergyAny(breaker, drain) == null) {
                    iterator.remove();
                    expel(breaker, session);
                    continue;
                }
            }
            // 观纹相位：按周期发真假流光帧（落子后停发）
            if (session.state == State.VIEWING && session.ageTicks >= session.nextFlowTick) {
                sendFlows(breaker, session);
                session.nextFlowTick = session.ageTicks + LockGridGenerator.cycleTicks(session.grade);
            }
        }
    }

    private static void sendFlows(ServerPlayer breaker, Session session) {
        int flowId = session.nextFlowId++;
        int runTicks = Math.max(20, LockGridGenerator.cycleTicks(session.grade) * 3 / 4);
        // 真流光：呼吸感只在魂强差 ≥ −1 时置位（≤ −2 服务端同质化，压根不发标记）
        ModNetwork.sendLockGridFlow(breaker, new LockGridFlowPacket(
                flowId,
                session.board.path(),
                session.soulDiff >= -1,
                false,
                runTicks
        ));
        for (List<Integer> fake : session.board.fakes()) {
            ModNetwork.sendLockGridFlow(breaker, new LockGridFlowPacket(
                    session.nextFlowId++,
                    fake,
                    false,
                    false,
                    runTicks
            ));
        }
    }

    // ------------------------------------------------------------------
    // 落子判定（全部秘密在服务端）
    // ------------------------------------------------------------------

    public static void handleClick(ServerPlayer breaker, LockGridClickPacket packet) {
        Session session = SESSIONS.get(breaker.getUUID());
        if (session == null || session.state == State.GAP) {
            return;
        }
        // nonce + 自增序号双重防重放：旧包/乱序包直接丢弃并记审计日志
        if (packet.sessionNonce() != session.nonce
                || packet.clickSequence() != session.expectedClickSequence) {
            ModMain.LOGGER.warn(
                    "[seal] replayed/out-of-order lock grid click from {} (nonce {} seq {})",
                    breaker.getName().getString(), packet.sessionNonce(), packet.clickSequence());
            return;
        }
        session.expectedClickSequence++;
        int nodeIndex = packet.nodeIndex();
        if (nodeIndex < 0 || nodeIndex >= session.board.nodes().size()) {
            return;
        }
        session.state = State.ACTING;

        List<Integer> path = session.board.path();
        if (path.get(session.progress).equals(nodeIndex)) {
            session.progress++;
            if (session.progress % 3 == 0) {
                session.stableProgress = session.progress;
                ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                        Result.STABLE, nodeIndex, session.progress, session.tolerance));
            } else {
                ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                        Result.HIT, nodeIndex, session.progress, session.tolerance));
            }
            if (checkDetonate(breaker, session)) {
                return;
            }
            if (session.progress >= path.size()) {
                breakLayer(breaker, session);
            }
            return;
        }
        // 点错：滞涩预警（魂强 ≥ 刻印者且轻触 <300ms 时免罚一次，判定也在服务端）
        if (session.tremorAvailable && packet.holdMillis() < TREMOR_HOLD_MILLIS) {
            session.tremorAvailable = false;
            ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                    Result.TREMOR, nodeIndex, session.progress, session.tolerance));
            return;
        }
        session.tolerance--;
        session.progress = session.stableProgress;
        if (session.tolerance < 0) {
            SESSIONS.remove(breaker.getUUID());
            fail(breaker, session);
            return;
        }
        ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                Result.SETBACK, nodeIndex, session.progress, session.tolerance));
    }

    /** 自毁禁：破解进度（已破层 + 当前层进度）÷ 总层数 越过阈值即湮灭。 */
    private static boolean checkDetonate(ServerPlayer breaker, Session session) {
        MinecraftServer server = breaker.server;
        SealRecord record = SealRecordSavedData.get(server).getRecord(session.sealId);
        if (record == null || record.layers().isEmpty()) {
            return false;
        }
        int brokenLayers = session.totalLayersAtStart - record.layers().size();
        double ratio = (brokenLayers
                + (double) session.progress / session.board.path().size())
                / session.totalLayersAtStart;
        for (SealLayer layer : record.layers()) {
            if (layer.kind() == SealKind.DETONATE
                    && !SealAnchorUtility.isLayerDormant(server, record.ownerUuid(), layer)
                    && ratio >= layer.detonateThreshold()) {
                SESSIONS.remove(breaker.getUUID());
                ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                        Result.DETONATED, -1, session.progress, session.tolerance));
                SealManager.detonate(breaker, session.hand, record);
                return true;
            }
        }
        return false;
    }

    private static void breakLayer(ServerPlayer breaker, Session session) {
        MinecraftServer server = breaker.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        SealRecord record = savedData.getRecord(session.sealId);
        if (record == null || record.layers().isEmpty()) {
            SESSIONS.remove(breaker.getUUID());
            return;
        }
        SealLayer brokenLayer = record.layers().remove(0);
        savedData.setDirty();
        // 层规则结算（M4）：遗念禁在此刻以主人口吻响起并落指纹
        SealManager.onLayerBroken(breaker, record, brokenLayer);
        if (record.layers().isEmpty()) {
            SESSIONS.remove(breaker.getUUID());
            ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                    Result.COMPLETE, -1, session.progress, session.tolerance));
            SealManager.completeBreak(breaker, session.hand, record, true);
            return;
        }
        ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                Result.LAYER_BROKEN, -1, session.progress, session.tolerance));
        session.state = State.GAP;
        session.gapTicksRemaining = GAP_TICKS;
    }

    private static void fail(ServerPlayer breaker, Session session) {
        MinecraftServer server = breaker.server;
        ModNetwork.sendLockGridResult(breaker, LockGridResultPacket.of(
                Result.FAILED, -1, session.progress, session.tolerance));
        // 基础魂伤（数值化魂伤体系；风险伤痕由魂伤累积自行触发）
        SoulInjuryUtility.addInjury(
                breaker,
                CommonConfig.SEAL_LOCK_GRID_FAIL_SOUL_INJURY.get(),
                SoulInjuryUtility.InjurySource.SEAL_BACKLASH
        );
        SealRecord record = SealRecordSavedData.get(server).getRecord(session.sealId);
        if (record != null) {
            SealManager.appendFingerprint(server, record, breaker, false);
            SealManager.notifyTouchAlert(server, record, breaker.serverLevel(), breaker.position(),
                    "message.ninecun_lingtai.seal.alert.crack_failed");
            // 反噬禁（M4）：正被破解的这一层若是反噬层，失败瞬间全额反击（15 × 层品阶）
            if (!record.layers().isEmpty()) {
                SealLayer currentLayer = record.layers().get(0);
                if (currentLayer.kind() == SealKind.BACKLASH
                        && !SealAnchorUtility.isLayerDormant(server, record.ownerUuid(), currentLayer)) {
                    SealManager.applyBacklashInjury(breaker, currentLayer);
                }
            }
        }
        addDisturb(server, session);
        breaker.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.seal.crack_failed"), false);
    }

    // ------------------------------------------------------------------
    // 收手 / 弹出
    // ------------------------------------------------------------------

    public static void handleExit(ServerPlayer breaker, LockGridExitPacket packet) {
        Session session = SESSIONS.get(breaker.getUUID());
        if (session == null || packet.sessionNonce() != session.nonce) {
            return;
        }
        SESSIONS.remove(breaker.getUUID());
        if (packet.mode() == LockGridExitPacket.Mode.FORCE_BREAK) {
            SealRecord record = SealRecordSavedData.get(breaker.server).getRecord(session.sealId);
            ModNetwork.sendLockGridResult(breaker,
                    LockGridResultPacket.of(Result.EXPELLED, -1, 0, 0));
            if (record != null && !record.layers().isEmpty()) {
                SealManager.startForceBreak(breaker, session.hand, record);
            }
            return;
        }
        expel(breaker, session);
    }

    public static void onPlayerHurt(ServerPlayer breaker) {
        Session session = SESSIONS.remove(breaker.getUUID());
        if (session != null) {
            expel(breaker, session);
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer breaker) {
        Session session = SESSIONS.remove(breaker.getUUID());
        if (session != null) {
            addDisturb(breaker.server, session);
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static void expel(ServerPlayer breaker, Session session) {
        ModNetwork.sendLockGridResult(breaker,
                LockGridResultPacket.of(Result.EXPELLED, -1, session.progress, session.tolerance));
        addDisturb(breaker.server, session);
    }

    /** 惊扰 +1；满 3 锁死巧破 1 修真日并清零。 */
    private static void addDisturb(MinecraftServer server, Session session) {
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        SealRecord record = savedData.getRecord(session.sealId);
        if (record == null) {
            return;
        }
        int disturb = record.disturbCount() + 1;
        if (disturb >= 3) {
            record.setDisturbCount(0);
            record.setDisturbLockUntilCalendarTick(
                    MonotonicCalendarUtility.getCalendarTick(server)
                            + CommonConfig.SEAL_LOCK_GRID_DISTURB_LOCK_DAYS.get()
                            * MonotonicCalendarUtility.TICKS_PER_DAY);
        } else {
            record.setDisturbCount(disturb);
        }
        savedData.setDirty();
    }

    private enum State {
        VIEWING,
        ACTING,
        GAP
    }

    private static final class Session {
        private final UUID breakerUuid;
        private final InteractionHand hand;
        private final UUID sealId;
        private final int totalLayersAtStart;
        private LockGridBoard board;
        private long nonce;
        private int grade;
        private int soulDiff;
        private int tolerance;
        private int progress;
        private int stableProgress;
        private int expectedClickSequence;
        private boolean tremorAvailable;
        private State state = State.VIEWING;
        private int gapTicksRemaining;
        private int ageTicks;
        private int nextFlowTick;
        private int nextFlowId = 1;

        private Session(UUID breakerUuid, InteractionHand hand, UUID sealId, int totalLayersAtStart) {
            this.breakerUuid = breakerUuid;
            this.hand = hand;
            this.sealId = sealId;
            this.totalLayersAtStart = Math.max(1, totalLayersAtStart);
        }
    }
}
