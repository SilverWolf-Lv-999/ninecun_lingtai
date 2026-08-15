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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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

    public List<RealmDefinition> getRealms() {
        return List.copyOf(this.thisRealms.values());
    }

    public Optional<RealmDefinition> getRealm(ResourceLocation realmId) {
        return Optional.ofNullable(this.thisRealms.get(realmId));
    }

    public int getRealmCount() {
        return this.thisRealms.size();
    }

    public RealmDefinition createRealm(Random random, int maximumRealmCount) {
        if (maximumRealmCount < 1 || maximumRealmCount > MAXIMUM_REALM_CAPACITY) {
            throw new IllegalArgumentException(
                    "maximumRealmCount must be between 1 and " + MAXIMUM_REALM_CAPACITY
            );
        }
        if (this.thisRealms.size() >= maximumRealmCount) {
            throw new IllegalStateException("The configured three-thousand-realm limit has been reached");
        }

        Set<String> existingDisplayNames = new HashSet<>();
        for (RealmDefinition realm : this.thisRealms.values()) {
            existingDisplayNames.add(realm.getDisplayName());
        }

        RealmNameUtility.RealmName realmName = RealmNameUtility.createRandomName(existingDisplayNames, random);
        ResourceLocation realmId = ResourceLocation.fromNamespaceAndPath(
                ModMain.MOD_ID,
                realmName.getPathPrefix() + "_" + UUID.randomUUID()
        );
        RealmDefinition realmDefinition = new RealmDefinition(
                realmId,
                realmName.getDisplayName(),
                random.nextLong(),
                System.currentTimeMillis()
        );
        this.thisRealms.put(realmId, realmDefinition);
        if (!this.save()) {
            this.thisRealms.remove(realmId);
            throw new IllegalStateException("Unable to persist the three-thousand-realm state");
        }
        return realmDefinition;
    }

    public boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error("Unable to save three-thousand-realm state to {}", this.thisFilePath, exception);
            return false;
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
