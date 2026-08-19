package net.seraphina.ninecun_lingtai.common.ability;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import net.seraphina.ninecun_lingtai.utility.PressureUtility;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PressureManager {
    private static final Set<UUID> ACTIVE_SOURCES = new HashSet<>();
    private static final Map<UUID, PressureEffectState> ACTIVE_PLAYER_EFFECTS = new HashMap<>();
    private static final Map<UUID, Vec3> LOCKED_PLAYER_POSITIONS = new HashMap<>();


    public static void setActive(ServerPlayer player, boolean active) {
        if (active) {
            ACTIVE_SOURCES.add(player.getUUID());
        } else {
            ACTIVE_SOURCES.remove(player.getUUID());
        }
    }

    public static void tick(MinecraftServer server) {
        ACTIVE_SOURCES.removeIf(playerId -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            return player == null || !player.isAlive();
        });

        Map<LivingEntity, PressureEffectState> nextEffects = new HashMap<>();
        for (UUID sourceId : ACTIVE_SOURCES) {
            ServerPlayer source = server.getPlayerList().getPlayer(sourceId);
            if (source == null || !source.isAlive()) {
                continue;
            }

            CultivationStage sourceStage = CreatureCultivationData.getStage(source)
                    .orElse(CultivationStage.first());
            if (!PressureUtility.hasAdvancedEffect(sourceStage)) {
                continue;
            }

            double range = PressureUtility.getPressureRange(sourceStage);
            double rangeSquared = range * range;
            AABB searchBox = source.getBoundingBox().inflate(range);
            for (LivingEntity target : source.serverLevel().getEntitiesOfClass(
                    LivingEntity.class,
                    searchBox,
                    entity -> entity != source
                            && entity.isAlive()
                            && entity.distanceToSqr(source) <= rangeSquared
            )) {
                var targetStage = CreatureCultivationData.getStage(target);
                if (!PressureUtility.isLower(sourceStage, targetStage)) {
                    continue;
                }
                if (PressureUtility.shouldInstantlyKill(sourceStage, targetStage)) {
                    killByPressure(source, target);
                    continue;
                }

                PressureEffectState candidate = new PressureEffectState(
                        source.getUUID(),
                        PressureUtility.getCultivationRank(sourceStage),
                        PressureUtility.getSeverity(sourceStage, targetStage),
                        true
                );
                PressureEffectState current = nextEffects.get(target);
                if (current == null || isStronger(candidate, current)) {
                    nextEffects.put(target, candidate);
                }
            }
        }

        Map<UUID, PressureEffectState> nextPlayerEffects = new HashMap<>();
        for (Map.Entry<LivingEntity, PressureEffectState> entry : nextEffects.entrySet()) {
            LivingEntity target = entry.getKey();
            applyPressure(target);
            if (target instanceof ServerPlayer player) {
                nextPlayerEffects.put(player.getUUID(), entry.getValue());
            }
        }

        syncPlayerEffects(server, nextPlayerEffects);
        ACTIVE_PLAYER_EFFECTS.clear();
        ACTIVE_PLAYER_EFFECTS.putAll(nextPlayerEffects);
        LOCKED_PLAYER_POSITIONS.keySet().removeIf(playerId -> !nextPlayerEffects.containsKey(playerId));
    }

    public static void clear(ServerPlayer player) {
        ACTIVE_SOURCES.remove(player.getUUID());
        if (ACTIVE_PLAYER_EFFECTS.remove(player.getUUID()) != null) {
            ModNetwork.sendPressureState(player, false, 0.0F, false);
        }
        LOCKED_PLAYER_POSITIONS.remove(player.getUUID());
    }

    public static void clearAll() {
        ACTIVE_SOURCES.clear();
        ACTIVE_PLAYER_EFFECTS.clear();
        LOCKED_PLAYER_POSITIONS.clear();
    }

    private static boolean isStronger(PressureEffectState candidate, PressureEffectState current) {
        return candidate.sourceRank() > current.sourceRank()
                || candidate.sourceRank() == current.sourceRank()
                && candidate.severity() > current.severity();
    }

    private static void applyPressure(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setAggressive(false);
        }
        if (target instanceof ServerPlayer player) {
            Vec3 lockedPosition = LOCKED_PLAYER_POSITIONS.computeIfAbsent(
                    player.getUUID(), ignored -> player.position()
            );
            if (player.distanceToSqr(lockedPosition) > 1.0E-6D) {
                player.setPos(lockedPosition);
            }
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static void syncPlayerEffects(
            MinecraftServer server,
            Map<UUID, PressureEffectState> nextPlayerEffects
    ) {
        for (UUID playerId : ACTIVE_PLAYER_EFFECTS.keySet()) {
            if (!nextPlayerEffects.containsKey(playerId)) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    ModNetwork.sendPressureState(player, false, 0.0F, false);
                }
            }
        }
        for (Map.Entry<UUID, PressureEffectState> entry : nextPlayerEffects.entrySet()) {
            if (entry.getValue().equals(ACTIVE_PLAYER_EFFECTS.get(entry.getKey()))) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                PressureEffectState state = entry.getValue();
                ModNetwork.sendPressureState(player, true, state.severity(), state.immobilized());
            }
        }
    }

    private static void killByPressure(ServerPlayer source, LivingEntity target) {
        EntityDefenseResourceData.deplete(target);
        if (target instanceof ServerPlayer player) {
            ModNetwork.sendDefenseResource(player);
        }

        EntityUtility.killedEntity(target, source);
    }
}
