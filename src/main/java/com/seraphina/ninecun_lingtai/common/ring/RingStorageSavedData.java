package com.seraphina.ninecun_lingtai.common.ring;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                            pocketDataTag.getBoolean(INITIALIZED_TAG)
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
        this.pockets.put(ringId, new PocketData(pocketIndex, false));
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
            pocketTags.add(pocketTag);
        }
        tag.put(POCKETS_TAG, pocketTags);
        return tag;
    }

    private static final class PocketData {
        private final int pocketIndex;
        private boolean initialized;

        private PocketData(int pocketIndex, boolean initialized) {
            this.pocketIndex = pocketIndex;
            this.initialized = initialized;
        }
    }
}
