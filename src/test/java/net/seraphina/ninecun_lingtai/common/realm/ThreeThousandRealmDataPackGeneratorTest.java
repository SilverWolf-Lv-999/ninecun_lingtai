package net.seraphina.ninecun_lingtai.common.realm;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeThousandRealmDataPackGeneratorTest {
    @TempDir
    private Path thisTemporaryDirectory;

    @Test
    void writesCanonicalWorldLocalDimensionData() throws IOException {
        ThreeThousandRealmDataPackGenerator generator =
                new ThreeThousandRealmDataPackGenerator(this.thisTemporaryDirectory);
        RealmDefinition realmDefinition = new RealmDefinition(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "test_jie"),
                "测试界",
                12345L,
                67890L
        );

        generator.ensureRealmDefinition(realmDefinition);

        Path dataPackDirectory = this.thisTemporaryDirectory
                .resolve(ThreeThousandRealmDataPackGenerator.DATA_PACK_DIRECTORY_NAME);
        Path packMetadataPath = dataPackDirectory.resolve("pack.mcmeta");
        Path dimensionPath = dataPackDirectory
                .resolve("data")
                .resolve("ninecun_lingtai")
                .resolve("dimension")
                .resolve("test_jie.json");
        assertEquals(dataPackDirectory, generator.getDataPackDirectory());
        assertTrue(Files.isRegularFile(packMetadataPath));
        assertTrue(Files.isRegularFile(dimensionPath));

        JsonObject packMetadata = JsonFileUtility.read(packMetadataPath).getAsJsonObject();
        assertEquals(15, packMetadata.getAsJsonObject("pack").get("pack_format").getAsInt());

        JsonObject dimension = JsonFileUtility.read(dimensionPath).getAsJsonObject();
        assertEquals("minecraft:overworld", dimension.get("type").getAsString());
        JsonObject generatorObject = dimension.getAsJsonObject("generator");
        assertEquals("minecraft:noise", generatorObject.get("type").getAsString());
        assertEquals("minecraft:overworld", generatorObject.get("settings").getAsString());
        JsonObject biomeSource = generatorObject.getAsJsonObject("biome_source");
        assertEquals("minecraft:multi_noise", biomeSource.get("type").getAsString());
        assertEquals("minecraft:overworld", biomeSource.get("preset").getAsString());
    }

    @Test
    void keepsExistingDataPackFilesUntouched() throws IOException {
        ThreeThousandRealmDataPackGenerator generator =
                new ThreeThousandRealmDataPackGenerator(this.thisTemporaryDirectory);
        RealmDefinition realmDefinition = new RealmDefinition(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "repair_jie"),
                "修复界",
                12345L,
                67890L
        );
        generator.ensureRealmDefinition(realmDefinition);

        Path packMetadataPath = generator.getDataPackDirectory().resolve("pack.mcmeta");
        Path dimensionPath = generator.getDimensionFilePath(realmDefinition.getId());
        JsonObject modifiedMetadata = JsonFileUtility.read(packMetadataPath).getAsJsonObject();
        modifiedMetadata.getAsJsonObject("pack").addProperty("description", "custom description");
        JsonFileUtility.write(packMetadataPath, modifiedMetadata);

        JsonObject modifiedDefinition = JsonFileUtility.read(dimensionPath).getAsJsonObject();
        modifiedDefinition.addProperty("type", "minecraft:the_nether");
        JsonFileUtility.write(dimensionPath, modifiedDefinition);

        generator.ensureRealmDefinition(realmDefinition);

        JsonObject retainedMetadata = JsonFileUtility.read(packMetadataPath).getAsJsonObject();
        assertEquals(
                "custom description",
                retainedMetadata.getAsJsonObject("pack").get("description").getAsString()
        );
        JsonObject retainedDefinition = JsonFileUtility.read(dimensionPath).getAsJsonObject();
        assertEquals("minecraft:the_nether", retainedDefinition.get("type").getAsString());
    }

    @Test
    void persistsDataDefinitionsWhenExistingRealmMetadataIsCompletedToConfiguredCount()
            throws IOException {
        Path stateFilePath = this.thisTemporaryDirectory
                .resolve("world")
                .resolve("data")
                .resolve("ninecun_lingtai")
                .resolve("three_thousand_realms.json");
        ThreeThousandRealmState state = ThreeThousandRealmState.load(stateFilePath);
        RealmDefinition existingRealm = state.createRealm(new Random(24680L), 10);
        List<RealmDefinition> createdRealms = state.createMissingRealms(new Random(13579L), 10);
        ThreeThousandRealmDataPackGenerator generator = new ThreeThousandRealmDataPackGenerator(
                this.thisTemporaryDirectory.resolve("world").resolve("datapacks")
        );

        assertEquals(9, createdRealms.size());
        for (RealmDefinition realmDefinition : state.getRealms()) {
            assertFalse(generator.hasRealmDefinition(realmDefinition));
            generator.ensureRealmDefinition(realmDefinition);
        }

        ThreeThousandRealmState reloadedState = ThreeThousandRealmState.load(stateFilePath);
        assertEquals(10, reloadedState.getRealmCount());
        assertEquals(existingRealm.getId(), reloadedState.getRealms().get(0).getId());
        assertTrue(
                reloadedState.getRealms().stream()
                        .allMatch(generator::hasRealmDefinition)
        );
    }

    @Test
    void discoversPersistedDefinitionsOutsideTheCurrentRealmMetadata() throws IOException {
        ThreeThousandRealmDataPackGenerator generator =
                new ThreeThousandRealmDataPackGenerator(this.thisTemporaryDirectory);
        RealmDefinition activeRealm = new RealmDefinition(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "active_jie"),
                "现存界",
                12345L,
                67890L
        );
        RealmDefinition staleRealm = new RealmDefinition(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "stale_jie"),
                "遗留界",
                13579L,
                24680L
        );
        generator.ensureRealmDefinition(activeRealm);
        generator.ensureRealmDefinition(staleRealm);

        assertEquals(
                Set.of(activeRealm.getId(), staleRealm.getId()),
                generator.getPersistedRealmDefinitionIds()
        );
    }

    @Test
    void excludesManagedRealmStemsFromInitialServerLevelConstruction() {
        ResourceLocation overworldId = ResourceLocation.withDefaultNamespace("overworld");
        ResourceLocation managedRealmId =
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "lazy_jie");
        ResourceLocation otherModDimensionId =
                ResourceLocation.fromNamespaceAndPath("example_mod", "persistent_dimension");
        Set<Map.Entry<ResourceLocation, String>> levelStemEntries = new LinkedHashSet<>();
        levelStemEntries.add(new AbstractMap.SimpleImmutableEntry<>(overworldId, "overworld"));
        levelStemEntries.add(new AbstractMap.SimpleImmutableEntry<>(managedRealmId, "managed"));
        levelStemEntries.add(new AbstractMap.SimpleImmutableEntry<>(otherModDimensionId, "other"));

        Set<Map.Entry<ResourceLocation, String>> filteredEntries =
                ThreeThousandRealmRegistryUtility.excludeManagedEntries(
                        levelStemEntries,
                        Map.Entry::getKey,
                        Set.of(managedRealmId)
                );

        List<ResourceLocation> filteredKeys = filteredEntries.stream()
                .map(Map.Entry::getKey)
                .toList();
        assertEquals(List.of(overworldId, otherModDimensionId), filteredKeys);
    }

    @Test
    void rejectsRealmIdsOutsideTheModNamespace() {
        ThreeThousandRealmDataPackGenerator generator =
                new ThreeThousandRealmDataPackGenerator(this.thisTemporaryDirectory);
        RealmDefinition invalidRealmDefinition = new RealmDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "invalid"),
                "无效界",
                12345L,
                67890L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.ensureRealmDefinition(invalidRealmDefinition)
        );
    }
}
