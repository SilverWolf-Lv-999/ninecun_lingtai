package com.seraphina.ninecun_lingtai.common.ring;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.menu.StorageRingMenu;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncStorageRingProjectionPacket;
import com.seraphina.ninecun_lingtai.utility.RingStorageUtility;
import com.seraphina.ninecun_lingtai.utility.RingStorageUtility.StoragePocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RingStorageManager {
    private static final String LEGACY_SESSION_TAG = "NinecunLingtaiStorageRingSession";

    private static final float INITIAL_CAMERA_Y_ROT = 45.0F;
    private static final float INITIAL_CAMERA_X_ROT = 65.0F;
    private static final double MAXIMUM_INTERACTION_DISTANCE_SQUARED = 64.0D;
    private static final double HIT_POSITION_EPSILON = 0.01D;
    private static final long PROJECTION_REFRESH_INTERVAL_TICKS = 10L;
    private static final int PROJECTION_HORIZONTAL_RADIUS = 32;
    private static final int MAXIMUM_PROJECTION_BLOCKS = 8_192;
    private static final double PROJECTION_REFRESH_DISTANCE_SQUARED = 64.0D;
    private static final Map<UUID, StorageRingSession> ACTIVE_SESSIONS = new HashMap<>();
    private static final Map<StoragePocket, Integer> POCKET_CHUNK_TICKET_REFERENCES = new HashMap<>();

    public static void open(ServerPlayer player, ItemStack ringStack) {
        UUID ringId = RingStorageUtility.getOrCreateRingId(ringStack);
        StorageRingSession activeSession = ACTIVE_SESSIONS.get(player.getUUID());
        if (activeSession != null) {
            if (activeSession.ringId.equals(ringId)) {
                openMenu(player, activeSession);
                sendProjection(player, activeSession);
                return;
            }
            close(player);
        }

        ServerLevel storageLevel = player.server.getLevel(RingStorageUtility.STORAGE_DIMENSION);
        if (storageLevel == null) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.storage_ring.unavailable"),
                    true
            );
            return;
        }

        RingStorageSavedData savedData = RingStorageSavedData.get(player.server);
        migrateLegacyPockets(storageLevel, savedData);
        int pocketIndex = savedData.getOrCreatePocketIndex(ringId);
        StoragePocket pocket = RingStorageUtility.getPocket(pocketIndex);
        acquirePocketChunkTickets(storageLevel, pocket);
        if (!savedData.isInitialized(ringId)) {
            initializePocket(storageLevel, pocket);
            savedData.markInitialized(ringId);
        }

        StorageRingSession session = new StorageRingSession(
                ringId,
                pocket,
                pocket.initialCameraPosition(),
                INITIAL_CAMERA_Y_ROT,
                INITIAL_CAMERA_X_ROT
        );
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        openMenu(player, session);
        sendProjection(player, session);
    }

    public static boolean isSessionActive(Player player, UUID ringId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        StorageRingSession session = ACTIVE_SESSIONS.get(serverPlayer.getUUID());
        return session != null && session.ringId.equals(ringId);
    }

    public static boolean isStorageSessionActive(ServerPlayer player) {
        return ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    public static void onRingMenuRemoved(Player player, UUID ringId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        StorageRingSession session = ACTIVE_SESSIONS.get(serverPlayer.getUUID());
        if (session != null && session.ringId.equals(ringId)) {
            session.reopenMenuAtTick = serverPlayer.server.getTickCount() + 1L;
        }
    }

    public static void updateCamera(
            ServerPlayer player,
            float leftImpulse,
            float forwardImpulse,
            boolean movingUp,
            boolean movingDown,
            float yRot,
            float xRot
    ) {
        StorageRingSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null
                || !Float.isFinite(leftImpulse)
                || !Float.isFinite(forwardImpulse)
                || !Float.isFinite(yRot)
                || !Float.isFinite(xRot)) {
            return;
        }

        session.leftImpulse = Mth.clamp(leftImpulse, -1.0F, 1.0F);
        session.forwardImpulse = Mth.clamp(forwardImpulse, -1.0F, 1.0F);
        session.movingUp = movingUp;
        session.movingDown = movingDown;
        session.cameraYRot = Mth.wrapDegrees(yRot);
        session.cameraXRot = Mth.clamp(xRot, -75.0F, 75.0F);
        session.lastCameraUpdateTick = player.server.getTickCount();
    }

    public static void interact(
            ServerPlayer player,
            BlockPos localPosition,
            Direction direction,
            Vec3 localHitLocation,
            boolean use
    ) {
        StorageRingSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null
                || !(player.containerMenu instanceof StorageRingMenu menu)
                || !menu.getRingId().equals(session.ringId)
                || !isValidLocalPosition(localPosition)
                || !isValidLocalHit(localPosition, direction, localHitLocation)) {
            return;
        }

        ServerLevel storageLevel = player.server.getLevel(RingStorageUtility.STORAGE_DIMENSION);
        if (storageLevel == null) {
            return;
        }
        BlockPos worldPosition = session.pocket.toWorldPosition(localPosition);
        Vec3 worldHitLocation = session.pocket.toWorldPosition(localHitLocation);
        if (!session.pocket.contains(worldPosition)
                || session.cameraPosition.distanceToSqr(worldHitLocation) > MAXIMUM_INTERACTION_DISTANCE_SQUARED) {
            return;
        }

        if (use) {
            useBlock(player, menu, storageLevel, worldPosition, direction, worldHitLocation);
        } else {
            destroyBlock(player, storageLevel, worldPosition);
        }
        sendProjection(player, session);
    }

    public static void tick(ServerPlayer player) {
        StorageRingSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        long currentTick = player.server.getTickCount();
        if (currentTick - session.lastCameraUpdateTick > 5L) {
            session.clearInput();
        }
        if (session.hasMovementInput()) {
            double yRotRadians = Math.toRadians(session.cameraYRot);
            Vec3 forward = new Vec3(-Math.sin(yRotRadians), 0.0D, Math.cos(yRotRadians));
            Vec3 right = new Vec3(Math.cos(yRotRadians), 0.0D, Math.sin(yRotRadians));
            double verticalMovement = (session.movingUp ? 1.0D : 0.0D) - (session.movingDown ? 1.0D : 0.0D);
            Vec3 movement = forward.scale(session.forwardImpulse)
                    .add(right.scale(session.leftImpulse))
                    .add(0.0D, verticalMovement, 0.0D)
                    .scale(RingStorageUtility.CAMERA_SPEED);
            Vec3 nextPosition = session.pocket.clampCameraPosition(session.cameraPosition.add(movement));
            if (session.cameraPosition.distanceToSqr(nextPosition) > 0.000001D) {
                session.cameraPosition = nextPosition;
                ModNetwork.sendStorageRingCamera(
                        player,
                        session.ringId,
                        session.getLocalCameraPosition(),
                        session.cameraYRot,
                        session.cameraXRot
                );
            }
        }

        if (session.reopenMenuAtTick >= 0L
                && currentTick >= session.reopenMenuAtTick
                && player.containerMenu == player.inventoryMenu) {
            session.reopenMenuAtTick = -1L;
            openMenu(player, session);
        }

        if (session.requiresProjectionRefresh(currentTick)) {
            sendProjection(player, session);
        }
    }

    public static void close(ServerPlayer player) {
        StorageRingSession session = ACTIVE_SESSIONS.remove(player.getUUID());
        if (session == null) {
            clearLegacyPersistentSession(player);
            return;
        }

        releasePocketChunkTickets(player.server, session.pocket);
        ModNetwork.sendStorageRingProjectionInactive(player);
        player.closeContainer();
    }

    public static void restorePersistentSession(ServerPlayer player) {
        clearLegacyPersistentSession(player);
    }

    public static void releaseAllChunkTickets(MinecraftServer server) {
        ServerLevel storageLevel = server.getLevel(RingStorageUtility.STORAGE_DIMENSION);
        if (storageLevel != null) {
            for (StoragePocket pocket : POCKET_CHUNK_TICKET_REFERENCES.keySet()) {
                setPocketChunkTickets(storageLevel, pocket, false);
            }
        }
        POCKET_CHUNK_TICKET_REFERENCES.clear();
        ACTIVE_SESSIONS.clear();
    }

    private static void useBlock(
            ServerPlayer player,
            StorageRingMenu menu,
            ServerLevel storageLevel,
            BlockPos worldPosition,
            Direction direction,
            Vec3 worldHitLocation
    ) {
        BlockState blockState = storageLevel.getBlockState(worldPosition);
        if (blockState.isAir()) {
            return;
        }
        BlockHitResult hitResult = new BlockHitResult(
                worldHitLocation,
                direction,
                worldPosition,
                false
        );
        ItemStack carriedStack = menu.getCarried();
        InteractionResult blockResult = blockState.use(
                storageLevel,
                player,
                InteractionHand.MAIN_HAND,
                hitResult
        );
        if (carriedStack.isEmpty() || blockResult.consumesAction()) {
            menu.broadcastChanges();
            return;
        }

        carriedStack.useOn(new UseOnContext(
                storageLevel,
                player,
                InteractionHand.MAIN_HAND,
                carriedStack,
                hitResult
        ));
        menu.setCarried(carriedStack);
        menu.broadcastChanges();
    }

    private static void destroyBlock(ServerPlayer player, ServerLevel storageLevel, BlockPos worldPosition) {
        BlockState blockState = storageLevel.getBlockState(worldPosition);
        if (blockState.isAir()) {
            return;
        }

        blockState.getBlock().playerWillDestroy(storageLevel, worldPosition, blockState, player);
        BlockEntity blockEntity = storageLevel.getBlockEntity(worldPosition);
        List<ItemStack> drops = player.getAbilities().instabuild
                ? List.of()
                : Block.getDrops(
                        blockState,
                        storageLevel,
                        worldPosition,
                        blockEntity,
                        player,
                        player.getMainHandItem()
                );
        storageLevel.removeBlock(worldPosition, false);
        if (!player.getAbilities().instabuild) {
            player.getMainHandItem().mineBlock(storageLevel, blockState, worldPosition, player);
        }
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
    }

    private static void openMenu(ServerPlayer player, StorageRingSession session) {
        if (player.containerMenu instanceof StorageRingMenu menu && menu.getRingId().equals(session.ringId)) {
            return;
        }
        NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new StorageRingMenu(
                                containerId,
                                playerInventory,
                                session.ringId
                        ),
                        Component.translatable("screen.ninecun_lingtai.storage_ring.title")
                ),
                buffer -> buffer.writeUUID(session.ringId)
        );
    }

    private static void sendProjection(ServerPlayer player, StorageRingSession session) {
        ServerLevel storageLevel = player.server.getLevel(RingStorageUtility.STORAGE_DIMENSION);
        if (storageLevel == null) {
            return;
        }

        List<SyncStorageRingProjectionPacket.BlockSnapshot> snapshots = new ArrayList<>();
        Vec3 localCameraPosition = session.getLocalCameraPosition();
        int minimumX = Math.max(0, Mth.floor(localCameraPosition.x) - PROJECTION_HORIZONTAL_RADIUS);
        int maximumX = Math.min(
                RingStorageUtility.POCKET_SIZE - 1,
                Mth.floor(localCameraPosition.x) + PROJECTION_HORIZONTAL_RADIUS
        );
        int minimumZ = Math.max(0, Mth.floor(localCameraPosition.z) - PROJECTION_HORIZONTAL_RADIUS);
        int maximumZ = Math.min(
                RingStorageUtility.POCKET_SIZE - 1,
                Mth.floor(localCameraPosition.z) + PROJECTION_HORIZONTAL_RADIUS
        );
        snapshotLoop:
        for (int y = 0; y <= RingStorageUtility.MAXIMUM_STORAGE_Y - RingStorageUtility.FLOOR_Y; y++) {
            for (int x = minimumX; x <= maximumX; x++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    BlockPos localPosition = new BlockPos(x, y, z);
                    BlockState blockState = storageLevel.getBlockState(session.pocket.toWorldPosition(localPosition));
                    if (!blockState.isAir()) {
                        snapshots.add(new SyncStorageRingProjectionPacket.BlockSnapshot(
                                localPosition,
                                Block.getId(blockState)
                        ));
                        if (snapshots.size() >= MAXIMUM_PROJECTION_BLOCKS) {
                            break snapshotLoop;
                        }
                    }
                }
            }
        }
        ModNetwork.sendStorageRingProjection(
                player,
                session.ringId,
                localCameraPosition,
                session.cameraYRot,
                session.cameraXRot,
                snapshots
        );
        session.lastProjectionTick = player.server.getTickCount();
        session.lastProjectionPosition = session.cameraPosition;
    }

    private static void acquirePocketChunkTickets(ServerLevel storageLevel, StoragePocket pocket) {
        int referenceCount = POCKET_CHUNK_TICKET_REFERENCES.getOrDefault(pocket, 0);
        if (referenceCount == 0) {
            setPocketChunkTickets(storageLevel, pocket, true);
        }
        POCKET_CHUNK_TICKET_REFERENCES.put(pocket, referenceCount + 1);
    }

    private static void releasePocketChunkTickets(MinecraftServer server, StoragePocket pocket) {
        int referenceCount = POCKET_CHUNK_TICKET_REFERENCES.getOrDefault(pocket, 0);
        if (referenceCount <= 1) {
            POCKET_CHUNK_TICKET_REFERENCES.remove(pocket);
            ServerLevel storageLevel = server.getLevel(RingStorageUtility.STORAGE_DIMENSION);
            if (storageLevel != null) {
                setPocketChunkTickets(storageLevel, pocket, false);
            }
            return;
        }
        POCKET_CHUNK_TICKET_REFERENCES.put(pocket, referenceCount - 1);
    }

    private static void setPocketChunkTickets(ServerLevel storageLevel, StoragePocket pocket, boolean add) {
        BlockPos ticketOwner = pocket.getChunkTicketOwner();
        for (ChunkPos chunkPosition : pocket.getStorageChunks()) {
            ForgeChunkManager.forceChunk(
                    storageLevel,
                    ModMain.MOD_ID,
                    ticketOwner,
                    chunkPosition.x,
                    chunkPosition.z,
                    add,
                    true
            );
            if (add) {
                storageLevel.getChunk(chunkPosition.x, chunkPosition.z);
            }
        }
    }

    private static void migrateLegacyPockets(ServerLevel level, RingStorageSavedData savedData) {
        if (!savedData.requiresLayoutMigration()) {
            return;
        }

        List<LegacyPocketSnapshot> snapshots = new ArrayList<>();
        for (int pocketIndex : savedData.getInitializedPocketIndices()) {
            snapshots.add(captureLegacyPocket(level, pocketIndex));
        }

        for (LegacyPocketSnapshot snapshot : snapshots) {
            StoragePocket pocket = RingStorageUtility.getPocket(snapshot.pocketIndex());
            clearPocketInterior(level, pocket);
            initializePocket(level, pocket);
        }
        for (LegacyPocketSnapshot snapshot : snapshots) {
            restoreLegacyPocket(level, snapshot);
        }
        savedData.markLayoutCurrent();
    }

    private static LegacyPocketSnapshot captureLegacyPocket(ServerLevel level, int pocketIndex) {
        StoragePocket legacyPocket = RingStorageUtility.getLegacyPocket(pocketIndex);
        List<LegacyBlockSnapshot> blocks = new ArrayList<>();
        for (int x = 0; x < legacyPocket.size(); x++) {
            for (int y = 0; y <= RingStorageUtility.MAXIMUM_STORAGE_Y - RingStorageUtility.FLOOR_Y; y++) {
                for (int z = 0; z < legacyPocket.size(); z++) {
                    BlockPos localPosition = new BlockPos(x, y, z);
                    BlockPos worldPosition = legacyPocket.toWorldPosition(localPosition);
                    BlockState blockState = level.getBlockState(worldPosition);
                    if (blockState.isAir()) {
                        continue;
                    }

                    BlockEntity blockEntity = level.getBlockEntity(worldPosition);
                    CompoundTag blockEntityTag = blockEntity == null
                            ? null
                            : blockEntity.saveWithFullMetadata();
                    blocks.add(new LegacyBlockSnapshot(
                            localPosition,
                            blockState,
                            blockEntityTag
                    ));
                }
            }
        }
        return new LegacyPocketSnapshot(pocketIndex, List.copyOf(blocks));
    }

    private static void restoreLegacyPocket(ServerLevel level, LegacyPocketSnapshot snapshot) {
        StoragePocket pocket = RingStorageUtility.getPocket(snapshot.pocketIndex());
        for (LegacyBlockSnapshot block : snapshot.blocks()) {
            BlockPos worldPosition = pocket.toWorldPosition(block.localPosition());
            level.setBlock(worldPosition, block.blockState(), Block.UPDATE_CLIENTS);
            if (block.blockEntityTag() == null) {
                continue;
            }

            CompoundTag blockEntityTag = block.blockEntityTag().copy();
            blockEntityTag.putInt("x", worldPosition.getX());
            blockEntityTag.putInt("y", worldPosition.getY());
            blockEntityTag.putInt("z", worldPosition.getZ());
            BlockEntity blockEntity = BlockEntity.loadStatic(
                    worldPosition,
                    block.blockState(),
                    blockEntityTag
            );
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
    }

    private static void clearPocketInterior(ServerLevel level, StoragePocket pocket) {
        for (int x = pocket.minimumX(); x <= pocket.maximumX(); x++) {
            for (int y = RingStorageUtility.FLOOR_Y + 1; y <= RingStorageUtility.MAXIMUM_STORAGE_Y; y++) {
                for (int z = pocket.minimumZ(); z <= pocket.maximumZ(); z++) {
                    BlockPos worldPosition = new BlockPos(x, y, z);
                    if (!level.getBlockState(worldPosition).isAir()) {
                        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void initializePocket(ServerLevel level, StoragePocket pocket) {
        for (int x = pocket.minimumX(); x <= pocket.maximumX(); x++) {
            for (int z = pocket.minimumZ(); z <= pocket.maximumZ(); z++) {
                level.setBlock(
                        new BlockPos(x, RingStorageUtility.FLOOR_Y, z),
                        Blocks.SMOOTH_STONE.defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );
                level.setBlock(
                        new BlockPos(x, RingStorageUtility.MAXIMUM_STORAGE_Y + 1, z),
                        Blocks.BARRIER.defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );
            }
        }

        for (int x = pocket.minimumX() - 1; x <= pocket.maximumX() + 1; x++) {
            for (int y = RingStorageUtility.FLOOR_Y; y <= RingStorageUtility.MAXIMUM_STORAGE_Y + 1; y++) {
                level.setBlock(new BlockPos(x, y, pocket.minimumZ() - 1), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, y, pocket.maximumZ() + 1), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int z = pocket.minimumZ() - 1; z <= pocket.maximumZ() + 1; z++) {
            for (int y = RingStorageUtility.FLOOR_Y; y <= RingStorageUtility.MAXIMUM_STORAGE_Y + 1; y++) {
                level.setBlock(new BlockPos(pocket.minimumX() - 1, y, z), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(pocket.maximumX() + 1, y, z), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isValidLocalPosition(BlockPos localPosition) {
        return localPosition.getX() >= 0
                && localPosition.getX() < RingStorageUtility.POCKET_SIZE
                && localPosition.getY() >= 0
                && localPosition.getY() <= RingStorageUtility.MAXIMUM_STORAGE_Y - RingStorageUtility.FLOOR_Y
                && localPosition.getZ() >= 0
                && localPosition.getZ() < RingStorageUtility.POCKET_SIZE;
    }

    private static boolean isValidLocalHit(
            BlockPos localPosition,
            Direction direction,
            Vec3 localHitLocation
    ) {
        if (!Double.isFinite(localHitLocation.x)
                || !Double.isFinite(localHitLocation.y)
                || !Double.isFinite(localHitLocation.z)
                || localHitLocation.x < localPosition.getX() - HIT_POSITION_EPSILON
                || localHitLocation.x > localPosition.getX() + 1.0D + HIT_POSITION_EPSILON
                || localHitLocation.y < localPosition.getY() - HIT_POSITION_EPSILON
                || localHitLocation.y > localPosition.getY() + 1.0D + HIT_POSITION_EPSILON
                || localHitLocation.z < localPosition.getZ() - HIT_POSITION_EPSILON
                || localHitLocation.z > localPosition.getZ() + 1.0D + HIT_POSITION_EPSILON) {
            return false;
        }

        return switch (direction) {
            case DOWN -> Math.abs(localHitLocation.y - localPosition.getY()) <= HIT_POSITION_EPSILON;
            case UP -> Math.abs(localHitLocation.y - (localPosition.getY() + 1.0D)) <= HIT_POSITION_EPSILON;
            case NORTH -> Math.abs(localHitLocation.z - localPosition.getZ()) <= HIT_POSITION_EPSILON;
            case SOUTH -> Math.abs(localHitLocation.z - (localPosition.getZ() + 1.0D)) <= HIT_POSITION_EPSILON;
            case WEST -> Math.abs(localHitLocation.x - localPosition.getX()) <= HIT_POSITION_EPSILON;
            case EAST -> Math.abs(localHitLocation.x - (localPosition.getX() + 1.0D)) <= HIT_POSITION_EPSILON;
        };
    }

    private static void clearLegacyPersistentSession(ServerPlayer player) {
        player.getPersistentData().remove(LEGACY_SESSION_TAG);
    }

    private static final class StorageRingSession {
        private final UUID ringId;
        private final StoragePocket pocket;
        private Vec3 cameraPosition;
        private float cameraYRot;
        private float cameraXRot;
        private float leftImpulse;
        private float forwardImpulse;
        private boolean movingUp;
        private boolean movingDown;
        private long lastCameraUpdateTick;
        private long lastProjectionTick = -PROJECTION_REFRESH_INTERVAL_TICKS;
        private Vec3 lastProjectionPosition;
        private boolean initialProjectionRefreshPending = true;
        private long reopenMenuAtTick = -1L;

        private StorageRingSession(
                UUID ringId,
                StoragePocket pocket,
                Vec3 cameraPosition,
                float cameraYRot,
                float cameraXRot
        ) {
            this.ringId = ringId;
            this.pocket = pocket;
            this.cameraPosition = cameraPosition;
            this.cameraYRot = cameraYRot;
            this.cameraXRot = cameraXRot;
        }

        private Vec3 getLocalCameraPosition() {
            return this.pocket.toLocalPosition(this.cameraPosition);
        }

        private boolean hasMovementInput() {
            return this.leftImpulse != 0.0F
                    || this.forwardImpulse != 0.0F
                    || this.movingUp
                    || this.movingDown;
        }

        private void clearInput() {
            this.leftImpulse = 0.0F;
            this.forwardImpulse = 0.0F;
            this.movingUp = false;
            this.movingDown = false;
        }

        private boolean requiresProjectionRefresh(long currentTick) {
            if (this.initialProjectionRefreshPending
                    && currentTick - this.lastProjectionTick >= PROJECTION_REFRESH_INTERVAL_TICKS) {
                this.initialProjectionRefreshPending = false;
                return true;
            }
            return this.lastProjectionPosition == null
                    || this.cameraPosition.distanceToSqr(this.lastProjectionPosition)
                    >= PROJECTION_REFRESH_DISTANCE_SQUARED;
        }
    }

    private record LegacyPocketSnapshot(int pocketIndex, List<LegacyBlockSnapshot> blocks) {
    }

    private record LegacyBlockSnapshot(
            BlockPos localPosition,
            BlockState blockState,
            CompoundTag blockEntityTag
    ) {
    }
}
