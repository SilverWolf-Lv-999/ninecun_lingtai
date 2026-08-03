package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.menu.AlchemyRecipeLibraryMenu;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AlchemyRecipeLibraryScreen extends AbstractContainerScreen<AlchemyRecipeLibraryMenu> {
    private static final int PREFERRED_IMAGE_WIDTH = 320;
    private static final int PREFERRED_IMAGE_HEIGHT = 250;
    private static final int MINIMUM_IMAGE_WIDTH = 250;
    private static final int MINIMUM_IMAGE_HEIGHT = 190;
    private static final int ROW_HEIGHT = 34;
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int ROW_COLOR = 0xD9282C38;
    private static final int ROW_HOVER_COLOR = 0xE23A3E4A;
    private static final int ROW_SELECTED_COLOR = 0xE252493A;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final long DOUBLE_CLICK_INTERVAL_MILLIS = 300L;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private EditBox searchBox;
    private String searchQuery = "";
    private int scrollRow;
    private int refreshTicks;
    private ResourceLocation selectedRecipeId;
    private ResourceLocation lastClickedRecipeId;
    private long lastClickTime;

    public AlchemyRecipeLibraryScreen() {
        super(
                new AlchemyRecipeLibraryMenu(),
                Minecraft.getInstance().player.getInventory(),
                Component.translatable("screen.ninecun_lingtai.alchemy_recipe_library")
        );
        this.imageWidth = PREFERRED_IMAGE_WIDTH;
        this.imageHeight = PREFERRED_IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 34, MINIMUM_IMAGE_WIDTH, PREFERRED_IMAGE_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 34, MINIMUM_IMAGE_HEIGHT, PREFERRED_IMAGE_HEIGHT);
        super.init();
        this.searchBox = new EditBox(
                this.font,
                this.leftPos + 12,
                this.topPos + 30,
                this.imageWidth - 24,
                18,
                Component.translatable("screen.ninecun_lingtai.search_alchemy_recipes")
        );
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(
                Component.translatable("screen.ninecun_lingtai.search_alchemy_recipes")
        );
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(updatedQuery -> {
            this.searchQuery = updatedQuery;
            this.scrollRow = 0;
        });
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    protected void containerTick() {
        this.refreshTicks++;
        if (this.refreshTicks >= REFRESH_INTERVAL_TICKS) {
            this.refreshTicks = 0;
            ClientAlchemyRecipeManager.requestRefresh();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int panelRight = this.leftPos + this.imageWidth;
        int panelBottom = this.topPos + this.imageHeight;
        guiGraphics.fill(this.leftPos, this.topPos, panelRight, panelBottom, PANEL_COLOR);
        guiGraphics.renderOutline(
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.leftPos + this.imageWidth / 2,
                this.topPos + 10,
                PRIMARY_TEXT_COLOR
        );

        int listTop = getListTop();
        int listBottom = getListBottom();
        List<AlchemyRecipeClientData> filteredRecipes = getFilteredRecipes();
        int visibleRows = getVisibleRows();
        int maximumScrollRow = Math.max(0, filteredRecipes.size() - visibleRows);
        this.scrollRow = Mth.clamp(this.scrollRow, 0, maximumScrollRow);

        guiGraphics.enableScissor(this.leftPos + 8, listTop, panelRight - 8, listBottom);
        for (int visibleRowIndex = 0; visibleRowIndex < visibleRows; visibleRowIndex++) {
            int recipeIndex = this.scrollRow + visibleRowIndex;
            if (recipeIndex >= filteredRecipes.size()) {
                break;
            }
            AlchemyRecipeClientData recipe = filteredRecipes.get(recipeIndex);
            int rowTop = listTop + visibleRowIndex * ROW_HEIGHT;
            boolean hovered = mouseX >= this.leftPos + 10
                    && mouseX < panelRight - 10
                    && mouseY >= rowTop
                    && mouseY < rowTop + ROW_HEIGHT - 3;
            guiGraphics.fill(
                    this.leftPos + 10,
                    rowTop,
                    panelRight - 10,
                    rowTop + ROW_HEIGHT - 3,
                    recipe.id().equals(this.selectedRecipeId)
                            ? ROW_SELECTED_COLOR
                            : (hovered ? ROW_HOVER_COLOR : ROW_COLOR)
            );
            drawTrimmedText(
                    guiGraphics,
                    recipe.name(),
                    this.leftPos + 17,
                    rowTop + 5,
                    this.imageWidth - 34,
                    PRIMARY_TEXT_COLOR
            );
            String description = normalizeText(recipe.description());
            drawTrimmedText(
                    guiGraphics,
                    description.isBlank() ? recipe.id().toString() : description,
                    this.leftPos + 17,
                    rowTop + 18,
                    this.imageWidth - 34,
                    SECONDARY_TEXT_COLOR
            );
        }
        guiGraphics.disableScissor();

        if (!ClientAlchemyRecipeManager.isInitialized()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.loading_alchemy_recipes"),
                    this.leftPos + this.imageWidth / 2,
                    listTop + 18,
                    MUTED_TEXT_COLOR
            );
        } else if (filteredRecipes.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(this.searchQuery.isBlank()
                            ? "screen.ninecun_lingtai.no_learned_alchemy_recipes"
                            : "screen.ninecun_lingtai.no_matching_alchemy_recipes"),
                    this.leftPos + this.imageWidth / 2,
                    listTop + 18,
                    MUTED_TEXT_COLOR
            );
        }

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.double_click_alchemy_recipe_hint"),
                this.leftPos + this.imageWidth / 2,
                panelBottom - 12,
                MUTED_TEXT_COLOR
        );
        renderScrollBar(guiGraphics, filteredRecipes.size(), visibleRows, listTop, listBottom);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= this.leftPos + 10
                && mouseX < this.leftPos + this.imageWidth - 10
                && mouseY >= getListTop()
                && mouseY < getListBottom()) {
            int visibleRowIndex = ((int) mouseY - getListTop()) / ROW_HEIGHT;
            int recipeIndex = this.scrollRow + visibleRowIndex;
            List<AlchemyRecipeClientData> filteredRecipes = getFilteredRecipes();
            if (recipeIndex >= 0 && recipeIndex < filteredRecipes.size()) {
                AlchemyRecipeClientData recipe = filteredRecipes.get(recipeIndex);
                long clickTime = Util.getMillis();
                boolean doubleClick = recipe.id().equals(this.lastClickedRecipeId)
                        && clickTime - this.lastClickTime <= DOUBLE_CLICK_INTERVAL_MILLIS;
                this.selectedRecipeId = recipe.id();
                this.lastClickedRecipeId = recipe.id();
                this.lastClickTime = clickTime;
                if (doubleClick && this.minecraft != null) {
                    this.minecraft.setScreen(new AlchemyRecipeReadingScreen(recipe, this, false));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<AlchemyRecipeClientData> filteredRecipes = getFilteredRecipes();
        int maximumScrollRow = Math.max(0, filteredRecipes.size() - getVisibleRows());
        if (maximumScrollRow <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.scrollRow = Mth.clamp(
                this.scrollRow - (int) Math.signum(delta),
                0,
                maximumScrollRow
        );
        return true;
    }

    private List<AlchemyRecipeClientData> getFilteredRecipes() {
        String normalizedQuery = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        return ClientAlchemyRecipeManager.getLearnedRecipes().stream()
                .filter(recipe -> normalizedQuery.isEmpty()
                        || recipe.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || recipe.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || recipe.content().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || recipe.id().toString().contains(normalizedQuery))
                .sorted(Comparator.comparing(AlchemyRecipeClientData::name)
                        .thenComparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private void drawTrimmedText(
            GuiGraphics guiGraphics,
            String text,
            int left,
            int top,
            int maximumWidth,
            int color
    ) {
        String displayText = text;
        if (this.font.width(displayText) > maximumWidth) {
            displayText = this.font.plainSubstrByWidth(
                    displayText,
                    Math.max(0, maximumWidth - this.font.width("..."))
            ) + "...";
        }
        guiGraphics.drawString(this.font, displayText, left, top, color, false);
    }

    private void renderScrollBar(
            GuiGraphics guiGraphics,
            int recipeCount,
            int visibleRows,
            int listTop,
            int listBottom
    ) {
        int maximumScrollRow = Math.max(0, recipeCount - visibleRows);
        if (maximumScrollRow <= 0) {
            return;
        }
        int trackLeft = this.leftPos + this.imageWidth - 7;
        int trackHeight = listBottom - listTop;
        int thumbHeight = Math.max(12, trackHeight * visibleRows / recipeCount);
        int thumbTop = listTop + (trackHeight - thumbHeight) * this.scrollRow / maximumScrollRow;
        guiGraphics.fill(trackLeft, listTop, trackLeft + 2, listBottom, 0xFF2B2D35);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFF9C8768);
    }

    private int getListTop() {
        return this.topPos + 55;
    }

    private int getListBottom() {
        return this.topPos + this.imageHeight - 20;
    }

    private int getVisibleRows() {
        return Math.max(1, (getListBottom() - getListTop()) / ROW_HEIGHT);
    }

    private static String normalizeText(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
