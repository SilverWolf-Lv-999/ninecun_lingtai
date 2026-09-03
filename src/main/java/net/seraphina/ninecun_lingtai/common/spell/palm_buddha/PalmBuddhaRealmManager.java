package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.RealmDefinition;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 掌中佛国的服务端领域门面。
 *
 * <p>它只管理佛国业务定义，维度注册、数据定义生成、懒加载和卸载全部
 * 复用 {@link ThreeThousandRealmManager} 的动态维度 API。</p>
 */
public final class PalmBuddhaRealmManager {
    private static final String STATE_DIRECTORY_NAME = ModMain.MOD_ID;
    private static final String STATE_FILE_NAME = "palm_buddha_realms.json";
    private static final Map<MinecraftServer, PalmBuddhaRealmManager> MANAGERS = new WeakHashMap<>();

    private final MinecraftServer thisServer;
    private final PalmBuddhaRealmState thisState;

    private PalmBuddhaRealmManager(MinecraftServer server) {
        this.thisServer = server;
        this.thisState = PalmBuddhaRealmState.load(getStateFilePath(server));
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
        RealmDefinition realmDefinition = this.thisState.getOrCreateRealm(
                owner.getUUID(),
                owner.getGameProfile().getName(),
                new Random()
        );
        this.registerWithDynamicRealmApi(realmDefinition);
        return realmDefinition;
    }

    public synchronized ServerLevel getOrLoadRealm(ServerPlayer owner) {
        RealmDefinition realmDefinition = this.getOrCreateRealmDefinition(owner);
        return ThreeThousandRealmManager.get(this.thisServer)
                .getOrLoadManagedRealm(realmDefinition);
    }

    public synchronized Optional<RealmDefinition> getRealmDefinition(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.getOwnerId(levelKey).flatMap(this.thisState::getRealm);
    }

    public synchronized Optional<UUID> getOwnerId(ResourceKey<Level> levelKey) {
        Objects.requireNonNull(levelKey, "levelKey");
        return this.thisState.getOwner(levelKey.location());
    }

    public synchronized boolean isPalmBuddhaRealm(ResourceKey<Level> levelKey) {
        return this.getRealmDefinition(levelKey).isPresent();
    }

    private synchronized void registerPersistedRealmDefinitions() {
        for (RealmDefinition realmDefinition : this.thisState.getRealms()) {
            this.registerWithDynamicRealmApi(realmDefinition);
        }
    }

    private void registerWithDynamicRealmApi(RealmDefinition realmDefinition) {
        ThreeThousandRealmManager.get(this.thisServer)
                .registerManagedRealmDefinition(realmDefinition);
    }

    private static Path getStateFilePath(MinecraftServer server) {
        return server.getWorldPath(new LevelResource("data"))
                .resolve(STATE_DIRECTORY_NAME)
                .resolve(STATE_FILE_NAME);
    }
}

