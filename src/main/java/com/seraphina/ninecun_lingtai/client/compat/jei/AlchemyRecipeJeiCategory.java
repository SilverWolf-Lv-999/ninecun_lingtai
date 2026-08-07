package com.seraphina.ninecun_lingtai.client.compat.jei;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AlchemyRecipeJeiCategory implements IRecipeCategory<AlchemyRecipeJeiData> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 82;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public AlchemyRecipeJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ALCHEMY_FURNACE.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<AlchemyRecipeJeiData> getRecipeType() {
        return AlchemyRecipeJeiPlugin.RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ninecun_lingtai.alchemy");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            AlchemyRecipeJeiData recipe,
            IFocusGroup focuses
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 2)
                .addItemStack(createIngredientStack(recipe.primaryIngredient()));
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 28)
                .addItemStack(createIngredientStack(recipe.secondaryIngredients().get(0)));
        builder.addSlot(RecipeIngredientRole.INPUT, 70, 28)
                .addItemStack(createIngredientStack(recipe.secondaryIngredients().get(1)));
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 57)
                .addItemStack(createIngredientStack(recipe.auxiliaryIngredients().get(0)));
        builder.addSlot(RecipeIngredientRole.INPUT, 70, 57)
                .addItemStack(createIngredientStack(recipe.auxiliaryIngredients().get(1)));

        ItemStack formulaStack = createFormulaStack(recipe);
        builder.addSlot(RecipeIngredientRole.CATALYST, 102, 57)
                .addItemStack(formulaStack);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStack(formulaStack);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, 31)
                .addItemStack(AlchemyPillUtility.createStack(
                        BuiltInRegistries.ITEM.get(recipe.outputItem()),
                        recipe.outputCount(),
                        recipe.grade()
                ));
    }

    @Override
    public void draw(
            AlchemyRecipeJeiData recipe,
            mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        arrow.draw(guiGraphics, 108, 32);
    }

    private static ItemStack createIngredientStack(AlchemyIngredient ingredient) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ingredient.item()), ingredient.count());
    }

    private static ItemStack createFormulaStack(AlchemyRecipeJeiData recipe) {
        return ClientAlchemyRecipeManager.getRecipe(recipe.id())
                .map(AlchemyRecipeItemUtility::createStack)
                .orElseGet(() -> new ItemStack(ModItems.ALCHEMY_RECIPE.get()));
    }
}
