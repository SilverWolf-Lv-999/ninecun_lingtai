package com.seraphina.ninecun_lingtai.api.alchemy;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlchemyRecipeJeiDataTest {
    private static final ResourceLocation RECIPE_ID = ResourceLocation.fromNamespaceAndPath(
            "test",
            "restoration_pill"
    );

    @Test
    void copiesJeiDisplayDataFromRecipeDefinition() {
        AlchemyRecipeDefinition definition = new AlchemyRecipeDefinition(
                RECIPE_ID,
                "回元丹方",
                "疗伤丹方",
                "丹方内容",
                new AlchemyGrade(1, 1),
                Set.of(CultivationRealm.QI_REFINING),
                CultivationRealm.QI_REFINING,
                new AlchemyIngredient(ResourceLocation.withDefaultNamespace("grass"), 6),
                List.of(
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("fern"), 4),
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("moss_block"), 2)
                ),
                List.of(
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("azalea_leaves"), 4),
                        new AlchemyIngredient(
                                ResourceLocation.withDefaultNamespace("flowering_azalea_leaves"),
                                4
                        )
                ),
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "grass_restoration_pill"),
                1,
                200,
                new AlchemyPillEffect(0.1D, 3.0D, 0, 0)
        );

        AlchemyRecipeJeiData jeiData = AlchemyRecipeJeiData.fromDefinition(definition);

        assertEquals(RECIPE_ID, jeiData.id());
        assertEquals(definition.primaryIngredient(), jeiData.primaryIngredient());
        assertEquals(definition.secondaryIngredients(), jeiData.secondaryIngredients());
        assertEquals(definition.auxiliaryIngredients(), jeiData.auxiliaryIngredients());
        assertEquals(definition.outputItem(), jeiData.outputItem());
        assertEquals(definition.outputCount(), jeiData.outputCount());
        assertEquals(definition.grade(), jeiData.grade());
    }

    @Test
    void rejectsMissingSecondaryIngredientSlot() {
        assertThrows(IllegalArgumentException.class, () -> new AlchemyRecipeJeiData(
                RECIPE_ID,
                new AlchemyIngredient(ResourceLocation.withDefaultNamespace("grass"), 1),
                List.of(new AlchemyIngredient(ResourceLocation.withDefaultNamespace("fern"), 1)),
                List.of(
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("moss_block"), 1),
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("dirt"), 1)
                ),
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "grass_restoration_pill"),
                1,
                AlchemyGrade.LOWEST
        ));
    }
}
