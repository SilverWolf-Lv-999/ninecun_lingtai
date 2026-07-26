package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonParser;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledTechniqueDataTest {
    private static final Path TECHNIQUE_DIRECTORY = Path.of(
            "src", "main", "resources", "data", "ninecun_lingtai", "techniques"
    );

    @Test
    void allBundledTechniqueFilesParse() throws IOException {
        List<Path> techniqueFiles = listTechniqueFiles();
        assertFalse(techniqueFiles.isEmpty(), "No bundled technique files found");
        for (Path techniqueFile : techniqueFiles) {
            assertDoesNotThrow(
                    () -> parseTechnique(techniqueFile),
                    "Failed to parse bundled technique: " + techniqueFile.getFileName()
            );
        }
    }

    @Test
    void bundledBreathingTechniquesAreMarkedAsBreathing() throws IOException {
        for (Path techniqueFile : listTechniqueFiles()) {
            if (!techniqueFile.getFileName().toString().endsWith("_breathing.json")) {
                continue;
            }
            TechniqueDefinition technique = parseTechnique(techniqueFile);
            assertTrue(
                    technique.isBreathingTechnique(),
                    technique.name() + " should be a breathing technique"
            );
            assertTrue(
                    technique.breathing().orElseThrow().absorptionMultiplier() >= 1.0D,
                    technique.name() + " should be at least as strong as the basic baseline"
            );
        }
    }

    private static List<Path> listTechniqueFiles() throws IOException {
        try (Stream<Path> files = Files.walk(TECHNIQUE_DIRECTORY)) {
            return files
                    .filter(file -> file.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static TechniqueDefinition parseTechnique(Path techniqueFile) throws IOException {
        String fileName = techniqueFile.getFileName().toString();
        ResourceLocation techniqueId = ResourceLocation.fromNamespaceAndPath(
                "ninecun_lingtai",
                fileName.substring(0, fileName.length() - ".json".length())
        );
        String json = Files.readString(techniqueFile, StandardCharsets.UTF_8);
        return TechniqueJsonUtility.parse(techniqueId, JsonParser.parseString(json));
    }
}
