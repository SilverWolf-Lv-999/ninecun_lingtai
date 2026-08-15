package net.seraphina.ninecun_lingtai.utility;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmNameUtilityTest {
    @Test
    void generatesThreeThousandUniqueChineseRealmNames() {
        Random random = new Random(20260815L);
        Set<String> names = new HashSet<>();

        for (int index = 0; index < 3000; index++) {
            RealmNameUtility.RealmName realmName = RealmNameUtility.createRandomName(names, random);
            assertTrue(realmName.getDisplayName().endsWith("界"));
            assertTrue(realmName.getPathPrefix().endsWith("_jie"));
            assertTrue(names.add(realmName.getDisplayName()));
        }

        assertEquals(3000, names.size());
        assertTrue(RealmNameUtility.getNameCapacity() >= 3000);
    }
}
