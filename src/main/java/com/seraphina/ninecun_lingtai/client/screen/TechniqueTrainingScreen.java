package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

public final class TechniqueTrainingScreen extends AbstractTechniqueImprintScreen {
    private static final int MINIMUM_COLUMN_WIDTH = 56;
    private static final int MAXIMUM_VISIBLE_COLUMNS = 8;
    private static final int RETURN_BUTTON_WIDTH = 68;
    private static final int PAGE_BUTTON_WIDTH = 68;
    private static final int ACTION_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 30;
    private static final float NAME_FONT_SCALE = 0.42F;
    private static final float PROFICIENCY_FONT_SCALE = 0.28F;

    @Nullable
    private final Screen parentScreen;

    private int layoutLeft;
    private int layoutRight;
    private int contentTop;
    private int contentBottom;
    private int actionButtonTop;
    private int returnButtonLeft;
    private int previousButtonLeft;
    private int actionButtonLeft;
    private int nextButtonLeft;
    private int visibleTechniqueCount;
    private int pageOffset;
    @Nullable
    private ResourceLocation selectedTechniqueId;
    private boolean closeToWorld;
    private boolean submitted;

    public TechniqueTrainingScreen(@Nullable Screen parentScreen) {
        super(Component.translatable("screen.ninecun_lingtai.train.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.layoutLeft = 38;
        this.layoutRight = this.width - 42;
        this.actionButtonTop = this.height - 43;
        this.contentTop = 40;
        this.contentBottom = this.actionButtonTop - 15;
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
        renderControls(guiGraphics, techniques, mouseX, mouseY, currentTime);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !acceptsInput()) {
            return super.mouseClicked(mouseX, mouseY, button);
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
            startTraining();
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
            startTraining();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Nullable
    @Override
    protected Screen getParentScreen() {
        return this.closeToWorld ? null : this.parentScreen;
    }

    private void renderSideTitles(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 5, currentTime);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.train.title").getString(),
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
                Component.translatable("screen.ninecun_lingtai.train.choose_hint").getString(),
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
                    Component.translatable("screen.ninecun_lingtai.no_learned_techniques").getString(),
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

        int maximumNameSymbols = Math.max(2, (this.contentBottom - this.contentTop) / 20);
        int maximumProficiencySymbols = Math.max(2, (this.contentBottom - this.contentTop) / 14);
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
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    getProficiencyText(technique.id()),
                    centerX - 14.0F,
                    this.contentTop + 20.0F,
                    PROFICIENCY_FONT_SCALE,
                    13.0F,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                    columnAlpha * (selected || hovered ? 0.95F : 0.62F),
                    maximumProficiencySymbols,
                    false
            );
        }
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
                translatedTechniqueText("screen.ninecun_lingtai.train.start"),
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                this.selectedTechniqueId != null && !this.submitted,
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

    private String getProficiencyText(ResourceLocation techniqueId) {
        return ClientTechniqueManager.getLearnedTechniqueData(techniqueId)
                .map(learnedTechnique -> {
                    String stageName = TechniqueMasteryStage.getDisplayName(learnedTechnique.mastery());
                    StringBuilder text = new StringBuilder(stageName)
                            .append("·")
                            .append(Component.translatable("screen.ninecun_lingtai.train.proficiency").getString())
                            .append(ChineseNumberUtility.toChineseInteger(learnedTechnique.proficiency()));
                    TechniqueMasteryStage.fromMastery(learnedTechnique.mastery())
                            .flatMap(TechniqueMasteryStage::next)
                            .ifPresent(nextStage -> text
                                    .append("／")
                                    .append(ChineseNumberUtility.toChineseInteger(
                                            nextStage.getRequiredProficiency()
                                    )));
                    return text.toString();
                })
                .orElse("");
    }

    private List<TechniqueClientData> getTechniques() {
        return ClientTechniqueManager.getLearnedTechniques();
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

    private void startTraining() {
        if (this.selectedTechniqueId == null || this.submitted) {
            return;
        }
        this.submitted = true;
        this.closeToWorld = true;
        ClientMeditationManager.startTechniqueCultivation(this.selectedTechniqueId);
        beginClosing();
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
}
