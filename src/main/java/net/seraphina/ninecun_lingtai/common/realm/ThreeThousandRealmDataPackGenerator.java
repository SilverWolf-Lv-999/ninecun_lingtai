package net.seraphina.ninecun_lingtai.common.realm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.LevelStem;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.JsonFileUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class ThreeThousandRealmDataPackGenerator {
    static final String DATA_PACK_DIRECTORY_NAME = "ninecun_lingtai_realms";
    private static final int DATA_PACK_FORMAT = 15;
    private static final String DATA_DIRECTORY_NAME = "data";
    private static final String DIMENSION_DIRECTORY_NAME = "dimension";
    private static final String PACK_METADATA_FILE_NAME = "pack.mcmeta";

    private final Path thisDataPackDirectory;

    public ThreeThousandRealmDataPackGenerator(Path dataPackRootDirectory) {
        Objects.requireNonNull(dataPackRootDirectory, "dataPackRootDirectory");
        this.thisDataPackDirectory = dataPackRootDirectory
                .toAbsolutePath()
                .normalize()
                .resolve(DATA_PACK_DIRECTORY_NAME);
    }

    public synchronized void ensureRealmDefinition(RealmDefinition realmDefinition) throws IOException {
        Objects.requireNonNull(realmDefinition, "realmDefinition");
        validateRealmId(realmDefinition.getId());
        writeIfMissing(this.thisDataPackDirectory.resolve(PACK_METADATA_FILE_NAME), createPackMetadata());
        writeIfMissing(this.getDimensionFilePath(realmDefinition.getId()), createDimensionDefinition());
    }

    public boolean hasRealmDefinition(RealmDefinition realmDefinition) {
        Objects.requireNonNull(realmDefinition, "realmDefinition");
        validateRealmId(realmDefinition.getId());
        return Files.isRegularFile(this.getDimensionFilePath(realmDefinition.getId()));
    }

    public synchronized Set<ResourceLocation> getPersistedRealmDefinitionIds() {
        Path dimensionDirectory = this.getDimensionDirectory();
        if (!Files.isDirectory(dimensionDirectory)) {
            return Set.of();
        }

        Set<ResourceLocation> realmIds = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(dimensionDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(".json"))
                    .sorted()
                    .forEach(fileName -> {
                        String path = fileName.substring(0, fileName.length() - ".json".length());
                        ResourceLocation realmId = ResourceLocation.tryParse(ModMain.MOD_ID + ":" + path);
                        if (realmId != null) {
                            realmIds.add(realmId);
                        }
                    });
        } catch (IOException exception) {
            ModMain.LOGGER.warn(
                    "Unable to list generated three-thousand-realm definitions in {}",
                    dimensionDirectory,
                    exception
            );
            return Set.of();
        }
        return Collections.unmodifiableSet(realmIds);
    }

    public synchronized LevelStem readRealmLevelStem(
            ResourceLocation realmId,
            RegistryAccess registryAccess
    ) throws IOException {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(registryAccess, "registryAccess");
        validateRealmId(realmId);

        JsonElement definition = JsonFileUtility.read(this.getDimensionFilePath(realmId));
        return LevelStem.CODEC.parse(
                        RegistryOps.create(JsonOps.INSTANCE, registryAccess),
                        definition
                )
                .resultOrPartial(error -> ModMain.LOGGER.error(
                        "Unable to decode generated three-thousand-realm dimension {}: {}",
                        realmId,
                        error
                ))
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to decode generated three-thousand-realm dimension " + realmId
                ));
    }

    Path getDataPackDirectory() {
        return this.thisDataPackDirectory;
    }

    Path getDimensionFilePath(ResourceLocation realmId) {
        validateRealmId(realmId);
        return this.getDimensionDirectory()
                .resolve(realmId.getPath() + ".json");
    }

    static JsonObject createDimensionDefinition() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("type", "minecraft:overworld");

        JsonObject generatorObject = new JsonObject();
        generatorObject.addProperty("type", "minecraft:noise");

        JsonObject biomeSourceObject = new JsonObject();
        biomeSourceObject.addProperty("type", "minecraft:multi_noise");
        biomeSourceObject.addProperty("preset", "minecraft:overworld");
        generatorObject.add("biome_source", biomeSourceObject);
        generatorObject.addProperty("settings", "minecraft:overworld");

        rootObject.add("generator", generatorObject);
        return rootObject;
    }

    private static void writeIfMissing(Path filePath, JsonObject definition) throws IOException {
        if (Files.exists(filePath)) {
            return;
        }
        JsonFileUtility.write(filePath, definition);
    }

    private Path getDimensionDirectory() {
        return this.thisDataPackDirectory
                .resolve(DATA_DIRECTORY_NAME)
                .resolve(ModMain.MOD_ID)
                .resolve(DIMENSION_DIRECTORY_NAME);
    }

    private static JsonObject createPackMetadata() {
        JsonObject packObject = new JsonObject();
        packObject.addProperty("pack_format", DATA_PACK_FORMAT);
        packObject.addProperty(
                "description",
                "Ninecun Lingtai generated three-thousand-realm dimensions"
        );

        JsonObject rootObject = new JsonObject();
        rootObject.add("pack", packObject);
        return rootObject;
    }

    private static void validateRealmId(ResourceLocation realmId) {
        if (!ModMain.MOD_ID.equals(realmId.getNamespace())) {
            throw new IllegalArgumentException("Three-thousand-realm id must use the mod namespace");
        }
    }
}
