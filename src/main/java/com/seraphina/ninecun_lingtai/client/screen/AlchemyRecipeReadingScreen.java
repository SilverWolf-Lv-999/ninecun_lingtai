package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.VerticalTextUtility;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.LearnAlchemyRecipePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

public final class AlchemyRecipeReadingScreen extends AbstractTechniqueImprintScreen {
    private static final float BODY_FONT_SCALE = 0.375F;
    private static final float TITLE_FONT_SCALE = 0.46F;
    private static final int BODY_LINE_HEIGHT = 18;
    private static final int COLUMN_GAP = 24;
    private static final int PAGE_BUTTON_WIDTH = 64;
    private static final int ACTION_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 30;

    private final AlchemyRecipeClientData recipe;
    @Nullable
    private final Screen parentScreen;
    private final boolean allowLearning;

    private List<List<String>> columns = List.of();
    private int columnOffset;
    private int maximumColumnOffset;
    private int layoutLeft;
    private int layoutRight;
    private int contentTop;
    private int contentBottom;
    private int actionButtonTop;
    private int previousButtonLeft;
    private int actionButtonLeft;
    private int nextButtonLeft;
    private boolean awaitingLearning;
    private int learningRequestRevision;

    public AlchemyRecipeReadingScreen(
            AlchemyRecipeClientData recipe,
            @Nullable Screen parentScreen,
            boolean allowLearning
    ) {
        super(Component.literal(recipe.name()));
        this.recipe = recipe;
        this.parentScreen = parentScreen;
        this.allowLearning = allowLearning;
    }

    @Override
    protected void init() {
        super.init();
        int layoutWidth = Mth.clamp(this.width - 32, 260, 620);
        this.layoutLeft = (this.width - layoutWidth) / 2;
        this.layoutRight = this.layoutLeft + layoutWidth;
        this.actionButtonTop = this.height - 43;
        this.contentTop = 30;
        this.contentBottom = this.actionButtonTop - 18;
        this.previousButtonLeft = this.layoutLeft + 8;
        this.actionButtonLeft = (this.width - ACTION_BUTTON_WIDTH) / 2;
        this.nextButtonLeft = this.layoutRight - PAGE_BUTTON_WIDTH - 8;

        int maximumRows = Math.max(1, (this.contentBottom - this.contentTop) / BODY_LINE_HEIGHT);
        this.columns = VerticalTextUtility.splitIntoColumns(this.recipe.content(), maximumRows);
        int visibleColumns = getVisibleColumnCount();
        this.maximumColumnOffset = Math.max(0, this.columns.size() - visibleColumns);
        this.columnOffset = Mth.clamp(this.columnOffset, 0, this.maximumColumnOffset);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.awaitingLearning
                && ClientAlchemyRecipeManager.getRevision() > this.learningRequestRevision) {
            this.awaitingLearning = false;
            beginClosing();
        }
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
                getElementAlpha(0, 5, currentTime)
        );
        renderRecipeMetadata(guiGraphics, currentTime);
        renderRecipeTitle(guiGraphics, currentTime);
        renderRecipeContent(guiGraphics, currentTime);
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
        ) && this.columnOffset > 0) {
            this.columnOffset = Math.max(0, this.columnOffset - getVisibleColumnCount());
            return true;
        }
        if (TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT
        ) && this.columnOffset < this.maximumColumnOffset) {
            this.columnOffset = Math.min(
                    this.maximumColumnOffset,
                    this.columnOffset + getVisibleColumnCount()
            );
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
            activateActionButton();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!acceptsInput() || this.maximumColumnOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.columnOffset = Mth.clamp(
                this.columnOffset - (int) Math.signum(delta),
                0,
                this.maximumColumnOffset
        );
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (acceptsInput() && keyCode == GLFW.GLFW_KEY_LEFT && this.columnOffset > 0) {
            this.columnOffset--;
            return true;
        }
        if (acceptsInput()
                && keyCode == GLFW.GLFW_KEY_RIGHT
                && this.columnOffset < this.maximumColumnOffset) {
            this.columnOffset++;
            return true;
        }
        if (acceptsInput()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            activateActionButton();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Nullable
    @Override
    protected Screen getParentScreen() {
        return this.parentScreen;
    }

    private void renderRecipeMetadata(GuiGraphics guiGraphics, long currentTime) {
        float metadataAlpha = getElementAlpha(1, 4, currentTime);
        float metadataX = this.layoutLeft + 16.0F;
        int maximumSymbols = Math.max(1, (this.contentBottom - this.contentTop) / 18);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                this.recipe.grade().getChineseName(),
                metadataX,
                this.contentTop + 8.0F,
                BODY_FONT_SCALE,
                18.0F,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                metadataAlpha,
                maximumSymbols,
                false
        );
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                this.recipe.getAllowedRealmNames(),
                metadataX + 20.0F,
                this.contentTop + 8.0F,
                BODY_FONT_SCALE,
                18.0F,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                metadataAlpha * 0.82F,
                maximumSymbols,
                false
        );
    }

    private void renderRecipeTitle(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 4, currentTime);
        float titleX = this.layoutRight - 16.0F;
        TechniqueImprintRenderUtility.drawSeal(
                guiGraphics,
                titleX,
                this.contentTop - 7.0F,
                6.0F,
                true,
                false,
                currentTime,
                titleAlpha
        );
        int maximumSymbols = Math.max(1, (this.contentBottom - this.contentTop) / 20);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                this.recipe.name(),
                titleX,
                this.contentTop + 7.0F,
                TITLE_FONT_SCALE,
                20.0F,
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR,
                titleAlpha,
                maximumSymbols,
                true
        );
    }

    private void renderRecipeContent(GuiGraphics guiGraphics, long currentTime) {
        int visibleColumns = getVisibleColumnCount();
        int renderedColumns = Math.min(visibleColumns, this.columns.size() - this.columnOffset);
        float contentRight = this.layoutRight - 48.0F;
        for (int visibleColumnIndex = 0; visibleColumnIndex < renderedColumns; visibleColumnIndex++) {
            List<String> column = this.columns.get(this.columnOffset + visibleColumnIndex);
            float columnX = contentRight - visibleColumnIndex * COLUMN_GAP;
            float columnAlpha = getElementAlpha(
                    2 + Math.min(visibleColumnIndex, 6),
                    1 + Math.min(visibleColumnIndex, 4),
                    currentTime
            );
            TechniqueImprintRenderUtility.drawSeal(
                    guiGraphics,
                    columnX,
                    this.contentTop - 7.0F,
                    3.1F,
                    false,
                    false,
                    currentTime,
                    columnAlpha * 0.72F
            );
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    String.join("", column),
                    columnX,
                    this.contentTop,
                    BODY_FONT_SCALE,
                    BODY_LINE_HEIGHT,
                    TechniqueImprintRenderUtility.GOLD_COLOR,
                    columnAlpha,
                    column.size(),
                    false
            );
        }
    }

    private void renderControls(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        float controlsAlpha = getElementAlpha(
                3 + Math.min(getVisibleColumnCount(), 6),
                0,
                currentTime
        );
        boolean previousHovered = TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.previousButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
        boolean actionHovered = TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
        boolean nextHovered = TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.previous_page"),
                this.previousButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.columnOffset > 0,
                previousHovered,
                currentTime,
                controlsAlpha
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                getActionButtonText(),
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                isActionButtonActive(),
                actionHovered,
                currentTime,
                controlsAlpha
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.next_page"),
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.columnOffset < this.maximumColumnOffset,
                nextHovered,
                currentTime,
                controlsAlpha
        );
    }

    private Component getActionButtonText() {
        if (!this.allowLearning) {
            return Component.translatable("screen.ninecun_lingtai.return");
        }
        if (ClientAlchemyRecipeManager.isLearned(this.recipe.id())) {
            return Component.translatable("screen.ninecun_lingtai.alchemy_recipe_learned");
        }
        if (this.awaitingLearning) {
            return Component.translatable("screen.ninecun_lingtai.learning_alchemy_recipe");
        }
        return Component.translatable("screen.ninecun_lingtai.learn_alchemy_recipe");
    }

    private boolean isActionButtonActive() {
        return !this.awaitingLearning;
    }

    private void activateActionButton() {
        if (this.awaitingLearning) {
            return;
        }
        if (!this.allowLearning || ClientAlchemyRecipeManager.isLearned(this.recipe.id())) {
            beginClosing();
            return;
        }
        this.awaitingLearning = true;
        this.learningRequestRevision = ClientAlchemyRecipeManager.getRevision();
        ModNetwork.sendToServer(new LearnAlchemyRecipePacket(this.recipe.id()));
    }

    private int getVisibleColumnCount() {
        return Math.max(1, (this.layoutRight - this.layoutLeft - 116) / COLUMN_GAP);
    }
}
