package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonParser;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledAlchemyRecipeDataTest {
    private static final Path RECIPE_DIRECTORY = Path.of(
            "src", "main", "resources", "data", "ninecun_lingtai", "alchemy_recipes"
    );

    @Test
    void allBundledAlchemyRecipeFilesParse() throws IOException {
        List<Path> recipeFiles = listRecipeFiles();
        assertFalse(recipeFiles.isEmpty(), "No bundled alchemy recipe files found");
        for (Path recipeFile : recipeFiles) {
            assertDoesNotThrow(
                    () -> parseRecipe(recipeFile),
                    "Failed to parse bundled alchemy recipe: " + recipeFile.getFileName()
            );
        }
    }

    @Test
    void grassRestorationPillIsAQiAndFoundationRemedy() throws IOException {
        AlchemyRecipeDefinition recipe = parseRecipe(
                RECIPE_DIRECTORY.resolve("grass_restoration_pill.json")
        );

        assertTrue(recipe.allowedRealms().contains(
                com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm.QI_REFINING
        ));
        assertTrue(recipe.allowedRealms().contains(
                com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm.FOUNDATION_ESTABLISHMENT
        ));
        assertTrue(recipe.pillEffect().regenerationAmplifier() == 4);
        assertEquals(3, recipe.primaryIngredient().count());
        assertEquals(200, recipe.brewingDuration());
    }

    private static List<Path> listRecipeFiles() throws IOException {
        try (Stream<Path> files = Files.walk(RECIPE_DIRECTORY)) {
            return files
                    .filter(file -> file.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static AlchemyRecipeDefinition parseRecipe(Path recipeFile) throws IOException {
        String fileName = recipeFile.getFileName().toString();
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                "ninecun_lingtai",
                fileName.substring(0, fileName.length() - ".json".length())
        );
        String json = Files.readString(recipeFile, StandardCharsets.UTF_8);
        return AlchemyRecipeJsonUtility.parse(recipeId, JsonParser.parseString(json));
    }
}
