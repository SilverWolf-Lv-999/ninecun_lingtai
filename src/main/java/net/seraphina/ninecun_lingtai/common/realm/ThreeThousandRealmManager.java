package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public final class ThreeThousandRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String STATE_FILE_NAME = "three_thousand_realms.json";
    private static final String PLAYER_RETURN_STATE_FILE_NAME =
            "three_thousand_realm_player_returns.json";
    private static final String CATALOG_THREAD_NAME = "ninecun-lingtai-realm-catalog";
    private static final int CATALOG_CREATION_BATCH_SIZE = 32;
    private static final long CATALOG_PROGRESS_DELAY_MILLIS = 60L;
    private static final Map<MinecraftServer, ThreeThousandRealmManager> MANAGERS = new WeakHashMap<>();
    private static final ChunkProgressListener NO_OP_CHUNK_PROGRESS_LISTENER = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos center) {
        }

        @Override
        public void onStatusChange(ChunkPos chunkPosition, ChunkStatus chunkStatus) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private final MinecraftServer thisServer;
    private final ThreeThousandRealmState thisState;
    private final PlayerThreeThousandRealmReturnState thisPlayerReturnState;
    private final Map<ResourceKey<Level>, Long> thisEmptyRealmTicks = new HashMap<>();
    private final ExecutorService thisCatalogExecutor;
    private final Set<UUID> thisCatalogObservers = new HashSet<>();
    private ThreeThousandRealmCatalogSnapshot thisCatalogSnapshot;
    @Nullable
    private Future<?> thisCatalogTask;
    private volatile boolean thisClosed;

    private ThreeThousandRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisState = ThreeThousandRealmState.load(getStateFilePath(server));
        this.thisPlayerReturnState = PlayerThreeThousandRealmReturnState.load(
                getPlayerReturnStateFilePath(server)
        );
        int currentRealmCount = this.thisState.getRealmCount();
        this.thisCatalogSnapshot = createCatalogSnapshot(
                ThreeThousandRealmCatalogStatus.LOADING,
                currentRealmCount,
                Math.max(1, currentRealmCount)
        );
        this.thisCatalogExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, CATALOG_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
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

    public static void clear(MinecraftServer server) {
        ThreeThousandRealmManager manager;
        synchronized (MANAGERS) {
            manager = MANAGERS.remove(server);
        }
        if (manager != null) {
            manager.shutdown();
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
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, realmDefinition.getId());
        Map<ResourceKey<Level>, ServerLevel> worlds = this.thisServer.forgeGetWorldMap();
        ServerLevel loadedRealm = worlds.get(levelKey);
        if (loadedRealm != null) {
            return loadedRealm;
        }

        ServerLevel createdRealm = this.createServerLevel(realmDefinition, levelKey);
        worlds.put(levelKey, createdRealm);
        this.thisServer.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(createdRealm));
        ModMain.LOGGER.info(
                "Loaded three-thousand realm {} ({}) with seed {}",
                realmDefinition.getDisplayName(),
                realmDefinition.getId(),
                realmDefinition.getSeed()
        );
        return createdRealm;
    }

    public synchronized boolean isThreeThousandRealm(ResourceKey<Level> levelKey) {
        return this.getRealmDefinition(levelKey).isPresent();
    }

    public synchronized Optional<RealmDefinition> getRealmDefinition(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.thisState.getRealm(levelKey.location());
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
                createRealmTeleporter(spawnPosition, player.getYRot(), player.getXRot())
        ) != null;
    }

    public void openRealmCatalog(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ThreeThousandRealmCatalogSnapshot snapshot;
        synchronized (this) {
            if (this.thisClosed) {
                return;
            }
            this.thisCatalogObservers.add(player.getUUID());
            this.startRealmCatalogInitializationIfNeeded();
            snapshot = this.thisCatalogSnapshot;
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
                createRealmTeleporter(spawnPosition, player.getYRot(), player.getXRot())
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
                createRealmTeleporter(
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

    private synchronized void startRealmCatalogInitializationIfNeeded() {
        int configuredRealmCount = CommonConfig.THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT.get();
        int existingRealmCount = this.thisState.getRealmCount();
        int targetRealmCount = Math.max(configuredRealmCount, existingRealmCount);
        if (existingRealmCount >= targetRealmCount) {
            this.thisCatalogSnapshot = createReadyCatalogSnapshot(
                    this.thisState.getRealms(),
                    targetRealmCount
            );
            return;
        }
        if (this.thisCatalogTask != null && !this.thisCatalogTask.isDone()) {
            return;
        }

        this.thisCatalogSnapshot = createCatalogSnapshot(
                ThreeThousandRealmCatalogStatus.LOADING,
                existingRealmCount,
                targetRealmCount
        );
        this.thisCatalogTask = this.thisCatalogExecutor.submit(
                () -> initializeRealmCatalog(targetRealmCount, existingRealmCount)
        );
    }

    private void initializeRealmCatalog(int targetRealmCount, int initialRealmCount) {
        try {
            Random random = new Random();
            int completedRealmCount = initialRealmCount;
            while (completedRealmCount < targetRealmCount) {
                this.thisState.createRealmBatch(
                        random,
                        targetRealmCount,
                        CATALOG_CREATION_BATCH_SIZE
                );
                completedRealmCount = this.thisState.getRealmCount();
                int publishedRealmCount = completedRealmCount;
                executeOnServerAndWait(
                        () -> publishRealmCatalogProgress(
                                publishedRealmCount,
                                targetRealmCount
                        )
                );
                if (completedRealmCount < targetRealmCount) {
                    Thread.sleep(CATALOG_PROGRESS_DELAY_MILLIS);
                }
            }
            List<RealmDefinition> realms = this.thisState.getRealms();
            ThreeThousandRealmCatalogSnapshot readySnapshot = createReadyCatalogSnapshot(
                    realms,
                    targetRealmCount
            );
            executeOnServer(() -> completeRealmCatalogInitialization(readySnapshot));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ModMain.LOGGER.debug("Three-thousand-realm catalog initialization was interrupted");
        } catch (CancellationException exception) {
            ModMain.LOGGER.debug("Three-thousand-realm catalog initialization was cancelled");
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error("Unable to initialize the three-thousand-realm catalog", exception);
            executeOnServer(() -> failRealmCatalogInitialization(targetRealmCount));
        }
    }

    private void executeOnServer(Runnable task) {
        if (this.thisClosed) {
            return;
        }
        this.thisServer.execute(() -> {
            if (!this.thisClosed) {
                task.run();
            }
        });
    }

    private void executeOnServerAndWait(Runnable task) throws InterruptedException {
        if (this.thisClosed) {
            throw new CancellationException("Three-thousand-realm catalog initialization was cancelled");
        }
        CountDownLatch completionLatch = new CountDownLatch(1);
        this.thisServer.execute(() -> {
            try {
                if (!this.thisClosed) {
                    task.run();
                }
            } finally {
                completionLatch.countDown();
            }
        });
        completionLatch.await();
        if (this.thisClosed) {
            throw new CancellationException("Three-thousand-realm catalog initialization was cancelled");
        }
    }

    private synchronized void publishRealmCatalogProgress(
            int completedRealmCount,
            int targetRealmCount
    ) {
        if (this.thisClosed || this.thisCatalogSnapshot.getTargetRealmCount() != targetRealmCount) {
            return;
        }
        this.thisCatalogSnapshot = createCatalogSnapshot(
                ThreeThousandRealmCatalogStatus.LOADING,
                completedRealmCount,
                targetRealmCount
        );
        sendRealmCatalogToObservers();
    }

    private synchronized void completeRealmCatalogInitialization(
            ThreeThousandRealmCatalogSnapshot readySnapshot
    ) {
        if (this.thisClosed) {
            return;
        }
        this.thisCatalogSnapshot = readySnapshot;
        this.thisCatalogTask = null;
        sendRealmCatalogToObservers();
    }

    private synchronized void failRealmCatalogInitialization(int targetRealmCount) {
        if (this.thisClosed || this.thisCatalogSnapshot.getTargetRealmCount() != targetRealmCount) {
            return;
        }
        this.thisCatalogSnapshot = new ThreeThousandRealmCatalogSnapshot(
                ThreeThousandRealmCatalogStatus.FAILED,
                this.thisState.getRealmCount(),
                targetRealmCount,
                List.of()
        );
        this.thisCatalogTask = null;
        sendRealmCatalogToObservers();
    }

    private void sendRealmCatalogToObservers() {
        this.thisCatalogObservers.removeIf(playerId -> {
            ServerPlayer player = this.thisServer.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return true;
            }
            ModNetwork.sendThreeThousandRealmCatalog(player, this.thisCatalogSnapshot);
            return false;
        });
    }

    private void shutdown() {
        synchronized (this) {
            this.thisClosed = true;
            this.thisCatalogObservers.clear();
            if (this.thisCatalogTask != null) {
                this.thisCatalogTask.cancel(true);
                this.thisCatalogTask = null;
            }
        }
        this.thisCatalogExecutor.shutdownNow();
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

    private ThreeThousandRealmCatalogSnapshot createCatalogSnapshot(
            ThreeThousandRealmCatalogStatus status,
            int completedRealmCount,
            int targetRealmCount
    ) {
        List<ThreeThousandRealmCatalogEntry> catalogEntries = createCatalogEntries(
                this.thisState.getRealms()
        );
        if (catalogEntries.size() != completedRealmCount) {
            throw new IllegalStateException("Three-thousand-realm catalog progress is inconsistent");
        }
        return new ThreeThousandRealmCatalogSnapshot(
                status,
                completedRealmCount,
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
        for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
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

    private ServerLevel createServerLevel(RealmDefinition realmDefinition, ResourceKey<Level> levelKey) {
        LevelStem overworldStem = this.thisServer.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(LevelStem.OVERWORLD);
        if (overworldStem == null) {
            throw new IllegalStateException("The overworld level stem is unavailable");
        }

        LevelStem realmStem = new LevelStem(overworldStem.type(), overworldStem.generator());
        ServerLevelData realmLevelData = new DerivedLevelData(
                this.thisServer.getWorldData(),
                this.thisServer.getWorldData().overworldData()
        );
        try (ThreeThousandRealmSeedContext.RealmSeedScope ignored =
                     ThreeThousandRealmSeedContext.push(realmDefinition.getSeed())) {
            return new ServerLevel(
                    this.thisServer,
                    this.thisServer.executor,
                    this.thisServer.storageSource,
                    realmLevelData,
                    levelKey,
                    realmStem,
                    NO_OP_CHUNK_PROGRESS_LISTENER,
                    this.thisServer.getWorldData().isDebugWorld(),
                    BiomeManager.obfuscateSeed(realmDefinition.getSeed()),
                    List.of(),
                    false,
                    null
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

    private static ITeleporter createRealmTeleporter(Vec3 destination, float yRot, float xRot) {
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
