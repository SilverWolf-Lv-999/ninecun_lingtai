package net.seraphina.ninecun_lingtai.common.causality;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.DimensionNameUtility;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CausalityHistoryManager {
    private static final String DIRECTORY_NAME = "causality";
    private static final String PLAYER_KEY = "player";
    private static final String RECORDS_KEY = "records";
    private static final int FLUSH_INTERVAL_TICKS = 20;

    private static final Map<Path, CachedHistory> HISTORIES = new HashMap<>();

    private CausalityHistoryManager() {
    }

    public static void record(
            ServerPlayer player,
            String category,
            String description
    ) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        CachedHistory history = getOrLoad(player.server, player.getUUID());
        ServerLevel level = player.serverLevel();
        history.records.add(new CausalityRecord(
                Instant.now().toString(),
                level.getGameTime(),
                level.dimension().location().toString(),
                player.blockPosition().getX(),
                player.blockPosition().getY(),
                player.blockPosition().getZ(),
                category,
                description
        ));
        history.dirty = true;
        if (player.tickCount % FLUSH_INTERVAL_TICKS == 0) {
            flush(history);
        }
    }

    public static List<CausalityRecord> getRecords(
            MinecraftServer server,
            UUID playerId,
            int maximumRecords
    ) {
        if (maximumRecords <= 0) {
            return List.of();
        }
        List<CausalityRecord> records = getOrLoad(server, playerId).records;
        int start = Math.max(0, records.size() - maximumRecords);
        return records.subList(start, records.size()).stream()
                .map(record -> new CausalityRecord(
                        record.timestamp(),
                        record.gameTime(),
                        DimensionNameUtility.getDisplayName(server, record.dimension()),
                        record.x(),
                        record.y(),
                        record.z(),
                        record.category(),
                        record.description()
                ))
                .toList();
    }

    public static void flush(MinecraftServer server) {
        HISTORIES.values().stream()
                .filter(history -> history.server == server)
                .forEach(CausalityHistoryManager::flush);
    }

    public static void flush(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        CachedHistory history = HISTORIES.get(getFilePath(player.server, player.getUUID()));
        if (history != null) {
            flush(history);
        }
    }

    public static void clear(MinecraftServer server) {
        flush(server);
        HISTORIES.entrySet().removeIf(entry -> entry.getValue().server == server);
    }

    private static CachedHistory getOrLoad(MinecraftServer server, UUID playerId) {
        Path filePath = getFilePath(server, playerId);
        CachedHistory existing = HISTORIES.get(filePath);
        if (existing != null) {
            return existing;
        }
        CachedHistory loaded = new CachedHistory(server, filePath, load(filePath));
        HISTORIES.put(filePath, loaded);
        return loaded;
    }

    private static Path getFilePath(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(new LevelResource("data"))
                .resolve(ModMain.MOD_ID)
                .resolve(DIRECTORY_NAME)
                .resolve(playerId + ".json")
                .toAbsolutePath()
                .normalize();
    }

    private static List<CausalityRecord> load(Path filePath) {
        if (!Files.isRegularFile(filePath)) {
            return new ArrayList<>();
        }
        try {
            JsonElement root = JsonFileUtility.read(filePath);
            if (!root.isJsonObject()) {
                return new ArrayList<>();
            }
            JsonArray records = root.getAsJsonObject().getAsJsonArray(RECORDS_KEY);
            if (records == null) {
                return new ArrayList<>();
            }
            List<CausalityRecord> result = new ArrayList<>();
            for (JsonElement element : records) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject record = element.getAsJsonObject();
                result.add(new CausalityRecord(
                        getString(record, "timestamp", "unknown"),
                        getLong(record, "game_time", 0L),
                        getString(record, "dimension", "unknown"),
                        getInt(record, "x", 0),
                        getInt(record, "y", 0),
                        getInt(record, "z", 0),
                        getString(record, "category", "other"),
                        getString(record, "description", "")
                ));
            }
            return result;
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.warn("Unable to load causality history from {}", filePath, exception);
            return new ArrayList<>();
        }
    }

    private static void flush(CachedHistory history) {
        if (!history.dirty) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty(PLAYER_KEY, history.filePath.getFileName().toString().replace(".json", ""));
        JsonArray records = new JsonArray();
        history.records.forEach(record -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("timestamp", record.timestamp());
            entry.addProperty("game_time", record.gameTime());
            entry.addProperty("dimension", record.dimension());
            entry.addProperty("x", record.x());
            entry.addProperty("y", record.y());
            entry.addProperty("z", record.z());
            entry.addProperty("category", record.category());
            entry.addProperty("description", record.description());
            records.add(entry);
        });
        root.add(RECORDS_KEY, records);
        try {
            JsonFileUtility.write(history.filePath, root);
            history.dirty = false;
        } catch (IOException exception) {
            ModMain.LOGGER.warn("Unable to save causality history to {}", history.filePath, exception);
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString()
                : fallback;
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsLong()
                : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsInt()
                : fallback;
    }

    private static final class CachedHistory {
        private final MinecraftServer server;
        private final Path filePath;
        private final List<CausalityRecord> records;
        private boolean dirty;

        private CachedHistory(
                MinecraftServer server,
                Path filePath,
                List<CausalityRecord> records
        ) {
            this.server = server;
            this.filePath = filePath;
            this.records = records;
        }
    }
}
