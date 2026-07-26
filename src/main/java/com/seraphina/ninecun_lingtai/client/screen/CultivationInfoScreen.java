package com.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.menu.CultivationInfoMenu;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CultivationInfoScreen extends AbstractContainerScreen<CultivationInfoMenu> {
    private static final int PREFERRED_IMAGE_WIDTH = 286;
    private static final int PREFERRED_IMAGE_HEIGHT = 226;
    private static final int MINIMUM_IMAGE_WIDTH = 230;
    private static final int MINIMUM_IMAGE_HEIGHT = 178;
    private static final int PROFILE_HEIGHT = 72;
    private static final int CARD_HEIGHT = 44;
    private static final int CARD_GAP = 5;
    private static final int ENERGY_ROW_HEIGHT = 15;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int CARD_COLOR = 0xD9282C38;
    private static final int CARD_BORDER_COLOR = 0xFF4A4D59;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final List<ElementType> ELEMENT_DISPLAY_ORDER = List.of(
            ElementType.WATER,
            ElementType.WOOD,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.METAL,
            ElementType.YIN_YANG,
            ElementType.THUNDER,
            ElementType.CHAOS
    );

    private double scrollOffset;
    private int maximumScrollOffset;
    private int refreshTicks;

    public CultivationInfoScreen() {
        super(
                new CultivationInfoMenu(),
                Minecraft.getInstance().player.getInventory(),
                Component.literal("九寸灵台")
        );
        this.imageWidth = PREFERRED_IMAGE_WIDTH;
        this.imageHeight = PREFERRED_IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 34, MINIMUM_IMAGE_WIDTH, PREFERRED_IMAGE_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 34, MINIMUM_IMAGE_HEIGHT, PREFERRED_IMAGE_HEIGHT);
        super.init();
    }

    @Override
    protected void containerTick() {
        this.refreshTicks++;
        if (this.refreshTicks >= REFRESH_INTERVAL_TICKS) {
            this.refreshTicks = 0;
            ClientCultivationInfoManager.requestRefresh();
        }
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
                styledText("九寸灵台", PRIMARY_TEXT_COLOR),
                panelLeft + this.imageWidth / 2,
                panelTop + 9,
                this.imageWidth - 28,
                PRIMARY_TEXT_COLOR
        );
        guiGraphics.fill(
                panelLeft + 12,
                panelTop + 24,
                panelRight - 12,
                panelTop + 25,
                0xFF5B4E3C
        );

        Optional<PlayerCultivationInfo> cultivationInfo = ClientCultivationInfoManager.getCultivationInfo();
        if (cultivationInfo.isEmpty()) {
            drawCenteredFittedText(
                    guiGraphics,
                    styledText("正在读取灵台信息……", SECONDARY_TEXT_COLOR),
                    panelLeft + this.imageWidth / 2,
                    panelTop + this.imageHeight / 2 - 4,
                    this.imageWidth - 32,
                    SECONDARY_TEXT_COLOR
            );
            return;
        }

        renderProfile(guiGraphics, cultivationInfo.get(), panelLeft + 10, panelTop + 31);
        int sectionTitleY = panelTop + 31 + PROFILE_HEIGHT + 8;
        drawFittedText(
                guiGraphics,
                styledText("境界与突破", PRIMARY_TEXT_COLOR),
                panelLeft + 13,
                sectionTitleY,
                this.imageWidth - 30,
                PRIMARY_TEXT_COLOR
        );

        int viewportTop = sectionTitleY + 14;
        int viewportBottom = panelBottom - 10;
        renderScrollableDetails(
                guiGraphics,
                cultivationInfo.get(),
                panelLeft + 10,
                panelRight - 15,
                viewportTop,
                viewportBottom
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.maximumScrollOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.scrollOffset = Mth.clamp(
                this.scrollOffset - delta * 18.0D,
                0.0D,
                this.maximumScrollOffset
        );
        return true;
    }

    private void renderProfile(
            GuiGraphics guiGraphics,
            PlayerCultivationInfo cultivationInfo,
            int left,
            int top
    ) {
        int right = this.leftPos + this.imageWidth - 10;
        drawBorderedPanel(guiGraphics, left, top, right, top + PROFILE_HEIGHT, CARD_COLOR, CARD_BORDER_COLOR);
        Optional<SpiritualRoot> spiritualRoot = cultivationInfo.spiritualRoot();
        String spiritualRootName = spiritualRoot
                .map(root -> SpiritualRootUtility.getSpiritualRootName(root.elements()))
                .orElse("尚未测定");
        String spiritualRootLength = spiritualRoot
                .map(root -> ChineseNumberUtility.formatTenths(root.lengthInches(), "寸", "分"))
                .orElse("尚未测定");
        String soulStrength = cultivationInfo.soulStrength()
                .map(strength -> ChineseNumberUtility.toChineseInteger(strength.illuminatedStars()) + "星")
                .orElse("尚未测定");
        String meditationState = ClientMeditationManager.isLocalPlayerMeditating() ? "打坐中" : "未打坐";
        int textLeft = left + 9;
        int maximumWidth = right - textLeft - 8;
        drawFittedText(
                guiGraphics,
                styledText("灵根属性　" + spiritualRootName, PRIMARY_TEXT_COLOR),
                textLeft,
                top + 8,
                maximumWidth,
                PRIMARY_TEXT_COLOR
        );
        drawFittedText(
                guiGraphics,
                styledText("灵根长度　" + spiritualRootLength, SECONDARY_TEXT_COLOR),
                textLeft,
                top + 23,
                maximumWidth,
                SECONDARY_TEXT_COLOR
        );
        drawFittedText(
                guiGraphics,
                styledText("魂魄强度　" + soulStrength, SECONDARY_TEXT_COLOR),
                textLeft,
                top + 38,
                maximumWidth,
                SECONDARY_TEXT_COLOR
        );
        drawFittedText(
                guiGraphics,
                styledText("修炼状态　" + meditationState, SECONDARY_TEXT_COLOR),
                textLeft,
                top + 53,
                maximumWidth,
                SECONDARY_TEXT_COLOR
        );
    }

    private void renderScrollableDetails(
            GuiGraphics guiGraphics,
            PlayerCultivationInfo cultivationInfo,
            int left,
            int right,
            int viewportTop,
            int viewportBottom
    ) {
        List<ElementType> rootElements = getRootElements(cultivationInfo);
        List<ElementType> storedEnergyElements = getStoredEnergyElements(cultivationInfo);
        int cultivationHeight = rootElements.isEmpty()
                ? 19
                : rootElements.size() * (CARD_HEIGHT + CARD_GAP);
        int energyHeight = 22 + Math.max(1, storedEnergyElements.size()) * ENERGY_ROW_HEIGHT;
        int contentHeight = cultivationHeight + energyHeight;
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        this.maximumScrollOffset = Math.max(0, contentHeight - viewportHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0D, this.maximumScrollOffset);

        guiGraphics.enableScissor(left, viewportTop, right, viewportBottom);
        int contentY = viewportTop - (int) Math.round(this.scrollOffset);
        if (rootElements.isEmpty()) {
            drawFittedText(
                    guiGraphics,
                    styledText("尚无可修行的灵根境界", MUTED_TEXT_COLOR),
                    left + 4,
                    contentY + 4,
                    right - left - 8,
                    MUTED_TEXT_COLOR
            );
            contentY += cultivationHeight;
        } else {
            for (ElementType element : rootElements) {
                renderCultivationCard(guiGraphics, cultivationInfo, element, left, right, contentY);
                contentY += CARD_HEIGHT + CARD_GAP;
            }
        }

        drawFittedText(
                guiGraphics,
                styledText("体内灵力", PRIMARY_TEXT_COLOR),
                left + 3,
                contentY + 3,
                right - left - 8,
                PRIMARY_TEXT_COLOR
        );
        contentY += 20;
        if (storedEnergyElements.isEmpty()) {
            drawFittedText(
                    guiGraphics,
                    styledText("暂无灵力积蓄", MUTED_TEXT_COLOR),
                    left + 8,
                    contentY,
                    right - left - 14,
                    MUTED_TEXT_COLOR
            );
        } else {
            for (ElementType element : storedEnergyElements) {
                int color = SpiritualRootUtility.getElementColor(element);
                guiGraphics.fill(left + 3, contentY + 2, left + 5, contentY + 11, 0xFF000000 | color);
                String energyText = getEnergyName(element)
                        + "　"
                        + formatEnergy(cultivationInfo.getSpiritualEnergy(element));
                drawFittedText(
                        guiGraphics,
                        styledText(energyText, SECONDARY_TEXT_COLOR),
                        left + 9,
                        contentY,
                        right - left - 16,
                        SECONDARY_TEXT_COLOR
                );
                contentY += ENERGY_ROW_HEIGHT;
            }
        }
        guiGraphics.disableScissor();
        renderScrollBar(guiGraphics, right + 2, viewportTop, viewportBottom, contentHeight);
    }

    private void renderCultivationCard(
            GuiGraphics guiGraphics,
            PlayerCultivationInfo cultivationInfo,
            ElementType element,
            int left,
            int right,
            int top
    ) {
        drawBorderedPanel(guiGraphics, left, top, right, top + CARD_HEIGHT, CARD_COLOR, CARD_BORDER_COLOR);
        int elementColor = SpiritualRootUtility.getElementColor(element);
        guiGraphics.fill(left + 1, top + 1, left + 4, top + CARD_HEIGHT - 1, 0xFF000000 | elementColor);

        Optional<CultivationStage> currentStage = Optional.ofNullable(
                cultivationInfo.cultivationStages().get(element)
        );
        Optional<CultivationStage> targetStage = currentStage
                .map(CultivationStage::next)
                .orElseGet(() -> Optional.of(CultivationStage.first()));
        String currentStageName = currentStage.map(CultivationUtility::getDisplayName).orElse("未入道");
        String elementName = SpiritualRootUtility.getElementName(element) + "灵根";
        drawFittedText(
                guiGraphics,
                styledText(elementName + "　" + currentStageName, PRIMARY_TEXT_COLOR),
                left + 9,
                top + 5,
                right - left - 16,
                PRIMARY_TEXT_COLOR
        );

        double currentEnergy = cultivationInfo.getSpiritualEnergy(element);
        String energyName = getEnergyName(element);
        if (targetStage.isEmpty()) {
            drawFittedText(
                    guiGraphics,
                    styledText("已至极境", SECONDARY_TEXT_COLOR),
                    left + 9,
                    top + 18,
                    right - left - 16,
                    SECONDARY_TEXT_COLOR
            );
            drawFittedText(
                    guiGraphics,
                    styledText(energyName + "　" + formatEnergy(currentEnergy), MUTED_TEXT_COLOR),
                    left + 9,
                    top + 30,
                    right - left - 16,
                    MUTED_TEXT_COLOR
            );
            return;
        }

        BigInteger requiredEnergy = CultivationUtility.getRequiredSpiritualEnergy(targetStage.get());
        BigDecimal missingEnergy = new BigDecimal(requiredEnergy)
                .subtract(BigDecimal.valueOf(currentEnergy))
                .max(BigDecimal.ZERO);
        String nextStageText = "下境　" + CultivationUtility.getDisplayName(targetStage.get());
        String energyText = energyName + "　"
                + formatEnergy(currentEnergy)
                + "　所需　"
                + ChineseNumberUtility.toChineseInteger(requiredEnergy)
                + "　尚缺　"
                + ChineseNumberUtility.toChineseDecimal(missingEnergy, 1);
        drawFittedText(
                guiGraphics,
                styledText(nextStageText, SECONDARY_TEXT_COLOR),
                left + 9,
                top + 18,
                right - left - 16,
                SECONDARY_TEXT_COLOR
        );
        drawFittedText(
                guiGraphics,
                styledText(energyText, MUTED_TEXT_COLOR),
                left + 9,
                top + 30,
                right - left - 16,
                MUTED_TEXT_COLOR
        );
        renderProgressBar(guiGraphics, currentEnergy, requiredEnergy, elementColor, left + 5, right - 5, top + 40);
    }

    private void renderProgressBar(
            GuiGraphics guiGraphics,
            double currentEnergy,
            BigInteger requiredEnergy,
            int color,
            int left,
            int right,
            int top
    ) {
        guiGraphics.fill(left, top, right, top + 2, 0xFF161820);
        double requiredValue = requiredEnergy.doubleValue();
        double progress = requiredValue <= 0.0D
                ? 1.0D
                : Mth.clamp(currentEnergy / requiredValue, 0.0D, 1.0D);
        int progressRight = left + (int) Math.round((right - left) * progress);
        if (progressRight > left) {
            guiGraphics.fill(left, top, progressRight, top + 2, 0xFF000000 | color);
        }
    }

    private void renderScrollBar(
            GuiGraphics guiGraphics,
            int left,
            int viewportTop,
            int viewportBottom,
            int contentHeight
    ) {
        if (this.maximumScrollOffset <= 0) {
            return;
        }
        int trackHeight = viewportBottom - viewportTop;
        guiGraphics.fill(left, viewportTop, left + 2, viewportBottom, 0xFF2B2D35);
        int thumbHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbTop = viewportTop + (int) Math.round(
                thumbTravel * this.scrollOffset / this.maximumScrollOffset
        );
        guiGraphics.fill(left, thumbTop, left + 2, thumbTop + thumbHeight, 0xFF9C8768);
    }

    private List<ElementType> getRootElements(PlayerCultivationInfo cultivationInfo) {
        Set<ElementType> independentElements = cultivationInfo.spiritualRoot()
                .map(SpiritualRootUtility::getIndependentRootElements)
                .orElse(Set.of());
        List<ElementType> orderedElements = new ArrayList<>();
        for (ElementType element : ELEMENT_DISPLAY_ORDER) {
            if (independentElements.contains(element)) {
                orderedElements.add(element);
            }
        }
        return orderedElements;
    }

    private List<ElementType> getStoredEnergyElements(PlayerCultivationInfo cultivationInfo) {
        Set<ElementType> independentElements = cultivationInfo.spiritualRoot()
                .map(SpiritualRootUtility::getIndependentRootElements)
                .orElse(Set.of());
        List<ElementType> orderedElements = new ArrayList<>();
        for (ElementType element : ELEMENT_DISPLAY_ORDER) {
            if (independentElements.contains(element)
                    && cultivationInfo.getSpiritualEnergy(element) > 0.0D) {
                orderedElements.add(element);
            }
        }
        return orderedElements;
    }

    private static String getEnergyName(ElementType element) {
        return element == ElementType.CHAOS
                ? "混沌气"
                : SpiritualRootUtility.getElementName(element) + "灵力";
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

    private static Component styledText(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color & 0xFFFFFF));
    }

    private static String formatEnergy(double energy) {
        return ChineseNumberUtility.toChineseDecimal(energy, 1);
    }
}
