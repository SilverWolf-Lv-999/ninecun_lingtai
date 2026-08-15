package net.seraphina.ninecun_lingtai.common.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeThousandRealmSeedContextTest {
    @Test
    void exposesTheInnermostRealmSeedAndCleansUpAfterClosing() {
        assertFalse(ThreeThousandRealmSeedContext.getCurrentSeed().isPresent());

        try (ThreeThousandRealmSeedContext.RealmSeedScope outerScope =
                     ThreeThousandRealmSeedContext.push(100L)) {
            assertTrue(ThreeThousandRealmSeedContext.getCurrentSeed().isPresent());
            assertEquals(100L, ThreeThousandRealmSeedContext.getCurrentSeed().getAsLong());

            try (ThreeThousandRealmSeedContext.RealmSeedScope innerScope =
                         ThreeThousandRealmSeedContext.push(-50L)) {
                assertEquals(-50L, ThreeThousandRealmSeedContext.getCurrentSeed().getAsLong());
            }

            assertEquals(100L, ThreeThousandRealmSeedContext.getCurrentSeed().getAsLong());
        }

        assertFalse(ThreeThousandRealmSeedContext.getCurrentSeed().isPresent());
    }
}
