package net.seraphina.ninecun_lingtai.common.instance;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * 心境分格分配/回收/传送（心魔即己 §4，方案 A：静态专用维度 + 分格）。
 *
 * <p>维度 {@code ninecun_lingtai:mind_realm} 为数据包静态维度（flat + 空 layers +
 * the_void 群系，照储物戒指维度写法）。每场试炼占一个 pocket（stride 读配置
 * mindDemon.pocketStride，防跨格视野污染），格内程序化搭 33×33 灵台平台。</p>
 */
public final class MindRealmManager {
    public static final ResourceKey<Level> MIND_REALM_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "mind_realm")
    );

    public static final int POCKETS_PER_ROW = 4_096;
    public static final int FLOOR_Y = 64;
    /** 平台 33×33：半径 16。 */
    public static final int PLATFORM_HALF = 16;
    private static final int CLEAR_HEIGHT = 32;
    private static final int BARRIER_HEIGHT = 4;
    /** release 后延迟清理平台的 tick 数（防掉落物瞬失）。 */
    private static final int RELEASE_DELAY_TICKS = 100;

    private static final List<PendingRelease> PENDING_RELEASES = new ArrayList<>();

    private MindRealmManager() {
    }

    // ------------------------------------------------------------------ 分配与回收

    /** 分配分格并搭建平台，返回入场坐标（服务端）。 */
    public static MindArena allocate(MinecraftServer server, UUID trialId, UUID ownerUuid) {
        MindRealmSavedData savedData = MindRealmSavedData.get(server);
        int pocketIndex = savedData.allocatePocket(trialId, ownerUuid);
        MindArena arena = new MindArena(pocketIndex, arenaCenter(pocketIndex));
        ServerLevel level = mindRealmLevel(server);
        if (level != null) {
            setArenaChunksForced(level, arena, true);
            buildPlatform(level, arena.center());
        } else {
            ModMain.LOGGER.error(
                    "[mind_realm] dimension {} is missing; datapack not loaded?",
                    MIND_REALM_DIMENSION.location()
            );
        }
        return arena;
    }

    /** 战斗结束清理：延迟 {@value #RELEASE_DELAY_TICKS} tick 清平台方块 + 释放 pocket。 */
    public static void release(MinecraftServer server, UUID trialId) {
        MindRealmSavedData savedData = MindRealmSavedData.get(server);
        savedData.findPocketByTrial(trialId).ifPresent(pocketIndex ->
                PENDING_RELEASES.add(new PendingRelease(pocketIndex, RELEASE_DELAY_TICKS))
        );
    }

    /** 立即释放（启动扫描兜底用，无延迟）。 */
    public static void releaseImmediately(MinecraftServer server, int pocketIndex) {
        clearPocket(server, pocketIndex);
    }

    /** 由 {@code MindTrialEventHandler} 每服务器 tick 调用，处理延迟清理队列。 */
    public static void tickPendingReleases(MinecraftServer server) {
        if (PENDING_RELEASES.isEmpty()) {
            return;
        }
        Iterator<PendingRelease> iterator = PENDING_RELEASES.iterator();
        while (iterator.hasNext()) {
            PendingRelease pending = iterator.next();
            if (--pending.remainingTicks <= 0) {
                clearPocket(server, pending.pocketIndex);
                iterator.remove();
            }
        }
    }

    // ------------------------------------------------------------------ 传送

    /** 传送玩家进入心境（复用三千界 ITeleporter + PortalInfo 写法）。 */
    public static boolean enter(ServerPlayer player, MindArena arena) {
        MinecraftServer server = player.server;
        ServerLevel level = mindRealmLevel(server);
        if (level == null) {
            return false;
        }
        Vec3 destination = Vec3.atBottomCenterOf(arena.center().above());
        if (player.serverLevel() == level) {
            player.teleportTo(destination.x, destination.y, destination.z);
            return true;
        }
        return player.changeDimension(
                level,
                createTeleporter(destination, player.getYRot(), player.getXRot())
        ) != null;
    }

    /** 送回劫场原点（从 TribulationSession 读回程）。 */
    public static boolean exit(ServerPlayer player, TribulationSession session) {
        ServerLevel targetLevel = player.server.getLevel(session.dimension());
        if (targetLevel == null) {
            targetLevel = player.server.overworld();
        }
        Vec3 destination = session.center();
        if (player.serverLevel() == targetLevel) {
            player.teleportTo(destination.x, destination.y, destination.z);
            return true;
        }
        return player.changeDimension(
                targetLevel,
                createTeleporter(destination, player.getYRot(), player.getXRot())
        ) != null;
    }

    /** 非 owner 弹出：送回主世界重生点（队友无法进入的硬保证）。 */
    public static void ejectToRespawn(ServerPlayer player) {
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            respawnLevel = player.server.overworld();
        }
        BlockPos respawnPosition = player.getRespawnPosition() != null
                ? player.getRespawnPosition()
                : respawnLevel.getSharedSpawnPos();
        Vec3 destination = Vec3.atBottomCenterOf(respawnPosition);
        if (player.serverLevel() == respawnLevel) {
            player.teleportTo(destination.x, destination.y, destination.z);
        } else {
            player.changeDimension(
                    respawnLevel,
                    createTeleporter(destination, player.getYRot(), player.getXRot())
            );
        }
    }

    // ------------------------------------------------------------------ 查询

    public static boolean isMindRealm(ResourceKey<Level> dimension) {
        return MIND_REALM_DIMENSION.equals(dimension);
    }

    public static ServerLevel mindRealmLevel(MinecraftServer server) {
        return server.getLevel(MIND_REALM_DIMENSION);
    }

    /** 分格间距（读配置 mindDemon.pocketStride；存档内有会话时不应改动）。 */
    public static int pocketStride() {
        return CommonConfig.MIND_DEMON_POCKET_STRIDE.get();
    }

    /** 分格中心（平台中心块坐标）。 */
    public static BlockPos arenaCenter(int pocketIndex) {
        int stride = pocketStride();
        int column = Math.floorMod(pocketIndex, POCKETS_PER_ROW);
        int row = Math.floorDiv(pocketIndex, POCKETS_PER_ROW);
        return new BlockPos(
                column * stride + stride / 2,
                FLOOR_Y,
                row * stride + stride / 2
        );
    }

    /** 某坐标所属的分格索引（弹出检查用）。 */
    public static int pocketIndexAt(BlockPos position) {
        int stride = pocketStride();
        int column = Math.floorDiv(position.getX(), stride);
        int row = Math.floorDiv(position.getZ(), stride);
        return row * POCKETS_PER_ROW + column;
    }

    // ------------------------------------------------------------------ 平台搭建/清理

    /** R4：复用前先 fill air 再铺台，保证残留方块不污染新试炼。 */
    private static void buildPlatform(ServerLevel level, BlockPos center) {
        fillArena(level, center, true);
    }

    private static void clearPocket(MinecraftServer server, int pocketIndex) {
        ServerLevel level = mindRealmLevel(server);
        MindArena arena = new MindArena(pocketIndex, arenaCenter(pocketIndex));
        if (level != null) {
            fillArena(level, arena.center(), false);
            setArenaChunksForced(level, arena, false);
        }
        MindRealmSavedData.get(server).releasePocket(pocketIndex);
    }

    private static void fillArena(ServerLevel level, BlockPos center, boolean build) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState accent = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -PLATFORM_HALF; dx <= PLATFORM_HALF; dx++) {
            for (int dz = -PLATFORM_HALF; dz <= PLATFORM_HALF; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                // 先整柱清空（含旧屏障），再按需铺设。
                for (int dy = 0; dy <= CLEAR_HEIGHT; dy++) {
                    cursor.set(x, FLOOR_Y + dy, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, air, 2);
                    }
                }
                if (!build) {
                    continue;
                }
                boolean edge = Math.abs(dx) == PLATFORM_HALF || Math.abs(dz) == PLATFORM_HALF;
                boolean accentSpot = (Math.abs(dx) == PLATFORM_HALF - 2 && Math.abs(dz) == PLATFORM_HALF - 2)
                        || (dx == 0 && dz == 0);
                cursor.set(x, FLOOR_Y, z);
                level.setBlock(cursor, accentSpot ? accent : floor, 2);
                if (edge) {
                    for (int dy = 1; dy <= BARRIER_HEIGHT; dy++) {
                        cursor.set(x, FLOOR_Y + dy, z);
                        level.setBlock(cursor, barrier, 2);
                    }
                }
            }
        }
    }

    private static void setArenaChunksForced(ServerLevel level, MindArena arena, boolean add) {
        BlockPos center = arena.center();
        int minimumChunkX = (center.getX() - PLATFORM_HALF) >> 4;
        int maximumChunkX = (center.getX() + PLATFORM_HALF) >> 4;
        int minimumChunkZ = (center.getZ() - PLATFORM_HALF) >> 4;
        int maximumChunkZ = (center.getZ() + PLATFORM_HALF) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                ForgeChunkManager.forceChunk(
                        level,
                        ModMain.MOD_ID,
                        center,
                        chunkX,
                        chunkZ,
                        add,
                        true
                );
            }
        }
        if (add) {
            // 确保搭台前区块已同步加载。
            level.getChunk(new ChunkPos(center).x, new ChunkPos(center).z);
        }
    }

    private static ITeleporter createTeleporter(Vec3 destination, float yRot, float xRot) {
        return new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(
                    Entity entity,
                    ServerLevel destinationLevel,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo
            ) {
                return new PortalInfo(destination, Vec3.ZERO, yRot, xRot);
            }

            @Override
            public Entity placeEntity(
                    Entity entity,
                    ServerLevel currentLevel,
                    ServerLevel destinationLevel,
                    float yaw,
                    Function<Boolean, Entity> repositionEntity
            ) {
                return repositionEntity.apply(false);
            }

            @Override
            public boolean playTeleportSound(
                    ServerPlayer player,
                    ServerLevel sourceLevel,
                    ServerLevel destinationLevel
            ) {
                return false;
            }
        };
    }

    public record MindArena(int pocketIndex, BlockPos center) {
    }

    private static final class PendingRelease {
        private final int pocketIndex;
        private int remainingTicks;

        private PendingRelease(int pocketIndex, int remainingTicks) {
            this.pocketIndex = pocketIndex;
            this.remainingTicks = remainingTicks;
        }
    }
}
