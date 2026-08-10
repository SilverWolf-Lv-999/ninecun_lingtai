package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RingStorageUtility {
    public static final ResourceKey<Level> STORAGE_DIMENSION = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "ring_storage")
    );
    public static final int POCKET_SIZE = 80;
    public static final int POCKET_STRIDE = 96;
    public static final int LEGACY_POCKET_SIZE = 5;
    public static final int LEGACY_POCKET_STRIDE = 16;
    public static final int POCKETS_PER_ROW = 4_096;
    public static final int CAMERA_CHUNK_LOAD_RADIUS = 1;
    public static final int FLOOR_Y = 64;
    public static final int MAXIMUM_STORAGE_Y = FLOOR_Y + 15;
    public static final double MINIMUM_CAMERA_Y = FLOOR_Y + 1.25D;
    public static final double MAXIMUM_CAMERA_Y = FLOOR_Y + 14.75D;
    public static final int PROJECTION_HORIZONTAL_RADIUS = 32;
    public static final double CAMERA_SPEED_PER_TICK = 0.35D;

    private static final String RING_ID_TAG = "StorageRingId";

    private RingStorageUtility() {
    }

    public static UUID getOrCreateRingId(ItemStack itemStack) {
        if (itemStack.getOrCreateTag().hasUUID(RING_ID_TAG)) {
            return itemStack.getOrCreateTag().getUUID(RING_ID_TAG);
        }
        UUID ringId = UUID.randomUUID();
        itemStack.getOrCreateTag().putUUID(RING_ID_TAG, ringId);
        return ringId;
    }

    public static StoragePocket getPocket(int pocketIndex) {
        int column = Math.floorMod(pocketIndex, POCKETS_PER_ROW);
        int row = Math.floorDiv(pocketIndex, POCKETS_PER_ROW);
        return new StoragePocket(column * POCKET_STRIDE, row * POCKET_STRIDE, POCKET_SIZE);
    }

    public static StoragePocket getLegacyPocket(int pocketIndex) {
        int column = Math.floorMod(pocketIndex, POCKETS_PER_ROW);
        int row = Math.floorDiv(pocketIndex, POCKETS_PER_ROW);
        return new StoragePocket(
                column * LEGACY_POCKET_STRIDE,
                row * LEGACY_POCKET_STRIDE,
                LEGACY_POCKET_SIZE
        );
    }

    public record StoragePocket(int minimumX, int minimumZ, int size) {
        public int maximumX() {
            return this.minimumX + this.size - 1;
        }

        public int maximumZ() {
            return this.minimumZ + this.size - 1;
        }

        public Vec3 initialCameraPosition() {
            return new Vec3(
                    this.minimumX + this.size * 0.5D,
                    FLOOR_Y + 7.0D,
                    this.minimumZ + this.size * 0.5D
            );
        }

        public boolean contains(BlockPos blockPosition) {
            return blockPosition.getX() >= this.minimumX
                    && blockPosition.getX() <= this.maximumX()
                    && blockPosition.getZ() >= this.minimumZ
                    && blockPosition.getZ() <= this.maximumZ()
                    && blockPosition.getY() >= FLOOR_Y
                    && blockPosition.getY() <= MAXIMUM_STORAGE_Y;
        }

        public BlockPos toWorldPosition(BlockPos localPosition) {
            return new BlockPos(
                    this.minimumX + localPosition.getX(),
                    FLOOR_Y + localPosition.getY(),
                    this.minimumZ + localPosition.getZ()
            );
        }

        public BlockPos getChunkTicketOwner() {
            return new BlockPos(this.minimumX, FLOOR_Y, this.minimumZ);
        }

        public List<ChunkPos> getStorageChunks() {
            int minimumChunkX = (this.minimumX >> 4) - CAMERA_CHUNK_LOAD_RADIUS;
            int maximumChunkX = (this.maximumX() >> 4) + CAMERA_CHUNK_LOAD_RADIUS;
            int minimumChunkZ = (this.minimumZ >> 4) - CAMERA_CHUNK_LOAD_RADIUS;
            int maximumChunkZ = (this.maximumZ() >> 4) + CAMERA_CHUNK_LOAD_RADIUS;
            List<ChunkPos> chunks = new ArrayList<>(
                    (maximumChunkX - minimumChunkX + 1) * (maximumChunkZ - minimumChunkZ + 1)
            );
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                    chunks.add(new ChunkPos(chunkX, chunkZ));
                }
            }
            return List.copyOf(chunks);
        }

        public BlockPos toLocalPosition(BlockPos worldPosition) {
            return new BlockPos(
                    worldPosition.getX() - this.minimumX,
                    worldPosition.getY() - FLOOR_Y,
                    worldPosition.getZ() - this.minimumZ
            );
        }

        public Vec3 toLocalPosition(Vec3 worldPosition) {
            return new Vec3(
                    worldPosition.x - this.minimumX,
                    worldPosition.y - FLOOR_Y,
                    worldPosition.z - this.minimumZ
            );
        }

        public Vec3 toWorldPosition(Vec3 localPosition) {
            return new Vec3(
                    this.minimumX + localPosition.x,
                    FLOOR_Y + localPosition.y,
                    this.minimumZ + localPosition.z
            );
        }

        public Vec3 clampCameraPosition(Vec3 position) {
            return new Vec3(
                    Mth.clamp(position.x, this.minimumX + 0.5D, this.maximumX() + 0.5D),
                    Mth.clamp(position.y, MINIMUM_CAMERA_Y, MAXIMUM_CAMERA_Y),
                    Mth.clamp(position.z, this.minimumZ + 0.5D, this.maximumZ() + 0.5D)
            );
        }
    }
}
