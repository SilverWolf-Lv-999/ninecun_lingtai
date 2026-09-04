package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.RealmDefinition;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.utility.RealmSpawnUtility;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PalmBuddhaRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String REALM_STATE_FILE_NAME = "palm_buddha_realms.json";
    private static final String MEMBER_STATE_FILE_NAME = "palm_buddha_members.json";
    private static final String ENTITY_MEMBER_STATE_FILE_NAME = "palm_buddha_entity_members.json";
    private static final int ENTITY_STATE_UPDATE_INTERVAL_TICKS = 200;
    private static final Map<MinecraftServer, PalmBuddhaRealmManager> MANAGERS = new WeakHashMap<>();

    private final MinecraftServer thisServer;
    private final PalmBuddhaRealmState thisRealmState;
    private final PalmBuddhaMemberState thisMemberState;
    private final PalmBuddhaEntityState thisEntityState;
    private final Set<UUID> thisAuthorizedReleasePlayerIds = new HashSet<>();
    private final Set<UUID> thisAuthorizedReleaseEntityIds = new HashSet<>();
    private long thisEntityStateTick;

    private PalmBuddhaRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisRealmState = PalmBuddhaRealmState.load(getStateFilePath(server, REALM_STATE_FILE_NAME));
        this.thisMemberState = PalmBuddhaMemberState.load(getStateFilePath(server, MEMBER_STATE_FILE_NAME));
        this.thisEntityState = PalmBuddhaEntityState.load(
                getStateFilePath(server, ENTITY_MEMBER_STATE_FILE_NAME)
        );
    }

    public static void initialize(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (MANAGERS) {
            MANAGERS.computeIfAbsent(server, PalmBuddhaRealmManager::new);
        }
    }

    public static PalmBuddhaRealmManager get(MinecraftServer server) {
        initialize(server);
        synchronized (MANAGERS) {
            return MANAGERS.get(server);
        }
    }

    public static void registerPersistedRealmDefinitions(MinecraftServer server) {
        get(server).registerPersistedRealmDefinitions();
    }

    public static void clear(MinecraftServer server) {
        synchronized (MANAGERS) {
            MANAGERS.remove(server);
        }
    }

    public synchronized RealmDefinition getOrCreateRealmDefinition(ServerPlayer owner) {
        Objects.requireNonNull(owner, "owner");
        RealmDefinition realmDefinition = this.thisRealmState.getOrCreateRealm(
                owner.getUUID(),
                owner.getGameProfile().getName(),
                new Random()
        );
        this.registerWithDynamicRealmApi(realmDefinition);
        return realmDefinition;
    }

    public synchronized ServerLevel getOrLoadRealm(ServerPlayer owner) {
        RealmDefinition realmDefinition = this.getOrCreateRealmDefinition(owner);
        return this.getOrLoadRealmByDefinition(realmDefinition);
    }

    private ServerLevel getOrLoadRealmByDefinition(RealmDefinition realmDefinition) {
        this.registerWithDynamicRealmApi(realmDefinition);
        return ThreeThousandRealmManager.get(this.thisServer)
                .getOrLoadManagedRealm(realmDefinition);
    }

    public synchronized Optional<RealmDefinition> getRealmDefinition(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.getOwnerId(levelKey).flatMap(this.thisRealmState::getRealm);
    }

    public synchronized Optional<UUID> getOwnerId(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.thisRealmState.getOwner(levelKey.location());
    }

    public synchronized boolean isPalmBuddhaRealm(ResourceKey<Level> levelKey) {
        return this.getRealmDefinition(levelKey).isPresent();
    }

    public synchronized Optional<UUID> getMemberOwnerId(UUID memberId) {
        return this.thisMemberState.get(memberId).map(PalmBuddhaMemberState.MemberRecord::ownerId);
    }

    public synchronized boolean isAnyMember(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return this.thisMemberState.contains(memberId) || this.thisEntityState.contains(memberId);
    }

    public synchronized Optional<UUID> getEntityMemberOwnerId(UUID memberId) {
        return this.thisEntityState.get(memberId).map(PalmBuddhaEntityState.EntityRecord::ownerId);
    }

    /**
     * Returns only the real entity loaded in its recorded owner realm.  The UUID
     * is never searched in arbitrary dimensions, so an entity in a foreign realm
     * cannot be mistaken for a protected palm-buddha member.
     */
    public synchronized Optional<Entity> getLoadedMemberEntity(UUID memberId) {
        PalmBuddhaEntityState.EntityRecord entityRecord = this.thisEntityState.get(memberId).orElse(null);
        if (entityRecord == null) {
            return Optional.empty();
        }
        ResourceLocation realmId = this.thisRealmState.getRealm(entityRecord.ownerId())
                .map(RealmDefinition::getId)
                .orElse(null);
        if (realmId == null) {
            return Optional.empty();
        }
        ServerLevel realmLevel = this.thisServer.getLevel(ResourceKey.create(
                Registries.DIMENSION,
                realmId
        ));
        if (realmLevel == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(realmLevel.getEntity(memberId));
    }

    public synchronized boolean isEntityMemberInExpectedRealm(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return this.getEntityMemberOwnerId(entity.getUUID())
                .flatMap(ownerId -> this.thisRealmState.getRealm(ownerId))
                .map(realm -> realm.getId().equals(entity.level().dimension().location()))
                .orElse(false);
    }

    public synchronized List<PalmBuddhaMemberState.MemberEntry> getMembers(UUID ownerId) {
        return this.thisMemberState.getByOwner(ownerId);
    }

    /**
     * 创建一页仅供 owner 使用的成员摘要。
     *
     * <p>分页在服务端完成，客户端不能通过请求包扩大响应大小；成员排序也在服务端
     * 固定为 UUID 顺序，避免离线成员名称变化导致翻页漂移。</p>
     */
    public synchronized PalmBuddhaMemberPageSnapshot getMemberPage(
            ServerPlayer owner,
            int requestedPageIndex
    ) {
        Objects.requireNonNull(owner, "owner");
        Map<UUID, PalmBuddhaMemberSnapshot> allMembersById = new LinkedHashMap<>();
        this.getMembers(owner.getUUID()).stream()
                .map(entry -> PalmBuddhaMemberSnapshot.from(this.thisServer, this, entry))
                .forEach(snapshot -> allMembersById.put(snapshot.memberId(), snapshot));
        this.thisEntityState.getByOwner(owner.getUUID()).stream()
                .map(entry -> PalmBuddhaMemberSnapshot.fromEntity(this.thisServer, this, entry))
                .forEach(snapshot -> allMembersById.putIfAbsent(snapshot.memberId(), snapshot));
        List<PalmBuddhaMemberSnapshot> allMembers = allMembersById.values().stream()
                .sorted(Comparator.comparing(snapshot -> snapshot.memberId().toString()))
                .limit(PalmBuddhaMemberPacketLimits.MAXIMUM_TOTAL_MEMBER_COUNT)
                .toList();
        int totalMemberCount = allMembers.size();
        int pageCount = totalMemberCount == 0
                ? 0
                : (totalMemberCount + PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE - 1)
                        / PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE;
        int pageIndex = pageCount == 0
                ? 0
                : Math.max(0, Math.min(pageCount - 1, requestedPageIndex));
        int fromIndex = pageIndex * PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE;
        int toIndex = Math.min(
                fromIndex + PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE,
                totalMemberCount
        );
        List<PalmBuddhaMemberSnapshot> snapshots = allMembers.subList(fromIndex, toIndex);
        return new PalmBuddhaMemberPageSnapshot(
                pageIndex,
                pageCount,
                totalMemberCount,
                snapshots
        );
    }

    public synchronized PalmBuddhaCandidateSet getCandidateSet(ServerPlayer owner) {
        Objects.requireNonNull(owner, "owner");
        if (ThreeThousandRealmManager.get(this.thisServer)
                .isManagedRealm(owner.serverLevel().dimension())) {
            return PalmBuddhaCandidateSet.empty();
        }
        AABB searchArea = owner.getBoundingBox().inflate(PalmBuddhaSpell.MAXIMUM_TARGET_DISTANCE);
        List<PalmBuddhaCandidateSnapshot> candidates = owner.serverLevel()
                .getEntitiesOfClass(LivingEntity.class, searchArea, target -> {
                    if (target == owner
                            || !target.isAlive()
                            || target.isPassenger()
                            || !target.getPassengers().isEmpty()
                            || !target.getType().canSerialize()
                            || this.isAnyMember(target.getUUID())
                            || target.level() != owner.level()
                            || owner.distanceToSqr(target) > PalmBuddhaSpell.MAXIMUM_TARGET_DISTANCE_SQUARED
                            || !owner.hasLineOfSight(target)) {
                        return false;
                    }
                    return PalmBuddhaCultivationUtility.canAffect(owner, target);
                })
                .stream()
                .map(target -> PalmBuddhaCandidateSnapshot.from(
                        target,
                        Math.sqrt(owner.distanceToSqr(target))
                ))
                .sorted(Comparator.comparingDouble(candidate -> candidate.distance())
                        .thenComparing(candidate -> candidate.memberId().toString()))
                .limit(PalmBuddhaMemberPacketLimits.MAXIMUM_CANDIDATE_COUNT)
                .toList();
        return new PalmBuddhaCandidateSet(candidates);
    }
    public synchronized boolean isMember(UUID memberId) {
        return this.thisMemberState.contains(memberId);
    }

    public synchronized boolean isEntityMember(UUID memberId) {
        return this.thisEntityState.contains(memberId);
    }

    public synchronized Optional<ResourceLocation> getMemberRealmId(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        Optional<UUID> playerOwnerId = this.thisMemberState.get(memberId)
                .map(PalmBuddhaMemberState.MemberRecord::ownerId);
        Optional<UUID> entityOwnerId = this.thisEntityState.get(memberId)
                .map(PalmBuddhaEntityState.EntityRecord::ownerId);
        return playerOwnerId.or(() -> entityOwnerId)
                .flatMap(this.thisRealmState::getRealm)
                .map(RealmDefinition::getId);
    }

    public synchronized boolean isMemberInExpectedRealm(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return this.getMemberRealmId(player.getUUID())
                .map(realmId -> realmId.equals(player.serverLevel().dimension().location()))
                .orElse(false);
    }

    public synchronized boolean canMemberTravelTo(
            ServerPlayer player,
            ResourceKey<Level> destinationKey
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(destinationKey, "destinationKey");
        Optional<ResourceLocation> expectedRealmId = this.getMemberRealmId(player.getUUID());
        if (this.thisMemberState.contains(player.getUUID())) {
            if (this.thisAuthorizedReleasePlayerIds.contains(player.getUUID())) {
                return true;
            }
            return expectedRealmId.map(destinationKey.location()::equals).orElse(false);
        }

        Optional<UUID> realmOwnerId = this.getOwnerId(destinationKey);
        return realmOwnerId.isEmpty() || realmOwnerId.get().equals(player.getUUID());
    }

    public synchronized boolean canEntityTravelTo(
            Entity entity,
            ResourceKey<Level> destinationKey
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(destinationKey, "destinationKey");
        if (entity instanceof Player) {
            return true;
        }
        if (this.thisAuthorizedReleaseEntityIds.contains(entity.getUUID())) {
            return true;
        }
        if (this.thisEntityState.contains(entity.getUUID())) {
            PalmBuddhaEntityState.EntityRecord entityRecord = this.thisEntityState
                    .get(entity.getUUID())
                    .orElse(null);
            if (entityRecord == null) {
                return false;
            }
            return this.thisRealmState.getRealm(entityRecord.ownerId())
                    .map(realm -> realm.getId().equals(destinationKey.location()))
                    .orElse(false);
        }
        return !this.isPalmBuddhaRealm(destinationKey);
    }

    public synchronized boolean isReleaseAuthorized(UUID memberId) {
        return this.thisAuthorizedReleasePlayerIds.contains(memberId)
                || this.thisAuthorizedReleaseEntityIds.contains(memberId);
    }

    /**
     * Drops a persisted creature member only when Minecraft reports a terminal
     * removal. Chunk unloads and dimension changes intentionally keep the record
     * so the same real entity can be found again after it is loaded or moved.
     */
    public synchronized void handleEntityRemoval(
            Entity entity,
            Entity.RemovalReason removalReason
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(removalReason, "removalReason");
        if (removalReason != Entity.RemovalReason.KILLED
                && removalReason != Entity.RemovalReason.DISCARDED) {
            return;
        }
        if (this.thisEntityState.contains(entity.getUUID())
                && !this.thisEntityState.remove(entity.getUUID())) {
            ModMain.LOGGER.error(
                    "Unable to remove terminal palm-buddha entity state for {}",
                    entity.getUUID()
            );
        }
    }

    /**
     * Reconciles a logged-in or respawned member with the real dimension owned by
     * the recorded caster.  The player is never shown a fake camera page: a
     * normal server-side dimension change performs the complete transition.
     */
    public synchronized boolean restoreMemberToRealm(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PalmBuddhaMemberState.MemberEntry memberEntry = this.thisMemberState
                .get(player.getUUID())
                .map(memberRecord -> new PalmBuddhaMemberState.MemberEntry(player.getUUID(), memberRecord))
                .orElse(null);
        if (memberEntry == null) {
            return true;
        }

        RealmDefinition realmDefinition = this.thisRealmState
                .getRealm(memberEntry.memberRecord().ownerId())
                .orElse(null);
        if (realmDefinition == null) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha member {}: missing realm for owner {}; recovering to the saved world",
                    player.getGameProfile().getName(),
                    memberEntry.memberRecord().ownerId()
            );
            return this.recoverMemberToSavedWorld(player, memberEntry);
        }

        if (realmDefinition.getId().equals(player.serverLevel().dimension().location())) {
            return true;
        }

        try {
            ServerLevel realmLevel = this.getOrLoadRealmByDefinition(realmDefinition);
            Vec3 spawnPosition = RealmSpawnUtility.findSafeSpawnPosition(
                    realmLevel,
                    new Random(),
                    16
            );
            return this.moveMemberToRealm(player, realmLevel, spawnPosition);
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha member {} to {}",
                    player.getGameProfile().getName(),
                    realmDefinition.getId(),
                    exception
            );
            return false;
        }
    }

    /**
     * Moves a member to its owner's realm while preserving the member record.
     * Used by login/respawn lifecycle mixins and therefore intentionally has no
     * Forge event dependency.
     */
    public synchronized boolean moveMemberToRealm(
            ServerPlayer player,
            ServerLevel realmLevel,
            Vec3 destination
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(realmLevel, "realmLevel");
        Objects.requireNonNull(destination, "destination");
        if (!this.isMember(player.getUUID())) {
            return false;
        }
        if (realmLevel.dimension().location().equals(player.serverLevel().dimension().location())) {
            player.teleportTo(
                    realmLevel,
                    destination.x,
                    destination.y,
                    destination.z,
                    player.getYRot(),
                    player.getXRot()
            );
            return true;
        }
        return player.changeDimension(
                realmLevel,
                ThreeThousandRealmManager.createManagedRealmTeleporter(
                        destination,
                        player.getYRot(),
                        player.getXRot()
                )
        ) != null;
    }

    /**
     * 将玩家收入 owner 的佛国，并保存其离开前的位置。
     */
    public synchronized boolean imprisonPlayer(ServerPlayer owner, ServerPlayer target) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        if (owner == target || this.thisMemberState.contains(target.getUUID())) {
            return false;
        }

        ServerLevel realmLevel;
        Vec3 spawnPosition;
        try {
            realmLevel = this.getOrLoadRealm(owner);
            spawnPosition = RealmSpawnUtility.findSafeSpawnPosition(
                    realmLevel,
                    new Random(),
                    16
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to prepare palm-buddha realm for owner {}",
                    owner.getGameProfile().getName(),
                    exception
            );
            return false;
        }
        PalmBuddhaMemberState.MemberRecord memberRecord = new PalmBuddhaMemberState.MemberRecord(
                owner.getUUID(),
                target.serverLevel().dimension().location(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot()
        );
        if (!this.thisMemberState.put(target.getUUID(), memberRecord)) {
            return false;
        }

        Entity movedEntity;
        try {
            movedEntity = target.changeDimension(
                    realmLevel,
                    ThreeThousandRealmManager.createManagedRealmTeleporter(
                            spawnPosition,
                            target.getYRot(),
                            target.getXRot()
                    )
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to move palm-buddha member {} into the realm",
                    target.getGameProfile().getName(),
                    exception
            );
            movedEntity = null;
        }
        if (movedEntity == null) {
            if (!this.thisMemberState.remove(target.getUUID())) {
                ModMain.LOGGER.error(
                        "Unable to roll back palm-buddha member record for {} after a failed move",
                        target.getGameProfile().getName()
                );
            }
            return false;
        }
        return true;
    }

    /**
     * 将一个普通生灵作为真实实体迁入 owner 的佛国，不创建客户端副本。
     */
    public synchronized boolean imprisonEntity(ServerPlayer owner, LivingEntity target) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        if (target instanceof Player
                || !target.isAlive()
                || !target.getType().canSerialize()
                || target.isPassenger()
                || !target.getPassengers().isEmpty()
                || this.isAnyMember(target.getUUID())
                || !(target.level() instanceof ServerLevel targetLevel)) {
            return false;
        }

        ServerLevel realmLevel;
        Vec3 spawnPosition;
        try {
            realmLevel = this.getOrLoadRealm(owner);
            spawnPosition = RealmSpawnUtility.findSafeSpawnPosition(
                    realmLevel,
                    new Random(),
                    16
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to prepare palm-buddha realm for entity owner {}",
                    owner.getGameProfile().getName(),
                    exception
            );
            return false;
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityType == null) {
            return false;
        }
        String nbt;
        try {
            CompoundTag tag = target.saveWithoutId(new CompoundTag());
            tag.putString("id", entityType.toString());
            nbt = NbtUtils.structureToSnbt(tag);
        } catch (RuntimeException exception) {
            ModMain.LOGGER.warn(
                    "Unable to serialize palm-buddha entity {}",
                    target.getUUID(),
                    exception
            );
            return false;
        }
        if (nbt.length() > PalmBuddhaEntityState.MAXIMUM_NBT_STRING_LENGTH) {
            return false;
        }

        PalmBuddhaEntityState.EntityRecord entityRecord = new PalmBuddhaEntityState.EntityRecord(
                owner.getUUID(),
                entityType,
                targetLevel.dimension().location(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot(),
                spawnPosition.x,
                spawnPosition.y,
                spawnPosition.z,
                target.getYRot(),
                target.getXRot(),
                nbt
        );
        if (!this.thisEntityState.put(target.getUUID(), entityRecord)) {
            return false;
        }

        Entity movedEntity = null;
        try {
            movedEntity = target.changeDimension(
                    realmLevel,
                    ThreeThousandRealmManager.createManagedRealmTeleporter(
                            spawnPosition,
                            target.getYRot(),
                            target.getXRot()
                    )
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to move palm-buddha entity {} into the realm",
                    target.getUUID(),
                    exception
            );
        }
        if (movedEntity == null) {
            if (!this.thisEntityState.remove(target.getUUID())) {
                ModMain.LOGGER.error(
                        "Unable to roll back palm-buddha entity record for {} after a failed move",
                        target.getUUID()
                );
            }
            return false;
        }
        return true;
    }

    /**
     * 将被收入佛国的玩家送回其进入前的维度与位置。
     */
    public synchronized boolean releasePlayer(ServerPlayer target) {
        return this.releasePlayerWithResult(target).successful();
    }

    /**
     * 将被收入佛国的玩家送回其进入前的维度与位置，并返回可供施法者显示的轻量结果。
     */
    public synchronized PalmBuddhaReleaseResult releasePlayerWithResult(ServerPlayer target) {
        Objects.requireNonNull(target, "target");
        PalmBuddhaMemberState.MemberRecord memberRecord = this.thisMemberState
                .get(target.getUUID())
                .orElse(null);
        if (memberRecord == null) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_NOT_OWNED
            );
        }

        ResourceKey<Level> destinationKey = ResourceKey.create(
                Registries.DIMENSION,
                memberRecord.dimension()
        );
        ServerLevel destinationLevel = this.resolveSavedDestinationLevel(destinationKey);
        Vec3 destinationPosition = new Vec3(memberRecord.x(), memberRecord.y(), memberRecord.z());
        if (destinationLevel == null) {
            ModMain.LOGGER.warn(
                    "Original dimension {} for palm-buddha member {} is unavailable; using the overworld safe spawn",
                    memberRecord.dimension(),
                    target.getGameProfile().getName()
            );
            destinationLevel = this.thisServer.overworld();
            try {
                destinationPosition = RealmSpawnUtility.findSafeSpawnPosition(
                        destinationLevel,
                        new Random(),
                        16
                );
            } catch (RuntimeException exception) {
                ModMain.LOGGER.error(
                        "Unable to find an overworld recovery position for palm-buddha member {}",
                        target.getGameProfile().getName(),
                        exception
                );
                return PalmBuddhaReleaseResult.failure(
                        PalmBuddhaReleaseResult.FailureReason.RECOVERY_DESTINATION_UNAVAILABLE
                );
            }
        }

        // Remove the authoritative record before moving. A failed move restores it,
        // preventing a successful move followed by a persistence failure from
        // leaving the member outside while the server still considers them trapped.
        if (!this.thisMemberState.remove(target.getUUID())) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.STATE_UPDATE_FAILED
            );
        }

        Entity movedEntity = null;
        try {
            this.thisAuthorizedReleasePlayerIds.add(target.getUUID());
            movedEntity = this.movePlayer(
                    target,
                    destinationLevel,
                    destinationPosition,
                    memberRecord.yRot(),
                    memberRecord.xRot()
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to release palm-buddha member {}",
                    target.getGameProfile().getName(),
                    exception
            );
        } finally {
            this.thisAuthorizedReleasePlayerIds.remove(target.getUUID());
        }
        if (movedEntity != null) {
            return PalmBuddhaReleaseResult.success();
        }

        if (!this.thisMemberState.put(target.getUUID(), memberRecord)) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha member record for {} after a failed release",
                    target.getGameProfile().getName()
            );
        }
        return PalmBuddhaReleaseResult.failure(
                PalmBuddhaReleaseResult.FailureReason.TRANSFER_FAILED
        );
    }

    private Entity movePlayer(
            ServerPlayer player,
            ServerLevel destinationLevel,
            Vec3 destination,
            float yRot,
            float xRot
    ) {
        if (player.serverLevel() == destinationLevel) {
            player.teleportTo(
                    destinationLevel,
                    destination.x,
                    destination.y,
                    destination.z,
                    yRot,
                    xRot
            );
            return player;
        }
        return player.changeDimension(
                destinationLevel,
                ThreeThousandRealmManager.createManagedRealmTeleporter(destination, yRot, xRot)
        );
    }

    /**
     * Resolves a saved dimension without assuming that every dynamic realm is
     * already loaded.  The normal server lookup remains the fast path; a
     * managed realm is loaded lazily through the existing dynamic-realm API.
     */
    private ServerLevel resolveSavedDestinationLevel(ResourceKey<Level> destinationKey) {
        ServerLevel destinationLevel = this.thisServer.getLevel(destinationKey);
        if (destinationLevel != null) {
            return destinationLevel;
        }

        ThreeThousandRealmManager realmManager = ThreeThousandRealmManager.get(this.thisServer);
        if (realmManager.isManagedRealm(destinationKey)) {
            try {
                return realmManager.getOrLoadManagedRealm(destinationKey.location());
            } catch (RuntimeException exception) {
                ModMain.LOGGER.error(
                        "Unable to lazily load saved managed dimension {} for a palm-buddha member",
                        destinationKey.location(),
                        exception
                );
            }
        }

        // A persisted Palm Buddha definition may not have reached the dynamic
        // manager yet during an unusual lifecycle ordering. Register it here
        // before giving up, while still keeping the dynamic API authoritative.
        Optional<UUID> palmBuddhaOwnerId = this.thisRealmState.getOwner(destinationKey.location());
        if (palmBuddhaOwnerId.isPresent()) {
            Optional<RealmDefinition> palmBuddhaRealm = this.thisRealmState.getRealm(palmBuddhaOwnerId.get());
            if (palmBuddhaRealm.isEmpty()) {
                return null;
            }
            try {
                return this.getOrLoadRealmByDefinition(palmBuddhaRealm.get());
            } catch (RuntimeException exception) {
                ModMain.LOGGER.error(
                        "Unable to lazily load saved palm-buddha dimension {}",
                        destinationKey.location(),
                        exception
                );
            }
        }
        return null;
    }

    private boolean recoverMemberToSavedWorld(
            ServerPlayer player,
            PalmBuddhaMemberState.MemberEntry memberEntry
    ) {
        PalmBuddhaMemberState.MemberRecord memberRecord = memberEntry.memberRecord();
        ResourceKey<Level> destinationKey = ResourceKey.create(
                Registries.DIMENSION,
                memberRecord.dimension()
        );
        ServerLevel destinationLevel = this.resolveSavedDestinationLevel(destinationKey);
        Vec3 destination = new Vec3(memberRecord.x(), memberRecord.y(), memberRecord.z());
        if (destinationLevel == null) {
            ModMain.LOGGER.warn(
                    "Saved dimension {} for palm-buddha member {} is unavailable; recovering to the overworld safe spawn",
                    memberRecord.dimension(),
                    player.getGameProfile().getName()
            );
            destinationLevel = this.thisServer.overworld();
            destination = RealmSpawnUtility.findSafeSpawnPosition(
                    destinationLevel,
                    new Random(),
                    16
            );
        }

        if (!this.thisMemberState.remove(player.getUUID())) {
            return false;
        }

        Entity movedEntity = null;
        try {
            this.thisAuthorizedReleasePlayerIds.add(player.getUUID());
            movedEntity = this.movePlayer(
                    player,
                    destinationLevel,
                    destination,
                    memberRecord.yRot(),
                    memberRecord.xRot()
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to recover palm-buddha member {} to the saved world",
                    player.getGameProfile().getName(),
                    exception
            );
        } finally {
            this.thisAuthorizedReleasePlayerIds.remove(player.getUUID());
        }
        if (movedEntity != null) {
            return true;
        }
        if (!this.thisMemberState.put(player.getUUID(), memberRecord)) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha member record for {} after a failed recovery",
                    player.getGameProfile().getName()
            );
        }
        return false;
    }

    public synchronized boolean releasePlayer(ServerPlayer owner, UUID memberId) {
        return this.releasePlayerWithResult(owner, memberId).successful();
    }

    /**
     * 只允许施法者释放自己佛国中的在线成员；所有权与成员所在维度均由服务端复核。
     */
    public synchronized PalmBuddhaReleaseResult releasePlayerWithResult(
            ServerPlayer owner,
            UUID memberId
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(memberId, "memberId");
        PalmBuddhaMemberState.MemberRecord memberRecord = this.thisMemberState.get(memberId).orElse(null);
        if (memberRecord == null || !memberRecord.ownerId().equals(owner.getUUID())) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_NOT_OWNED
            );
        }
        ServerPlayer target = this.thisServer.getPlayerList().getPlayer(memberId);
        if (target == null) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_OFFLINE
            );
        }
        if (!this.isMemberInExpectedRealm(target)) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_NOT_IN_EXPECTED_REALM
            );
        }
        return this.releasePlayerWithResult(target);
    }

    public synchronized PalmBuddhaReleaseResult releaseMemberWithResult(
            ServerPlayer owner,
            UUID memberId
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(memberId, "memberId");
        if (this.thisMemberState.contains(memberId)) {
            return this.releasePlayerWithResult(owner, memberId);
        }

        PalmBuddhaEntityState.EntityRecord entityRecord = this.thisEntityState.get(memberId).orElse(null);
        if (entityRecord == null || !entityRecord.ownerId().equals(owner.getUUID())) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_NOT_OWNED
            );
        }
        return this.releaseEntityWithResult(owner, memberId, entityRecord);
    }

    private PalmBuddhaReleaseResult releaseEntityWithResult(
            ServerPlayer owner,
            UUID memberId,
            PalmBuddhaEntityState.EntityRecord entityRecord
    ) {
        Entity entity = this.getLoadedMemberEntity(memberId).orElse(null);
        if (entity == null || !this.isEntityMemberInExpectedRealm(entity)) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.MEMBER_NOT_IN_EXPECTED_REALM
            );
        }
        if (!entity.isAlive()) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.TRANSFER_FAILED
            );
        }

        ResourceKey<Level> destinationKey = ResourceKey.create(
                Registries.DIMENSION,
                entityRecord.originalDimension()
        );
        ServerLevel destinationLevel = this.resolveSavedDestinationLevel(destinationKey);
        Vec3 destinationPosition = new Vec3(
                entityRecord.originalX(),
                entityRecord.originalY(),
                entityRecord.originalZ()
        );
        if (destinationLevel == null) {
            destinationLevel = this.thisServer.overworld();
            try {
                destinationPosition = RealmSpawnUtility.findSafeSpawnPosition(
                        destinationLevel,
                        new Random(),
                        16
                );
            } catch (RuntimeException exception) {
                ModMain.LOGGER.error(
                        "Unable to find an overworld recovery position for palm-buddha entity {}",
                        memberId,
                        exception
                );
                return PalmBuddhaReleaseResult.failure(
                        PalmBuddhaReleaseResult.FailureReason.RECOVERY_DESTINATION_UNAVAILABLE
                );
            }
        }

        if (!this.thisEntityState.remove(memberId)) {
            return PalmBuddhaReleaseResult.failure(
                    PalmBuddhaReleaseResult.FailureReason.STATE_UPDATE_FAILED
            );
        }

        Entity movedEntity = null;
        try {
            this.thisAuthorizedReleaseEntityIds.add(memberId);
            movedEntity = entity.changeDimension(
                    destinationLevel,
                    ThreeThousandRealmManager.createManagedRealmTeleporter(
                            destinationPosition,
                            entityRecord.originalYRot(),
                            entityRecord.originalXRot()
                    )
            );
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to release palm-buddha entity {} for owner {}",
                    memberId,
                    owner.getGameProfile().getName(),
                    exception
            );
        } finally {
            this.thisAuthorizedReleaseEntityIds.remove(memberId);
        }
        if (movedEntity != null) {
            return PalmBuddhaReleaseResult.success();
        }

        if (!this.thisEntityState.put(memberId, entityRecord)) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha entity record for {} after a failed release",
                    memberId
            );
        }
        return PalmBuddhaReleaseResult.failure(
                PalmBuddhaReleaseResult.FailureReason.TRANSFER_FAILED
        );
    }

    /**
     * Periodically reconciles the bounded entity state with the real loaded
     * entities. It deliberately does not force-load every Palm Buddha realm or
     * write JSON once per entity on every server tick.
     */
    public synchronized void tickEntityMembers() {
        this.thisEntityStateTick++;
        if (this.thisEntityStateTick % ENTITY_STATE_UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        Map<UUID, ServerLevel> loadedRealms = new HashMap<>();
        Map<UUID, PalmBuddhaEntityState.EntityRecord> updatedRecords = new LinkedHashMap<>();
        Set<UUID> removedMemberIds = new HashSet<>();
        for (PalmBuddhaEntityState.EntityEntry entityEntry : this.thisEntityState.getAll()) {
            UUID memberId = entityEntry.memberId();
            PalmBuddhaEntityState.EntityRecord record = entityEntry.entityRecord();
            RealmDefinition realmDefinition = this.thisRealmState.getRealm(record.ownerId()).orElse(null);
            if (realmDefinition == null) {
                continue;
            }
            ServerLevel realmLevel = loadedRealms.computeIfAbsent(
                    record.ownerId(),
                    ignored -> this.getLoadedRealmForEntityState(realmDefinition)
            );
            if (realmLevel == null) {
                // A realm that is not currently loaded is intentionally left
                // untouched; loading it here would turn this reconciliation pass
                // into a hidden world-loading loop.
                continue;
            }

            Entity entity = realmLevel.getEntity(memberId);
            if (entity == null) {
                // A missing entity in an unloaded chunk is not evidence of death;
                // do not force-load arbitrary chunks on every reconciliation pass.
                int chunkX = ((int) Math.floor(record.currentX())) >> 4;
                int chunkZ = ((int) Math.floor(record.currentZ())) >> 4;
                if (!realmLevel.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                Entity restoredEntity = this.restoreEntityFromRecord(realmLevel, memberId, record);
                if (restoredEntity == null) {
                    ModMain.LOGGER.warn(
                            "Removing missing palm-buddha entity {} from a loaded chunk",
                            memberId
                    );
                    removedMemberIds.add(memberId);
                }
                continue;
            }
            if (!entity.isAlive() || entity.isRemoved()) {
                removedMemberIds.add(memberId);
                continue;
            }

            String nbt = this.serializeEntity(entity, memberId);
            PalmBuddhaEntityState.EntityRecord updatedRecord = nbt != null
                    && nbt.length() <= PalmBuddhaEntityState.MAXIMUM_NBT_STRING_LENGTH
                    ? record.withCurrentState(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            entity.getYRot(),
                            entity.getXRot(),
                            nbt
                    )
                    : record.withCurrentState(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            entity.getYRot(),
                            entity.getXRot()
                    );
            if (!updatedRecord.equals(record)) {
                updatedRecords.put(memberId, updatedRecord);
            }
        }
        if (!this.thisEntityState.applyReconciliation(updatedRecords, removedMemberIds)) {
            ModMain.LOGGER.error("Unable to persist a palm-buddha entity reconciliation pass");
        }
    }

    private ServerLevel getLoadedRealmForEntityState(RealmDefinition realmDefinition) {
        ResourceKey<Level> realmKey = ResourceKey.create(Registries.DIMENSION, realmDefinition.getId());
        return this.thisServer.getLevel(realmKey);
    }

    private Entity restoreEntityFromRecord(
            ServerLevel realmLevel,
            UUID memberId,
            PalmBuddhaEntityState.EntityRecord record
    ) {
        try {
            CompoundTag tag = NbtUtils.snbtToStructure(record.nbt());
            tag.putString("id", record.entityType().toString());
            tag.putUUID("UUID", memberId);
            Entity entity = EntityType.create(tag, realmLevel).orElse(null);
            if (entity == null) {
                return null;
            }
            entity.setUUID(memberId);
            entity.moveTo(
                    record.currentX(),
                    record.currentY(),
                    record.currentZ(),
                    record.currentYRot(),
                    record.currentXRot()
            );
            if (!realmLevel.addFreshEntity(entity)) {
                return null;
            }
            return entity;
        } catch (Exception exception) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha entity {} from persisted NBT",
                    memberId,
                    exception
            );
            return null;
        }
    }

    private String serializeEntity(Entity entity, UUID memberId) {
        try {
            ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityType == null) {
                return null;
            }
            CompoundTag tag = entity.saveWithoutId(new CompoundTag());
            tag.putString("id", entityType.toString());
            return NbtUtils.structureToSnbt(tag);
        } catch (RuntimeException exception) {
            ModMain.LOGGER.warn("Unable to update persisted NBT for palm-buddha entity {}", memberId, exception);
            return null;
        }
    }

    private synchronized void registerPersistedRealmDefinitions() {
        for (RealmDefinition realmDefinition : this.thisRealmState.getRealms()) {
            this.registerWithDynamicRealmApi(realmDefinition);
        }
    }

    private void registerWithDynamicRealmApi(RealmDefinition realmDefinition) {
        ThreeThousandRealmManager.get(this.thisServer)
                .registerManagedRealmDefinition(realmDefinition);
    }

    private static Path getStateFilePath(MinecraftServer server, String fileName) {
        return server.getWorldPath(new LevelResource("data"))
                .resolve(STATE_DIRECTORY_NAME)
                .resolve(fileName);
    }
}

