package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class AlchemyRecipeItemUtility {
    private static final String RECIPE_ID_KEY = ModMain.MOD_ID + ":alchemy_recipe_id";
    private static final String RECIPE_NAME_KEY = ModMain.MOD_ID + ":alchemy_recipe_name";
    private static final String RECIPE_DESCRIPTION_KEY = ModMain.MOD_ID + ":alchemy_recipe_description";
    private static final String RECIPE_PRIMARY_ELEMENT_KEY =
            ModMain.MOD_ID + ":alchemy_recipe_primary_element";
    private static final String RECIPE_TIER_KEY = ModMain.MOD_ID + ":alchemy_recipe_tier";
    private static final String RECIPE_RANK_KEY = ModMain.MOD_ID + ":alchemy_recipe_rank";

    public static ItemStack createStack(AlchemyRecipeDefinition recipe) {
        return createStack(
                recipe.id(),
                recipe.name(),
                recipe.description(),
                recipe.grade(),
                recipe.primaryElement()
        );
    }

    public static ItemStack createStack(AlchemyRecipeClientData recipe) {
        return createStack(
                recipe.id(),
                recipe.name(),
                recipe.description(),
                recipe.grade(),
                recipe.primaryElement()
        );
    }

    public static Optional<ResourceLocation> getRecipeId(ItemStack itemStack) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(
                itemStack.getTag().getString(RECIPE_ID_KEY)
        ));
    }

    public static boolean isRecipe(ItemStack itemStack, ResourceLocation recipeId) {
        return getRecipeId(itemStack).filter(recipeId::equals).isPresent();
    }

    public static boolean isEmptyAlchemyRecipe(ItemStack itemStack) {
        return itemStack.is(ModItems.ALCHEMY_RECIPE.get()) && getRecipeId(itemStack).isEmpty();
    }

    public static Optional<String> getRecipeName(ItemStack itemStack) {
        return getText(itemStack, RECIPE_NAME_KEY);
    }

    public static Optional<String> getRecipeDescription(ItemStack itemStack) {
        return getText(itemStack, RECIPE_DESCRIPTION_KEY);
    }

    public static Optional<ElementType> getPrimaryElement(ItemStack itemStack) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        return ElementType.fromSerializedName(
                itemStack.getTag().getString(RECIPE_PRIMARY_ELEMENT_KEY)
        );
    }

    public static AlchemyGrade getGrade(ItemStack itemStack) {
        if (!hasStoredGrade(itemStack)) {
            return AlchemyGrade.LOWEST;
        }
        try {
            return new AlchemyGrade(
                    itemStack.getTag().getInt(RECIPE_TIER_KEY),
                    itemStack.getTag().getInt(RECIPE_RANK_KEY)
            );
        } catch (IllegalArgumentException exception) {
            return AlchemyGrade.LOWEST;
        }
    }

    public static boolean hasStoredGrade(ItemStack itemStack) {
        return itemStack.is(ModItems.ALCHEMY_RECIPE.get())
                && itemStack.hasTag()
                && itemStack.getTag().contains(RECIPE_TIER_KEY)
                && itemStack.getTag().contains(RECIPE_RANK_KEY);
    }

    private static ItemStack createStack(
            ResourceLocation recipeId,
            String name,
            String description,
            AlchemyGrade grade,
            ElementType primaryElement
    ) {
        ItemStack itemStack = new ItemStack(ModItems.ALCHEMY_RECIPE.get());
        writeRecipeData(itemStack, recipeId, name, description, grade, primaryElement);
        return itemStack;
    }

    public static void imprintRecipe(ItemStack itemStack, AlchemyRecipeDefinition recipe) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get())) {
            throw new IllegalArgumentException("Only an alchemy formula jade slip can carry an alchemy recipe");
        }
        writeRecipeData(
                itemStack,
                recipe.id(),
                recipe.name(),
                recipe.description(),
                recipe.grade(),
                recipe.primaryElement()
        );
    }

    private static void writeRecipeData(
            ItemStack itemStack,
            ResourceLocation recipeId,
            String name,
            String description,
            AlchemyGrade grade,
            ElementType primaryElement
    ) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(RECIPE_ID_KEY, recipeId.toString());
        itemData.putString(RECIPE_NAME_KEY, name);
        itemData.putString(RECIPE_DESCRIPTION_KEY, description);
        itemData.putString(
                RECIPE_PRIMARY_ELEMENT_KEY,
                primaryElement == null ? "" : primaryElement.getSerializedName()
        );
        itemData.putInt(RECIPE_TIER_KEY, grade.tier());
        itemData.putInt(RECIPE_RANK_KEY, grade.rank());
    }

    private static Optional<String> getText(ItemStack itemStack, String key) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        String value = itemStack.getTag().getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
