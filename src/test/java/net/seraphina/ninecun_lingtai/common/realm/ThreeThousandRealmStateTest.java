package net.seraphina.ninecun_lingtai.common.realm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThreeThousandRealmStateTest {
    @TempDir
    private Path thisTemporaryDirectory;

    @Test
    void createsAndPersistsRealmsOneAtATime() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);

        RealmDefinition firstRealm = state.createRealm(new Random(12345L), 10);
        RealmDefinition secondRealm = state.createRealm(new Random(67890L), 10);

        assertEquals(2, state.getRealmCount());
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.getRealms().add(firstRealm)
        );

        ThreeThousandRealmState reloadedState = ThreeThousandRealmState.load(filePath);
        assertEquals(
                List.of(firstRealm.getId(), secondRealm.getId()),
                reloadedState.getRealms().stream().map(RealmDefinition::getId).toList()
        );
    }

    @Test
    void rejectsCreationAfterConfiguredRealmLimit() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);
        RealmDefinition realm = state.createRealm(new Random(24680L), 1);

        assertThrows(
                IllegalStateException.class,
                () -> state.createRealm(new Random(13579L), 1)
        );

        assertEquals(1, state.getRealmCount());
        assertEquals(realm.getId(), state.getRealms().get(0).getId());
        ThreeThousandRealmState reloadedState = ThreeThousandRealmState.load(filePath);
        assertEquals(1, reloadedState.getRealmCount());
        assertEquals(realm.getId(), reloadedState.getRealms().get(0).getId());
    }

    @Test
    void createsAndPersistsMissingRealmsToConfiguredCount() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);
        RealmDefinition existingRealm = state.createRealm(new Random(24680L), 10);

        List<RealmDefinition> createdRealms = state.createMissingRealms(new Random(13579L), 10);

        assertEquals(9, createdRealms.size());
        assertEquals(10, state.getRealmCount());
        assertEquals(existingRealm.getId(), state.getRealms().get(0).getId());
        assertEquals(List.of(), state.createMissingRealms(new Random(98765L), 10));
        assertEquals(10, ThreeThousandRealmState.load(filePath).getRealmCount());
    }

    @Test
    void rejectsConfiguredRealmLimitAboveHardCapacity() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);

        assertThrows(
                IllegalArgumentException.class,
                () -> state.createRealm(new Random(24680L), 11)
        );
        assertEquals(0, state.getRealmCount());
    }

}
