package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.ImprintAlchemyRecipeToFormulaSlipPacket;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public final class AlchemyRecipeImprintScreen extends AbstractTechniqueImprintScreen {
    private static final int MINIMUM_COLUMN_WIDTH = 56;
    private static final int MAXIMUM_VISIBLE_COLUMNS = 8;
    private static final int PAGE_BUTTON_WIDTH = 68;
    private static final int ACTION_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 30;
    private static final float NAME_FONT_SCALE = 0.42F;
    private static final float DESCRIPTION_FONT_SCALE = 0.28F;

    private final InteractionHand hand;
    private final List<AlchemyRecipeClientData> recipes;

    private int layoutLeft;
    private int layoutRight;
    private int contentTop;
    private int contentBottom;
    private int actionButtonTop;
    private int previousButtonLeft;
    private int actionButtonLeft;
    private int nextButtonLeft;
    private int visibleRecipeCount;
    private int pageOffset;
    private ResourceLocation selectedRecipeId;
    private boolean submitted;

    public AlchemyRecipeImprintScreen(
            InteractionHand hand,
            List<AlchemyRecipeClientData> recipes
    ) {
        super(Component.translatable("screen.ninecun_lingtai.imprint_alchemy_recipe"));
        this.hand = hand;
        this.recipes = recipes.stream()
                .sorted(Comparator.comparing(AlchemyRecipeClientData::name)
                        .thenComparing(recipe -> recipe.id().toString()))
                .toList();
        if (!this.recipes.isEmpty()) {
            this.selectedRecipeId = this.recipes.get(0).id();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.layoutLeft = 38;
        this.layoutRight = this.width - 42;
        this.contentTop = 40;
        this.actionButtonTop = this.height - 43;
        this.contentBottom = this.actionButtonTop - 15;
        int availableWidth = Math.max(MINIMUM_COLUMN_WIDTH, this.layoutRight - this.layoutLeft);
        this.visibleRecipeCount = Mth.clamp(
                availableWidth / MINIMUM_COLUMN_WIDTH,
                1,
                MAXIMUM_VISIBLE_COLUMNS
        );
        this.pageOffset = Mth.clamp(this.pageOffset, 0, getMaximumPageOffset());
        this.previousButtonLeft = this.layoutLeft;
        this.actionButtonLeft = (this.width - ACTION_BUTTON_WIDTH) / 2;
        this.nextButtonLeft = this.layoutRight - PAGE_BUTTON_WIDTH;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long currentTime = getCurrentTime();
        renderAnimatedBackground(guiGraphics, currentTime);
        TechniqueImprintRenderUtility.drawConstellationField(
                guiGraphics,
                this.width,
                this.height,
                currentTime,
                getElementAlpha(0, 6, currentTime)
        );
        renderSideTitles(guiGraphics, currentTime);
        renderRecipeColumns(guiGraphics, mouseX, mouseY, currentTime);
        renderControls(guiGraphics, mouseX, mouseY, currentTime);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !acceptsInput()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.previousButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT
        ) && this.pageOffset > 0) {
            changePage(-1);
            return true;
        }
        if (TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT
        ) && this.pageOffset < getMaximumPageOffset()) {
            changePage(1);
            return true;
        }
        if (TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT
        )) {
            imprintSelectedRecipe();
            return true;
        }

        int visibleCount = getCurrentPageSize();
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            if (isInsideRecipeColumn(mouseX, mouseY, visibleIndex)) {
                this.selectedRecipeId = this.recipes.get(this.pageOffset + visibleIndex).id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!acceptsInput() || getMaximumPageOffset() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        changePage(delta > 0.0D ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (acceptsInput() && keyCode == GLFW.GLFW_KEY_LEFT) {
            moveSelection(1);
            return true;
        }
        if (acceptsInput() && keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveSelection(-1);
            return true;
        }
        if (acceptsInput()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            imprintSelectedRecipe();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderSideTitles(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 5, currentTime);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.blank_alchemy_recipe").getString(),
                this.width - 19.0F,
                23.0F,
                0.43F,
                19.0F,
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR,
                titleAlpha,
                8,
                true
        );
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.choose_for_alchemy_recipe").getString(),
                18.0F,
                28.0F,
                0.29F,
                14.0F,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                titleAlpha * 0.86F,
                14,
                false
        );
    }

    private void renderRecipeColumns(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        int visibleCount = getCurrentPageSize();
        if (visibleCount <= 0) {
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    Component.translatable(
                            "screen.ninecun_lingtai.no_learned_alchemy_recipes"
                    ).getString(),
                    this.width * 0.5F,
                    this.height * 0.32F,
                    0.36F,
                    17.0F,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                    getElementAlpha(2, 2, currentTime),
                    16,
                    false
            );
            return;
        }

        for (int visibleIndex = 0; visibleIndex < visibleCount - 1; visibleIndex++) {
            float connectorAlpha = Math.min(
                    getElementAlpha(
                            2 + Math.min(visibleIndex, 6),
                            1 + Math.min(visibleIndex, 4),
                            currentTime
                    ),
                    getElementAlpha(
                            2 + Math.min(visibleIndex + 1, 6),
                            1 + Math.min(visibleIndex + 1, 4),
                            currentTime
                    )
            );
            com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    new ScreenPoint(getColumnCenterX(visibleIndex), this.contentTop),
                    new ScreenPoint(getColumnCenterX(visibleIndex + 1), this.contentTop),
                    0.65F,
                    ColorUtility.withAlpha(
                            TechniqueImprintRenderUtility.GOLD_COLOR,
                            connectorAlpha * 0.34F
                    )
            );
        }

        int maximumNameSymbols = Math.max(2, (this.contentBottom - this.contentTop) / 20);
        int maximumDescriptionSymbols = Math.max(2, (this.contentBottom - this.contentTop) / 14);
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            AlchemyRecipeClientData recipe = this.recipes.get(this.pageOffset + visibleIndex);
            float centerX = getColumnCenterX(visibleIndex);
            boolean selected = recipe.id().equals(this.selectedRecipeId);
            boolean hovered = isInsideRecipeColumn(mouseX, mouseY, visibleIndex);
            float columnAlpha = getElementAlpha(
                    2 + Math.min(visibleIndex, 6),
                    1 + Math.min(visibleIndex, 4),
                    currentTime
            );
            TechniqueImprintRenderUtility.drawSeal(
                    guiGraphics,
                    centerX,
                    this.contentTop,
                    selected ? 8.0F : 6.0F,
                    selected,
                    hovered,
                    currentTime,
                    columnAlpha
            );
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    recipe.name(),
                    centerX + 14.0F,
                    this.contentTop + 17.0F,
                    NAME_FONT_SCALE,
                    19.0F,
                    selected
                            ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                            : TechniqueImprintRenderUtility.GOLD_COLOR,
                    columnAlpha,
                    maximumNameSymbols,
                    true
            );
            String description = normalizeText(recipe.description());
            if (!description.isBlank()) {
                TechniqueImprintRenderUtility.drawVerticalText(
                        guiGraphics,
                        this.font,
                        description,
                        centerX - 14.0F,
                        this.contentTop + 20.0F,
                        DESCRIPTION_FONT_SCALE,
                        13.0F,
                        TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                        columnAlpha * (selected || hovered ? 0.9F : 0.58F),
                        maximumDescriptionSymbols,
                        false
                );
            }
        }
    }

    private void renderControls(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        float controlsAlpha = getElementAlpha(
                3 + Math.min(this.visibleRecipeCount, 6),
                0,
                currentTime
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedFormulaText("screen.ninecun_lingtai.previous_page"),
                this.previousButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.pageOffset > 0,
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        this.previousButtonLeft,
                        this.actionButtonTop,
                        PAGE_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                controlsAlpha
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedFormulaText("screen.ninecun_lingtai.imprint_to_alchemy_recipe"),
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.selectedRecipeId != null && !this.submitted,
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        this.actionButtonLeft,
                        this.actionButtonTop,
                        ACTION_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                controlsAlpha
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedFormulaText("screen.ninecun_lingtai.next_page"),
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.pageOffset < getMaximumPageOffset(),
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        this.nextButtonLeft,
                        this.actionButtonTop,
                        PAGE_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                controlsAlpha
        );
    }

    private void changePage(int direction) {
        if (direction == 0) {
            return;
        }
        this.pageOffset = Mth.clamp(
                this.pageOffset + direction * this.visibleRecipeCount,
                0,
                getMaximumPageOffset()
        );
        if (getCurrentPageSize() > 0) {
            this.selectedRecipeId = this.recipes.get(this.pageOffset).id();
        }
    }

    private void moveSelection(int direction) {
        if (this.recipes.isEmpty() || direction == 0) {
            return;
        }
        int currentIndex = 0;
        if (this.selectedRecipeId != null) {
            for (int recipeIndex = 0; recipeIndex < this.recipes.size(); recipeIndex++) {
                if (this.recipes.get(recipeIndex).id().equals(this.selectedRecipeId)) {
                    currentIndex = recipeIndex;
                    break;
                }
            }
        }
        int selectedIndex = Mth.clamp(currentIndex + direction, 0, this.recipes.size() - 1);
        this.selectedRecipeId = this.recipes.get(selectedIndex).id();
        if (selectedIndex < this.pageOffset) {
            this.pageOffset = selectedIndex / this.visibleRecipeCount * this.visibleRecipeCount;
        } else if (selectedIndex >= this.pageOffset + this.visibleRecipeCount) {
            this.pageOffset = Math.min(
                    getMaximumPageOffset(),
                    selectedIndex / this.visibleRecipeCount * this.visibleRecipeCount
            );
        }
    }

    private void imprintSelectedRecipe() {
        if (this.selectedRecipeId == null || this.submitted) {
            return;
        }
        this.submitted = true;
        ModNetwork.sendToServer(new ImprintAlchemyRecipeToFormulaSlipPacket(
                this.selectedRecipeId,
                this.hand
        ));
        beginClosing();
    }

    private float getColumnCenterX(int visibleIndex) {
        int visibleCount = Math.max(1, getCurrentPageSize());
        float availableWidth = this.layoutRight - this.layoutLeft;
        return this.layoutRight - availableWidth * (visibleIndex + 0.5F) / visibleCount;
    }

    private boolean isInsideRecipeColumn(double mouseX, double mouseY, int visibleIndex) {
        float centerX = getColumnCenterX(visibleIndex);
        float halfWidth = (this.layoutRight - this.layoutLeft)
                / (float) Math.max(1, getCurrentPageSize()) * 0.46F;
        return mouseX >= centerX - halfWidth
                && mouseX < centerX + halfWidth
                && mouseY >= this.contentTop - 12
                && mouseY < this.contentBottom;
    }

    private int getCurrentPageSize() {
        return Math.max(0, Math.min(
                this.visibleRecipeCount,
                this.recipes.size() - this.pageOffset
        ));
    }

    private int getMaximumPageOffset() {
        if (this.recipes.isEmpty()) {
            return 0;
        }
        return (this.recipes.size() - 1) / this.visibleRecipeCount * this.visibleRecipeCount;
    }

    private static Component translatedFormulaText(String translationKey) {
        return Component.translatable(translationKey).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }

    private static String normalizeText(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
