package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueJsonUtilityTest {
    private static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            "test",
            "azure_sword"
    );

    @Test
    void parsesDataDrivenTechniqueDefinition() {
        TechniqueDefinition technique = TechniqueJsonUtility.parse(TECHNIQUE_ID, JsonParser.parseString("""
                {
                  "name": "青冥剑诀",
                  "description": "剑诀",
                  "content": "引木气入经脉，以气御剑。",
                  "cultivation_path": "test:sword_cultivation",
                  "compatible_elements": ["wood", "metal"],
                  "requirements": {
                    "minimum_cultivation": {"realm": "qi_refining", "layer": 3},
                    "minimum_soul_strength": 2,
                    "minimum_spiritual_root_elements": 2,
                    "maximum_spiritual_root_elements": 4,
                    "required_elements": ["wood"],
                    "required_techniques": ["ninecun_lingtai:basic_breathing"]
                  },
                  "comprehension": {
                    "minimum_learning_score": 60,
                    "key_concepts": ["以气御剑"],
                    "grading_criteria": ["说明运气顺序"],
                    "mastery_levels": [
                      {"id": "ninecun_lingtai:initial_understanding", "minimum_score": 60},
                      {"id": "ninecun_lingtai:proficient", "minimum_score": 80}
                    ]
                  },
                  "effects": [
                    {"type": "test:sword_damage", "parameters": {"multiplier": 1.15}}
                  ]
                }
                """));

        assertEquals(TECHNIQUE_ID, technique.id());
        assertEquals("青冥剑诀", technique.name());
        assertEquals(ResourceLocation.fromNamespaceAndPath("test", "sword_cultivation"), technique.cultivationPath());
        assertTrue(technique.compatibleElements().contains(ElementType.WOOD));
        assertEquals(CultivationRealm.QI_REFINING, technique.requirements().minimumCultivation().orElseThrow().realm());
        assertEquals(3, technique.requirements().minimumCultivation().orElseThrow().layer());
        assertEquals(2, technique.requirements().minimumSoulStrength());
        assertEquals(2, technique.requirements().minimumSpiritualRootElements());
        assertEquals(4, technique.requirements().maximumSpiritualRootElements());
        assertEquals(ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "minor_mastery"),
                technique.comprehension().getMastery(100).orElseThrow());
        assertEquals(1.15D, technique.effects().get(0).parameters().get("multiplier").getAsDouble());
        assertFalse(technique.isBreathingTechnique());
    }

    @Test
    void parsesBreathingTechnique() {
        TechniqueDefinition technique = TechniqueJsonUtility.parse(TECHNIQUE_ID, JsonParser.parseString("""
                {
                  "name": "基础吐纳诀",
                  "content": "引气入膻中。",
                  "breathing": {
                    "absorption_multiplier": 1.5
                  },
                  "comprehension": {
                    "minimum_learning_score": 60
                  }
                }
                """));

        assertTrue(technique.isBreathingTechnique());
        assertEquals(1.5D, technique.breathing().orElseThrow().absorptionMultiplier());
    }

    @Test
    void rejectsNonPositiveBreathingMultiplier() {
        assertThrows(JsonParseException.class, () -> TechniqueJsonUtility.parse(
                TECHNIQUE_ID,
                JsonParser.parseString("""
                        {
                          "name": "错误呼吸法",
                          "content": "内容",
                          "breathing": {
                            "absorption_multiplier": 0
                          },
                          "comprehension": {
                            "minimum_learning_score": 60
                          }
                        }
                        """)
        ));
    }

    @Test
    void rejectsMasteryLevelsThatDoNotBeginAtLearningScore() {
        assertThrows(JsonParseException.class, () -> TechniqueJsonUtility.parse(
                TECHNIQUE_ID,
                JsonParser.parseString("""
                        {
                          "name": "错误功法",
                          "content": "内容",
                          "comprehension": {
                            "minimum_learning_score": 60,
                            "mastery_levels": [
                              {"id": "test:proficient", "minimum_score": 80}
                            ]
                          }
                        }
                        """)
        ));
    }

    @Test
    void rejectsInvertedSpiritualRootElementRange() {
        assertThrows(JsonParseException.class, () -> TechniqueJsonUtility.parse(
                TECHNIQUE_ID,
                JsonParser.parseString("""
                        {
                          "name": "错误复合法",
                          "content": "内容",
                          "requirements": {
                            "minimum_spiritual_root_elements": 4,
                            "maximum_spiritual_root_elements": 3
                          },
                          "comprehension": {
                            "minimum_learning_score": 60
                          }
                        }
                        """)
        ));
    }
}
