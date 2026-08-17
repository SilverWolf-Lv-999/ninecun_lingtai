package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import net.seraphina.ninecun_lingtai.client.utility.CultivationRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import net.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SubmitAscensionAnswerPacket;
import net.seraphina.ninecun_lingtai.utility.ChineseNumeralUtility;
import org.lwjgl.glfw.GLFW;

public final class AscensionQuestionScreen extends AbstractTechniqueImprintScreen {
    private static final int BUTTON_WIDTH = 112;
    private static final int BUTTON_HEIGHT = 30;
    private static final int INPUT_HEIGHT = 18;
    private static final float TITLE_SCALE = 0.42F;
    private static final float PROMPT_SCALE = 0.30F;

    private final int thisQuestionIndex;
    private final int thisQuestionTotal;
    private final String thisQuestionTitle;
    private final String thisQuestionPrompt;
    private final int thisFeedbackBaselineRevision;

    private CultivationTextInput thisAnswerInput;
    private int thisInputLeft;
    private int thisInputTop;
    private int thisInputWidth;
    private int thisSubmitButtonLeft;
    private int thisSubmitButtonTop;
    private int thisSubmitFeedbackRevision;
    private boolean thisAwaitingEvaluation;

    public AscensionQuestionScreen(
            int questionIndex,
            int questionTotal,
            String questionTitle,
            String questionPrompt
    ) {
        super(Component.translatable("screen.ninecun_lingtai.ascension.title"));
        this.thisQuestionIndex = Math.max(0, questionIndex);
        this.thisQuestionTotal = Math.max(1, questionTotal);
        this.thisQuestionTitle = ChineseNumeralUtility.replaceArabicNumerals(questionTitle);
        this.thisQuestionPrompt = ChineseNumeralUtility.replaceArabicNumerals(questionPrompt);
        this.thisFeedbackBaselineRevision = ClientCultivationFeedbackManager.getRevision();
    }

    @Override
    protected void init() {
        super.init();
        this.thisInputWidth = Mth.clamp(this.width - 230, 200, 520);
        this.thisInputLeft = (this.width - this.thisInputWidth) / 2;
        this.thisSubmitButtonLeft = (this.width - BUTTON_WIDTH) / 2;
        this.thisSubmitButtonTop = this.height - 43;
        this.thisInputTop = this.thisSubmitButtonTop - 28;
        if (this.thisAnswerInput == null) {
            this.thisAnswerInput = new CultivationTextInput(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.ascension.input_hint"),
                    SubmitAscensionAnswerPacket.MAXIMUM_ANSWER_LENGTH
            );
        }
        this.thisAnswerInput.setBounds(
                this.thisInputLeft,
                this.thisInputTop,
                this.thisInputWidth,
                INPUT_HEIGHT
        );
        this.thisAnswerInput.setFocused(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.thisAwaitingEvaluation
                && ClientCultivationFeedbackManager.getRevision() > this.thisSubmitFeedbackRevision) {
            this.thisAwaitingEvaluation = false;
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
        renderTitles(guiGraphics, currentTime);
        renderQuestion(guiGraphics, currentTime);
        renderFeedback(guiGraphics, currentTime);
        renderInput(guiGraphics, currentTime);
        renderSubmitButton(guiGraphics, mouseX, mouseY, currentTime);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !acceptsInput()) {
            return true;
        }
        if (this.thisAnswerInput != null && this.thisAnswerInput.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.thisSubmitButtonLeft,
                this.thisSubmitButtonTop,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        )) {
            submitAnswer();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        if (acceptsInput()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitAnswer();
            return true;
        }
        if (acceptsInput()
                && this.thisAnswerInput != null
                && this.thisAnswerInput.keyPressed(keyCode, modifiers)) {
            normalizeAnswerInput();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!acceptsInput() || this.thisAnswerInput == null) {
            return false;
        }
        boolean consumed = this.thisAnswerInput.charTyped(codePoint);
        normalizeAnswerInput();
        return consumed;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    boolean representsQuestion(
            int questionIndex,
            int questionTotal,
            String questionTitle,
            String questionPrompt
    ) {
        return this.thisQuestionIndex == questionIndex
                && this.thisQuestionTotal == questionTotal
                && this.thisQuestionTitle.equals(questionTitle)
                && this.thisQuestionPrompt.equals(questionPrompt);
    }

    private void renderTitles(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 4, currentTime);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.ascension.title").getString(),
                this.width - 19.0F,
                24.0F,
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
                Component.translatable(
                        "screen.ninecun_lingtai.ascension.progress",
                        ChineseNumeralUtility.toChinese(this.thisQuestionIndex + 1L),
                        ChineseNumeralUtility.toChinese(this.thisQuestionTotal)
                ).getString(),
                18.0F,
                28.0F,
                0.28F,
                14.0F,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                titleAlpha * 0.9F,
                12,
                false
        );
    }

    private void renderQuestion(GuiGraphics guiGraphics, long currentTime) {
        float questionAlpha = getElementAlpha(2, 3, currentTime);
        float centerX = this.width * 0.5F;
        float auraRadius = Math.min(this.width, this.height) * 0.19F;
        CultivationRenderUtility.drawMeditationAura(
                guiGraphics,
                centerX,
                this.height * 0.39F,
                auraRadius,
                currentTime,
                questionAlpha * 0.78F
        );
        CultivationRenderUtility.drawWrappedText(
                guiGraphics,
                this.font,
                Component.literal(this.thisQuestionTitle).withStyle(
                        style -> style.withFont(CustomFontConstants.TECHNIQUE_DISPLAY_FONT_ID)
                ),
                centerX,
                this.height * 0.19F,
                (int) (this.width * 0.62F),
                TITLE_SCALE,
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR,
                questionAlpha,
                2
        );
        CultivationRenderUtility.drawWrappedText(
                guiGraphics,
                this.font,
                Component.literal(this.thisQuestionPrompt).withStyle(
                        style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
                ),
                centerX,
                this.height * 0.30F,
                (int) (this.width * 0.68F),
                PROMPT_SCALE,
                TechniqueImprintRenderUtility.GOLD_COLOR,
                questionAlpha * 0.92F,
                5
        );
    }

    private void renderFeedback(GuiGraphics guiGraphics, long currentTime) {
        if (ClientCultivationFeedbackManager.getRevision() <= this.thisFeedbackBaselineRevision) {
            return;
        }
        ClientCultivationFeedbackManager.getMessage().ifPresent(message -> CultivationRenderUtility.drawWrappedText(
                guiGraphics,
                this.font,
                Component.empty()
                        .append(message)
                        .withStyle(style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)),
                this.width * 0.5F,
                this.thisInputTop - 38.0F,
                (int) (this.width * 0.65F),
                0.26F,
                ClientCultivationFeedbackManager.isSuccess()
                        ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                        : TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                getElementAlpha(3, 2, currentTime),
                2
        ));
    }

    private void renderInput(GuiGraphics guiGraphics, long currentTime) {
        if (this.thisAnswerInput == null) {
            return;
        }
        float inputAlpha = getElementAlpha(3, 1, currentTime);
        this.thisAnswerInput.setEditable(!this.thisAwaitingEvaluation);
        CultivationRenderUtility.drawInputBoxFrame(
                guiGraphics,
                this.thisInputLeft,
                this.thisInputTop,
                this.thisInputWidth,
                INPUT_HEIGHT,
                this.thisAnswerInput.isFocused(),
                currentTime,
                inputAlpha
        );
        this.thisAnswerInput.render(guiGraphics, currentTime, inputAlpha);
    }

    private void renderSubmitButton(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            long currentTime
    ) {
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                Component.translatable(
                        this.thisAwaitingEvaluation
                                ? "screen.ninecun_lingtai.ascension.evaluating"
                                : "screen.ninecun_lingtai.ascension.submit"
                ).withStyle(style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)),
                this.thisSubmitButtonLeft,
                this.thisSubmitButtonTop,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                canSubmit(),
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        this.thisSubmitButtonLeft,
                        this.thisSubmitButtonTop,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                getElementAlpha(4, 0, currentTime)
        );
    }

    private void submitAnswer() {
        if (!canSubmit() || this.thisAnswerInput == null) {
            return;
        }
        String answer = this.thisAnswerInput.getValue().trim();
        if (answer.isBlank()) {
            return;
        }
        this.thisAwaitingEvaluation = true;
        this.thisSubmitFeedbackRevision = ClientCultivationFeedbackManager.getRevision();
        ModNetwork.sendToServer(new SubmitAscensionAnswerPacket(this.thisQuestionIndex, answer));
    }

    private boolean canSubmit() {
        return acceptsInput()
                && !this.thisAwaitingEvaluation
                && this.thisAnswerInput != null
                && !this.thisAnswerInput.getValue().isBlank();
    }

    private void normalizeAnswerInput() {
        if (this.thisAnswerInput == null) {
            return;
        }
        String answer = this.thisAnswerInput.getValue();
        String normalizedAnswer = ChineseNumeralUtility.replaceArabicNumerals(answer);
        if (!answer.equals(normalizedAnswer)) {
            this.thisAnswerInput.setValue(normalizedAnswer);
        }
    }
}
