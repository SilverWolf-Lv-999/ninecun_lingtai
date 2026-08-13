package net.seraphina.ninecun_lingtai.client.compat.jei;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatBand;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AlchemyRecipeJeiCategory implements IRecipeCategory<AlchemyRecipeJeiData> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 124;

    private static final int INFORMATION_LEFT = 6;
    private static final int INFORMATION_RIGHT = 170;
    private static final int SECOND_COLUMN_LEFT = 92;
    private static final int SEPARATOR_TOP = 84;
    private static final int GRADE_LINE_TOP = 89;
    private static final int HEAT_LINE_TOP = 100;
    private static final int DURATION_LINE_TOP = 111;

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int SEPARATOR_COLOR = 0x30000000;

    private static final int GENTLE_BAND_COLOR = 0xFF8B3A2E;
    private static final int MEDIUM_BAND_COLOR = 0xFFE08030;
    private static final int FIERCE_BAND_COLOR = 0xFFF0D048;
    private static final int EMPTY_BAND_COLOR = 0xFF3A3835;
    private static final int BAND_FRAME_COLOR = 0xFF1A1815;
    private static final int HEAT_BAND_HEIGHT = 5;

    private final IDrawable icon;
    private final IDrawable arrow;

    public AlchemyRecipeJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ALCHEMY_FURNACE.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<AlchemyRecipeJeiData> getRecipeType() {
        return AlchemyRecipeJeiPlugin.RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ninecun_lingtai.alchemy");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
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
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        arrow.draw(guiGraphics, 108, 32);
        guiGraphics.fill(INFORMATION_LEFT, SEPARATOR_TOP, INFORMATION_RIGHT, SEPARATOR_TOP + 1, SEPARATOR_COLOR);

        Font font = Minecraft.getInstance().font;
        Optional<AlchemyRecipeClientData> clientData = ClientAlchemyRecipeManager.getRecipe(recipe.id());
        drawFeedLabels(guiGraphics, font, clientData);
        drawGradeLine(guiGraphics, font, recipe, clientData);
        drawHeatCurveLine(guiGraphics, font, clientData);
        drawDurationLine(guiGraphics, font, clientData);
    }

    private static void drawFeedLabels(
            GuiGraphics guiGraphics,
            Font font,
            Optional<AlchemyRecipeClientData> clientData
    ) {
        if (clientData.isEmpty()) {
            return;
        }
        drawFeedLabel(
                guiGraphics,
                font,
                clientData.get(),
                AlchemyIngredientSlot.PRIMARY,
                56,
                7
        );
        drawFeedLabel(
                guiGraphics,
                font,
                clientData.get(),
                AlchemyIngredientSlot.SECONDARY_1,
                24,
                33
        );
        drawFeedLabel(
                guiGraphics,
                font,
                clientData.get(),
                AlchemyIngredientSlot.SECONDARY_2,
                88,
                33
        );
        drawFeedLabel(
                guiGraphics,
                font,
                clientData.get(),
                AlchemyIngredientSlot.AUXILIARY_1,
                24,
                62
        );
        drawFeedLabel(
                guiGraphics,
                font,
                clientData.get(),
                AlchemyIngredientSlot.AUXILIARY_2,
                88,
                62
        );
    }

    private static void drawFeedLabel(
            GuiGraphics guiGraphics,
            Font font,
            AlchemyRecipeClientData recipe,
            AlchemyIngredientSlot slot,
            int x,
            int y
    ) {
        Component label = recipe.feedPlan().stream()
                .filter(step -> step.slot() == slot)
                .findFirst()
                .map(AlchemyFeedStep::stageIndex)
                .map(stageIndex -> Component.translatable(
                        "jei.ninecun_lingtai.alchemy.feed.stage_short",
                        stageIndex + 1
                ))
                .orElseGet(() -> Component.translatable(
                        "jei.ninecun_lingtai.alchemy.feed.initial_short"
                ));
        int width = font.width(label);
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + 9, 0xB8FFFFFF);
        guiGraphics.drawString(font, label, x, y, TEXT_COLOR, false);
    }

    private static void drawGradeLine(
            GuiGraphics guiGraphics,
            Font font,
            AlchemyRecipeJeiData recipe,
            Optional<AlchemyRecipeClientData> clientData
    ) {
        guiGraphics.drawString(
                font,
                Component.translatable("jei.ninecun_lingtai.alchemy.grade", recipe.grade().getChineseName()),
                INFORMATION_LEFT,
                GRADE_LINE_TOP,
                TEXT_COLOR,
                false
        );
        if (clientData.isEmpty()) {
            return;
        }
        Component elementText = clientData.get().getPrimaryElement()
                .map(element -> Component.literal(SpiritualRootUtility.getElementName(element)))
                .orElseGet(() -> Component.translatable("jei.ninecun_lingtai.alchemy.primary_element.none"));
        guiGraphics.drawString(
                font,
                Component.translatable("jei.ninecun_lingtai.alchemy.primary_element", elementText),
                SECOND_COLUMN_LEFT,
                GRADE_LINE_TOP,
                TEXT_COLOR,
                false
        );
    }

    private static void drawHeatCurveLine(
            GuiGraphics guiGraphics,
            Font font,
            Optional<AlchemyRecipeClientData> clientData
    ) {
        if (clientData.isEmpty()) {
            return;
        }
        Component label = Component.translatable("jei.ninecun_lingtai.alchemy.heat_curve");
        guiGraphics.drawString(font, label, INFORMATION_LEFT, HEAT_LINE_TOP, TEXT_COLOR, false);

        int bandLeft = INFORMATION_LEFT + font.width(label) + 5;
        int bandTop = HEAT_LINE_TOP + 1;
        int bandBottom = bandTop + HEAT_BAND_HEIGHT;
        guiGraphics.fill(bandLeft - 1, bandTop - 1, INFORMATION_RIGHT + 1, bandBottom + 1, BAND_FRAME_COLOR);

        List<HeatStage> heatStages = clientData.get().heatStages();
        if (heatStages.isEmpty()) {
            guiGraphics.fill(bandLeft, bandTop, INFORMATION_RIGHT, bandBottom, EMPTY_BAND_COLOR);
            return;
        }
        double totalPortion = heatStages.stream().mapToDouble(HeatStage::portion).sum();
        int bandWidth = INFORMATION_RIGHT - bandLeft;
        double accumulatedPortion = 0.0D;
        int segmentLeft = bandLeft;
        for (int stageIndex = 0; stageIndex < heatStages.size(); stageIndex++) {
            HeatStage stage = heatStages.get(stageIndex);
            accumulatedPortion += stage.portion();
            int segmentRight = stageIndex == heatStages.size() - 1
                    ? INFORMATION_RIGHT
                    : bandLeft + (int) Math.round(bandWidth * accumulatedPortion / totalPortion);
            int fillLeft = stageIndex == 0 ? segmentLeft : segmentLeft + 1;
            if (fillLeft < segmentRight) {
                guiGraphics.fill(fillLeft, bandTop, segmentRight, bandBottom, getBandColor(stage.band()));
            }
            segmentLeft = segmentRight;
        }
    }

    private static void drawDurationLine(
            GuiGraphics guiGraphics,
            Font font,
            Optional<AlchemyRecipeClientData> clientData
    ) {
        if (clientData.isEmpty()) {
            return;
        }
        AlchemyRecipeClientData recipeData = clientData.get();
        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.ninecun_lingtai.alchemy.duration",
                        formatBrewingDuration(recipeData.brewingDuration())
                ),
                INFORMATION_LEFT,
                DURATION_LINE_TOP,
                TEXT_COLOR,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.ninecun_lingtai.alchemy.toxin",
                        String.format(Locale.ROOT, "%.1f", recipeData.pillToxin())
                ),
                SECOND_COLUMN_LEFT,
                DURATION_LINE_TOP,
                TEXT_COLOR,
                false
        );
    }

    private static Component formatBrewingDuration(int brewingDuration) {
        long totalSeconds = Math.max(0L, (brewingDuration + 19L) / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0L) {
            return Component.translatable("jei.ninecun_lingtai.alchemy.duration.seconds", seconds);
        }
        if (seconds == 0L) {
            return Component.translatable("jei.ninecun_lingtai.alchemy.duration.minutes", minutes);
        }
        return Component.translatable("jei.ninecun_lingtai.alchemy.duration.minutes_seconds", minutes, seconds);
    }

    private static int getBandColor(HeatBand band) {
        return switch (band) {
            case GENTLE -> GENTLE_BAND_COLOR;
            case MEDIUM -> MEDIUM_BAND_COLOR;
            case FIERCE -> FIERCE_BAND_COLOR;
        };
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
