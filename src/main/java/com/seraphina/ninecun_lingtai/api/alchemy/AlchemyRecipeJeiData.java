package com.seraphina.ninecun_lingtai.api.alchemy;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 客户端 JEI 展示所需的已学丹方数据。
 */
public record AlchemyRecipeJeiData(
        ResourceLocation id,
        AlchemyIngredient primaryIngredient,
        List<AlchemyIngredient> secondaryIngredients,
        List<AlchemyIngredient> auxiliaryIngredients,
        ResourceLocation outputItem,
        int outputCount,
        AlchemyGrade grade
) {
    public AlchemyRecipeJeiData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(primaryIngredient, "primaryIngredient");
        secondaryIngredients = requireIngredientPair(secondaryIngredients, "secondaryIngredients");
        auxiliaryIngredients = requireIngredientPair(auxiliaryIngredients, "auxiliaryIngredients");
        Objects.requireNonNull(outputItem, "outputItem");
        if (outputCount < 1 || outputCount > 64) {
            throw new IllegalArgumentException("Alchemy output count must be between one and 64");
        }
        Objects.requireNonNull(grade, "grade");
    }

    public static AlchemyRecipeJeiData fromDefinition(AlchemyRecipeDefinition recipe) {
        Objects.requireNonNull(recipe, "recipe");
        return new AlchemyRecipeJeiData(
                recipe.id(),
                recipe.primaryIngredient(),
                recipe.secondaryIngredients(),
                recipe.auxiliaryIngredients(),
                recipe.outputItem(),
                recipe.outputCount(),
                recipe.grade()
        );
    }

    private static List<AlchemyIngredient> requireIngredientPair(
            List<AlchemyIngredient> ingredients,
            String fieldName
    ) {
        Objects.requireNonNull(ingredients, fieldName);
        List<AlchemyIngredient> copiedIngredients = List.copyOf(ingredients);
        if (copiedIngredients.size() != 2) {
            throw new IllegalArgumentException(
                    "Alchemy recipe must define exactly two " + fieldName
            );
        }
        return copiedIngredients;
    }
}
