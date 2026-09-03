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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 持久化被收入掌中佛国的玩家及其原世界位置。
 *
 * <p>实体迁移的第一阶段只接入玩家。记录使用玩家 UUID 而不是玩家实例，
 * 因而服务器重启或玩家重新登录后仍能恢复其归属与离开位置。</p>
 */
public final class PalmBuddhaMemberState {
    private static final int FORMAT_VERSION = 1;
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String MEMBERS_KEY = "members";
    private static final String MEMBER_ID_KEY = "memberId";
    private static final String OWNER_ID_KEY = "ownerId";
    private static final String DIMENSION_KEY = "dimension";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String Y_ROT_KEY = "yRot";
    private static final String X_ROT_KEY = "xRot";

    private final Path thisFilePath;
    private final Map<UUID, MemberRecord> thisMembersById = new LinkedHashMap<>();

    private PalmBuddhaMemberState(Path filePath) {
        this.thisFilePath = filePath.toAbsolutePath().normalize();
    }

    public static PalmBuddhaMemberState load(Path filePath) {
        PalmBuddhaMemberState state = new PalmBuddhaMemberState(filePath);
        if (!Files.exists(state.thisFilePath)) {
            return state;
        }

        try {
            state.read(JsonFileUtility.read(state.thisFilePath).getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            ModMain.LOGGER.error(
                    "Unable to read palm-buddha member state from {}",
                    state.thisFilePath,
                    exception
            );
        }
        return state;
    }

    public synchronized Optional<MemberRecord> get(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return Optional.ofNullable(this.thisMembersById.get(memberId));
    }

    public synchronized java.util.List<MemberEntry> getByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return this.thisMembersById.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId().equals(ownerId))
                .map(entry -> new MemberEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized java.util.List<MemberEntry> getAll() {
        return this.thisMembersById.entrySet().stream()
                .map(entry -> new MemberEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized boolean contains(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return this.thisMembersById.containsKey(memberId);
    }

    public synchronized boolean put(UUID memberId, MemberRecord memberRecord) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(memberRecord, "memberRecord");
        if (this.thisMembersById.containsKey(memberId)) {
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
        MemberRecord removedRecord = this.thisMembersById.remove(memberId);
        if (removedRecord == null) {
            return true;
        }
        if (!this.save()) {
            this.thisMembersById.put(memberId, removedRecord);
            ModMain.LOGGER.error(
                    "Unable to persist removal of palm-buddha member {}",
                    memberId
            );
            return false;
        }
        return true;
    }

    private synchronized boolean save() {
        try {
            JsonFileUtility.write(this.thisFilePath, this.createJson());
            return true;
        } catch (IOException exception) {
            ModMain.LOGGER.error(
                    "Unable to save palm-buddha member state to {}",
                    this.thisFilePath,
                    exception
            );
            return false;
        }
    }

    private void read(JsonObject rootObject) {
        int formatVersion = getRequiredInt(rootObject, FORMAT_VERSION_KEY);
        if (formatVersion != FORMAT_VERSION) {
            throw new JsonParseException("Unsupported palm-buddha member state format: " + formatVersion);
        }

        JsonArray members = getRequiredArray(rootObject, MEMBERS_KEY);
        for (JsonElement memberElement : members) {
            if (!memberElement.isJsonObject()) {
                ModMain.LOGGER.warn("Ignoring non-object palm-buddha member entry");
                continue;
            }
            this.readMember(memberElement.getAsJsonObject());
        }
    }

    private void readMember(JsonObject memberObject) {
        try {
            UUID memberId = UUID.fromString(getRequiredString(memberObject, MEMBER_ID_KEY));
            UUID ownerId = UUID.fromString(getRequiredString(memberObject, OWNER_ID_KEY));
            ResourceLocation dimension = ResourceLocation.parse(
                    getRequiredString(memberObject, DIMENSION_KEY)
            );
            MemberRecord memberRecord = new MemberRecord(
                    ownerId,
                    dimension,
                    getRequiredDouble(memberObject, X_KEY),
                    getRequiredDouble(memberObject, Y_KEY),
                    getRequiredDouble(memberObject, Z_KEY),
                    getRequiredFloat(memberObject, Y_ROT_KEY),
                    getRequiredFloat(memberObject, X_ROT_KEY)
            );
            if (this.thisMembersById.putIfAbsent(memberId, memberRecord) != null) {
                ModMain.LOGGER.warn("Ignoring duplicate palm-buddha member {}", memberId);
            }
        } catch (IllegalArgumentException | JsonParseException exception) {
            ModMain.LOGGER.warn("Ignoring malformed palm-buddha member entry", exception);
        }
    }

    private JsonObject createJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        JsonArray members = new JsonArray();
        for (Map.Entry<UUID, MemberRecord> entry : this.thisMembersById.entrySet()) {
            MemberRecord memberRecord = entry.getValue();
            JsonObject memberObject = new JsonObject();
            memberObject.addProperty(MEMBER_ID_KEY, entry.getKey().toString());
            memberObject.addProperty(OWNER_ID_KEY, memberRecord.ownerId().toString());
            memberObject.addProperty(DIMENSION_KEY, memberRecord.dimension().toString());
            memberObject.addProperty(X_KEY, memberRecord.x());
            memberObject.addProperty(Y_KEY, memberRecord.y());
            memberObject.addProperty(Z_KEY, memberRecord.z());
            memberObject.addProperty(Y_ROT_KEY, memberRecord.yRot());
            memberObject.addProperty(X_ROT_KEY, memberRecord.xRot());
            members.add(memberObject);
        }
        rootObject.add(MEMBERS_KEY, members);
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

    public record MemberEntry(
            UUID memberId,
            MemberRecord memberRecord
    ) {
        public MemberEntry {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(memberRecord, "memberRecord");
        }
    }

    public record MemberRecord(
            UUID ownerId,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
        public MemberRecord {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(dimension, "dimension");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Member coordinates must be finite");
            }
            if (!Float.isFinite(yRot) || !Float.isFinite(xRot)) {
                throw new IllegalArgumentException("Member rotations must be finite");
            }
        }
    }
}
