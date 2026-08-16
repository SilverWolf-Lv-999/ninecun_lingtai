package net.seraphina.ninecun_lingtai.utility;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmSpawnUtilityTest {
    @Test
    void createsPositionsInsideTheConfiguredCircularRadius() {
        int spawnRadius = 10_000;
        long squaredRadius = (long) spawnRadius * spawnRadius;
        Random random = new Random(20260815L);

        for (int index = 0; index < 10_000; index++) {
            RealmSpawnUtility.RealmSpawnPosition position =
                    RealmSpawnUtility.createRandomSpawnPosition(random, spawnRadius);
            long squaredDistance = (long) position.x() * position.x()
                    + (long) position.z() * position.z();

            assertTrue(squaredDistance <= squaredRadius);
        }
    }

    @Test
    void supportsAnOriginOnlySpawnRadius() {
        RealmSpawnUtility.RealmSpawnPosition position =
                RealmSpawnUtility.createRandomSpawnPosition(new Random(1L), 0);

        assertEquals(0, position.x());
        assertEquals(0, position.z());
    }

    @Test
    void rejectsNegativeSpawnRadii() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RealmSpawnUtility.createRandomSpawnPosition(new Random(1L), -1)
        );
    }

    @Test
    void rejectsNullRandomSources() {
        assertThrows(
                NullPointerException.class,
                () -> RealmSpawnUtility.createRandomSpawnPosition(null, 10)
        );
    }
}
