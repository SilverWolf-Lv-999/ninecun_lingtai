package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.utility.CultivationRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.LearnTechniquePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

public final class TechniqueLearningScreen extends AbstractTechniqueImprintScreen {
    private static final int MINIMUM_COLUMN_WIDTH = 56;
    private static final int MAXIMUM_VISIBLE_COLUMNS = 8;
    private static final int RETURN_BUTTON_WIDTH = 68;
    private static final int PAGE_BUTTON_WIDTH = 68;
    private static final int ACTION_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 30;
    private static final int INPUT_HEIGHT = 18;
    private static final int MAXIMUM_UNDERSTANDING_LENGTH = 1000;
    private static final float NAME_FONT_SCALE = 0.42F;
    private static final float DESCRIPTION_FONT_SCALE = 0.28F;

    @Nullable
    private final Screen parentScreen;
    private final int feedbackBaselineRevision;

    @Nullable
    private CultivationTextInput understandingInput;
    private int layoutLeft;
    private int layoutRight;
    private int contentTop;
    private int contentBottom;
    private int feedbackTop;
    private int inputLeft;
    private int inputTop;
    private int inputWidth;
    private int actionButtonTop;
    private int returnButtonLeft;
    private int previousButtonLeft;
    private int actionButtonLeft;
    private int nextButtonLeft;
    private int visibleTechniqueCount;
    private int pageOffset;
    @Nullable
    private ResourceLocation selectedTechniqueId;
    private boolean awaitingResult;
    private int submitRequestRevision;

    public TechniqueLearningScreen(@Nullable Screen parentScreen) {
        super(Component.translatable("screen.ninecun_lingtai.learn.title"));
        this.parentScreen = parentScreen;
        this.feedbackBaselineRevision = ClientCultivationFeedbackManager.getRevision();
    }

    @Override
    protected void init() {
        super.init();
        this.layoutLeft = 38;
        this.layoutRight = this.width - 42;
        this.actionButtonTop = this.height - 43;
        this.inputWidth = Mth.clamp(this.width - 300, 200, 400);
        this.inputLeft = (this.width - this.inputWidth) / 2;
        this.inputTop = this.actionButtonTop - 28;
        this.feedbackTop = this.inputTop - 36;
        this.contentTop = 40;
        this.contentBottom = this.inputTop - 44;
        int availableWidth = Math.max(MINIMUM_COLUMN_WIDTH, this.layoutRight - this.layoutLeft);
        this.visibleTechniqueCount = Mth.clamp(
                availableWidth / MINIMUM_COLUMN_WIDTH,
                1,
                MAXIMUM_VISIBLE_COLUMNS
        );
        this.returnButtonLeft = this.layoutLeft;
        this.previousButtonLeft = this.layoutLeft + RETURN_BUTTON_WIDTH + 8;
        this.actionButtonLeft = (this.width - ACTION_BUTTON_WIDTH) / 2;
        this.nextButtonLeft = this.layoutRight - PAGE_BUTTON_WIDTH;

        if (this.understandingInput == null) {
            this.understandingInput = new CultivationTextInput(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.learn.input_hint"),
                    MAXIMUM_UNDERSTANDING_LENGTH
            );
        }
        this.understandingInput.setBounds(
                this.inputLeft,
                this.inputTop,
                this.inputWidth,
                INPUT_HEIGHT
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (this.awaitingResult
                && ClientCultivationFeedbackManager.getRevision() > this.submitRequestRevision) {
            this.awaitingResult = false;
            if (ClientCultivationFeedbackManager.isSuccess() && this.understandingInput != null) {
                this.understandingInput.setValue("");
            }
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
                getElementAlpha(0, 6, currentTime)
        );
        renderSideTitles(guiGraphics, currentTime);
        List<TechniqueClientData> techniques = getTechniques();
        ensureSelection(techniques);
        renderTechniqueColumns(guiGraphics, techniques, mouseX, mouseY, currentTime);
        renderFeedback(guiGraphics, currentTime);
        renderInput(guiGraphics, currentTime);
        renderControls(guiGraphics, techniques, mouseX, mouseY, currentTime);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !acceptsInput()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.understandingInput != null && this.understandingInput.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        List<TechniqueClientData> techniques = getTechniques();
        if (isInside(mouseX, mouseY, this.returnButtonLeft, this.actionButtonTop, RETURN_BUTTON_WIDTH)) {
            beginClosing();
            return true;
        }
        if (isInside(mouseX, mouseY, this.previousButtonLeft, this.actionButtonTop, PAGE_BUTTON_WIDTH)
                && this.pageOffset > 0) {
            changePage(-1, techniques);
            return true;
        }
        if (isInside(mouseX, mouseY, this.nextButtonLeft, this.actionButtonTop, PAGE_BUTTON_WIDTH)
                && this.pageOffset < getMaximumPageOffset(techniques)) {
            changePage(1, techniques);
            return true;
        }
        if (isInside(mouseX, mouseY, this.actionButtonLeft, this.actionButtonTop, ACTION_BUTTON_WIDTH)) {
            submitUnderstanding();
            return true;
        }
        int visibleCount = getCurrentPageSize(techniques);
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            if (isInsideTechniqueColumn(mouseX, mouseY, visibleIndex, techniques)) {
                this.selectedTechniqueId = techniques.get(this.pageOffset + visibleIndex).id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<TechniqueClientData> techniques = getTechniques();
        if (!acceptsInput() || getMaximumPageOffset(techniques) <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        changePage(delta > 0.0D ? -1 : 1, techniques);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (acceptsInput()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitUnderstanding();
            return true;
        }
        if (acceptsInput()
                && this.understandingInput != null
                && this.understandingInput.keyPressed(keyCode, modifiers)) {
            return true;
        }
        boolean typing = this.understandingInput != null && this.understandingInput.isFocused();
        if (acceptsInput() && !typing && keyCode == GLFW.GLFW_KEY_LEFT) {
            moveSelection(1);
            return true;
        }
        if (acceptsInput() && !typing && keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveSelection(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (acceptsInput()
                && this.understandingInput != null
                && this.understandingInput.charTyped(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Nullable
    @Override
    protected Screen getParentScreen() {
        return this.parentScreen;
    }

    private void renderSideTitles(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 5, currentTime);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.learn.title").getString(),
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
                Component.translatable("screen.ninecun_lingtai.learn.choose_hint").getString(),
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

    private void renderTechniqueColumns(
            GuiGraphics guiGraphics,
            List<TechniqueClientData> techniques,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        int visibleCount = getCurrentPageSize(techniques);
        if (visibleCount <= 0) {
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.learn.no_techniques").getString(),
                    this.width * 0.5F,
                    this.height * 0.24F,
                    0.36F,
                    17.0F,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                    getElementAlpha(2, 2, currentTime),
                    16,
                    false
            );
            return;
        }

        int maximumNameSymbols = Math.max(2, (this.contentBottom - this.contentTop) / 20);
        int maximumDescriptionSymbols = Math.max(2, (this.contentBottom - this.contentTop) / 14);
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            TechniqueClientData technique = techniques.get(this.pageOffset + visibleIndex);
            float centerX = getColumnCenterX(visibleIndex, techniques);
            boolean selected = technique.id().equals(this.selectedTechniqueId);
            boolean hovered = isInsideTechniqueColumn(mouseX, mouseY, visibleIndex, techniques);
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
                    technique.name(),
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
            String description = normalizeText(technique.description());
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

    private void renderFeedback(GuiGraphics guiGraphics, long currentTime) {
        if (ClientCultivationFeedbackManager.getRevision() <= this.feedbackBaselineRevision) {
            return;
        }
        ClientCultivationFeedbackManager.getMessage().ifPresent(message -> CultivationRenderUtility.drawWrappedText(
                guiGraphics,
                this.font,
                Component.empty()
                        .append(message)
                        .withStyle(style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)),
                this.width / 2.0F,
                this.feedbackTop,
                (int) (this.width * 0.66F),
                0.28F,
                ClientCultivationFeedbackManager.isSuccess()
                        ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                        : TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                getElementAlpha(2, 2, currentTime),
                2
        ));
    }

    private void renderInput(GuiGraphics guiGraphics, long currentTime) {
        if (this.understandingInput == null) {
            return;
        }
        float inputAlpha = getElementAlpha(3, 1, currentTime);
        this.understandingInput.setEditable(!this.awaitingResult);
        CultivationRenderUtility.drawInputBoxFrame(
                guiGraphics,
                this.inputLeft,
                this.inputTop,
                this.inputWidth,
                INPUT_HEIGHT,
                this.understandingInput.isFocused(),
                currentTime,
                inputAlpha
        );
        this.understandingInput.render(guiGraphics, currentTime, inputAlpha);
    }

    private void renderControls(
            GuiGraphics guiGraphics,
            List<TechniqueClientData> techniques,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        float controlsAlpha = getElementAlpha(4, 0, currentTime);
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedTechniqueText("screen.ninecun_lingtai.return"),
                this.returnButtonLeft,
                this.actionButtonTop,
                RETURN_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                true,
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        this.returnButtonLeft,
                        this.actionButtonTop,
                        RETURN_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                controlsAlpha
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedTechniqueText("screen.ninecun_lingtai.previous_page"),
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
                translatedTechniqueText(this.awaitingResult
                        ? "screen.ninecun_lingtai.learn.submitting"
                        : "screen.ninecun_lingtai.learn.submit"),
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                canSubmit(),
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
                translatedTechniqueText("screen.ninecun_lingtai.next_page"),
                this.nextButtonLeft,
                this.actionButtonTop,
                PAGE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.pageOffset < getMaximumPageOffset(techniques),
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

    private List<TechniqueClientData> getTechniques() {
        return ClientTechniqueManager.getImprintedUnlearnedTechniques();
    }

    private void ensureSelection(List<TechniqueClientData> techniques) {
        this.pageOffset = Mth.clamp(this.pageOffset, 0, getMaximumPageOffset(techniques));
        if (techniques.isEmpty()) {
            this.selectedTechniqueId = null;
            return;
        }
        if (this.selectedTechniqueId == null || techniques.stream()
                .noneMatch(technique -> technique.id().equals(this.selectedTechniqueId))) {
            this.selectedTechniqueId = techniques.get(
                    Math.min(this.pageOffset, techniques.size() - 1)
            ).id();
        }
    }

    private void submitUnderstanding() {
        if (!canSubmit() || this.understandingInput == null || this.selectedTechniqueId == null) {
            return;
        }
        String understanding = this.understandingInput.getValue().trim();
        if (understanding.isBlank()) {
            return;
        }
        this.awaitingResult = true;
        this.submitRequestRevision = ClientCultivationFeedbackManager.getRevision();
        ModNetwork.sendToServer(new LearnTechniquePacket(this.selectedTechniqueId, understanding));
    }

    private boolean canSubmit() {
        return !this.awaitingResult
                && this.selectedTechniqueId != null
                && this.understandingInput != null
                && !this.understandingInput.getValue().isBlank();
    }

    private void changePage(int direction, List<TechniqueClientData> techniques) {
        if (direction == 0) {
            return;
        }
        this.pageOffset = Mth.clamp(
                this.pageOffset + direction * this.visibleTechniqueCount,
                0,
                getMaximumPageOffset(techniques)
        );
        if (getCurrentPageSize(techniques) > 0) {
            this.selectedTechniqueId = techniques.get(this.pageOffset).id();
        }
    }

    private void moveSelection(int direction) {
        List<TechniqueClientData> techniques = getTechniques();
        if (techniques.isEmpty() || direction == 0) {
            return;
        }
        int currentIndex = 0;
        if (this.selectedTechniqueId != null) {
            for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
                if (techniques.get(techniqueIndex).id().equals(this.selectedTechniqueId)) {
                    currentIndex = techniqueIndex;
                    break;
                }
            }
        }
        int selectedIndex = Mth.clamp(currentIndex + direction, 0, techniques.size() - 1);
        this.selectedTechniqueId = techniques.get(selectedIndex).id();
        if (selectedIndex < this.pageOffset) {
            this.pageOffset = selectedIndex / this.visibleTechniqueCount
                    * this.visibleTechniqueCount;
        } else if (selectedIndex >= this.pageOffset + this.visibleTechniqueCount) {
            this.pageOffset = Math.min(
                    getMaximumPageOffset(techniques),
                    selectedIndex / this.visibleTechniqueCount * this.visibleTechniqueCount
            );
        }
    }

    private float getColumnCenterX(int visibleIndex, List<TechniqueClientData> techniques) {
        int visibleCount = Math.max(1, getCurrentPageSize(techniques));
        float availableWidth = this.layoutRight - this.layoutLeft;
        return this.layoutRight - availableWidth * (visibleIndex + 0.5F) / visibleCount;
    }

    private boolean isInsideTechniqueColumn(
            double mouseX,
            double mouseY,
            int visibleIndex,
            List<TechniqueClientData> techniques
    ) {
        float centerX = getColumnCenterX(visibleIndex, techniques);
        float halfWidth = (this.layoutRight - this.layoutLeft)
                / (float) Math.max(1, getCurrentPageSize(techniques)) * 0.46F;
        return mouseX >= centerX - halfWidth
                && mouseX < centerX + halfWidth
                && mouseY >= this.contentTop - 12
                && mouseY < this.contentBottom;
    }

    private int getCurrentPageSize(List<TechniqueClientData> techniques) {
        return Math.max(0, Math.min(
                this.visibleTechniqueCount,
                techniques.size() - this.pageOffset
        ));
    }

    private int getMaximumPageOffset(List<TechniqueClientData> techniques) {
        if (techniques.isEmpty()) {
            return 0;
        }
        return (techniques.size() - 1) / this.visibleTechniqueCount
                * this.visibleTechniqueCount;
    }

    private boolean isInside(double mouseX, double mouseY, int left, int top, int width) {
        return TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                left,
                top,
                width,
                BUTTON_HEIGHT
        );
    }

    private static Component translatedTechniqueText(String translationKey) {
        return Component.translatable(translationKey).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }

    private static String normalizeText(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
