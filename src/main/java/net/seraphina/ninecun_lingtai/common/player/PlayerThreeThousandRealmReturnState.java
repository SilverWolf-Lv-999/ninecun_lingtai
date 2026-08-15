package net.seraphina.ninecun_lingtai.common.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerThreeThousandRealmReturnState {
    private static final int FORMAT_VERSION = 1;
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String RETURN_POSITIONS_KEY = "returnPositions";
    private static final String PLAYER_ID_KEY = "playerId";
    private static final String REALM_ID_KEY = "realmId";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String Y_ROT_KEY = "yRot";
    private static final String X_ROT_KEY = "xRot";

    private final Path thisFilePath;
    private final Map<UUID, PlayerThreeThousandRealmReturnPosition> thisReturnPositions =
            new LinkedHashMap<>();

    private PlayerThreeThousandRealmReturnState(Path filePath) {
        this.thisFilePath = Objects.requireNonNull(filePath, "filePath").toAbsolutePath().normalize();
    }

    public static PlayerThreeThousandRealmReturnState load(Path filePath) {
        PlayerThreeThousandRealmReturnState state = new PlayerThreeThousandRealmReturnState(filePath);
        if (!Files.exists(state.thisFilePath)) {
            return state;
        }

        try {
            state.read(JsonFileUtility.read(state.thisFilePath));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.error(
                    "Unable to read three-thousand-realm player return state from {}",
                    state.thisFilePath,
                    exception
            );
        }
        return state;
    }

    public Optional<PlayerThreeThousandRealmReturnPosition> getReturnPosition(UUID playerId) {
        return Optional.ofNullable(this.thisReturnPositions.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public boolean putReturnPosition(UUID playerId, PlayerThreeThousandRealmReturnPosition returnPosition) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(returnPosition, "returnPosition");

        PlayerThreeThousandRealmReturnPosition previousPosition = this.thisReturnPositions.put(
                playerId,
                returnPosition
        );
        if (this.save()) {
            return true;
        }

        if (previousPosition == null) {
            this.thisReturnPositions.remove(playerId);
        } else {
            this.thisReturnPositions.put(playerId, previousPosition);
        }
        return false;
    }

    public boolean removeReturnPosition(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerThreeThousandRealmReturnPosition previousPosition = this.thisReturnPositions.remove(playerId);
        if (previousPosition == null || this.save()) {
            return true;
        }

        this.thisReturnPositions.put(playerId, previousPosition);
        return false;
    }

    private boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error(
                    "Unable to save three-thousand-realm player return state to {}",
                    this.thisFilePath,
                    exception
            );
            return false;
        }
    }

    private void read(JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            throw new JsonParseException("Three-thousand-realm player return state root must be an object");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();
        if (getRequiredInt(rootObject, FORMAT_VERSION_KEY) != FORMAT_VERSION) {
            throw new JsonParseException("Unsupported three-thousand-realm player return state format");
        }

        for (JsonElement returnPositionElement : getRequiredArray(rootObject, RETURN_POSITIONS_KEY)) {
            this.readReturnPosition(returnPositionElement);
        }
    }

    private void readReturnPosition(JsonElement returnPositionElement) {
        try {
            if (!returnPositionElement.isJsonObject()) {
                throw new JsonParseException("Player return position must be an object");
            }

            JsonObject returnPositionObject = returnPositionElement.getAsJsonObject();
            UUID playerId = UUID.fromString(getRequiredString(returnPositionObject, PLAYER_ID_KEY));
            ResourceLocation realmId = ResourceLocation.tryParse(
                    getRequiredString(returnPositionObject, REALM_ID_KEY)
            );
            if (realmId == null || !ModMain.MOD_ID.equals(realmId.getNamespace())) {
                throw new JsonParseException("Player return position has an invalid realm id");
            }
            if (this.thisReturnPositions.containsKey(playerId)) {
                throw new JsonParseException("Player return position duplicates an existing player id");
            }

            this.thisReturnPositions.put(
                    playerId,
                    new PlayerThreeThousandRealmReturnPosition(
                            realmId,
                            getRequiredDouble(returnPositionObject, X_KEY),
                            getRequiredDouble(returnPositionObject, Y_KEY),
                            getRequiredDouble(returnPositionObject, Z_KEY),
                            getRequiredFloat(returnPositionObject, Y_ROT_KEY),
                            getRequiredFloat(returnPositionObject, X_ROT_KEY)
                    )
            );
        } catch (IllegalArgumentException | JsonParseException exception) {
            ModMain.LOGGER.warn("Ignoring malformed three-thousand-realm player return position", exception);
        }
    }

    private JsonObject createJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        JsonArray returnPositionArray = new JsonArray();
        for (Map.Entry<UUID, PlayerThreeThousandRealmReturnPosition> entry :
                this.thisReturnPositions.entrySet()) {
            PlayerThreeThousandRealmReturnPosition returnPosition = entry.getValue();
            JsonObject returnPositionObject = new JsonObject();
            returnPositionObject.addProperty(PLAYER_ID_KEY, entry.getKey().toString());
            returnPositionObject.addProperty(REALM_ID_KEY, returnPosition.getRealmId().toString());
            returnPositionObject.addProperty(X_KEY, returnPosition.getX());
            returnPositionObject.addProperty(Y_KEY, returnPosition.getY());
            returnPositionObject.addProperty(Z_KEY, returnPosition.getZ());
            returnPositionObject.addProperty(Y_ROT_KEY, returnPosition.getYRot());
            returnPositionObject.addProperty(X_ROT_KEY, returnPosition.getXRot());
            returnPositionArray.add(returnPositionObject);
        }
        rootObject.add(RETURN_POSITIONS_KEY, returnPositionArray);
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

    private static double getRequiredDouble(JsonObject jsonObject, String key) {
        try {
            return getRequiredNumber(jsonObject, key).getAsDouble();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(key + " must be a double", exception);
        }
    }

    private static float getRequiredFloat(JsonObject jsonObject, String key) {
        try {
            return getRequiredNumber(jsonObject, key).getAsFloat();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(key + " must be a float", exception);
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
