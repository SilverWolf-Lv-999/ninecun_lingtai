package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.RealmDefinition;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.utility.RealmSpawnUtility;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 掌中佛国的服务端业务门面。
 *
 * <p>本阶段接入玩家准心单选：施法成功后，目标被迁入施法者独有的动态佛国，
 * 并持久化目标的原维度与坐标。范围多选、灵兽实体和 UI 留给后续切片。</p>
 */
public final class PalmBuddhaRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String REALM_STATE_FILE_NAME = "palm_buddha_realms.json";
    private static final String MEMBER_STATE_FILE_NAME = "palm_buddha_members.json";
    private static final Map<MinecraftServer, PalmBuddhaRealmManager> MANAGERS = new WeakHashMap<>();

    private final MinecraftServer thisServer;
    private final PalmBuddhaRealmState thisRealmState;
    private final PalmBuddhaMemberState thisMemberState;
    private final Set<UUID> thisAuthorizedReleasePlayerIds = new HashSet<>();

    private PalmBuddhaRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisRealmState = PalmBuddhaRealmState.load(getStateFilePath(server, REALM_STATE_FILE_NAME));
        this.thisMemberState = PalmBuddhaMemberState.load(getStateFilePath(server, MEMBER_STATE_FILE_NAME));
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
        List<PalmBuddhaMemberState.MemberEntry> allMembers = this.getMembers(owner.getUUID()).stream()
                .sorted(Comparator.comparing(entry -> entry.memberId().toString()))
                .toList();
        int totalMemberCount = Math.min(
                allMembers.size(),
                PalmBuddhaMemberPacketLimits.MAXIMUM_TOTAL_MEMBER_COUNT
        );
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
        List<PalmBuddhaMemberSnapshot> snapshots = allMembers.subList(fromIndex, toIndex).stream()
                .map(entry -> PalmBuddhaMemberSnapshot.from(this.thisServer, this, entry))
                .toList();
        return new PalmBuddhaMemberPageSnapshot(
                pageIndex,
                pageCount,
                totalMemberCount,
                snapshots
        );
    }

    public synchronized boolean isMember(UUID memberId) {
        return this.thisMemberState.contains(memberId);
    }

    public synchronized Optional<ResourceLocation> getMemberRealmId(UUID memberId) {
        return this.thisMemberState.get(memberId)
                .flatMap(member -> this.thisRealmState.getRealm(member.ownerId()))
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

    public synchronized boolean isReleaseAuthorized(UUID memberId) {
        return this.thisAuthorizedReleasePlayerIds.contains(memberId);
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
     * 将被收入佛国的玩家送回其进入前的维度与位置。
     */
    public synchronized boolean releasePlayer(ServerPlayer target) {
        Objects.requireNonNull(target, "target");
        PalmBuddhaMemberState.MemberRecord memberRecord = this.thisMemberState
                .get(target.getUUID())
                .orElse(null);
        if (memberRecord == null) {
            return false;
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
                return false;
            }
        }

        // Remove the authoritative record before moving. A failed move restores it,
        // preventing a successful move followed by a persistence failure from
        // leaving the member outside while the server still considers them trapped.
        if (!this.thisMemberState.remove(target.getUUID())) {
            return false;
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
            return true;
        }

        if (!this.thisMemberState.put(target.getUUID(), memberRecord)) {
            ModMain.LOGGER.error(
                    "Unable to restore palm-buddha member record for {} after a failed release",
                    target.getGameProfile().getName()
            );
        }
        return false;
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
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(memberId, "memberId");
        PalmBuddhaMemberState.MemberRecord memberRecord = this.thisMemberState.get(memberId).orElse(null);
        if (memberRecord == null || !memberRecord.ownerId().equals(owner.getUUID())) {
            return false;
        }
        ServerPlayer target = this.thisServer.getPlayerList().getPlayer(memberId);
        if (target == null || !this.isMemberInExpectedRealm(target)) {
            return false;
        }
        return this.releasePlayer(target);
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
