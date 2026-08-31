package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.seraphina.ninecun_lingtai.utility.PossessionUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 魂游会话管理。{@link SoulRoamingSavedData} 是权威数据源（跨重启存续）；
 * 静态 map 仅是在线运行期缓存（含运行期字段 soulEntityId、movementInput）。
 *
 * <p>生命周期语义：
 * <ul>
 *   <li>主动登出 = 魂游立即结束、魂体消散、归体（沿用旧语义）；</li>
 *   <li>干净关服 = 挂起：SavedData 记录与 SoulEntity（随区块落盘）都保留；</li>
 *   <li>重启登录 = 进入重绑窗口（{@value #RESUME_REBIND_TIMEOUT_TICKS} tick），
 *       期间按 soulUuid 找回 SoulEntity 恢复会话；超时未找到则安全返还本体；</li>
 *   <li>会话已消亡的落盘魂体在区块重载时作为孤魂清除。</li>
 * </ul>
 */
public final class PlayerSoulRoamingManager {
    private static final double SOUL_INTERACTION_DISTANCE = 6.0D;
    private static final int RESUME_REBIND_TIMEOUT_TICKS = 100;
    private static final Map<UUID, SoulRoamingSession> SOUL_ROAMING_PLAYERS = new HashMap<>();
    private static final Map<UUID, PendingResume> PENDING_RESUMES = new HashMap<>();

    public static boolean startSoulRoaming(ServerPlayer player) {
        if (isSoulRoaming(player)) {
            syncState(player);
            return true;
        }
        // 重绑窗口内禁止开新会话：等待旧会话恢复或超时安全返还完成，避免双魂体。
        if (PENDING_RESUMES.containsKey(player.getUUID())) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.cannot_start"),
                    true
            );
            return false;
        }
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        if (stage.isEmpty() || stage.get().realm().ordinal() < CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.realm_too_low"),
                    true
            );
            return false;
        }
        if (!PlayerMeditationManager.startMeditating(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.cannot_start"),
                    true
            );
            return false;
        }
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.cannot_start"),
                    true
            );
            return false;
        }

        SoulEntity soul = ModEntityTypes.SOUL_ENTITY.get().create(player.level());
        if (soul == null) {
            return false;
        }
        soul.setOwnerUuid(player.getUUID());
        soul.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        soul.setYHeadRot(player.yHeadRot);
        soul.setYBodyRot(player.yBodyRot);
        if (!player.serverLevel().addFreshEntity(soul)) {
            return false;
        }

        SoulRoamingSession session = new SoulRoamingSession(
                player.level().dimension(),
                player.position(),
                soul.getUUID(),
                soul.getId(),
                new SoulMovementInput()
        );
        SOUL_ROAMING_PLAYERS.put(player.getUUID(), session);
        SoulRoamingSavedData.get(player.server).putRecord(new SoulRoamingSavedData.SoulRoamingRecord(
                player.getUUID(),
                session.soulDimension(),
                session.bodyPosition(),
                session.soulUuid()
        ));
        syncState(player);
        syncBodyBoundData(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.started"),
                true
        );
        return true;
    }

    public static void tick(ServerPlayer player) {
        PendingResume pending = PENDING_RESUMES.get(player.getUUID());
        if (pending != null) {
            tickPendingResume(player, pending);
            return;
        }
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        keepBodyAtPosition(player, session);
        if (!PlayerMeditationManager.isMeditating(player)) {
            PlayerMeditationManager.startMeditating(player);
        }

        SoulEntity soul = getSoulEntity(player.server, session).orElse(null);
        if (soul == null || !soul.isAlive()) {
            finishSoulRoaming(player, session);
            return;
        }
        SoulMovementInput movementInput = session.movementInput();
        if (player.tickCount - movementInput.lastUpdateTick() > 5) {
            movementInput.clear();
        }
        soul.moveSoul(
                movementInput.leftImpulse(),
                movementInput.forwardImpulse(),
                movementInput.verticalImpulse(),
                movementInput.yRot(),
                movementInput.xRot()
        );
    }

    public static void moveSoul(
            ServerPlayer player,
            float leftImpulse,
            float forwardImpulse,
            boolean jumping,
            boolean shiftKeyDown,
            float yRot,
            float xRot
    ) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!Float.isFinite(leftImpulse)
                || !Float.isFinite(forwardImpulse)
                || !Float.isFinite(yRot)
                || !Float.isFinite(xRot)) {
            return;
        }
        SoulEntity soul = getSoulEntity(player.server, session).orElse(null);
        if (soul == null || !soul.isAlive() || soul.level() != player.level()) {
            finishSoulRoaming(player, session);
            return;
        }
        session.movementInput().update(
                Mth.clamp(leftImpulse, -1.0F, 1.0F),
                Mth.clamp(forwardImpulse, -1.0F, 1.0F),
                jumping,
                shiftKeyDown,
                yRot,
                xRot,
                player.tickCount
        );
        keepBodyAtPosition(player, session);
    }

    public static void interactWithEntity(ServerPlayer player, int targetEntityId) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        SoulEntity soul = getSoulEntity(player.server, session).orElse(null);
        Entity target = player.serverLevel().getEntity(targetEntityId);
        if (soul == null || target == null || target == soul || target.level() != soul.level()) {
            return;
        }
        if (soul.distanceToSqr(target) > SOUL_INTERACTION_DISTANCE * SOUL_INTERACTION_DISTANCE) {
            return;
        }
        if (target == player) {
            returnToBody(player);
            return;
        }
        if (target instanceof ServerPlayer targetPlayer) {
            tryPossess(player, targetPlayer);
        }
    }

    public static boolean returnToBody(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        finishSoulRoaming(player, session);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.returned"),
                true
        );
        return true;
    }

    public static boolean tryPossess(ServerPlayer player, ServerPlayer target) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        PossessionUtility.PossessionResult validationResult = PossessionUtility.validatePossession(player, target);
        if (!validationResult.success()) {
            player.displayClientMessage(validationResult.message(), true);
            return false;
        }
        finishSoulRoaming(player, session);
        PlayerMeditationManager.stopMeditating(player);
        PossessionUtility.PossessionResult result = PossessionUtility.possess(player, target);
        if (!result.success()) {
            player.displayClientMessage(result.message(), true);
            return false;
        }
        player.displayClientMessage(result.message(), true);
        return true;
    }

    public static boolean isSoulRoaming(ServerPlayer player) {
        return isSoulRoaming(player.getUUID());
    }

    public static boolean isSoulRoaming(UUID playerUuid) {
        return SOUL_ROAMING_PLAYERS.containsKey(playerUuid);
    }

    public static boolean ownsSoul(ServerPlayer player, SoulEntity soul) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        return session != null && session.soulUuid().equals(soul.getUUID());
    }

    public static void onSoulDestroyed(SoulEntity soul) {
        Optional<UUID> ownerUuid = soul.getOwnerUuid();
        if (ownerUuid.isEmpty()) {
            return;
        }
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(ownerUuid.get());
        if (session == null || !session.soulUuid().equals(soul.getUUID())) {
            return;
        }
        MinecraftServer server = soul.getServer();
        if (server == null) {
            SOUL_ROAMING_PLAYERS.remove(ownerUuid.get());
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid.get());
        if (player != null) {
            finishSoulRoaming(player, session);
        } else {
            SOUL_ROAMING_PLAYERS.remove(ownerUuid.get());
        }
    }

    /**
     * 区块加载回魂体时的裁决（仅 loadedFromDisk 路径）：
     * 活会话/挂起会话的魂体保留；本主在线且处于重绑窗口则当场重绑；
     * 会话已消亡的无主孤魂直接清除。
     */
    public static void onSoulLoaded(SoulEntity soul) {
        MinecraftServer server = soul.getServer();
        if (server == null) {
            return;
        }
        UUID soulUuid = soul.getUUID();
        UUID ownerUuid = soul.getOwnerUuid().orElse(null);
        if (ownerUuid != null) {
            SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(ownerUuid);
            if (session != null && session.soulUuid().equals(soulUuid)) {
                return;
            }
            SoulRoamingSavedData.SoulRoamingRecord record = SoulRoamingSavedData.get(server).getRecord(ownerUuid);
            if (record != null && record.soulUuid().equals(soulUuid)) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
                if (owner != null && PENDING_RESUMES.containsKey(ownerUuid)) {
                    resumeSession(owner, record, soul);
                }
                // 挂起会话的魂体保留：本主离线时等待其回归，重绑由登录流程负责。
                return;
            }
        }
        ModMain.LOGGER.info("Discarding orphaned soul entity {} (owner={})", soulUuid, ownerUuid);
        soul.discard();
    }

    /**
     * 登录入口：存在挂起会话则进入重绑窗口，由 {@link #tick} 每刻尝试按
     * soulUuid 找回魂体；找到即恢复会话，超时未找到则安全返还本体。
     */
    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (SOUL_ROAMING_PLAYERS.containsKey(player.getUUID())) {
            return;
        }
        SoulRoamingSavedData.SoulRoamingRecord record =
                SoulRoamingSavedData.get(player.server).getRecord(player.getUUID());
        if (record == null) {
            return;
        }
        PENDING_RESUMES.put(player.getUUID(), new PendingResume(record));
        ModMain.LOGGER.info(
                "Entering soul roaming rebind window for {} (soul={})",
                player.getGameProfile().getName(),
                record.soulUuid()
        );
    }

    /**
     * 登出统一入口：关服断连 = 挂起（SavedData 记录与魂体都保留，重启重绑）；
     * 主动登出 = 魂游立即结束、魂体消散、归体（沿用旧语义）。
     */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (player.hasDisconnected() && !player.server.isRunning()) {
            suspend(player);
            return;
        }
        PendingResume pending = PENDING_RESUMES.remove(player.getUUID());
        if (pending != null) {
            // 重绑窗口内主动登出 = 视同归体：删记录；魂体已加载则当场消散，
            // 未加载则留待孤魂清理。
            SoulRoamingSavedData.get(player.server).removeRecord(player.getUUID());
            findSoulEntity(player.server, pending.record.soulDimension(), pending.record.soulUuid())
                    .ifPresent(Entity::discard);
            return;
        }
        cleanup(player);
    }

    private static void suspend(ServerPlayer player) {
        PENDING_RESUMES.remove(player.getUUID());
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.remove(player.getUUID());
        if (session != null) {
            // 魂体故意不 discard：随区块落盘，重启后按 soulUuid 重绑。
            ModMain.LOGGER.info(
                    "Suspended soul roaming session of {} for server shutdown; it will resume after restart",
                    player.getGameProfile().getName()
            );
        }
    }

    public static void cleanup(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            syncInactive(player);
            return;
        }
        finishSoulRoaming(player, session);
    }

    public static void syncState(ServerPlayer player) {
        ModNetwork.sendSoulRoamingState(player, createSyncData(player));
    }

    /**
     * 关服后清空运行期缓存（单人模式下静态 map 会跨"退出世界再进入"存活，必须清）。
     * 只清内存不动 {@link SoulRoamingSavedData}：挂起的会话要留待重启恢复。
     * 此时区块已存盘完毕，discard 对落盘数据无影响，仅释放内存实体。
     */
    public static void clear(MinecraftServer server) {
        SOUL_ROAMING_PLAYERS.values().forEach(session ->
                getSoulEntity(server, session).ifPresent(Entity::discard)
        );
        SOUL_ROAMING_PLAYERS.clear();
        PENDING_RESUMES.clear();
    }

    private static void tickPendingResume(ServerPlayer player, PendingResume pending) {
        SoulRoamingSavedData.SoulRoamingRecord record = pending.record;
        SoulEntity soul = findSoulEntity(player.server, record.soulDimension(), record.soulUuid())
                .orElse(null);
        if (soul != null && soul.isAlive()) {
            resumeSession(player, record, soul);
            return;
        }
        if (++pending.ticksWaited >= RESUME_REBIND_TIMEOUT_TICKS) {
            abortPendingResume(player, "soul entity not found within rebind window");
        }
    }

    private static void resumeSession(
            ServerPlayer player,
            SoulRoamingSavedData.SoulRoamingRecord record,
            SoulEntity soul
    ) {
        PENDING_RESUMES.remove(player.getUUID());
        SoulRoamingSession session = new SoulRoamingSession(
                record.soulDimension(),
                record.bodyPosition(),
                soul.getUUID(),
                soul.getId(),
                new SoulMovementInput()
        );
        SOUL_ROAMING_PLAYERS.put(player.getUUID(), session);
        keepBodyAtPosition(player, session);
        if (!PlayerMeditationManager.isMeditating(player)) {
            PlayerMeditationManager.startMeditating(player);
        }
        syncState(player);
        syncBodyBoundData(player);
        ModMain.LOGGER.info(
                "Resumed suspended soul roaming session of {} (soul={})",
                player.getGameProfile().getName(),
                soul.getUUID()
        );
    }

    /** 恢复失败兜底：删记录、同步"未魂游"，玩家保持正常可操作状态，绝不卡死。 */
    private static void abortPendingResume(ServerPlayer player, String reason) {
        PENDING_RESUMES.remove(player.getUUID());
        SoulRoamingSavedData.get(player.server).removeRecord(player.getUUID());
        syncInactive(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.returned"),
                true
        );
        ModMain.LOGGER.info(
                "Safely returned {} to body instead of resuming soul roaming: {}",
                player.getGameProfile().getName(),
                reason
        );
    }

    private static void finishSoulRoaming(ServerPlayer player, SoulRoamingSession session) {
        keepBodyAtPosition(player, session);
        getSoulEntity(player.server, session).ifPresent(Entity::discard);
        SOUL_ROAMING_PLAYERS.remove(player.getUUID());
        SoulRoamingSavedData.get(player.server).removeRecord(player.getUUID());
        syncInactive(player);
        syncBodyBoundData(player);
    }

    private static void keepBodyAtPosition(ServerPlayer player, SoulRoamingSession session) {
        if (player.level().dimension() != session.soulDimension()) {
            return;
        }
        Vec3 bodyPosition = session.bodyPosition();
        if (player.distanceToSqr(bodyPosition) > 0.0001D) {
            player.teleportTo(bodyPosition.x, bodyPosition.y, bodyPosition.z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static SoulRoamingSyncData createSyncData(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return SoulRoamingSyncData.inactive();
        }
        return new SoulRoamingSyncData(
                true,
                session.soulDimension(),
                session.bodyPosition(),
                session.soulEntityId(),
                true
        );
    }

    private static Optional<SoulEntity> getSoulEntity(
            MinecraftServer server,
            SoulRoamingSession session
    ) {
        return findSoulEntity(server, session.soulDimension(), session.soulUuid());
    }

    private static Optional<SoulEntity> findSoulEntity(
            MinecraftServer server,
            ResourceKey<Level> dimension,
            UUID soulUuid
    ) {
        ServerLevel soulLevel = server.getLevel(dimension);
        if (soulLevel == null) {
            return Optional.empty();
        }
        Entity entity = soulLevel.getEntity(soulUuid);
        return entity instanceof SoulEntity soul
                ? Optional.of(soul)
                : Optional.empty();
    }

    private static void syncInactive(ServerPlayer player) {
        ModNetwork.sendSoulRoamingState(player, SoulRoamingSyncData.inactive());
    }

    private static void syncBodyBoundData(ServerPlayer player) {
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
        ModNetwork.sendTechniqueLibrary(player);
        ModNetwork.sendPlayerValueControlSettings(player);
    }

    /** 重启后登录的重绑窗口状态：持有挂起记录并计数等待 tick。 */
    private static final class PendingResume {
        private final SoulRoamingSavedData.SoulRoamingRecord record;
        private int ticksWaited;

        private PendingResume(SoulRoamingSavedData.SoulRoamingRecord record) {
            this.record = record;
        }
    }

    private record SoulRoamingSession(
            ResourceKey<Level> soulDimension,
            Vec3 bodyPosition,
            UUID soulUuid,
            int soulEntityId,
            SoulMovementInput movementInput
    ) {
    }

    private static final class SoulMovementInput {
        private float leftImpulse;
        private float forwardImpulse;
        private boolean jumping;
        private boolean shiftKeyDown;
        private float yRot;
        private float xRot;
        private int lastUpdateTick;

        private void update(
                float leftImpulse,
                float forwardImpulse,
                boolean jumping,
                boolean shiftKeyDown,
                float yRot,
                float xRot,
                int updateTick
        ) {
            this.leftImpulse = leftImpulse;
            this.forwardImpulse = forwardImpulse;
            this.jumping = jumping;
            this.shiftKeyDown = shiftKeyDown;
            this.yRot = yRot;
            this.xRot = xRot;
            this.lastUpdateTick = updateTick;
        }

        private void clear() {
            this.leftImpulse = 0.0F;
            this.forwardImpulse = 0.0F;
            this.jumping = false;
            this.shiftKeyDown = false;
        }

        private float leftImpulse() {
            return this.leftImpulse;
        }

        private float forwardImpulse() {
            return this.forwardImpulse;
        }

        private float verticalImpulse() {
            return (this.jumping ? 1.0F : 0.0F) + (this.shiftKeyDown ? -1.0F : 0.0F);
        }

        private float yRot() {
            return this.yRot;
        }

        private float xRot() {
            return this.xRot;
        }

        private int lastUpdateTick() {
            return this.lastUpdateTick;
        }
    }
}
