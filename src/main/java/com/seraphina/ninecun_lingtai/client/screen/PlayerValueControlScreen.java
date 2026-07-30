package com.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.client.menu.PlayerValueControlMenu;
import com.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetPlayerValueControlSettingsPacket;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlSettings;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PlayerValueControlScreen extends AbstractContainerScreen<PlayerValueControlMenu> {
    private static final int PREFERRED_IMAGE_WIDTH = 308;
    private static final int PREFERRED_IMAGE_HEIGHT = 174;
    private static final int MINIMUM_IMAGE_WIDTH = 260;
    private static final int MINIMUM_IMAGE_HEIGHT = 156;
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final int SAVED_TEXT_COLOR = 0xFF8FD694;
    private static final int ROW_SPACING = 29;
    private static final int SLIDER_HEIGHT = 20;

    private final Map<PlayerValueControlType, Double> draftApplications =
            new EnumMap<>(PlayerValueControlType.class);
    private final List<ApplicationSlider> sliders = new ArrayList<>();
    private int observedRevision = -1;
    private boolean dirty;
    private Button resetButton;
    private Button saveButton;
    private Component statusMessage = Component.empty();

    public PlayerValueControlScreen() {
        super(
                new PlayerValueControlMenu(),
                Minecraft.getInstance().player.getInventory(),
                Component.translatable("screen.ninecun_lingtai.value_control.title")
        );
        this.imageWidth = PREFERRED_IMAGE_WIDTH;
        this.imageHeight = PREFERRED_IMAGE_HEIGHT;
        copySettings(PlayerValueControlSettings.defaultSettings());
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 34, MINIMUM_IMAGE_WIDTH, PREFERRED_IMAGE_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 34, MINIMUM_IMAGE_HEIGHT, PREFERRED_IMAGE_HEIGHT);
        super.init();
        this.sliders.clear();
        if (ClientPlayerValueControlManager.isInitialized()
                && this.observedRevision != ClientPlayerValueControlManager.getRevision()
                && !this.dirty) {
            copySettings(ClientPlayerValueControlManager.getSettings());
            this.observedRevision = ClientPlayerValueControlManager.getRevision();
        }
        addApplicationSliders();
        addFooterButtons();
        updateButtonState();
    }

    @Override
    protected void containerTick() {
        if (!ClientPlayerValueControlManager.isInitialized()) {
            return;
        }
        int currentRevision = ClientPlayerValueControlManager.getRevision();
        if (currentRevision != this.observedRevision && !this.dirty) {
            copySettings(ClientPlayerValueControlManager.getSettings());
            this.observedRevision = currentRevision;
            this.rebuildWidgets();
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
        int panelLeft = this.leftPos;
        int panelTop = this.topPos;
        int panelRight = panelLeft + this.imageWidth;
        int panelBottom = panelTop + this.imageHeight;
        drawBorderedPanel(
                guiGraphics,
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                PANEL_COLOR,
                PANEL_BORDER_COLOR
        );
        drawCenteredFittedText(
                guiGraphics,
                this.title,
                panelLeft + this.imageWidth / 2,
                panelTop + 11,
                this.imageWidth - 30,
                PRIMARY_TEXT_COLOR
        );
        guiGraphics.fill(panelLeft + 12, panelTop + 27, panelRight - 12, panelTop + 28, 0xFF5B4E3C);

        if (!ClientPlayerValueControlManager.isInitialized()) {
            drawCenteredFittedText(
                    guiGraphics,
                    Component.translatable("screen.ninecun_lingtai.value_control.loading"),
                    panelLeft + this.imageWidth / 2,
                    panelTop + this.imageHeight / 2 - 6,
                    this.imageWidth - 32,
                    SECONDARY_TEXT_COLOR
            );
            return;
        }

        int labelLeft = panelLeft + 17;
        int rowTop = panelTop + 45;
        int sliderLeft = getSliderLeft();
        int labelWidth = sliderLeft - labelLeft - 10;
        for (int typeIndex = 0; typeIndex < PlayerValueControlType.values().length; typeIndex++) {
            PlayerValueControlType type = PlayerValueControlType.values()[typeIndex];
            boolean unlocked = ClientPlayerValueControlManager.isUnlocked(type);
            drawFittedText(
                    guiGraphics,
                    Component.translatable(getFieldTranslationKey(type)),
                    labelLeft,
                    rowTop + typeIndex * ROW_SPACING + 6,
                    labelWidth,
                    unlocked ? PRIMARY_TEXT_COLOR : MUTED_TEXT_COLOR
            );
        }
        if (!this.statusMessage.getString().isEmpty()) {
            drawCenteredFittedText(
                    guiGraphics,
                    this.statusMessage,
                    panelLeft + this.imageWidth / 2,
                    panelBottom - 45,
                    this.imageWidth - 24,
                    SAVED_TEXT_COLOR
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void addApplicationSliders() {
        int sliderLeft = getSliderLeft();
        int sliderWidth = this.leftPos + this.imageWidth - sliderLeft - 17;
        int rowTop = this.topPos + 45;
        boolean initialized = ClientPlayerValueControlManager.isInitialized();
        for (int typeIndex = 0; typeIndex < PlayerValueControlType.values().length; typeIndex++) {
            PlayerValueControlType type = PlayerValueControlType.values()[typeIndex];
            boolean unlocked = initialized && ClientPlayerValueControlManager.isUnlocked(type);
            ApplicationSlider slider = new ApplicationSlider(
                    sliderLeft,
                    rowTop + typeIndex * ROW_SPACING,
                    sliderWidth,
                    SLIDER_HEIGHT,
                    type,
                    unlocked,
                    this.draftApplications.getOrDefault(
                            type,
                            PlayerValueControlUtility.DEFAULT_APPLICATION
                    )
            );
            slider.active = unlocked;
            slider.setTooltip(Tooltip.create(Component.translatable(
                    unlocked
                            ? getTooltipTranslationKey(type)
                            : getLockedTooltipTranslationKey(type)
            )));
            this.sliders.add(slider);
            this.addRenderableWidget(slider);
        }
    }

    private void addFooterButtons() {
        int footerWidth = Math.min(246, this.imageWidth - 34);
        int footerLeft = this.leftPos + (this.imageWidth - footerWidth) / 2;
        int buttonWidth = (footerWidth - 8) / 3;
        int footerTop = this.topPos + this.imageHeight - 28;
        this.resetButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.value_control.reset"),
                        button -> resetDrafts()
                )
                .bounds(footerLeft, footerTop, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.resetButton);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.value_control.cancel"),
                        button -> onClose()
                )
                .bounds(footerLeft + buttonWidth + 4, footerTop, buttonWidth, 20)
                .build());
        this.saveButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.value_control.save"),
                        button -> saveSettings()
                )
                .bounds(footerLeft + (buttonWidth + 4) * 2, footerTop, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.saveButton);
    }

    private int getSliderLeft() {
        return this.leftPos + Math.max(118, this.imageWidth / 2 - 8);
    }

    private void copySettings(PlayerValueControlSettings settings) {
        this.draftApplications.clear();
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            this.draftApplications.put(type, settings.getApplication(type));
        }
    }

    private void resetDrafts() {
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            if (ClientPlayerValueControlManager.isUnlocked(type)) {
                this.draftApplications.put(type, PlayerValueControlUtility.DEFAULT_APPLICATION);
            }
        }
        this.dirty = true;
        this.statusMessage = Component.empty();
        this.rebuildWidgets();
    }

    private void saveSettings() {
        PlayerValueControlSettings settings = new PlayerValueControlSettings(this.draftApplications);
        ModNetwork.sendToServer(new SetPlayerValueControlSettingsPacket(settings));
        this.dirty = false;
        this.statusMessage = Component.translatable("screen.ninecun_lingtai.value_control.saved");
        updateButtonState();
    }

    private void updateButtonState() {
        boolean initialized = ClientPlayerValueControlManager.isInitialized();
        if (this.resetButton != null) {
            this.resetButton.active = initialized && ClientPlayerValueControlManager.hasUnlockedType();
        }
        if (this.saveButton != null) {
            this.saveButton.active = initialized
                    && ClientPlayerValueControlManager.hasUnlockedType()
                    && this.dirty;
        }
    }

    private void drawBorderedPanel(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            int bottom,
            int fillColor,
            int borderColor
    ) {
        guiGraphics.fill(left, top, right, bottom, borderColor);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, fillColor);
    }

    private void drawCenteredFittedText(
            GuiGraphics guiGraphics,
            Component text,
            int centerX,
            int top,
            int maximumWidth,
            int color
    ) {
        int textWidth = Math.max(1, this.font.width(text));
        float scale = Math.min(1.0F, maximumWidth / (float) textWidth);
        drawScaledText(guiGraphics, text, centerX - textWidth * scale * 0.5F, top, scale, color);
    }

    private void drawFittedText(
            GuiGraphics guiGraphics,
            Component text,
            int left,
            int top,
            int maximumWidth,
            int color
    ) {
        int textWidth = Math.max(1, this.font.width(text));
        float scale = Math.min(1.0F, maximumWidth / (float) textWidth);
        drawScaledText(guiGraphics, text, left, top, scale, color);
    }

    private void drawScaledText(
            GuiGraphics guiGraphics,
            Component text,
            float left,
            float top,
            float scale,
            int color
    ) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(left, top, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    private static String getFieldTranslationKey(PlayerValueControlType type) {
        return "screen.ninecun_lingtai.value_control.field." + type.getSerializedName();
    }

    private static String getTooltipTranslationKey(PlayerValueControlType type) {
        return getFieldTranslationKey(type) + ".tooltip";
    }

    private static String getLockedTooltipTranslationKey(PlayerValueControlType type) {
        return getFieldTranslationKey(type) + ".locked_tooltip";
    }

    private final class ApplicationSlider extends AbstractSliderButton {
        private final PlayerValueControlType controlType;
        private final boolean unlocked;

        private ApplicationSlider(
                int left,
                int top,
                int width,
                int height,
                PlayerValueControlType controlType,
                boolean unlocked,
                double application
        ) {
            super(
                    left,
                    top,
                    width,
                    height,
                    Component.empty(),
                    unlocked
                            ? PlayerValueControlUtility.sanitizeApplication(application)
                            : PlayerValueControlUtility.DEFAULT_APPLICATION
            );
            this.controlType = controlType;
            this.unlocked = unlocked;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (!this.unlocked) {
                this.setMessage(Component.translatable("screen.ninecun_lingtai.value_control.locked"));
                return;
            }
            this.setMessage(Component.translatable(
                    "screen.ninecun_lingtai.value_control.percent",
                    Math.round(this.value * 100.0D)
            ));
        }

        @Override
        protected void applyValue() {
            if (!this.unlocked) {
                return;
            }
            double application = Math.round(this.value * 100.0D) / 100.0D;
            this.value = application;
            draftApplications.put(this.controlType, application);
            dirty = true;
            statusMessage = Component.empty();
            updateButtonState();
            updateMessage();
        }
    }
}
