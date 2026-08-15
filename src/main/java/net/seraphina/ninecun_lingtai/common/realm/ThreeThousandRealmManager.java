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
import net.seraphina.ninecun_lingtai.utility.RealmSpawnUtility;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.level.LevelEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

public final class ThreeThousandRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String STATE_FILE_NAME = "three_thousand_realms.json";
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
    private final Map<ResourceKey<Level>, Long> thisEmptyRealmTicks = new HashMap<>();

    private ThreeThousandRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisState = ThreeThousandRealmState.load(getStateFilePath(server));
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

    public synchronized int getRealmCount() {
        return this.thisState.getRealmCount();
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
                    this.thisServer,
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
}
