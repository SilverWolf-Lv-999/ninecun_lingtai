package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmSeedContext;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmServerAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import it.unimi.dsi.fastutil.longs.LongIterator;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerRealmLoaderMixin implements ThreeThousandRealmServerAccess {
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

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    @Final
    private Executor executor;

    @Redirect(
            method = "createLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/Registry;entrySet()Ljava/util/Set;"
            )
    )
    private Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> skipManagedRealmsDuringInitialLevelLoad(
            Registry<LevelStem> levelStemRegistry
    ) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        return ThreeThousandRealmManager.excludeManagedRealmStemsFromInitialLoad(
                server,
                levelStemRegistry.entrySet()
        );
    }

    @Override
    public ServerLevel ninecunLingtai$loadRegisteredRealm(ResourceKey<Level> levelKey, long realmSeed) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Map<ResourceKey<Level>, ServerLevel> worlds = server.forgeGetWorldMap();
        ServerLevel loadedRealm = worlds.get(levelKey);
        if (loadedRealm != null) {
            return loadedRealm;
        }

        ResourceKey<LevelStem> levelStemKey = ResourceKey.create(
                Registries.LEVEL_STEM,
                levelKey.location()
        );
        LevelStem levelStem = server.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(levelStemKey);
        if (levelStem == null) {
            throw new IllegalStateException(
                    "The generated three-thousand-realm level stem is unavailable: " + levelKey.location()
            );
        }

        ServerLevel overworld = server.overworld();
        DerivedLevelData levelData = new DerivedLevelData(
                server.getWorldData(),
                server.getWorldData().overworldData()
        );
        ServerLevel realmLevel;
        try (ThreeThousandRealmSeedContext.RealmSeedScope ignored =
                     ThreeThousandRealmSeedContext.push(realmSeed)) {
            realmLevel = new ServerLevel(
                    server,
                    this.executor,
                    this.storageSource,
                    levelData,
                    levelKey,
                    levelStem,
                    NO_OP_CHUNK_PROGRESS_LISTENER,
                    server.getWorldData().isDebugWorld(),
                    BiomeManager.obfuscateSeed(realmSeed),
                    List.of(),
                    false,
                    overworld.getRandomSequences()
            );
        }

        WorldBorder overworldBorder = overworld.getWorldBorder();
        realmLevel.getWorldBorder().applySettings(overworldBorder.createSettings());
        overworldBorder.addListener(
                new BorderChangeListener.DelegateBorderChangeListener(realmLevel.getWorldBorder())
        );
        worlds.put(levelKey, realmLevel);
        restorePersistentChunks(realmLevel);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(realmLevel));
        return realmLevel;
    }

    private static void restorePersistentChunks(ServerLevel level) {
        ForcedChunksSavedData forcedChunks = level.getDataStorage().get(
                ForcedChunksSavedData::load,
                "chunks"
        );
        if (forcedChunks == null) {
            return;
        }

        LongIterator forcedChunkIterator = forcedChunks.getChunks().iterator();
        while (forcedChunkIterator.hasNext()) {
            level.getChunkSource().updateChunkForced(new ChunkPos(forcedChunkIterator.nextLong()), true);
        }
        ForgeChunkManager.reinstatePersistentChunks(level, forcedChunks);
    }
}
