package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.entity.SoulBodyEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.utility.PossessionUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerSoulRoamingManager {
    private static final float SOUL_FLYING_SPEED = 0.08F;
    private static final Map<UUID, SoulRoamingSession> SOUL_ROAMING_PLAYERS = new HashMap<>();

    private PlayerSoulRoamingManager() {
    }

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
        boolean wasMeditating = PlayerMeditationManager.isMeditating(player);
        if (!canStart(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.cannot_start"),
                    true
            );
            return false;
        }

        SoulBodyEntity body = ModEntityTypes.SOUL_BODY.get().create(player.level());
        if (body == null) {
            return false;
        }
        body.setOwnerUuid(player.getUUID());
        body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        body.setYHeadRot(player.yHeadRot);
        body.setYBodyRot(player.yBodyRot);
        body.yHeadRotO = player.yHeadRotO;
        body.yBodyRotO = player.yBodyRotO;
        body.setHealth(body.getMaxHealth());
        ServerLevel bodyLevel = player.serverLevel();
        ChunkPos bodyChunk = new ChunkPos(body.blockPosition());
        ForgeChunkManager.forceChunk(
                bodyLevel,
                ModMain.MOD_ID,
                player.getUUID(),
                bodyChunk.x,
                bodyChunk.z,
                true,
                true
        );
        if (!bodyLevel.addFreshEntity(body)) {
            ForgeChunkManager.forceChunk(
                    bodyLevel,
                    ModMain.MOD_ID,
                    player.getUUID(),
                    bodyChunk.x,
                    bodyChunk.z,
                    false,
                    true
            );
            return false;
        }

        AbilitySnapshot abilitySnapshot = AbilitySnapshot.capture(player);
        SoulRoamingSession session = new SoulRoamingSession(
                player.level().dimension(),
                body.position(),
                player.getUUID(),
                body.getUUID(),
                body.getId(),
                bodyChunk.x,
                bodyChunk.z,
                abilitySnapshot
        );
        SOUL_ROAMING_PLAYERS.put(player.getUUID(), session);
        if (wasMeditating) {
            PlayerMeditationManager.stopMeditating(player);
        }
        applySoulAbilities(player);
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
        applySoulAbilities(player);
        player.fallDistance = 0.0F;
        player.setHealth(Math.max(player.getHealth(), 1.0F));
        SoulBodyEntity body = getBodyEntity(player.server, session).orElse(null);
        if (body == null || !body.isAlive()) {
            if (session.bodyExists()) {
                notifyBodyDestroyed(player.server, player, session);
            }
        } else {
            session.updateBody(body.position(), body.getId());
        }
    }

    public static boolean returnToBody(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        SoulBodyEntity body = getBodyEntity(player.server, session).orElse(null);
        if (body == null || !body.isAlive()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.body_missing"),
                    true
            );
            syncState(player);
            return false;
        }
        ServerLevel bodyLevel = player.server.getLevel(session.bodyDimension());
        if (bodyLevel == null) {
            return false;
        }
        Vec3 bodyPosition = body.position();
        restoreSoulState(player, session, false);
        player.teleportTo(bodyLevel, bodyPosition.x, bodyPosition.y, bodyPosition.z, body.getYRot(), body.getXRot());
        releaseBodyChunk(player.server, session);
        body.discard();
        SOUL_ROAMING_PLAYERS.remove(player.getUUID());
        syncInactive(player);
        syncBodyBoundData(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.returned"),
                true
        );
        return true;
    }

    public static boolean tryPossess(ServerPlayer player, LivingEntity target) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        PossessionUtility.PossessionResult result = PossessionUtility.tryPossess(player, target);
        if (!result.success()) {
            player.displayClientMessage(result.message(), true);
            return false;
        }
        removeBody(player, session);
        restoreSoulState(player, session, true);
        SOUL_ROAMING_PLAYERS.remove(player.getUUID());
        syncInactive(player);
        syncBodyBoundData(player);
        player.displayClientMessage(result.message(), true);
        return true;
    }

    public static boolean isSoulRoaming(ServerPlayer player) {
        return isSoulRoaming(player.getUUID());
    }

    public static boolean isSoulRoaming(UUID playerUuid) {
        return SOUL_ROAMING_PLAYERS.containsKey(playerUuid);
    }

    public static boolean ownsBody(ServerPlayer player, SoulBodyEntity body) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        return session != null && session.bodyUuid().equals(body.getUUID());
    }

    public static void onBodyDestroyed(SoulBodyEntity body) {
        Optional<UUID> ownerUuid = body.getOwnerUuid();
        if (ownerUuid.isEmpty()) {
            return;
        }
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(ownerUuid.get());
        if (session != null && session.bodyUuid().equals(body.getUUID())) {
            MinecraftServer server = body.getServer();
            if (server == null) {
                session.markBodyDestroyed();
                return;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid.get());
            notifyBodyDestroyed(server, player, session);
        }
    }

    public static void cleanup(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.remove(player.getUUID());
        if (session == null) {
            syncInactive(player);
            return;
        }
        removeBody(player, session);
        restoreSoulState(player, session, false);
        syncInactive(player);
    }

    public static void syncState(ServerPlayer player) {
        ModNetwork.sendSoulRoamingState(player, createSyncData(player));
    }

    public static void clear(MinecraftServer server) {
        SOUL_ROAMING_PLAYERS.values().forEach(session -> releaseBodyChunk(server, session));
        SOUL_ROAMING_PLAYERS.clear();
    }

    private static boolean canStart(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && player.onGround();
    }

    private static SoulRoamingSyncData createSyncData(ServerPlayer player) {
        SoulRoamingSession session = SOUL_ROAMING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return SoulRoamingSyncData.inactive();
        }
        return new SoulRoamingSyncData(
                true,
                session.bodyDimension(),
                session.bodyPosition(),
                session.bodyEntityId(),
                session.bodyExists()
        );
    }

    private static Optional<SoulBodyEntity> getBodyEntity(
            net.minecraft.server.MinecraftServer server,
            SoulRoamingSession session
    ) {
        ServerLevel bodyLevel = server.getLevel(session.bodyDimension());
        if (bodyLevel == null) {
            return Optional.empty();
        }
        Entity entity = bodyLevel.getEntity(session.bodyUuid());
        return entity instanceof SoulBodyEntity body
                ? Optional.of(body)
                : Optional.empty();
    }

    private static void removeBody(ServerPlayer player, SoulRoamingSession session) {
        releaseBodyChunk(player.server, session);
        getBodyEntity(player.server, session).ifPresent(Entity::discard);
    }

    private static void notifyBodyDestroyed(
            MinecraftServer server,
            ServerPlayer player,
            SoulRoamingSession session
    ) {
        if (!session.bodyExists()) {
            return;
        }
        releaseBodyChunk(server, session);
        session.markBodyDestroyed();
        if (player == null) {
            return;
        }
        syncState(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.soul_roaming.body_destroyed"),
                true
        );
    }

    private static void releaseBodyChunk(MinecraftServer server, SoulRoamingSession session) {
        if (!session.bodyChunkForced()) {
            return;
        }
        ServerLevel bodyLevel = server.getLevel(session.bodyDimension());
        if (bodyLevel != null) {
            ForgeChunkManager.forceChunk(
                    bodyLevel,
                    ModMain.MOD_ID,
                    session.ownerUuid(),
                    session.bodyChunkX(),
                    session.bodyChunkZ(),
                    false,
                    true
            );
        }
        session.markBodyChunkReleased();
    }

    private static void applySoulAbilities(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setInvisible(true);
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.invulnerable = true;
        abilities.mayBuild = false;
        abilities.setFlyingSpeed(SOUL_FLYING_SPEED);
        player.onUpdateAbilities();
    }

    private static void restoreSoulState(
            ServerPlayer player,
            SoulRoamingSession session,
            boolean keepCurrentPosition
    ) {
        session.abilitySnapshot().restore(player);
        if (!keepCurrentPosition) {
            player.setDeltaMovement(Vec3.ZERO);
        }
        player.fallDistance = 0.0F;
        player.onUpdateAbilities();
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

    private record AbilitySnapshot(
            boolean invulnerable,
            boolean flying,
            boolean mayfly,
            boolean instabuild,
            boolean mayBuild,
            boolean invisible,
            boolean noGravity,
            boolean noPhysics,
            float flyingSpeed,
            float walkingSpeed
    ) {
        private static AbilitySnapshot capture(ServerPlayer player) {
            Abilities abilities = player.getAbilities();
            return new AbilitySnapshot(
                    abilities.invulnerable,
                    abilities.flying,
                    abilities.mayfly,
                    abilities.instabuild,
                    abilities.mayBuild,
                    player.isInvisible(),
                    player.isNoGravity(),
                    player.noPhysics,
                    abilities.getFlyingSpeed(),
                    abilities.getWalkingSpeed()
            );
        }

        private void restore(ServerPlayer player) {
            Abilities abilities = player.getAbilities();
            abilities.invulnerable = this.invulnerable;
            abilities.flying = this.flying;
            abilities.mayfly = this.mayfly;
            abilities.instabuild = this.instabuild;
            abilities.mayBuild = this.mayBuild;
            player.setInvisible(this.invisible);
            player.setNoGravity(this.noGravity);
            player.noPhysics = this.noPhysics;
            abilities.setFlyingSpeed(this.flyingSpeed);
            abilities.setWalkingSpeed(this.walkingSpeed);
        }
    }

    private static final class SoulRoamingSession {
        private final ResourceKey<Level> bodyDimension;
        private final UUID bodyUuid;
        private final UUID ownerUuid;
        private final int bodyChunkX;
        private final int bodyChunkZ;
        private final AbilitySnapshot abilitySnapshot;
        private Vec3 bodyPosition;
        private int bodyEntityId;
        private boolean bodyExists = true;
        private boolean bodyChunkForced = true;

        private SoulRoamingSession(
                ResourceKey<Level> bodyDimension,
                Vec3 bodyPosition,
                UUID ownerUuid,
                UUID bodyUuid,
                int bodyEntityId,
                int bodyChunkX,
                int bodyChunkZ,
                AbilitySnapshot abilitySnapshot
        ) {
            this.bodyDimension = bodyDimension;
            this.bodyPosition = bodyPosition;
            this.ownerUuid = ownerUuid;
            this.bodyUuid = bodyUuid;
            this.bodyEntityId = bodyEntityId;
            this.bodyChunkX = bodyChunkX;
            this.bodyChunkZ = bodyChunkZ;
            this.abilitySnapshot = abilitySnapshot;
        }

        private ResourceKey<Level> bodyDimension() {
            return this.bodyDimension;
        }

        private Vec3 bodyPosition() {
            return this.bodyPosition;
        }

        private UUID bodyUuid() {
            return this.bodyUuid;
        }

        private UUID ownerUuid() {
            return this.ownerUuid;
        }

        private int bodyEntityId() {
            return this.bodyEntityId;
        }

        private int bodyChunkX() {
            return this.bodyChunkX;
        }

        private int bodyChunkZ() {
            return this.bodyChunkZ;
        }

        private AbilitySnapshot abilitySnapshot() {
            return this.abilitySnapshot;
        }

        private boolean bodyExists() {
            return this.bodyExists;
        }

        private boolean bodyChunkForced() {
            return this.bodyChunkForced;
        }

        private void updateBody(Vec3 updatedBodyPosition, int updatedBodyEntityId) {
            this.bodyPosition = updatedBodyPosition;
            this.bodyEntityId = updatedBodyEntityId;
            this.bodyExists = true;
        }

        private void markBodyDestroyed() {
            this.bodyExists = false;
            this.bodyEntityId = -1;
        }

        private void markBodyChunkReleased() {
            this.bodyChunkForced = false;
        }
    }
}
