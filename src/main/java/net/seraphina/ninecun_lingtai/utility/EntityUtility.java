package net.seraphina.ninecun_lingtai.utility;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.jvm.JvmUtility;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class EntityUtility {
    private static final int KILLED_ENTITY_DEATH_TICKS = 20;
    private static EntityDataAccessor<Boolean> KILLED_ENTITY;
    private static final ThreadLocal<Boolean> RESTORING_HEALTH = ThreadLocal.withInitial(() -> false);
    private static final WeakHashMap<Entity, DoublePosition> BLOCKED_REMOVAL_POSITIONS = new WeakHashMap<>();

    public static void initializeKilledEntityDataAccessor() {
        KILLED_ENTITY = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.BOOLEAN
        );
    }

    public static boolean hasActiveDefense(LivingEntity entity) {
        return !isKilledEntity(entity) && CultivationDefenseUtility.hasActiveDefense(entity);
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

    public static boolean tryInstantlyKillLowerRealmCultivator(
            LivingEntity defender,
            DamageSource damageSource
    ) {
        if (defender.level().isClientSide
                || isKilledEntity(defender)
                || !CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(defender, damageSource)
                || !(damageSource.getEntity() instanceof LivingEntity attacker)) {
            return false;
        }

        EntityDefenseResourceData.deplete(defender);
        if (defender instanceof ServerPlayer player) {
            ModNetwork.sendDefenseResource(player);
        }
        killedEntity(defender, attacker);
        return true;
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

    public static void killedEntity(Entity targetAndKiller) {
        killedEntity(targetAndKiller, targetAndKiller);
    }

    public static void killedEntity(Entity target, Entity killer) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(killer);
        if (!(target instanceof LivingEntity targetLiving)) return;
        markKilledEntity(targetLiving);
        DamageSource damageSource = killer instanceof Player player ? targetLiving.damageSources().playerAttack(player) : killer instanceof LivingEntity living ? targetLiving.damageSources().mobAttack(living) : targetLiving.damageSources().generic();
        forceActuallyHurt(targetLiving, damageSource, Float.POSITIVE_INFINITY);
        if (killer.level instanceof ServerLevel serverLevel) {
            killer.killedEntity(serverLevel, targetLiving);
        }
        targetLiving.gameEvent(GameEvent.ENTITY_DIE);
        targetLiving.level().broadcastEntityEvent(targetLiving, (byte) 3);
        targetLiving.isAddedToWorld = false;
        targetLiving.deathTime = 0;
        targetLiving.hurtTime = 10;
        targetLiving.hurtDuration = 0;
        targetLiving.invulnerableTime = 20;
        targetLiving.dead = true;
        targetLiving.stopSleeping();
        targetLiving.stopRiding();
        targetLiving.stopUsingItem();
        targetLiving.die(killer.damageSources().generic());
        if (killer instanceof Player player)
            targetLiving.lastHurtByPlayer = player;
        else if (killer instanceof LivingEntity livingKiller)
            targetLiving.lastHurtByMob = livingKiller;
        targetLiving.deltaMovement = Vec3.ZERO;
        if (killer instanceof ServerPlayer serverPlayer)
            targetLiving.stopSeenByPlayer(serverPlayer);
        SoundEvent[] soundEvents = {targetLiving.getDeathSound(), targetLiving.getHurtSound(damageSource)};
        if (soundEvents[0] != null)
            targetLiving.playSound(soundEvents[0]);
        for (int i = 1; i < 5; i++) {
            if (soundEvents[1] != null)
                targetLiving.playSound(Objects.requireNonNull(soundEvents[1]));
        }
        dropAllDeathLoot(targetLiving, damageSource);
    }

    public static void dropAllDeathLoot(LivingEntity living, DamageSource damageSource) {
        int i = 42;
        living.captureDrops(new ArrayList());
        dropFromLootTable(living, damageSource);
        living.dropCustomDeathLoot(damageSource, i, true);

        living.dropEquipment();
        dropExperience(living);
        Collection<ItemEntity> drops = living.captureDrops(null);
        drops.forEach((e) -> living.level().addFreshEntity(e));
    }

    public static void dropAllDeathLoot(LivingEntity living) {
        dropAllDeathLoot(living, living.damageSources().generic());
    }

    public static void dropFromLootTable(LivingEntity living, DamageSource pDamageSource) {
        ResourceLocation resourcelocation = living.getLootTable();
        LootTable loottable = living.level().getServer().getLootData().getLootTable(resourcelocation);
        LootParams.Builder lootparams$builder = (new LootParams.Builder((ServerLevel)living.level())).withParameter(LootContextParams.THIS_ENTITY, living).withParameter(LootContextParams.ORIGIN, living.position()).withParameter(LootContextParams.DAMAGE_SOURCE, pDamageSource).withOptionalParameter(LootContextParams.KILLER_ENTITY, pDamageSource.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, pDamageSource.getDirectEntity());
        if (living.lastHurtByPlayer != null) {
            lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, living.lastHurtByPlayer).withLuck(living.lastHurtByPlayer.getLuck());
        }

        LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
        loottable.getRandomItems(lootparams, living.getLootTableSeed(), living::spawnAtLocation);
    }

    public static void dropExperience(LivingEntity livingEntity, int multiplier) {
        int reward = livingEntity.getExperienceReward() * multiplier;
        ExperienceOrb.award((ServerLevel)livingEntity.level(), livingEntity.position(), reward);
    }

    public static void dropExperience(LivingEntity livingEntity) {
        dropExperience(livingEntity, 1);
    }

    @SuppressWarnings("all")
    public static void forceActuallyHurt(
            LivingEntity target,
            DamageSource damageSource,
            float amount
    ) {
        if (target.level().isClientSide || !(amount > 0.0F) || Float.isNaN(amount)) {
            return;
        }

        ForgeHooks.onLivingHurt(target, damageSource, amount);
        if (Float.isInfinite(amount)) {
            target.setAbsorptionAmount(0.0F);
            target.entityData.set(LivingEntity.DATA_HEALTH_ID, 0.0F);
            target.gameEvent(GameEvent.ENTITY_DAMAGE);
            return;
        }

        float absorption = target.getAbsorptionAmount();
        float absorbedAmount = Math.min(absorption, amount);
        float healthDamage = amount - absorbedAmount;
        target.setAbsorptionAmount(absorption - absorbedAmount);
        if (healthDamage <= 0.0F) {
            return;
        }

        float currentHealth = target.entityData.get(LivingEntity.DATA_HEALTH_ID);
        target.entityData.set(
                LivingEntity.DATA_HEALTH_ID,
                Math.max(0.0F, currentHealth - healthDamage)
        );
        target.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    public static void tickKilledEntity(LivingEntity entity) {
        entity.entityData.set(LivingEntity.DATA_HEALTH_ID, 0.0F);
        entity.setAbsorptionAmount(0.0F);
        entity.dead = true;
        entity.isAddedToWorld = false;
        entity.deathTime++;
        if (entity.deathTime >= KILLED_ENTITY_DEATH_TICKS
                && !entity.isRemoved()
                && !(entity instanceof Player)) {
            entity.remove(RemovalReason.KILLED);
        }
    }

    public static void forceRemoveEntity(Level level, Entity entity) {
        if (level instanceof ServerLevel serverLevel) {
            removedFromServer(serverLevel, entity);
            ModNetwork.sendForceRemoveEntity(serverLevel, entity);
        }
    }

    static void removedFromServer(ServerLevel level, Entity entity) {
        Set<Entity> managedEntities = getManagedEntities(entity);
        removePassengerRelations(entity);

        PersistentEntitySectionManager<Entity> entityManager = level.entityManager;
        removeFromPersistentEntityManager(entityManager, managedEntities);
        removeFromEntityTickList(level.entityTickList, managedEntities);
        level.players.removeIf(managedEntities::contains);
        level.navigatingMobs.removeIf(managedEntities::contains);
        removeFromEntityMap(level.dragonParts, managedEntities);
        removeFromChunkMap(level.chunkSource.chunkMap, managedEntities);

        markEntitiesRemoved(managedEntities);
    }

    @OnlyIn(Dist.CLIENT)
    public static void removedFromClient(ClientLevel level, Entity entity) {
        Set<Entity> managedEntities = getManagedEntities(entity);
        removePassengerRelations(entity);

        TransientEntitySectionManager<Entity> entityStorage = level.entityStorage;
        removeFromEntityLookup(entityStorage.entityStorage, managedEntities);
        removeFromEntitySections(entityStorage.sectionStorage, managedEntities);
        removeFromEntityTickList(level.tickingEntities, managedEntities);
        level.players.removeIf(managedEntities::contains);
        removeFromEntityMap(level.partEntities, managedEntities);

        markEntitiesRemoved(managedEntities);
    }

    private static Set<Entity> getManagedEntities(Entity entity) {
        Set<Entity> managedEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        managedEntities.add(entity);
        if (entity.isMultipartEntity()) {
            Collections.addAll(managedEntities, entity.getParts());
        }
        return managedEntities;
    }

    private static void removeFromPersistentEntityManager(
            PersistentEntitySectionManager<Entity> entityManager,
            Set<Entity> managedEntities
    ) {
        for (Entity managedEntity : managedEntities) {
            entityManager.knownUuids.remove(managedEntity.getUUID());
        }
        removeFromEntityLookup(entityManager.visibleEntityStorage, managedEntities);
        removeFromEntitySections(entityManager.sectionStorage, managedEntities);
        removeFromLoadingInbox(entityManager, managedEntities);
    }

    private static void removeFromLoadingInbox(
            PersistentEntitySectionManager<Entity> entityManager,
            Set<Entity> managedEntities
    ) {
        Iterator<ChunkEntities<Entity>> iterator = entityManager.loadingInbox.iterator();
        while (iterator.hasNext()) {
            ChunkEntities<Entity> chunkEntities = iterator.next();
            List<Entity> remainingEntities = new ArrayList<>(chunkEntities.entities.size());
            for (Entity queuedEntity : chunkEntities.entities) {
                if (!managedEntities.contains(queuedEntity)) {
                    remainingEntities.add(queuedEntity);
                }
            }
            if (remainingEntities.size() != chunkEntities.entities.size()) {
                iterator.remove();
                entityManager.loadingInbox.add(new ChunkEntities<>(chunkEntities.pos, remainingEntities));
            }
        }
    }

    private static void removeFromEntityLookup(
            EntityLookup<Entity> entityLookup,
            Set<Entity> managedEntities
    ) {
        removeFromEntityMap(entityLookup.byId, managedEntities);

        entityLookup.byUuid.entrySet().removeIf(uuidEntityEntry -> managedEntities.contains(uuidEntityEntry.getValue()));
    }

    private static void removeFromEntitySections(
            EntitySectionStorage<Entity> entitySectionStorage,
            Set<Entity> managedEntities
    ) {
        Iterator<Long2ObjectMap.Entry<EntitySection<Entity>>> iterator =
                entitySectionStorage.sections.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<EntitySection<Entity>> entry = iterator.next();
            EntitySection<Entity> entitySection = entry.getValue();
            removeFromClassInstanceMultiMap(entitySection.storage, managedEntities);
            if (entitySection.storage.allInstances.isEmpty()) {
                entitySectionStorage.sectionIds.remove(entry.getLongKey());
                iterator.remove();
            }
        }
    }

    private static void removeFromClassInstanceMultiMap(
            ClassInstanceMultiMap<Entity> entityStorage,
            Set<Entity> managedEntities
    ) {
        for (List<Entity> entities : entityStorage.byClass.values()) {
            entities.removeIf(managedEntities::contains);
        }
        entityStorage.allInstances.removeIf(managedEntities::contains);
    }

    private static void removeFromEntityTickList(
            EntityTickList entityTickList,
            Set<Entity> managedEntities
    ) {
        if (entityTickList.iterated == entityTickList.active) {
            entityTickList.passive.clear();
            entityTickList.passive.putAll(entityTickList.active);
            Int2ObjectMap<Entity> previouslyActive = entityTickList.active;
            entityTickList.active = entityTickList.passive;
            entityTickList.passive = previouslyActive;
            removeFromEntityMap(entityTickList.active, managedEntities);
            return;
        }

        Set<Int2ObjectMap<Entity>> tickLists = Collections.newSetFromMap(new IdentityHashMap<>());
        tickLists.add(entityTickList.active);
        tickLists.add(entityTickList.passive);
        if (entityTickList.iterated != null) {
            tickLists.add(entityTickList.iterated);
        }
        for (Int2ObjectMap<Entity> tickList : tickLists) {
            removeFromEntityMap(tickList, managedEntities);
        }
    }

    private static void removeFromChunkMap(ChunkMap chunkMap, Set<Entity> managedEntities) {
        Iterator<Int2ObjectMap.Entry<ChunkMap.TrackedEntity>> iterator =
                chunkMap.entityMap.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            ChunkMap.TrackedEntity trackedEntity = iterator.next().getValue();
            if (managedEntities.contains(trackedEntity.entity)) {
                trackedEntity.seenBy.clear();
                iterator.remove();
            }
        }

        for (Entity managedEntity : managedEntities) {
            if (managedEntity instanceof ServerPlayer serverPlayer) {
                chunkMap.playerMap.players.removeBoolean(serverPlayer);
                removePlayerFromDistanceManager(chunkMap.distanceManager, serverPlayer);
                for (ChunkMap.TrackedEntity trackedEntity : chunkMap.entityMap.values()) {
                    trackedEntity.seenBy.remove(serverPlayer.connection);
                }
            }
        }
    }

    private static void removePlayerFromDistanceManager(
            net.minecraft.server.level.DistanceManager distanceManager,
            ServerPlayer player
    ) {
        Iterator<Long2ObjectMap.Entry<ObjectSet<ServerPlayer>>> iterator =
                distanceManager.playersPerChunk.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            ObjectSet<ServerPlayer> players = iterator.next().getValue();
            players.removeIf(candidate -> candidate == player);
            if (players.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static <T extends Entity> void removeFromEntityMap(
            Int2ObjectMap<T> entityMap,
            Set<Entity> managedEntities
    ) {
        entityMap.int2ObjectEntrySet().removeIf(tEntry -> managedEntities.contains(tEntry.getValue()));
    }

    private static void removePassengerRelations(Entity entity) {
        if (entity.vehicle != null) {
            Entity vehicle = entity.vehicle;
            vehicle.passengers = vehicle.passengers.stream()
                    .filter(passenger -> passenger != entity)
                    .collect(ImmutableList.toImmutableList());
            entity.vehicle = null;
            entity.boardingCooldown = 60;
        }

        for (Entity passenger : entity.passengers) {
            if (passenger.vehicle == entity) {
                passenger.vehicle = null;
                passenger.boardingCooldown = 60;
            }
        }
        entity.passengers = ImmutableList.of();
    }

    private static void markEntitiesRemoved(Set<Entity> managedEntities) {
        for (Entity managedEntity : managedEntities) {
            if (managedEntity.removalReason == null) {
                managedEntity.removalReason = RemovalReason.KILLED;
            }
            managedEntity.levelCallback = EntityInLevelCallback.NULL;
            managedEntity.isAddedToWorld = false;
        }
    }

    private record DoublePosition(double x, double y, double z) { }

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
        public void onRemove(@NotNull RemovalReason reason) {
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
        if (!(localPlayer instanceof LocalPlayer player)) return;
        player.isAddedToWorld = true;
        JvmUtility.klassPtr(mc, Minecraft.class);
    }
}
