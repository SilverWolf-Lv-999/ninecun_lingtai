package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.RealmDefinition;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 持久化每个施法者的独立佛国定义。
 *
 * <p>佛国维度本身仍由现有动态维度桥接按需注册和加载；此状态只负责
 * “施法者 UUID → 唯一 RealmDefinition”的业务映射。</p>
 */
public final class PalmBuddhaRealmState {
    private static final int FORMAT_VERSION = 1;
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String REALMS_KEY = "realms";
    private static final String OWNER_ID_KEY = "ownerId";
    private static final String ID_KEY = "id";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SEED_KEY = "seed";
    private static final String CREATED_AT_EPOCH_MILLIS_KEY = "createdAtEpochMillis";
    private static final String REALM_PATH_PREFIX = "palm_buddha_";

    private final Path thisFilePath;
    private final Map<UUID, RealmDefinition> thisRealmsByOwner = new LinkedHashMap<>();
    private final Map<ResourceLocation, UUID> thisOwnersByRealm = new LinkedHashMap<>();

    private PalmBuddhaRealmState(Path filePath) {
        this.thisFilePath = filePath.toAbsolutePath().normalize();
    }

    public static PalmBuddhaRealmState load(Path filePath) {
        PalmBuddhaRealmState state = new PalmBuddhaRealmState(filePath);
        if (!Files.exists(state.thisFilePath)) {
            return state;
        }

        try {
            state.read(JsonFileUtility.read(state.thisFilePath));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.error(
                    "Unable to read palm-buddha-realm state from {}",
                    state.thisFilePath,
                    exception
            );
        }
        return state;
    }

    public synchronized List<RealmDefinition> getRealms() {
        return List.copyOf(this.thisRealmsByOwner.values());
    }

    public synchronized Optional<RealmDefinition> getRealm(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return Optional.ofNullable(this.thisRealmsByOwner.get(ownerId));
    }

    public synchronized Optional<UUID> getOwner(ResourceLocation realmId) {
        Objects.requireNonNull(realmId, "realmId");
        return Optional.ofNullable(this.thisOwnersByRealm.get(realmId));
    }

    public synchronized RealmDefinition getOrCreateRealm(
            UUID ownerId,
            String ownerName,
            Random random
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(random, "random");

        RealmDefinition existingRealm = this.thisRealmsByOwner.get(ownerId);
        if (existingRealm != null) {
            return existingRealm;
        }

        ResourceLocation realmId = ResourceLocation.fromNamespaceAndPath(
                ModMain.MOD_ID,
                REALM_PATH_PREFIX + ownerId.toString().replace("-", "")
        );
        String normalizedOwnerName = ownerName == null ? "" : ownerName.strip();
        if (normalizedOwnerName.isEmpty()) {
            normalizedOwnerName = ownerId.toString();
        }
        RealmDefinition realmDefinition = new RealmDefinition(
                realmId,
                "掌中佛国·" + normalizedOwnerName,
                random.nextLong(),
                System.currentTimeMillis()
        );
        if (this.thisOwnersByRealm.containsKey(realmId)) {
            throw new IllegalStateException("Palm-buddha realm id is already owned: " + realmId);
        }

        this.thisRealmsByOwner.put(ownerId, realmDefinition);
        this.thisOwnersByRealm.put(realmId, ownerId);
        if (!this.save()) {
            this.thisRealmsByOwner.remove(ownerId);
            this.thisOwnersByRealm.remove(realmId);
            throw new IllegalStateException("Unable to persist palm-buddha realm state");
        }
        return realmDefinition;
    }

    public synchronized boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error(
                    "Unable to save palm-buddha-realm state to {}",
                    this.thisFilePath,
                    exception
            );
            return false;
        }
    }

    private void read(JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            throw new JsonParseException("Palm-buddha-realm state must be an object");
        }
        JsonObject rootObject = rootElement.getAsJsonObject();
        int formatVersion = getRequiredInt(rootObject, FORMAT_VERSION_KEY);
        if (formatVersion != FORMAT_VERSION) {
            throw new JsonParseException("Unsupported palm-buddha-realm state format: " + formatVersion);
        }

        JsonArray realmArray = getRequiredArray(rootObject, REALMS_KEY);
        for (JsonElement realmElement : realmArray) {
            this.readRealm(realmElement);
        }
    }

    private void readRealm(JsonElement realmElement) {
        try {
            if (!realmElement.isJsonObject()) {
                throw new JsonParseException("Palm-buddha realm entry must be an object");
            }
            JsonObject realmObject = realmElement.getAsJsonObject();
            UUID ownerId = UUID.fromString(getRequiredString(realmObject, OWNER_ID_KEY));
            ResourceLocation realmId = ResourceLocation.tryParse(
                    getRequiredString(realmObject, ID_KEY)
            );
            if (realmId == null
                    || !ModMain.MOD_ID.equals(realmId.getNamespace())
                    || !realmId.getPath().startsWith(REALM_PATH_PREFIX)) {
                throw new JsonParseException("Palm-buddha realm entry has an invalid id");
            }
            if (this.thisRealmsByOwner.containsKey(ownerId) || this.thisOwnersByRealm.containsKey(realmId)) {
                throw new JsonParseException("Palm-buddha realm entry duplicates an owner or id");
            }

            RealmDefinition realmDefinition = new RealmDefinition(
                    realmId,
                    getRequiredString(realmObject, DISPLAY_NAME_KEY),
                    getRequiredLong(realmObject, SEED_KEY),
                    getRequiredLong(realmObject, CREATED_AT_EPOCH_MILLIS_KEY)
            );
            this.thisRealmsByOwner.put(ownerId, realmDefinition);
            this.thisOwnersByRealm.put(realmId, ownerId);
        } catch (IllegalArgumentException | JsonParseException exception) {
            ModMain.LOGGER.warn("Ignoring malformed palm-buddha-realm entry", exception);
        }
    }

    private JsonObject createJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        JsonArray realmArray = new JsonArray();
        for (Map.Entry<UUID, RealmDefinition> entry : this.thisRealmsByOwner.entrySet()) {
            RealmDefinition realmDefinition = entry.getValue();
            JsonObject realmObject = new JsonObject();
            realmObject.addProperty(OWNER_ID_KEY, entry.getKey().toString());
            realmObject.addProperty(ID_KEY, realmDefinition.getId().toString());
            realmObject.addProperty(DISPLAY_NAME_KEY, realmDefinition.getDisplayName());
            realmObject.addProperty(SEED_KEY, realmDefinition.getSeed());
            realmObject.addProperty(
                    CREATED_AT_EPOCH_MILLIS_KEY,
                    realmDefinition.getCreatedAtEpochMillis()
            );
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
        try {
            return getRequiredNumber(jsonObject, key).getAsInt();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(key + " must be an integer", exception);
        }
    }

    private static long getRequiredLong(JsonObject jsonObject, String key) {
        try {
            return getRequiredNumber(jsonObject, key).getAsLong();
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

