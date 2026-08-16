package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Random;

public final class RealmSpawnUtility {
    private static final int MAXIMUM_SPAWN_POSITION_ATTEMPTS = 128;
    private static final int MINIMUM_GROUND_DISTANCE_FROM_WORLD_FLOOR = 5;

    private RealmSpawnUtility() {
    }

    public static RealmSpawnPosition createRandomSpawnPosition(Random random, int spawnRadius) {
        Objects.requireNonNull(random, "random");
        if (spawnRadius < 0) {
            throw new IllegalArgumentException("spawnRadius must not be negative");
        }

        long squaredRadius = (long) spawnRadius * spawnRadius;
        int x;
        int z;
        do {
            x = random.nextInt(spawnRadius * 2 + 1) - spawnRadius;
            z = random.nextInt(spawnRadius * 2 + 1) - spawnRadius;
        } while ((long) x * x + (long) z * z > squaredRadius);
        return new RealmSpawnPosition(x, z);
    }

    public static Vec3 findSafeSpawnPosition(ServerLevel level, Random random, int spawnRadius) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(random, "random");

        for (int attempt = 0; attempt < MAXIMUM_SPAWN_POSITION_ATTEMPTS; attempt++) {
            RealmSpawnPosition spawnPosition = createRandomSpawnPosition(random, spawnRadius);
            level.getChunk(spawnPosition.x() >> 4, spawnPosition.z() >> 4);
            Vec3 safeSpawnPosition = findSafeSpawnPositionInColumn(
                    level,
                    spawnPosition.x(),
                    spawnPosition.z()
            );
            if (safeSpawnPosition != null) {
                return safeSpawnPosition;
            }
        }

        throw new IllegalStateException("Unable to find a safe three-thousand-realm spawn position");
    }

    private static Vec3 findSafeSpawnPositionInColumn(ServerLevel level, int x, int z) {
        int minimumGroundY = level.getMinBuildHeight() + MINIMUM_GROUND_DISTANCE_FROM_WORLD_FLOOR;
        for (int y = level.getMaxBuildHeight() - 3; y >= minimumGroundY; y--) {
            BlockPos groundPosition = new BlockPos(x, y, z);
            if (!isSafeGround(level, groundPosition)) {
                continue;
            }

            BlockPos feetPosition = groundPosition.above();
            BlockPos headPosition = feetPosition.above();
            if (isClear(level, feetPosition) && isClear(level, headPosition)) {
                return new Vec3(x + 0.5D, feetPosition.getY(), z + 0.5D);
            }
        }
        return null;
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos groundPosition) {
        BlockState groundState = level.getBlockState(groundPosition);
        return !groundState.isAir()
                && groundState.getFluidState().isEmpty()
                && !groundState.is(BlockTags.LEAVES)
                && groundState.isFaceSturdy(level, groundPosition, Direction.UP);
    }

    private static boolean isClear(ServerLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        return state.getFluidState().isEmpty()
                && state.getCollisionShape(level, position).isEmpty();
    }

    public record RealmSpawnPosition(int x, int z) {
    }
}
