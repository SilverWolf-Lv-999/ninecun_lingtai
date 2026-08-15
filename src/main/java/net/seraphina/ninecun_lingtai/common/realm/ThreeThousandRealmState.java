package net.seraphina.ninecun_lingtai.common.realm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;
import net.seraphina.ninecun_lingtai.utility.RealmNameUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.IntConsumer;

public final class ThreeThousandRealmState {
    private static final int FORMAT_VERSION = 1;
    private static final int MAXIMUM_REALM_CAPACITY = 3000;
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String REALMS_KEY = "realms";
    private static final String ID_KEY = "id";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SEED_KEY = "seed";
    private static final String CREATED_AT_EPOCH_MILLIS_KEY = "createdAtEpochMillis";

    private final Path thisFilePath;
    private final Map<ResourceLocation, RealmDefinition> thisRealms = new LinkedHashMap<>();

    private ThreeThousandRealmState(Path filePath) {
        this.thisFilePath = filePath.toAbsolutePath().normalize();
    }

    public static ThreeThousandRealmState load(Path filePath) {
        ThreeThousandRealmState state = new ThreeThousandRealmState(filePath);
        if (!Files.exists(state.thisFilePath)) {
            return state;
        }

        try {
            state.read(JsonFileUtility.read(state.thisFilePath));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.error("Unable to read three-thousand-realm state from {}", state.thisFilePath, exception);
        }
        return state;
    }

    public synchronized List<RealmDefinition> getRealms() {
        return List.copyOf(this.thisRealms.values());
    }

    public synchronized Optional<RealmDefinition> getRealm(ResourceLocation realmId) {
        return Optional.ofNullable(this.thisRealms.get(realmId));
    }

    public synchronized int getRealmCount() {
        return this.thisRealms.size();
    }

    public synchronized RealmDefinition createRealm(Random random, int maximumRealmCount) {
        Objects.requireNonNull(random, "random");
        validateMaximumRealmCount(maximumRealmCount);
        if (this.thisRealms.size() >= maximumRealmCount) {
            throw new IllegalStateException("The configured three-thousand-realm limit has been reached");
        }

        RealmDefinition realmDefinition = createRealmDefinition(
                random,
                createExistingDisplayNames()
        );
        this.thisRealms.put(realmDefinition.getId(), realmDefinition);
        if (!this.save()) {
            this.thisRealms.remove(realmDefinition.getId());
            throw new IllegalStateException("Unable to persist the three-thousand-realm state");
        }
        return realmDefinition;
    }

    public synchronized List<RealmDefinition> createRealmBatch(
            Random random,
            int maximumRealmCount,
            int maximumBatchSize
    ) {
        Objects.requireNonNull(random, "random");
        validateMaximumRealmCount(maximumRealmCount);
        if (maximumBatchSize < 1) {
            throw new IllegalArgumentException("maximumBatchSize must be at least 1");
        }
        if (this.thisRealms.size() >= maximumRealmCount) {
            return List.of();
        }

        int batchEndRealmCount = Math.min(
                maximumRealmCount,
                this.thisRealms.size() + maximumBatchSize
        );
        Set<String> existingDisplayNames = createExistingDisplayNames();
        List<RealmDefinition> createdRealms = new ArrayList<>(
                batchEndRealmCount - this.thisRealms.size()
        );
        try {
            while (this.thisRealms.size() < batchEndRealmCount) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Three-thousand-realm catalog initialization was interrupted");
                }
                RealmDefinition realmDefinition = createRealmDefinition(random, existingDisplayNames);
                this.thisRealms.put(realmDefinition.getId(), realmDefinition);
                createdRealms.add(realmDefinition);
            }
            if (!this.save()) {
                throw new IllegalStateException("Unable to persist the three-thousand-realm state");
            }
            return List.copyOf(createdRealms);
        } catch (RuntimeException exception) {
            for (RealmDefinition realm : createdRealms) {
                this.thisRealms.remove(realm.getId());
            }
            throw exception;
        }
    }

    public synchronized List<RealmDefinition> ensureRealmCount(
            Random random,
            int maximumRealmCount,
            IntConsumer progressConsumer
    ) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(progressConsumer, "progressConsumer");
        validateMaximumRealmCount(maximumRealmCount);

        if (this.thisRealms.size() >= maximumRealmCount) {
            return List.copyOf(this.thisRealms.values());
        }

        Set<String> existingDisplayNames = createExistingDisplayNames();
        List<ResourceLocation> newlyCreatedRealmIds = new ArrayList<>();
        try {
            while (this.thisRealms.size() < maximumRealmCount) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Three-thousand-realm catalog initialization was interrupted");
                }
                RealmDefinition realmDefinition = createRealmDefinition(random, existingDisplayNames);
                this.thisRealms.put(realmDefinition.getId(), realmDefinition);
                newlyCreatedRealmIds.add(realmDefinition.getId());
                progressConsumer.accept(this.thisRealms.size());
            }
            if (!this.save()) {
                throw new IllegalStateException("Unable to persist the three-thousand-realm state");
            }
            return List.copyOf(this.thisRealms.values());
        } catch (RuntimeException exception) {
            for (ResourceLocation realmId : newlyCreatedRealmIds) {
                this.thisRealms.remove(realmId);
            }
            throw exception;
        }
    }

    public synchronized boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error("Unable to save three-thousand-realm state to {}", this.thisFilePath, exception);
            return false;
        }
    }

    private RealmDefinition createRealmDefinition(
            Random random,
            Set<String> existingDisplayNames
    ) {
        RealmNameUtility.RealmName realmName = RealmNameUtility.createRandomName(
                existingDisplayNames,
                random
        );
        existingDisplayNames.add(realmName.getDisplayName());
        ResourceLocation realmId = ResourceLocation.fromNamespaceAndPath(
                ModMain.MOD_ID,
                realmName.getPathPrefix() + "_" + UUID.randomUUID()
        );
        return new RealmDefinition(
                realmId,
                realmName.getDisplayName(),
                random.nextLong(),
                System.currentTimeMillis()
        );
    }

    private Set<String> createExistingDisplayNames() {
        Set<String> existingDisplayNames = new HashSet<>();
        for (RealmDefinition realm : this.thisRealms.values()) {
            existingDisplayNames.add(realm.getDisplayName());
        }
        return existingDisplayNames;
    }

    private static void validateMaximumRealmCount(int maximumRealmCount) {
        if (maximumRealmCount < 1 || maximumRealmCount > MAXIMUM_REALM_CAPACITY) {
            throw new IllegalArgumentException(
                    "maximumRealmCount must be between 1 and " + MAXIMUM_REALM_CAPACITY
            );
        }
    }

    private void read(JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            throw new JsonParseException("Three-thousand-realm state root must be an object");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();
        int formatVersion = getRequiredInt(rootObject, FORMAT_VERSION_KEY);
        if (formatVersion != FORMAT_VERSION) {
            throw new JsonParseException("Unsupported three-thousand-realm state format: " + formatVersion);
        }
        JsonArray realmArray = getRequiredArray(rootObject, REALMS_KEY);
        for (JsonElement realmElement : realmArray) {
            if (this.thisRealms.size() >= MAXIMUM_REALM_CAPACITY) {
                ModMain.LOGGER.warn(
                        "Ignoring remaining realm entries because the hard limit of {} is reached",
                        MAXIMUM_REALM_CAPACITY
                );
                break;
            }
            this.readRealm(realmElement);
        }
    }

    private void readRealm(JsonElement realmElement) {
        try {
            if (!realmElement.isJsonObject()) {
                throw new JsonParseException("Realm entry must be an object");
            }

            JsonObject realmObject = realmElement.getAsJsonObject();
            ResourceLocation realmId = ResourceLocation.tryParse(getRequiredString(realmObject, ID_KEY));
            if (realmId == null || !ModMain.MOD_ID.equals(realmId.getNamespace())) {
                throw new JsonParseException("Realm entry has an invalid id");
            }
            String displayName = getRequiredString(realmObject, DISPLAY_NAME_KEY);
            if (this.thisRealms.containsKey(realmId) || this.containsDisplayName(displayName)) {
                throw new JsonParseException("Realm entry duplicates an existing id or display name");
            }

            RealmDefinition realmDefinition = new RealmDefinition(
                    realmId,
                    displayName,
                    getRequiredLong(realmObject, SEED_KEY),
                    getRequiredLong(realmObject, CREATED_AT_EPOCH_MILLIS_KEY)
            );
            this.thisRealms.put(realmId, realmDefinition);
        } catch (JsonParseException | IllegalArgumentException exception) {
            ModMain.LOGGER.warn("Ignoring malformed three-thousand-realm entry", exception);
        }
    }

    private boolean containsDisplayName(String displayName) {
        for (RealmDefinition realm : this.thisRealms.values()) {
            if (realm.getDisplayName().equals(displayName)) {
                return true;
            }
        }
        return false;
    }

    private JsonObject createJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        JsonArray realmArray = new JsonArray();
        for (RealmDefinition realm : this.thisRealms.values()) {
            JsonObject realmObject = new JsonObject();
            realmObject.addProperty(ID_KEY, realm.getId().toString());
            realmObject.addProperty(DISPLAY_NAME_KEY, realm.getDisplayName());
            realmObject.addProperty(SEED_KEY, realm.getSeed());
            realmObject.addProperty(CREATED_AT_EPOCH_MILLIS_KEY, realm.getCreatedAtEpochMillis());
            realmArray.add(realmObject);
        }
        rootObject.add(REALMS_KEY, realmArray);
        return rootObject;
    }

    private static JsonArray getRequiredArray(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new JsonParseException(key + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static int getRequiredInt(JsonObject jsonObject, String key) {
        JsonElement element = getRequiredNumber(jsonObject, key);
        try {
            return element.getAsInt();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(key + " must be an integer", exception);
        }
    }

    private static long getRequiredLong(JsonObject jsonObject, String key) {
        JsonElement element = getRequiredNumber(jsonObject, key);
        try {
            return element.getAsLong();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(key + " must be a long", exception);
        }
    }

    private static JsonElement getRequiredNumber(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element == null
                || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(key + " must be a number");
        }
        return element;
    }

    private static String getRequiredString(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element == null
                || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(key + " must be a string");
        }
        String value = element.getAsString().strip();
        if (value.isEmpty()) {
            throw new JsonParseException(key + " must not be blank");
        }
        return value;
    }
}
