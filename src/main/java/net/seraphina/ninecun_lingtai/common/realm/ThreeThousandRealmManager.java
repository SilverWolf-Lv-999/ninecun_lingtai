package net.seraphina.ninecun_lingtai.common.realm;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerThreeThousandRealmReturnPosition;
import net.seraphina.ninecun_lingtai.common.player.PlayerThreeThousandRealmReturnState;
import net.seraphina.ninecun_lingtai.utility.RealmSpawnUtility;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

public final class ThreeThousandRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String STATE_FILE_NAME = "three_thousand_realms.json";
    private static final String PLAYER_RETURN_STATE_FILE_NAME =
            "three_thousand_realm_player_returns.json";
    private static final Map<MinecraftServer, ThreeThousandRealmManager> MANAGERS = new WeakHashMap<>();

    private final MinecraftServer thisServer;
    private final ThreeThousandRealmState thisState;
    private final PlayerThreeThousandRealmReturnState thisPlayerReturnState;
    private final ThreeThousandRealmDataPackGenerator thisDataPackGenerator;
    /**
     * Definitions owned by other dynamic-realm features.  They share the existing
     * registry/datapack/lazy-loader bridge without becoming part of the
     * three-thousand-realm catalogue.
     */
    private final Map<ResourceLocation, RealmDefinition> thisExternalRealmDefinitions =
            new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, Long> thisEmptyRealmTicks = new HashMap<>();

    private ThreeThousandRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisState = ThreeThousandRealmState.load(getStateFilePath(server));
        this.thisPlayerReturnState = PlayerThreeThousandRealmReturnState.load(
                getPlayerReturnStateFilePath(server)
        );
        this.thisDataPackGenerator = new ThreeThousandRealmDataPackGenerator(
                server.getWorldPath(LevelResource.DATAPACK_DIR)
        );
    }

    public static void initialize(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (MANAGERS) {
            MANAGERS.computeIfAbsent(server, ThreeThousandRealmManager::new);
        }
    }

    public static ThreeThousandRealmManager get(MinecraftServer server) {
        initialize(server);
        synchronized (MANAGERS) {
            return MANAGERS.get(server);
        }
    }

    public static void tick(MinecraftServer server) {
        get(server).tick();
    }

    public static void registerPersistedRealmDefinitions(MinecraftServer server) {
        get(server).registerPersistedRealmDefinitions();
    }

    public static void generateConfiguredRealmDefinitions(MinecraftServer server) {
        get(server).generateConfiguredRealmDefinitions();
    }

    public static RegistryAccess.Frozen createWorldSaveRegistryAccess(
            MinecraftServer server,
            RegistryAccess registryAccess
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(registryAccess, "registryAccess");
        synchronized (MANAGERS) {
            ThreeThousandRealmManager manager = MANAGERS.get(server);
            if (manager == null) {
                return freezeRegistryAccess(registryAccess);
            }
            return manager.createWorldSaveRegistryAccess(registryAccess);
        }
    }

    public static Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> excludeManagedRealmStemsFromInitialLoad(
            MinecraftServer server,
            Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> levelStemEntries
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(levelStemEntries, "levelStemEntries");
        return get(server).excludeManagedRealmStemsFromInitialLoad(levelStemEntries);
    }

    public static void clear(MinecraftServer server) {
        synchronized (MANAGERS) {
            MANAGERS.remove(server);
        }
    }

    public synchronized ServerLevel getOrCreateRandomRealm(Random random) {
        Objects.requireNonNull(random, "random");
        List<RealmDefinition> realms = this.thisState.getRealms();
        if (realms.isEmpty()) {
            return this.createAndLoadRealm(random);
        }
        return this.getOrLoadRealm(realms.get(random.nextInt(realms.size())).getId());
    }

    public synchronized ServerLevel createAndLoadRealm(Random random) {
        Objects.requireNonNull(random, "random");
        RealmDefinition realmDefinition = this.thisState.createRealm(
                random,
                CommonConfig.THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT.get()
        );
        return this.getOrLoadRealm(realmDefinition.getId());
    }

    public synchronized ServerLevel getOrLoadRealm(ResourceLocation realmId) {
        Objects.requireNonNull(realmId, "realmId");
        RealmDefinition realmDefinition = this.thisState.getRealm(realmId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown three-thousand realm: " + realmId));
        return this.loadManagedRealm(realmDefinition, "three-thousand");
    }

    /**
     * Registers a definition owned by another feature on the existing dynamic
     * dimension bridge.  The definition is deliberately not added to the
     * three-thousand-realm catalogue.
     */
    public synchronized void registerManagedRealmDefinition(RealmDefinition realmDefinition) {
        Objects.requireNonNull(realmDefinition, "realmDefinition");
        ResourceLocation realmId = realmDefinition.getId();
        Optional<RealmDefinition> threeThousandRealm = this.thisState.getRealm(realmId);
        if (threeThousandRealm.isPresent()
                && (threeThousandRealm.get().getSeed() != realmDefinition.getSeed()
                || !threeThousandRealm.get().getDisplayName().equals(realmDefinition.getDisplayName()))) {
            throw new IllegalArgumentException("Dynamic realm id is already owned by another definition: " + realmId);
        }

        RealmDefinition existingDefinition = this.thisExternalRealmDefinitions.putIfAbsent(
                realmId,
                realmDefinition
        );
        if (existingDefinition != null
                && (existingDefinition.getSeed() != realmDefinition.getSeed()
                || !existingDefinition.getDisplayName().equals(realmDefinition.getDisplayName()))) {
            throw new IllegalArgumentException("Dynamic realm id is already registered: " + realmId);
        }
        this.registerRealmDefinition(realmDefinition);
    }

    /**
     * Registers and lazily loads an externally-owned dynamic realm.
     */
    public synchronized ServerLevel getOrLoadManagedRealm(RealmDefinition realmDefinition) {
        this.registerManagedRealmDefinition(realmDefinition);
        return this.loadManagedRealm(realmDefinition, "managed");
    }

    /**
     * Loads a previously registered dynamic realm by id.
     */
    public synchronized ServerLevel getOrLoadManagedRealm(ResourceLocation realmId) {
        Objects.requireNonNull(realmId, "realmId");
        RealmDefinition realmDefinition = this.thisState.getRealm(realmId)
                .or(() -> Optional.ofNullable(this.thisExternalRealmDefinitions.get(realmId)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown managed realm: " + realmId));
        return this.loadManagedRealm(realmDefinition, "managed");
    }

    public synchronized boolean isThreeThousandRealm(ResourceKey<Level> levelKey) {
        return this.getRealmDefinition(levelKey).isPresent();
    }

    public synchronized Optional<RealmDefinition> getRealmDefinition(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.thisState.getRealm(levelKey.location());
    }

    public synchronized boolean isManagedRealm(ResourceKey<Level> levelKey) {
        return this.getManagedRealmDefinition(levelKey).isPresent();
    }

    public synchronized Optional<RealmDefinition> getManagedRealmDefinition(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        Optional<RealmDefinition> threeThousandRealm = this.thisState.getRealm(levelKey.location());
        if (threeThousandRealm.isPresent()) {
            return threeThousandRealm;
        }
        return Optional.ofNullable(this.thisExternalRealmDefinitions.get(levelKey.location()));
    }

    private ServerLevel loadManagedRealm(RealmDefinition realmDefinition, String realmKind) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, realmDefinition.getId());
        Map<ResourceKey<Level>, ServerLevel> worlds = this.thisServer.forgeGetWorldMap();
        ServerLevel loadedRealm = worlds.get(levelKey);
        if (loadedRealm != null) {
            return loadedRealm;
        }

        this.registerRealmDefinition(realmDefinition);
        if (!(this.thisServer instanceof ThreeThousandRealmServerAccess serverAccess)) {
            throw new IllegalStateException("Dynamic realm lazy loading bridge is unavailable");
        }
        ServerLevel createdRealm = serverAccess.ninecunLingtai$loadRegisteredRealm(
                levelKey,
                realmDefinition.getSeed()
        );
        ModMain.LOGGER.info(
                "Loaded {} realm {} ({}) with seed {}",
                realmKind,
                realmDefinition.getDisplayName(),
                realmDefinition.getId(),
                realmDefinition.getSeed()
        );
        return createdRealm;
    }

    public synchronized boolean placePlayerInRandomRealm(ServerPlayer player, Random random) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(random, "random");

        ServerLevel realmLevel = this.getOrCreateRandomRealm(random);
        Vec3 spawnPosition = RealmSpawnUtility.findSafeSpawnPosition(
                realmLevel,
                random,
                CommonConfig.THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS.get()
        );
        if (player.serverLevel() == realmLevel) {
            player.teleportTo(
                    realmLevel,
                    spawnPosition.x,
                    spawnPosition.y,
                    spawnPosition.z,
                    player.getYRot(),
                    player.getXRot()
            );
            return true;
        }

        return player.changeDimension(
                realmLevel,
                createManagedRealmTeleporter(spawnPosition, player.getYRot(), player.getXRot())
        ) != null;
    }

    public void openRealmCatalog(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ThreeThousandRealmCatalogSnapshot snapshot;
        synchronized (this) {
            int targetRealmCount = Math.max(
                    CommonConfig.THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT.get(),
                    this.thisState.getRealmCount()
            );
            snapshot = createReadyCatalogSnapshot(
                    this.thisState.getRealms(),
                    targetRealmCount
            );
        }
        ModNetwork.sendOpenThreeThousandRealmScreen(player, snapshot);
    }

    public synchronized boolean placePlayerInRealm(
            ServerPlayer player,
            ResourceLocation realmId,
            Random random
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(random, "random");
        if (this.thisState.getRealm(realmId).isEmpty()) {
            return false;
        }

        ServerLevel realmLevel = this.getOrLoadRealm(realmId);
        Vec3 spawnPosition = RealmSpawnUtility.findSafeSpawnPosition(
                realmLevel,
                random,
                CommonConfig.THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS.get()
        );
        if (player.serverLevel() == realmLevel) {
            player.teleportTo(
                    realmLevel,
                    spawnPosition.x,
                    spawnPosition.y,
                    spawnPosition.z,
                    player.getYRot(),
                    player.getXRot()
            );
            return true;
        }

        return player.changeDimension(
                realmLevel,
                createManagedRealmTeleporter(spawnPosition, player.getYRot(), player.getXRot())
        ) != null;
    }

    public synchronized boolean savePlayerRealmReturnPosition(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ResourceKey<Level> realmLevelKey = player.serverLevel().dimension();
        if (!this.isThreeThousandRealm(realmLevelKey)) {
            return false;
        }

        boolean saved = this.thisPlayerReturnState.putReturnPosition(
                player.getUUID(),
                new PlayerThreeThousandRealmReturnPosition(
                        realmLevelKey.location(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                )
        );
        if (!saved) {
            ModMain.LOGGER.error(
                    "Unable to persist three-thousand-realm return position for player {}",
                    player.getGameProfile().getName()
            );
        }
        return saved;
    }

    @Nullable
    public synchronized Entity returnPlayerToSavedRealm(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerThreeThousandRealmReturnPosition returnPosition = this.thisPlayerReturnState
                .getReturnPosition(player.getUUID())
                .orElse(null);
        if (returnPosition == null) {
            return null;
        }

        if (this.thisState.getRealm(returnPosition.getRealmId()).isEmpty()) {
            ModMain.LOGGER.warn(
                    "Discarding unavailable three-thousand-realm return position {} for player {}",
                    returnPosition.getRealmId(),
                    player.getGameProfile().getName()
            );
            this.thisPlayerReturnState.removeReturnPosition(player.getUUID());
            return null;
        }

        ServerLevel realmLevel = this.getOrLoadRealm(returnPosition.getRealmId());
        Entity returnedPlayer = player.changeDimension(
                realmLevel,
                createManagedRealmTeleporter(
                        new Vec3(
                                returnPosition.getX(),
                                returnPosition.getY(),
                                returnPosition.getZ()
                        ),
                        returnPosition.getYRot(),
                        returnPosition.getXRot()
                )
        );
        if (returnedPlayer != null && !this.thisPlayerReturnState.removeReturnPosition(player.getUUID())) {
            ModMain.LOGGER.error(
                    "Unable to clear three-thousand-realm return position for player {}",
                    player.getGameProfile().getName()
            );
        }
        return returnedPlayer;
    }

    public synchronized int getRealmCount() {
        return this.thisState.getRealmCount();
    }

    private static ThreeThousandRealmCatalogSnapshot createReadyCatalogSnapshot(
            List<RealmDefinition> realms,
            int targetRealmCount
    ) {
        List<ThreeThousandRealmCatalogEntry> catalogEntries = createCatalogEntries(realms);
        return new ThreeThousandRealmCatalogSnapshot(
                ThreeThousandRealmCatalogStatus.READY,
                catalogEntries.size(),
                targetRealmCount,
                catalogEntries
        );
    }

    private static List<ThreeThousandRealmCatalogEntry> createCatalogEntries(
            List<RealmDefinition> realms
    ) {
        return realms.stream()
                .map(realm -> new ThreeThousandRealmCatalogEntry(
                        realm.getId(),
                        realm.getDisplayName()
                ))
                .toList();
    }

    private void tick() {
        int unloadDelayTicks = CommonConfig.THREE_THOUSAND_REALMS_UNLOAD_DELAY_TICKS.get();
        Map<ResourceKey<Level>, ServerLevel> worlds = this.thisServer.forgeGetWorldMap();
        Map<ResourceLocation, RealmDefinition> managedDefinitions = new LinkedHashMap<>();
        for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
            managedDefinitions.put(realmDefinition.getId(), realmDefinition);
        }
        managedDefinitions.putAll(this.thisExternalRealmDefinitions);
        for (RealmDefinition realmDefinition : managedDefinitions.values()) {
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, realmDefinition.getId());
            ServerLevel realmLevel = worlds.get(levelKey);
            if (realmLevel == null) {
                this.thisEmptyRealmTicks.remove(levelKey);
                continue;
            }

            if (!realmLevel.players().isEmpty() || !realmLevel.getForcedChunks().isEmpty()) {
                this.thisEmptyRealmTicks.remove(levelKey);
                continue;
            }

            long emptyTicks = this.thisEmptyRealmTicks.merge(levelKey, 1L, Long::sum);
            if (emptyTicks >= unloadDelayTicks) {
                this.unloadRealm(levelKey, realmLevel);
            }
        }
    }

    private synchronized void generateConfiguredRealmDefinitions() {
        int configuredRealmCount = CommonConfig.THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT.get();
        int targetRealmCount = Math.max(configuredRealmCount, this.thisState.getRealmCount());
        this.thisState.createMissingRealms(new Random(), targetRealmCount);
        try {
            for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
                this.thisDataPackGenerator.ensureRealmDefinition(realmDefinition);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate configured three-thousand-realm data definitions",
                    exception
            );
        }
    }

    private void unloadRealm(ResourceKey<Level> levelKey, ServerLevel realmLevel) {
        if (!realmLevel.players().isEmpty() || !realmLevel.getForcedChunks().isEmpty()) {
            this.thisEmptyRealmTicks.remove(levelKey);
            return;
        }

        try {
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(realmLevel));
            realmLevel.save(null, true, false);
            if (!this.thisServer.forgeGetWorldMap().remove(levelKey, realmLevel)) {
                return;
            }
            this.thisServer.markWorldsDirty();
            realmLevel.close();
            this.thisEmptyRealmTicks.remove(levelKey);
            ModMain.LOGGER.info("Unloaded empty three-thousand realm {}", levelKey.location());
        } catch (IOException | RuntimeException exception) {
            ModMain.LOGGER.error("Unable to unload three-thousand realm {}", levelKey.location(), exception);
        }
    }

    public static ITeleporter createManagedRealmTeleporter(Vec3 destination, float yRot, float xRot) {
        return new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(
                    Entity entity,
                    ServerLevel destinationLevel,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo
            ) {
                return new PortalInfo(destination, Vec3.ZERO, yRot, xRot);
            }

            @Override
            public Entity placeEntity(
                    Entity entity,
                    ServerLevel currentLevel,
                    ServerLevel destinationLevel,
                    float yaw,
                    Function<Boolean, Entity> repositionEntity
            ) {
                return repositionEntity.apply(false);
            }

            @Override
            public boolean playTeleportSound(
                    ServerPlayer player,
                    ServerLevel sourceLevel,
                    ServerLevel destinationLevel
            ) {
                return false;
            }
        };
    }

    private synchronized void registerPersistedRealmDefinitions() {
        for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
            if (!this.thisDataPackGenerator.hasRealmDefinition(realmDefinition)) {
                ModMain.LOGGER.warn(
                        "Deferring missing three-thousand-realm data definition {} until the Dao Scroll is opened",
                        realmDefinition.getId()
                );
                continue;
            }
            this.registerRealmDefinition(realmDefinition, false);
        }
    }

    private void registerRealmDefinition(RealmDefinition realmDefinition) {
        this.registerRealmDefinition(realmDefinition, true);
    }

    private void registerRealmDefinition(
            RealmDefinition realmDefinition,
            boolean createMissingDataDefinition
    ) {
        try {
            ResourceKey<LevelStem> levelStemKey = ResourceKey.create(
                    Registries.LEVEL_STEM,
                    realmDefinition.getId()
            );
            Registry<LevelStem> levelStemRegistry = this.thisServer.registryAccess()
                    .registryOrThrow(Registries.LEVEL_STEM);
            if (levelStemRegistry.containsKey(levelStemKey)) {
                return;
            }

            if (createMissingDataDefinition) {
                this.thisDataPackGenerator.ensureRealmDefinition(realmDefinition);
            }
            LevelStem levelStem = this.thisDataPackGenerator.readRealmLevelStem(
                    realmDefinition.getId(),
                    this.thisServer.registryAccess()
            );
            MappedRegistry<LevelStem> mutableLevelStemRegistry =
                    getMutableLevelStemRegistry(levelStemRegistry);
            mutableLevelStemRegistry.unfreeze();
            try {
                if (!mutableLevelStemRegistry.containsKey(levelStemKey)) {
                    mutableLevelStemRegistry.register(levelStemKey, levelStem, Lifecycle.stable());
                }
            } finally {
                mutableLevelStemRegistry.freeze();
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate three-thousand-realm dimension " + realmDefinition.getId(),
                    exception
            );
        }
    }

    private synchronized RegistryAccess.Frozen createWorldSaveRegistryAccess(
            RegistryAccess registryAccess
    ) {
        return ThreeThousandRealmRegistryUtility.createWorldSaveRegistryAccess(
                registryAccess,
                this.createManagedRealmIdSet()
        );
    }

    private synchronized Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> excludeManagedRealmStemsFromInitialLoad(
            Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> levelStemEntries
    ) {
        return ThreeThousandRealmRegistryUtility.excludeRealmLevelStemEntries(
                levelStemEntries,
                this.createManagedRealmIdSet()
        );
    }

    private Set<ResourceLocation> createManagedRealmIdSet() {
        Set<ResourceLocation> realmIds = new LinkedHashSet<>();
        for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
            realmIds.add(realmDefinition.getId());
        }
        realmIds.addAll(this.thisExternalRealmDefinitions.keySet());
        realmIds.addAll(this.thisDataPackGenerator.getPersistedRealmDefinitionIds());
        return Collections.unmodifiableSet(realmIds);
    }

    @SuppressWarnings("unchecked")
    private static MappedRegistry<LevelStem> getMutableLevelStemRegistry(
            Registry<LevelStem> levelStemRegistry
    ) {
        if (!(levelStemRegistry instanceof MappedRegistry<?>)) {
            throw new IllegalStateException("The level-stem registry is not mutable");
        }
        return (MappedRegistry<LevelStem>) levelStemRegistry;
    }

    private static RegistryAccess.Frozen freezeRegistryAccess(RegistryAccess registryAccess) {
        if (registryAccess instanceof RegistryAccess.Frozen frozenRegistryAccess) {
            return frozenRegistryAccess;
        }
        return registryAccess.freeze();
    }

    private static Path getStateFilePath(MinecraftServer server) {
        return server.getWorldPath(new LevelResource("data"))
                .resolve(STATE_DIRECTORY_NAME)
                .resolve(STATE_FILE_NAME);
    }

    private static Path getPlayerReturnStateFilePath(MinecraftServer server) {
        return server.getWorldPath(new LevelResource("data"))
                .resolve(STATE_DIRECTORY_NAME)
                .resolve(PLAYER_RETURN_STATE_FILE_NAME);
    }
}
