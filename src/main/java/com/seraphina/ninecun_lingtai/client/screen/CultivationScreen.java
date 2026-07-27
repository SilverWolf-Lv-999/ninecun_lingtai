package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.utility.CultivationRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.BreakthroughRequestPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.StartSoulRoamingPacket;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public final class CultivationScreen extends AbstractTechniqueImprintScreen {
    private static final int ACTION_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 30;

    private int modelCenterX;
    private int modelBottomY;
    private int modelScale;
    private int leftButtonLeft;
    private int rightButtonLeft;
    private int upperButtonTop;
    private int lowerButtonTop;
    private int soulButtonLeft;
    private int soulButtonTop;
    private int feedbackBaselineRevision;
    private boolean awaitingBreakthrough;
    private int breakthroughRequestRevision;

    public CultivationScreen() {
        super(Component.translatable("screen.ninecun_lingtai.cultivation"));
        ClientCultivationInfoManager.beginOpen();
        ClientTechniqueManager.requestRefresh();
    }

    @Override
    protected void init() {
        super.init();
        this.modelCenterX = this.width / 2;
        this.modelBottomY = (int) (this.height * 0.74F);
        this.modelScale = Mth.clamp((int) (this.height * 0.30F), 60, 110);
        this.leftButtonLeft = Math.max(
                20,
                this.width / 2 - this.modelScale - 34 - ACTION_BUTTON_WIDTH
        );
        this.rightButtonLeft = Math.min(
                this.width - 20 - ACTION_BUTTON_WIDTH,
                this.width / 2 + this.modelScale + 34
        );
        this.upperButtonTop = (int) (this.height * 0.32F) - BUTTON_HEIGHT / 2;
        this.lowerButtonTop = (int) (this.height * 0.60F) - BUTTON_HEIGHT / 2;
        this.soulButtonLeft = (this.width - ACTION_BUTTON_WIDTH) / 2;
        this.soulButtonTop = Math.max(18, this.upperButtonTop - BUTTON_HEIGHT - 34);
        this.feedbackBaselineRevision = ClientCultivationFeedbackManager.getRevision();
        if (this.minecraft != null
                && this.minecraft.player != null
                && !ClientMeditationManager.isLocalPlayerMeditating()) {
            MeditationAnimationManager.setMeditating(this.minecraft.player.getUUID(), true);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null
                && this.minecraft.player != null
                && !ClientMeditationManager.isLocalPlayerMeditating()) {
            MeditationAnimationManager.setMeditating(this.minecraft.player.getUUID(), false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.awaitingBreakthrough
                && ClientCultivationFeedbackManager.getRevision() > this.breakthroughRequestRevision) {
            this.awaitingBreakthrough = false;
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
        renderPlayerModel(guiGraphics, currentTime);
        renderStageCaption(guiGraphics, currentTime);
        renderButtons(guiGraphics, mouseX, mouseY, currentTime);
        renderFeedback(guiGraphics, currentTime);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !acceptsInput()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (isInsideButton(mouseX, mouseY, this.leftButtonLeft, this.upperButtonTop)) {
            openLearningScreen();
            return true;
        }
        if (isInsideButton(mouseX, mouseY, this.leftButtonLeft, this.lowerButtonTop)) {
            openTrainingScreen();
            return true;
        }
        if (isInsideButton(mouseX, mouseY, this.rightButtonLeft, this.upperButtonTop)) {
            startRealmCultivation();
            return true;
        }
        if (isInsideButton(mouseX, mouseY, this.rightButtonLeft, this.lowerButtonTop)) {
            requestBreakthrough();
            return true;
        }
        if (isInsideButton(mouseX, mouseY, this.soulButtonLeft, this.soulButtonTop)) {
            startSoulRoaming();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderSideTitles(GuiGraphics guiGraphics, long currentTime) {
        float titleAlpha = getElementAlpha(1, 5, currentTime);
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.cultivation").getString(),
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
                Component.translatable("screen.ninecun_lingtai.cultivation.side_note").getString(),
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

    private void renderPlayerModel(GuiGraphics guiGraphics, long currentTime) {
        float modelAlpha = getElementAlpha(1, 4, currentTime);
        float auraCenterY = this.modelBottomY - this.modelScale * 0.62F;
        CultivationRenderUtility.drawMeditationAura(
                guiGraphics,
                this.modelCenterX,
                auraCenterY,
                this.modelScale * 0.78F,
                currentTime,
                modelAlpha
        );
        if (modelAlpha > 0.35F && this.minecraft != null && this.minecraft.player != null) {
            CultivationRenderUtility.renderFrontFacingPlayer(
                    guiGraphics,
                    this.modelCenterX,
                    this.modelBottomY,
                    this.modelScale,
                    this.minecraft.player
            );
        }
    }

    private void renderStageCaption(GuiGraphics guiGraphics, long currentTime) {
        float captionAlpha = getElementAlpha(2, 3, currentTime);
        Optional<CultivationStage> stage = getStage();
        String stageText = stage
                .map(CultivationUtility::getDisplayName)
                .orElseGet(() -> Component.translatable(
                        "screen.ninecun_lingtai.cultivation.mortal"
                ).getString());
        float captionX = this.width / 2.0F - this.modelScale * 0.9F - 18.0F;
        TechniqueImprintRenderUtility.drawSeal(
                guiGraphics,
                captionX,
                this.height * 0.30F - 13.0F,
                4.4F,
                true,
                false,
                currentTime,
                captionAlpha
        );
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                stageText,
                captionX,
                this.height * 0.30F,
                0.4F,
                18.0F,
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR,
                captionAlpha,
                9,
                true
        );
        if (isAtRealmPeak()) {
            TechniqueImprintRenderUtility.drawVerticalText(
                    guiGraphics,
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.cultivation.realm_peak").getString(),
                    captionX - 15.0F,
                    this.height * 0.30F + 3.0F,
                    0.28F,
                    13.0F,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                    captionAlpha * 0.9F,
                    9,
                    false
            );
        }
    }

    private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, long currentTime) {
        float controlsAlpha = getElementAlpha(3, 1, currentTime);
        drawButton(
                guiGraphics,
                "screen.ninecun_lingtai.cultivation.learn_technique",
                this.leftButtonLeft,
                this.upperButtonTop,
                true,
                mouseX,
                mouseY,
                currentTime,
                controlsAlpha
        );
        drawButton(
                guiGraphics,
                "screen.ninecun_lingtai.cultivation.cultivate_technique",
                this.leftButtonLeft,
                this.lowerButtonTop,
                true,
                mouseX,
                mouseY,
                currentTime,
                controlsAlpha
        );
        drawButton(
                guiGraphics,
                "screen.ninecun_lingtai.cultivation.cultivate_realm",
                this.rightButtonLeft,
                this.upperButtonTop,
                true,
                mouseX,
                mouseY,
                currentTime,
                controlsAlpha
        );
        drawButton(
                guiGraphics,
                this.awaitingBreakthrough
                        ? "screen.ninecun_lingtai.cultivation.breaking_through"
                        : "screen.ninecun_lingtai.cultivation.breakthrough",
                this.rightButtonLeft,
                this.lowerButtonTop,
                canRequestBreakthrough(),
                mouseX,
                mouseY,
                currentTime,
                controlsAlpha
        );
        drawButton(
                guiGraphics,
                "screen.ninecun_lingtai.cultivation.soul_roaming",
                this.soulButtonLeft,
                this.soulButtonTop,
                canStartSoulRoaming(),
                mouseX,
                mouseY,
                currentTime,
                controlsAlpha
        );
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
                Math.max(this.height - 76.0F, this.soulButtonTop + BUTTON_HEIGHT + 14.0F),
                (int) (this.width * 0.62F),
                0.3F,
                ClientCultivationFeedbackManager.isSuccess()
                        ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                        : TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                getElementAlpha(2, 2, currentTime),
                2
        ));
    }

    private void drawButton(
            GuiGraphics guiGraphics,
            String translationKey,
            int left,
            int top,
            boolean active,
            int mouseX,
            int mouseY,
            long currentTime,
            float alpha
    ) {
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                translatedTechniqueText(translationKey),
                left,
                top,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                active,
                TechniqueImprintRenderUtility.isInside(
                        mouseX,
                        mouseY,
                        left,
                        top,
                        ACTION_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ),
                currentTime,
                alpha
        );
    }

    private void openLearningScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TechniqueLearningScreen(this));
        }
    }

    private void openTrainingScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TechniqueTrainingScreen(this));
        }
    }

    private void startRealmCultivation() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TechniqueTrainingScreen(
                    this,
                    TechniqueTrainingScreen.Mode.REALM
            ));
        }
    }

    private void requestBreakthrough() {
        if (!canRequestBreakthrough()) {
            return;
        }
        this.awaitingBreakthrough = true;
        this.breakthroughRequestRevision = ClientCultivationFeedbackManager.getRevision();
        ModNetwork.sendToServer(BreakthroughRequestPacket.INSTANCE);
    }

    private void startSoulRoaming() {
        if (!canStartSoulRoaming()) {
            return;
        }
        ModNetwork.sendToServer(StartSoulRoamingPacket.INSTANCE);
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private boolean canRequestBreakthrough() {
        return !this.awaitingBreakthrough && isAtRealmPeak();
    }

    private boolean canStartSoulRoaming() {
        return getStage()
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false);
    }

    private boolean isAtRealmPeak() {
        return getStage()
                .map(stage -> stage.layer() >= CultivationStage.MAXIMUM_LAYER)
                .orElse(false);
    }

    private Optional<CultivationStage> getStage() {
        return ClientCultivationInfoManager.getCultivationInfo()
                .flatMap(info -> info.cultivationStage());
    }

    private boolean isInsideButton(double mouseX, double mouseY, int left, int top) {
        return TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                left,
                top,
                ACTION_BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    private static Component translatedTechniqueText(String translationKey) {
        return Component.translatable(translationKey).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }
}
