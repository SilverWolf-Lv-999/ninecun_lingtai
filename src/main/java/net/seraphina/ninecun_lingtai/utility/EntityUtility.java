package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;
import java.util.WeakHashMap;

public final class EntityUtility {
    public static final EntityDataAccessor<Boolean> KILLED_ENTITY = SynchedEntityData.defineId(
            LivingEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final ThreadLocal<Boolean> RESTORING_HEALTH = ThreadLocal.withInitial(() -> false);
    private static final WeakHashMap<Entity, DoublePosition> BLOCKED_REMOVAL_POSITIONS = new WeakHashMap<>();

    public static boolean hasActiveDefense(LivingEntity entity) {
        return CultivationDefenseUtility.hasActiveDefense(entity);
    }

    public static boolean isRestoringHealth() {
        return RESTORING_HEALTH.get();
    }

    public static void defineKilledEntityData(LivingEntity entity) {
        entity.entityData.define(KILLED_ENTITY, false);
    }

    public static void markKilledEntity(LivingEntity entity) {
        entity.entityData.set(KILLED_ENTITY, true);
    }

    public static boolean isKilledEntity(LivingEntity entity) {
        return entity.entityData.get(KILLED_ENTITY);
    }

    public static void restoreHealth(LivingEntity entity) {
        if (isRestoringHealth()) {
            return;
        }
        RESTORING_HEALTH.set(true);
        try {
            entity.setHealth(entity.getMaxHealth());
        } finally {
            RESTORING_HEALTH.set(false);
        }
    }

    public static void protectEntity(Object object) {
        if (!(object instanceof LivingEntity living)) return;
        living.revive();
        restoreHealth(living);
        living.deathTime = 0;
        living.hurtTime = 0;
        living.hurtDuration = 0;
        living.invulnerableTime = 20;
        living.fallDistance = 0;
        living.canUpdate = true;
        living.isAddedToWorld = true;
        living.setAirSupply(300);
        living.clearFire();
        living.dead = false;
        if (living instanceof Player player) {
            player.removeStingerTime = 0;
            player.removeArrowTime = 0;
            player.noJumpDelay = 0;
            if (player.level.isClientSide())
                protectClientPlayer(player);
        }
    }

    public static void markRemovalIntercepted(Entity entity) {
        synchronized (BLOCKED_REMOVAL_POSITIONS) {
            BLOCKED_REMOVAL_POSITIONS.put(
                    entity,
                    new DoublePosition(entity.getX(), entity.getY(), entity.getZ())
            );
        }
    }

    public static void clearRemovalIntercepted(Entity entity) {
        synchronized (BLOCKED_REMOVAL_POSITIONS) {
            BLOCKED_REMOVAL_POSITIONS.remove(entity);
        }
    }

    public static boolean shouldBlockPositionChange(Entity entity, double x, double y, double z) {
        DoublePosition originalPosition;
        synchronized (BLOCKED_REMOVAL_POSITIONS) {
            originalPosition = BLOCKED_REMOVAL_POSITIONS.remove(entity);
        }
        if (originalPosition == null) {
            return false;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return true;
        }
        double deltaX = x - originalPosition.x();
        double deltaY = y - originalPosition.y();
        double deltaZ = z - originalPosition.z();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 4096.0D;
    }

    public static EntityInLevelCallback wrapLevelCallback(
            Entity entity,
            EntityInLevelCallback callback
    ) {
        return new ProtectedLevelCallback(entity, callback);
    }

    public static void killedEntity(Entity target, Entity killer) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(killer);
        if (!(target instanceof LivingEntity targetLiving)) return;
        if (killer.level instanceof ServerLevel serverLevel)
            killer.killedEntity(serverLevel, targetLiving);
        markKilledEntity(targetLiving);
        targetLiving.isAddedToWorld = false;
        targetLiving.deathTime = 20;
        targetLiving.hurtTime = 20;
        targetLiving.hurtDuration = 0;
        targetLiving.invulnerableTime = 20;
        targetLiving.fallDistance = 100000;
        targetLiving.entityData.set(LivingEntity.DATA_HEALTH_ID, 0.0f);
        targetLiving.setHealth(0.0f);
        targetLiving.dead = true;
        targetLiving.hurtArmor(killer.damageSources().fellOutOfWorld(), Float.POSITIVE_INFINITY);
        targetLiving.getArmorSlots().forEach(slot -> {
            slot.setDamageValue(Integer.MIN_VALUE);
            slot.shrink(1);
        });
        targetLiving.stopSleeping();
        targetLiving.stopRiding();
        targetLiving.stopUsingItem();
        if (killer instanceof ServerPlayer serverPlayer)
            targetLiving.stopSeenByPlayer(serverPlayer);
    }

    private record DoublePosition(double x, double y, double z) {
    }

    public static final class ProtectedLevelCallback implements EntityInLevelCallback {
        private final Entity entity;
        private final EntityInLevelCallback delegate;

        private ProtectedLevelCallback(Entity entity, EntityInLevelCallback delegate) {
            this.entity = entity;
            this.delegate = delegate;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public EntityInLevelCallback getDelegate() {
            return this.delegate;
        }

        @Override
        public void onMove() {
            this.delegate.onMove();
        }

        @Override
        public void onRemove(RemovalReason reason) {
            if (this.entity instanceof LivingEntity living
                    && EntityUtility.hasActiveDefense(living)) {
                EntityUtility.markRemovalIntercepted(this.entity);
                EntityUtility.protectEntity(living);
                return;
            }
            this.delegate.onRemove(reason);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static void protectClientPlayer(Player localPlayer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DeathScreen) {
            mc.screen = null;
            mc.setScreen(null);
        }
        if (!(localPlayer instanceof LocalPlayer player)) return;
        player.isAddedToWorld = true;
    }
}
