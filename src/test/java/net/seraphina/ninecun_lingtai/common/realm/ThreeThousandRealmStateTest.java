package net.seraphina.ninecun_lingtai.common.realm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeThousandRealmStateTest {
    @TempDir
    private Path thisTemporaryDirectory;

    @Test
    void createsPersistsAndDoesNotDuplicateTheRealmCatalog() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);
        List<Integer> progress = new ArrayList<>();

        List<RealmDefinition> createdRealms = state.ensureRealmCount(
                new Random(12345L),
                12,
                progress::add
        );

        assertEquals(12, createdRealms.size());
        assertEquals(12, state.getRealmCount());
        assertEquals(12, progress.size());
        assertEquals(1, progress.get(0));
        assertEquals(12, progress.get(progress.size() - 1));

        Set<String> displayNames = createdRealms.stream()
                .map(RealmDefinition::getDisplayName)
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(createdRealms.size(), displayNames.size());

        ThreeThousandRealmState reloadedState = ThreeThousandRealmState.load(filePath);
        assertEquals(12, reloadedState.getRealmCount());
        assertEquals(
                createdRealms.stream().map(RealmDefinition::getId).toList(),
                reloadedState.getRealms().stream().map(RealmDefinition::getId).toList()
        );

        List<Integer> repeatedProgress = new ArrayList<>();
        List<RealmDefinition> unchangedRealms = reloadedState.ensureRealmCount(
                new Random(67890L),
                12,
                repeatedProgress::add
        );
        assertEquals(createdRealms.stream().map(RealmDefinition::getId).toList(),
                unchangedRealms.stream().map(RealmDefinition::getId).toList());
        assertTrue(repeatedProgress.isEmpty());
    }

    @Test
    void rollsBackNewRealmMetadataWhenBatchCreationFails() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);
        RealmDefinition originalRealm = state.createRealm(new Random(24680L), 4);

        assertThrows(
                IllegalStateException.class,
                () -> state.ensureRealmCount(
                        new Random(13579L),
                        4,
                        completedRealmCount -> {
                            if (completedRealmCount == 3) {
                                throw new IllegalStateException("test failure");
                            }
                        }
                )
        );

        assertEquals(1, state.getRealmCount());
        assertEquals(originalRealm.getId(), state.getRealms().get(0).getId());
        ThreeThousandRealmState reloadedState = ThreeThousandRealmState.load(filePath);
        assertEquals(1, reloadedState.getRealmCount());
        assertEquals(originalRealm.getId(), reloadedState.getRealms().get(0).getId());
    }

    @Test
    void persistsEachIncrementalCatalogCreationBatch() {
        Path filePath = this.thisTemporaryDirectory.resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(filePath);

        List<RealmDefinition> firstBatch = state.createRealmBatch(new Random(98765L), 5, 2);
        assertEquals(2, firstBatch.size());
        assertEquals(2, state.getRealmCount());
        assertEquals(2, ThreeThousandRealmState.load(filePath).getRealmCount());

        List<RealmDefinition> secondBatch = state.createRealmBatch(new Random(56789L), 5, 2);
        List<RealmDefinition> finalBatch = state.createRealmBatch(new Random(12345L), 5, 2);
        assertEquals(2, secondBatch.size());
        assertEquals(1, finalBatch.size());
        assertEquals(5, state.getRealmCount());
        assertTrue(state.createRealmBatch(new Random(67890L), 5, 2).isEmpty());
        assertEquals(5, ThreeThousandRealmState.load(filePath).getRealmCount());
    }
}
