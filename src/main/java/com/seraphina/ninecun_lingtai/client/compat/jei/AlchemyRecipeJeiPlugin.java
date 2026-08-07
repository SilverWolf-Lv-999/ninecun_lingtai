package com.seraphina.ninecun_lingtai.client.compat.jei;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public final class AlchemyRecipeJeiPlugin implements IModPlugin {
    public static final RecipeType<AlchemyRecipeJeiData> RECIPE_TYPE = RecipeType.create(
            ModMain.MOD_ID,
            "alchemy",
            AlchemyRecipeJeiData.class
    );

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "alchemy");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(ModItems.ALCHEMY_RECIPE.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlchemyRecipeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<AlchemyRecipeJeiData> recipes = ClientAlchemyRecipeManager.getLearnedJeiRecipes();
        registration.addRecipes(RECIPE_TYPE, recipes);
        AlchemyRecipeJeiRuntimeManager.setInitialRecipes(recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.ALCHEMY_FURNACE.get(), RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        AlchemyRecipeJeiRuntimeManager.onRuntimeAvailable(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        AlchemyRecipeJeiRuntimeManager.onRuntimeUnavailable();
    }
}
