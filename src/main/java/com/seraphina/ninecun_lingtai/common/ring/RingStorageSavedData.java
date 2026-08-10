package com.seraphina.ninecun_lingtai.common.ring;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RingStorageSavedData extends SavedData {
    private static final int LEGACY_LAYOUT_VERSION = 1;
    private static final int CURRENT_LAYOUT_VERSION = 2;
    private static final String DATA_NAME = "ninecun_lingtai_ring_storage";
    private static final String LAYOUT_VERSION_TAG = "LayoutVersion";
    private static final String POCKETS_TAG = "Pockets";
    private static final String RING_ID_TAG = "RingId";
    private static final String POCKET_INDEX_TAG = "PocketIndex";
    private static final String INITIALIZED_TAG = "Initialized";
    private static final String CAMERA_STATE_TAG = "CameraState";
    private static final String CAMERA_X_TAG = "X";
    private static final String CAMERA_Y_TAG = "Y";
    private static final String CAMERA_Z_TAG = "Z";
    private static final String CAMERA_Y_ROT_TAG = "YRot";
    private static final String CAMERA_X_ROT_TAG = "XRot";
    private static final String NEXT_POCKET_INDEX_TAG = "NextPocketIndex";

    private final Map<UUID, PocketData> pockets = new HashMap<>();
    private int nextPocketIndex;
    private int layoutVersion = CURRENT_LAYOUT_VERSION;

    public static RingStorageSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RingStorageSavedData::load,
                RingStorageSavedData::new,
                DATA_NAME
        );
    }

    public static RingStorageSavedData load(CompoundTag tag) {
        RingStorageSavedData savedData = new RingStorageSavedData();
        savedData.nextPocketIndex = tag.getInt(NEXT_POCKET_INDEX_TAG);
        savedData.layoutVersion = tag.contains(LAYOUT_VERSION_TAG, Tag.TAG_INT)
                ? tag.getInt(LAYOUT_VERSION_TAG)
                : LEGACY_LAYOUT_VERSION;
        ListTag pocketTags = tag.getList(POCKETS_TAG, Tag.TAG_COMPOUND);
        for (Tag pocketTag : pocketTags) {
            if (!(pocketTag instanceof CompoundTag pocketDataTag)
                    || !pocketDataTag.hasUUID(RING_ID_TAG)) {
                continue;
            }
            savedData.pockets.put(
                    pocketDataTag.getUUID(RING_ID_TAG),
                    new PocketData(
                            pocketDataTag.getInt(POCKET_INDEX_TAG),
                            pocketDataTag.getBoolean(INITIALIZED_TAG),
                            readCameraState(pocketDataTag)
                    )
            );
        }
        return savedData;
    }

    public int getOrCreatePocketIndex(UUID ringId) {
        PocketData pocketData = this.pockets.get(ringId);
        if (pocketData != null) {
            return pocketData.pocketIndex;
        }
        int pocketIndex = this.nextPocketIndex++;
        this.pockets.put(ringId, new PocketData(pocketIndex, false, null));
        this.setDirty();
        return pocketIndex;
    }

    public boolean isInitialized(UUID ringId) {
        PocketData pocketData = this.pockets.get(ringId);
        return pocketData != null && pocketData.initialized;
    }

    public void markInitialized(UUID ringId) {
        PocketData pocketData = this.pockets.get(ringId);
        if (pocketData == null || pocketData.initialized) {
            return;
        }
        pocketData.initialized = true;
        this.setDirty();
    }

    public Optional<CameraState> getCameraState(UUID ringId) {
        PocketData pocketData = this.pockets.get(ringId);
        return pocketData == null ? Optional.empty() : Optional.ofNullable(pocketData.cameraState);
    }

    public void setCameraState(UUID ringId, CameraState cameraState) {
        PocketData pocketData = this.pockets.get(ringId);
        if (pocketData == null || cameraState.equals(pocketData.cameraState)) {
            return;
        }
        pocketData.cameraState = cameraState;
        this.setDirty();
    }

    public boolean requiresLayoutMigration() {
        return this.layoutVersion < CURRENT_LAYOUT_VERSION;
    }

    public List<Integer> getInitializedPocketIndices() {
        List<Integer> pocketIndices = new ArrayList<>();
        for (PocketData pocketData : this.pockets.values()) {
            if (pocketData.initialized) {
                pocketIndices.add(pocketData.pocketIndex);
            }
        }
        pocketIndices.sort(Integer::compareTo);
        return List.copyOf(pocketIndices);
    }

    public void markLayoutCurrent() {
        if (this.layoutVersion == CURRENT_LAYOUT_VERSION) {
            return;
        }
        this.layoutVersion = CURRENT_LAYOUT_VERSION;
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(NEXT_POCKET_INDEX_TAG, this.nextPocketIndex);
        tag.putInt(LAYOUT_VERSION_TAG, this.layoutVersion);
        ListTag pocketTags = new ListTag();
        for (Map.Entry<UUID, PocketData> entry : this.pockets.entrySet()) {
            CompoundTag pocketTag = new CompoundTag();
            pocketTag.putUUID(RING_ID_TAG, entry.getKey());
            pocketTag.putInt(POCKET_INDEX_TAG, entry.getValue().pocketIndex);
            pocketTag.putBoolean(INITIALIZED_TAG, entry.getValue().initialized);
            writeCameraState(pocketTag, entry.getValue().cameraState);
            pocketTags.add(pocketTag);
        }
        tag.put(POCKETS_TAG, pocketTags);
        return tag;
    }

    private static CameraState readCameraState(CompoundTag pocketDataTag) {
        if (!pocketDataTag.contains(CAMERA_STATE_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag cameraStateTag = pocketDataTag.getCompound(CAMERA_STATE_TAG);
        if (!cameraStateTag.contains(CAMERA_X_TAG, Tag.TAG_ANY_NUMERIC)
                || !cameraStateTag.contains(CAMERA_Y_TAG, Tag.TAG_ANY_NUMERIC)
                || !cameraStateTag.contains(CAMERA_Z_TAG, Tag.TAG_ANY_NUMERIC)
                || !cameraStateTag.contains(CAMERA_Y_ROT_TAG, Tag.TAG_ANY_NUMERIC)
                || !cameraStateTag.contains(CAMERA_X_ROT_TAG, Tag.TAG_ANY_NUMERIC)) {
            return null;
        }

        double x = cameraStateTag.getDouble(CAMERA_X_TAG);
        double y = cameraStateTag.getDouble(CAMERA_Y_TAG);
        double z = cameraStateTag.getDouble(CAMERA_Z_TAG);
        float yRot = cameraStateTag.getFloat(CAMERA_Y_ROT_TAG);
        float xRot = cameraStateTag.getFloat(CAMERA_X_ROT_TAG);
        if (!Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z)
                || !Float.isFinite(yRot)
                || !Float.isFinite(xRot)) {
            return null;
        }
        return new CameraState(new Vec3(x, y, z), yRot, xRot);
    }

    private static void writeCameraState(CompoundTag pocketDataTag, CameraState cameraState) {
        if (cameraState == null) {
            return;
        }

        CompoundTag cameraStateTag = new CompoundTag();
        cameraStateTag.putDouble(CAMERA_X_TAG, cameraState.localPosition.x);
        cameraStateTag.putDouble(CAMERA_Y_TAG, cameraState.localPosition.y);
        cameraStateTag.putDouble(CAMERA_Z_TAG, cameraState.localPosition.z);
        cameraStateTag.putFloat(CAMERA_Y_ROT_TAG, cameraState.yRot);
        cameraStateTag.putFloat(CAMERA_X_ROT_TAG, cameraState.xRot);
        pocketDataTag.put(CAMERA_STATE_TAG, cameraStateTag);
    }

    public record CameraState(Vec3 localPosition, float yRot, float xRot) {
    }

    private static final class PocketData {
        private final int pocketIndex;
        private boolean initialized;
        private CameraState cameraState;

        private PocketData(int pocketIndex, boolean initialized, CameraState cameraState) {
            this.pocketIndex = pocketIndex;
            this.initialized = initialized;
            this.cameraState = cameraState;
        }
    }
}
