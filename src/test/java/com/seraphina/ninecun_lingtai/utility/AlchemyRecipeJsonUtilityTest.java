package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlchemyRecipeJsonUtilityTest {
    private static final ResourceLocation RECIPE_ID = ResourceLocation.fromNamespaceAndPath(
            "test",
            "grass_restoration_pill"
    );

    @Test
    void parsesDataDrivenAlchemyRecipe() {
        AlchemyRecipeDefinition recipe = AlchemyRecipeJsonUtility.parse(RECIPE_ID, JsonParser.parseString("""
                {
                  "name": "草还丹方",
                  "description": "疗伤丹方",
                  "content": "主药入炉，文火凝丹。",
                  "grade": {"tier": 1, "rank": 1},
                  "allowed_realms": ["qi_refining", "foundation_establishment"],
                  "minimum_refining_realm": "qi_refining",
                  "ingredients": {
                    "primary": {"item": "minecraft:wheat", "count": 3},
                    "secondary": [
                      {"item": "minecraft:glow_berries", "count": 2},
                      {"item": "minecraft:kelp", "count": 2}
                    ],
                    "auxiliary": [
                      {"item": "minecraft:apple", "count": 1},
                      {"item": "minecraft:sugar", "count": 1}
                    ]
                  },
                  "output_item": "test:grass_restoration_pill",
                  "output_count": 1,
                  "brewing_duration": 200,
                  "pill_effect": {
                    "max_health_fraction": 0.75,
                    "minimum_healing": 8.0,
                    "regeneration_duration": 200,
                    "regeneration_amplifier": 4
                  }
                }
                """));

        assertEquals(RECIPE_ID, recipe.id());
        assertEquals("草还丹方", recipe.name());
        assertEquals("一阶一品", recipe.grade().getChineseName());
        assertTrue(recipe.allowedRealms().contains(CultivationRealm.QI_REFINING));
        assertTrue(recipe.allowedRealms().contains(CultivationRealm.FOUNDATION_ESTABLISHMENT));
        assertEquals(CultivationRealm.QI_REFINING, recipe.minimumRefiningRealm());
        assertEquals(ResourceLocation.fromNamespaceAndPath("test", "grass_restoration_pill"), recipe.outputItem());
        assertEquals(ResourceLocation.withDefaultNamespace("wheat"), recipe.primaryIngredient().item());
        assertEquals(3, recipe.primaryIngredient().count());
        assertEquals(200, recipe.brewingDuration());
        assertEquals(0.75D, recipe.pillEffect().maxHealthFraction());
        assertEquals(4, recipe.pillEffect().regenerationAmplifier());
    }

    @Test
    void rejectsRecipesWithoutAllowedRealms() {
        assertThrows(JsonParseException.class, () -> AlchemyRecipeJsonUtility.parse(
                RECIPE_ID,
                JsonParser.parseString("""
                        {
                          "name": "空丹方",
                          "content": "内容",
                          "grade": {"tier": 1, "rank": 1},
                          "allowed_realms": [],
                          "minimum_refining_realm": "qi_refining",
                          "ingredients": {
                            "primary": {"item": "minecraft:wheat", "count": 3},
                            "secondary": [
                              {"item": "minecraft:glow_berries", "count": 2},
                              {"item": "minecraft:kelp", "count": 2}
                            ],
                            "auxiliary": [
                              {"item": "minecraft:apple", "count": 1},
                              {"item": "minecraft:sugar", "count": 1}
                            ]
                          },
                          "output_item": "test:empty_pill",
                          "brewing_duration": 200,
                          "pill_effect": {
                            "max_health_fraction": 0.75,
                            "minimum_healing": 8.0,
                            "regeneration_duration": 200,
                            "regeneration_amplifier": 4
                          }
                        }
                        """)
        ));
    }

    @Test
    void rejectsInvalidPillEffect() {
        assertThrows(JsonParseException.class, () -> AlchemyRecipeJsonUtility.parse(
                RECIPE_ID,
                JsonParser.parseString("""
                        {
                          "name": "错误丹方",
                          "content": "内容",
                          "grade": {"tier": 1, "rank": 1},
                          "allowed_realms": ["qi_refining"],
                          "minimum_refining_realm": "qi_refining",
                          "ingredients": {
                            "primary": {"item": "minecraft:wheat", "count": 3},
                            "secondary": [
                              {"item": "minecraft:glow_berries", "count": 2},
                              {"item": "minecraft:kelp", "count": 2}
                            ],
                            "auxiliary": [
                              {"item": "minecraft:apple", "count": 1},
                              {"item": "minecraft:sugar", "count": 1}
                            ]
                          },
                          "output_item": "test:invalid_pill",
                          "brewing_duration": 200,
                          "pill_effect": {
                            "max_health_fraction": 1.5,
                            "minimum_healing": 8.0,
                            "regeneration_duration": 200,
                            "regeneration_amplifier": 4
                          }
                        }
                        """)
        ));
    }

    @Test
    void rejectsRecipeWithMissingAuxiliarySlot() {
        assertThrows(JsonParseException.class, () -> AlchemyRecipeJsonUtility.parse(
                RECIPE_ID,
                JsonParser.parseString("""
                        {
                          "name": "残缺丹方",
                          "content": "内容",
                          "grade": {"tier": 1, "rank": 1},
                          "allowed_realms": ["qi_refining"],
                          "minimum_refining_realm": "qi_refining",
                          "ingredients": {
                            "primary": {"item": "minecraft:wheat", "count": 3},
                            "secondary": [
                              {"item": "minecraft:glow_berries", "count": 2},
                              {"item": "minecraft:kelp", "count": 2}
                            ],
                            "auxiliary": [
                              {"item": "minecraft:apple", "count": 1}
                            ]
                          },
                          "output_item": "test:invalid_pill",
                          "brewing_duration": 200,
                          "pill_effect": {
                            "max_health_fraction": 0.75,
                            "minimum_healing": 8.0,
                            "regeneration_duration": 200,
                            "regeneration_amplifier": 4
                          }
                        }
                        """)
        ));
    }

    @Test
    void rejectsRecipesWithoutMinimumRefiningRealm() {
        assertThrows(JsonParseException.class, () -> AlchemyRecipeJsonUtility.parse(
                RECIPE_ID,
                JsonParser.parseString("""
                        {
                          "name": "缺少炼制境界的丹方",
                          "content": "内容",
                          "grade": {"tier": 1, "rank": 1},
                          "allowed_realms": ["qi_refining"],
                          "ingredients": {
                            "primary": {"item": "minecraft:wheat", "count": 3},
                            "secondary": [
                              {"item": "minecraft:glow_berries", "count": 2},
                              {"item": "minecraft:kelp", "count": 2}
                            ],
                            "auxiliary": [
                              {"item": "minecraft:apple", "count": 1},
                              {"item": "minecraft:sugar", "count": 1}
                            ]
                          },
                          "output_item": "test:invalid_pill",
                          "brewing_duration": 200,
                          "pill_effect": {
                            "max_health_fraction": 0.75,
                            "minimum_healing": 8.0,
                            "regeneration_duration": 200,
                            "regeneration_amplifier": 4
                          }
                        }
                        """)
        ));
    }
}
