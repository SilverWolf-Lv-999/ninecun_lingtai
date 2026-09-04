package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 持久化被收入掌中佛国的普通生物实体。
 *
 * <p>实体仍由 Minecraft 的真实维度与实体存档系统负责运行；这里仅保存
 * 归属、原世界位置、佛国内最后已知位置以及一份有界的恢复 NBT，供服务器
 * 重启或异常迁移时恢复同一个真实实体。</p>
 */
public final class PalmBuddhaEntityState {
    public static final int MAXIMUM_ENTITY_MEMBER_COUNT = 256;
    public static final int MAXIMUM_NBT_STRING_LENGTH = 262_144;

    private static final int FORMAT_VERSION = 1;
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String MEMBERS_KEY = "members";
    private static final String MEMBER_ID_KEY = "memberId";
    private static final String OWNER_ID_KEY = "ownerId";
    private static final String ENTITY_TYPE_KEY = "entityType";
    private static final String ORIGINAL_DIMENSION_KEY = "originalDimension";
    private static final String ORIGINAL_X_KEY = "originalX";
    private static final String ORIGINAL_Y_KEY = "originalY";
    private static final String ORIGINAL_Z_KEY = "originalZ";
    private static final String ORIGINAL_Y_ROT_KEY = "originalYRot";
    private static final String ORIGINAL_X_ROT_KEY = "originalXRot";
    private static final String CURRENT_X_KEY = "currentX";
    private static final String CURRENT_Y_KEY = "currentY";
    private static final String CURRENT_Z_KEY = "currentZ";
    private static final String CURRENT_Y_ROT_KEY = "currentYRot";
    private static final String CURRENT_X_ROT_KEY = "currentXRot";
    private static final String NBT_KEY = "nbt";

    private final Path thisFilePath;
    private final Map<UUID, EntityRecord> thisMembersById = new LinkedHashMap<>();

    private PalmBuddhaEntityState(Path filePath) {
        this.thisFilePath = filePath.toAbsolutePath().normalize();
    }

    public static PalmBuddhaEntityState load(Path filePath) {
        PalmBuddhaEntityState state = new PalmBuddhaEntityState(filePath);
        if (!Files.exists(state.thisFilePath)) {
            return state;
        }

        try {
            state.read(JsonFileUtility.read(state.thisFilePath));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.error(
                    "Unable to read palm-buddha entity state from {}",
                    state.thisFilePath,
                    exception
            );
        }
        return state;
    }

    public synchronized Optional<EntityRecord> get(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return Optional.ofNullable(this.thisMembersById.get(memberId));
    }

    public synchronized List<EntityEntry> getByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return this.thisMembersById.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId().equals(ownerId))
                .map(entry -> new EntityEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized boolean contains(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return this.thisMembersById.containsKey(memberId);
    }

    public synchronized boolean put(UUID memberId, EntityRecord memberRecord) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(memberRecord, "memberRecord");
        if (this.thisMembersById.containsKey(memberId)
                || this.thisMembersById.size() >= MAXIMUM_ENTITY_MEMBER_COUNT) {
            return false;
        }
        this.thisMembersById.put(memberId, memberRecord);
        if (!this.save()) {
            this.thisMembersById.remove(memberId);
            return false;
        }
        return true;
    }

    public synchronized boolean remove(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        EntityRecord removedRecord = this.thisMembersById.remove(memberId);
        if (removedRecord == null) {
            return true;
        }
        if (!this.save()) {
            this.thisMembersById.put(memberId, removedRecord);
            ModMain.LOGGER.error(
                    "Unable to persist removal of palm-buddha entity {}",
                    memberId
            );
            return false;
        }
        return true;
    }

    public synchronized boolean updateCurrentState(
            UUID memberId,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
        Objects.requireNonNull(memberId, "memberId");
        EntityRecord oldRecord = this.thisMembersById.get(memberId);
        if (oldRecord == null) {
            return false;
        }
        EntityRecord updatedRecord = oldRecord.withCurrentState(x, y, z, yRot, xRot);
        this.thisMembersById.put(memberId, updatedRecord);
        if (!this.save()) {
            this.thisMembersById.put(memberId, oldRecord);
            return false;
        }
        return true;
    }

    public synchronized List<EntityEntry> getAll() {
        return this.thisMembersById.entrySet().stream()
                .map(entry -> new EntityEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    private synchronized boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error(
                    "Unable to save palm-buddha entity state to {}",
                    this.thisFilePath,
                    exception
            );
            return false;
        }
    }

    private void read(JsonElement rootElement) {
        if (rootElement == null || !rootElement.isJsonObject()) {
            throw new JsonParseException("Palm-buddha entity state root must be an object");
        }
        JsonObject rootObject = rootElement.getAsJsonObject();
        int formatVersion = getRequiredInt(rootObject, FORMAT_VERSION_KEY);
        if (formatVersion != FORMAT_VERSION) {
            throw new JsonParseException("Unsupported palm-buddha entity state format: " + formatVersion);
        }
        JsonArray memberArray = getRequiredArray(rootObject, MEMBERS_KEY);
        for (JsonElement memberElement : memberArray) {
            this.readMember(memberElement);
            if (this.thisMembersById.size() >= MAXIMUM_ENTITY_MEMBER_COUNT) {
                break;
            }
        }
    }

    private void readMember(JsonElement memberElement) {
        try {
            if (!memberElement.isJsonObject()) {
                throw new JsonParseException("Palm-buddha entity entry must be an object");
            }
            JsonObject memberObject = memberElement.getAsJsonObject();
            UUID memberId = UUID.fromString(getRequiredString(memberObject, MEMBER_ID_KEY));
            EntityRecord memberRecord = new EntityRecord(
                    UUID.fromString(getRequiredString(memberObject, OWNER_ID_KEY)),
                    parseResourceLocation(memberObject, ENTITY_TYPE_KEY),
                    parseResourceLocation(memberObject, ORIGINAL_DIMENSION_KEY),
                    getRequiredDouble(memberObject, ORIGINAL_X_KEY),
                    getRequiredDouble(memberObject, ORIGINAL_Y_KEY),
                    getRequiredDouble(memberObject, ORIGINAL_Z_KEY),
                    getRequiredFloat(memberObject, ORIGINAL_Y_ROT_KEY),
                    getRequiredFloat(memberObject, ORIGINAL_X_ROT_KEY),
                    getRequiredDouble(memberObject, CURRENT_X_KEY),
                    getRequiredDouble(memberObject, CURRENT_Y_KEY),
                    getRequiredDouble(memberObject, CURRENT_Z_KEY),
                    getRequiredFloat(memberObject, CURRENT_Y_ROT_KEY),
                    getRequiredFloat(memberObject, CURRENT_X_ROT_KEY),
                    getRequiredString(memberObject, NBT_KEY)
            );
            if (this.thisMembersById.putIfAbsent(memberId, memberRecord) != null) {
                ModMain.LOGGER.warn("Ignoring duplicate palm-buddha entity {}", memberId);
            }
        } catch (IllegalArgumentException | JsonParseException exception) {
            ModMain.LOGGER.warn("Ignoring malformed palm-buddha entity entry", exception);
        }
    }

    private JsonObject createJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        JsonArray members = new JsonArray();
        for (Map.Entry<UUID, EntityRecord> entry : this.thisMembersById.entrySet()) {
            EntityRecord record = entry.getValue();
            JsonObject memberObject = new JsonObject();
            memberObject.addProperty(MEMBER_ID_KEY, entry.getKey().toString());
            memberObject.addProperty(OWNER_ID_KEY, record.ownerId().toString());
            memberObject.addProperty(ENTITY_TYPE_KEY, record.entityType().toString());
            memberObject.addProperty(ORIGINAL_DIMENSION_KEY, record.originalDimension().toString());
            memberObject.addProperty(ORIGINAL_X_KEY, record.originalX());
            memberObject.addProperty(ORIGINAL_Y_KEY, record.originalY());
            memberObject.addProperty(ORIGINAL_Z_KEY, record.originalZ());
            memberObject.addProperty(ORIGINAL_Y_ROT_KEY, record.originalYRot());
            memberObject.addProperty(ORIGINAL_X_ROT_KEY, record.originalXRot());
            memberObject.addProperty(CURRENT_X_KEY, record.currentX());
            memberObject.addProperty(CURRENT_Y_KEY, record.currentY());
            memberObject.addProperty(CURRENT_Z_KEY, record.currentZ());
            memberObject.addProperty(CURRENT_Y_ROT_KEY, record.currentYRot());
            memberObject.addProperty(CURRENT_X_ROT_KEY, record.currentXRot());
            memberObject.addProperty(NBT_KEY, record.nbt());
            members.add(memberObject);
        }
        rootObject.add(MEMBERS_KEY, members);
        return rootObject;
    }

    private static ResourceLocation parseResourceLocation(JsonObject jsonObject, String key) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(getRequiredString(jsonObject, key));
        if (resourceLocation == null) {
            throw new JsonParseException(key + " must be a resource location");
        }
        return resourceLocation;
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
        String value = element.getAsString();
        if (value.isEmpty() || (NBT_KEY.equals(key) && value.length() > MAXIMUM_NBT_STRING_LENGTH)) {
            throw new JsonParseException(key + " is empty or exceeds its limit");
        }
        return value;
    }

    public record EntityEntry(UUID memberId, EntityRecord entityRecord) {
        public EntityEntry {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(entityRecord, "entityRecord");
        }
    }

    public record EntityRecord(
            UUID ownerId,
            ResourceLocation entityType,
            ResourceLocation originalDimension,
            double originalX,
            double originalY,
            double originalZ,
            float originalYRot,
            float originalXRot,
            double currentX,
            double currentY,
            double currentZ,
            float currentYRot,
            float currentXRot,
            String nbt
    ) {
        public EntityRecord {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(entityType, "entityType");
            Objects.requireNonNull(originalDimension, "originalDimension");
            nbt = Objects.requireNonNull(nbt, "nbt");
            if (nbt.isEmpty() || nbt.length() > MAXIMUM_NBT_STRING_LENGTH) {
                throw new IllegalArgumentException("Entity NBT exceeds the palm-buddha limit");
            }
            validatePosition(originalX, originalY, originalZ, originalYRot, originalXRot);
            validatePosition(currentX, currentY, currentZ, currentYRot, currentXRot);
        }

        public EntityRecord withCurrentState(
                double x,
                double y,
                double z,
                float yRot,
                float xRot
        ) {
            return new EntityRecord(
                    this.ownerId,
                    this.entityType,
                    this.originalDimension,
                    this.originalX,
                    this.originalY,
                    this.originalZ,
                    this.originalYRot,
                    this.originalXRot,
                    x,
                    y,
                    z,
                    yRot,
                    xRot,
                    this.nbt
            );
        }

        private static void validatePosition(
                double x,
                double y,
                double z,
                float yRot,
                float xRot
        ) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Entity coordinates must be finite");
            }
            if (!Float.isFinite(yRot) || !Float.isFinite(xRot)) {
                throw new IllegalArgumentException("Entity rotations must be finite");
            }
        }
    }
}
