package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.utility.PossessionUtility;
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

public final class PlayerSoulRoamingManager {
    private static final double SOUL_INTERACTION_DISTANCE = 6.0D;
    private static final Map<UUID, SoulRoamingSession> SOUL_ROAMING_PLAYERS = new HashMap<>();

    public static boolean startSoulRoaming(ServerPlayer player) {
        if (isSoulRoaming(player)) {
            syncState(player);
            return true;
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
        syncState(player);
        syncBodyBoundData(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.started"),
                true
        );
        return true;
    }

    public static void tick(ServerPlayer player) {
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

    public static void clear(MinecraftServer server) {
        SOUL_ROAMING_PLAYERS.values().forEach(session ->
                getSoulEntity(server, session).ifPresent(Entity::discard)
        );
        SOUL_ROAMING_PLAYERS.clear();
    }

    private static void finishSoulRoaming(ServerPlayer player, SoulRoamingSession session) {
        keepBodyAtPosition(player, session);
        getSoulEntity(player.server, session).ifPresent(Entity::discard);
        SOUL_ROAMING_PLAYERS.remove(player.getUUID());
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
        ServerLevel soulLevel = server.getLevel(session.soulDimension());
        if (soulLevel == null) {
            return Optional.empty();
        }
        Entity entity = soulLevel.getEntity(session.soulUuid());
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
