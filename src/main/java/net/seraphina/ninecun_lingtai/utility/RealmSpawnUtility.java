package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Random;

public final class RealmSpawnUtility {
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
        RealmSpawnPosition spawnPosition = createRandomSpawnPosition(random, spawnRadius);
        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawnPosition.x(),
                spawnPosition.z()
        );
        return new Vec3(
                spawnPosition.x() + 0.5D,
                y,
                spawnPosition.z() + 0.5D
        );
    }

    public record RealmSpawnPosition(int x, int z) {
    }
}
