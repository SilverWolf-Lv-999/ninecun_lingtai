package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.utility.AnimationUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeFormationRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.client.utility.ElementRelationRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.SoulConstellationRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AptitudeSelectionScreen extends Screen {
    private static final ResourceLocation BLUR_EFFECT = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/aptitude_blur.json"
    );
    private static final String BLUR_EFFECT_NAME = BLUR_EFFECT.toString();
    private static final long ELEMENT_APPEAR_INTERVAL = 260L;
    private static final long ELEMENT_APPEAR_DURATION = 650L;
    private static final long TAIJI_APPEAR_DELAY = 1840L;
    private static final long TAIJI_APPEAR_DURATION = 800L;
    private static final long INTRO_DURATION = TAIJI_APPEAR_DELAY + TAIJI_APPEAR_DURATION;
    private static final long SELECTION_FADE_DURATION = 780L;
    private static final long ROOT_FADE_IN_DURATION = 520L;
    private static final long ROOT_HOLD_DURATION = 1000L;
    private static final long ROOT_FADE_OUT_DURATION = 620L;
    private static final long ROOT_DISPLAY_DURATION = ROOT_FADE_IN_DURATION
            + ROOT_HOLD_DURATION
            + ROOT_FADE_OUT_DURATION;
    private static final long SPROUT_GROW_DURATION = 920L;
    private static final long SPROUT_FADE_DURATION = 620L;
    private static final long SOUL_INTRO_DURATION = 1150L;
    private static final long SOUL_BLOOM_DURATION = 1800L;
    private static final long COMPLETE_FADE_DURATION = 720L;
    private static final float SELECTED_ELEMENT_SCALE = 1.14F;
    private static final float SELECTED_TAIJI_SCALE = 1.09F;

    private final EnumSet<ElementType> selectedElements = EnumSet.noneOf(ElementType.class);
    private final EnumMap<ElementType, Float> selectionScales = new EnumMap<>(ElementType.class);
    private final EnumMap<ElementType, Component> elementComponents = new EnumMap<>(ElementType.class);
    private final Component selectionHint = customFontText("灵根属性");
    private final Component lengthHint = customFontText("灵根长度");
    private final Component lengthSelectionHint = customFontText("拖动灵根");
    private final Component maximumLengthHint = customFontText("九寸");
    private final Component soulStrengthHint = customFontText("魂魄");
    private final Component soulSelectionHint = customFontText("点亮北斗");

    private AptitudePhase phase = AptitudePhase.INTRO;
    private long phaseStartedAt;
    private long lastFrameAt;
    private Set<ElementType> confirmedElements = Set.of();
    private Component spiritualRootName = Component.empty();
    private int spiritualRootColor = 0xFFFFFF;
    private float spiritualRootLength = SpiritualRoot.MINIMUM_LENGTH_INCHES;
    private int illuminatedSoulStarMask;
    private float taijiScale = 1.0F;
    private boolean draggingSprout;
    private boolean submitted;
    private boolean blurInstalled;
    private boolean blurLoadFailed;
    @Nullable
    private String previousPostEffectName;

    public AptitudeSelectionScreen() {
        super(Component.literal("资质"));
        for (ElementType element : SpiritualRoot.SELECTABLE_ELEMENTS) {
            this.selectionScales.put(element, 1.0F);
            this.elementComponents.put(element, displayFontText(SpiritualRootUtility.getElementName(element)));
        }
    }

    @Override
    public void added() {
        super.added();
        long currentTime = Util.getMillis();
        this.phaseStartedAt = currentTime;
        this.lastFrameAt = currentTime;
        installBlurEffect();
    }

    @Override
    public void removed() {
        restorePreviousPostEffect();
        if (!this.submitted && this.minecraft != null && this.minecraft.level != null) {
            ClientAptitudeScreenManager.requestOpen();
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long currentTime = Util.getMillis();
        if (advancePhase(currentTime)) {
            return;
        }
        ensureBlurEffect();
        updateSelectionScales(currentTime);

        switch (this.phase) {
            case INTRO, SELECTING, SELECTION_FADE -> renderElementSelection(guiGraphics, currentTime);
            case ROOT_DISPLAY -> renderSpiritualRootName(guiGraphics, currentTime);
            case SPROUT_GROW, LENGTH_SELECTION, SPROUT_FADE -> renderSproutStage(guiGraphics, currentTime);
            case SOUL_INTRO, SOUL_SELECTION, SOUL_BLOOM, COMPLETE_FADE -> renderSoulStage(
                    guiGraphics,
                    currentTime
            );
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.phase == AptitudePhase.SELECTING && !this.selectedElements.isEmpty()) {
                confirmElementSelection();
                return true;
            }
            if (this.phase == AptitudePhase.LENGTH_SELECTION) {
                confirmSpiritualRootLength();
                return true;
            }
            if (this.phase == AptitudePhase.SOUL_SELECTION
                    && getIlluminatedSoulStarCount() >= SoulStrength.MINIMUM_ILLUMINATED_STARS) {
                submitAptitude();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (this.phase == AptitudePhase.SELECTING) {
            ElementType clickedElement = findClickedElement(mouseX, mouseY);
            if (clickedElement != null) {
                toggleElement(clickedElement);
                return true;
            }
        }
        if (this.phase == AptitudePhase.SOUL_SELECTION) {
            int clickedStarIndex = findClickedSoulStar(mouseX, mouseY);
            if (clickedStarIndex >= 0) {
                toggleSoulStar(clickedStarIndex);
                return true;
            }
        }
        if (this.phase == AptitudePhase.LENGTH_SELECTION && isMouseOverSprout(mouseX, mouseY)) {
            this.draggingSprout = true;
            updateSpiritualRootLength(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && this.phase == AptitudePhase.LENGTH_SELECTION
                && this.draggingSprout) {
            updateSpiritualRootLength(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && this.phase == AptitudePhase.LENGTH_SELECTION
                && this.draggingSprout) {
            this.draggingSprout = false;
            updateSpiritualRootLength(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void renderElementSelection(GuiGraphics guiGraphics, long currentTime) {
        long phaseElapsed = currentTime - this.phaseStartedAt;
        float selectionAlpha = this.phase == AptitudePhase.SELECTION_FADE
                ? 1.0F - AnimationUtility.easeInOutCubic(
                        AnimationUtility.progress(phaseElapsed, SELECTION_FADE_DURATION)
                )
                : 1.0F;
        List<ScreenPoint> elementPositions = getElementPositions();
        float connectionProgress = this.phase == AptitudePhase.INTRO
                ? AnimationUtility.easeOutCubic(AnimationUtility.progress(phaseElapsed - 320L, 1500L))
                : 1.0F;
        float formationAlpha = selectionAlpha * connectionProgress;
        AptitudeFormationRenderUtility.drawFormation(
                guiGraphics,
                this.width * 0.5F,
                getDiagramCenterY(),
                getFormationRadius(),
                currentTime,
                formationAlpha
        );
        float nodeRadius = getElementNodeRadius();
        for (int index = 0; index < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); index++) {
            ElementType element = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            float introProgress = getElementIntroProgress(index, phaseElapsed);
            if (introProgress <= 0.0F) {
                continue;
            }
            AptitudeFormationRenderUtility.drawNodeSeal(
                    guiGraphics,
                    elementPositions.get(index),
                    nodeRadius,
                    element,
                    SpiritualRootUtility.getElementColor(element),
                    this.selectedElements.contains(element),
                    currentTime,
                    formationAlpha * introProgress
            );
        }
        renderElementRelationEffects(
                guiGraphics,
                elementPositions,
                currentTime,
                selectionAlpha * connectionProgress
        );

        float elementScale = getElementTextScale();
        float yinYangBalanceStrength = getElementEffectStrength(ElementType.YIN_YANG);
        float balancePulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.003F);
        for (int index = 0; index < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); index++) {
            ElementType element = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            float introProgress = getElementIntroProgress(index, phaseElapsed);
            if (introProgress <= 0.0F) {
                continue;
            }
            boolean selected = this.selectedElements.contains(element);
            boolean balanceFocus = yinYangBalanceStrength > 0.01F && isYinYangBalanceFocus(element);
            int baseColor = SpiritualRootUtility.getElementColor(element);
            int displayColor = selected
                    ? ColorUtility.brighten(baseColor, 0.28F)
                    : ColorUtility.darken(baseColor, 0.42F);
            if (balanceFocus) {
                displayColor = ColorUtility.blend(
                        displayColor,
                        selected ? 0xD8DCE3 : 0xF1DFC0,
                        yinYangBalanceStrength * (selected ? 0.34F : 0.48F) * balancePulse
                );
            }
            float alpha = selectionAlpha * introProgress * (selected ? 1.0F : 0.82F);
            float scale = elementScale
                    * this.selectionScales.get(element)
                    * (0.82F + 0.18F * AnimationUtility.easeOutBack(introProgress))
                    * (1.0F + yinYangBalanceStrength * 0.025F * balancePulse);
            ScreenPoint position = elementPositions.get(index);
            float textCenterX = position.x() - getElementTextOpticalOffsetX(element) * scale;
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.elementComponents.get(element),
                    textCenterX,
                    position.y(),
                    scale,
                    ColorUtility.withAlpha(displayColor, alpha),
                    ColorUtility.brighten(baseColor, 0.56F),
                    selected ? alpha * 0.48F : alpha * 0.08F
            );
        }

        float taijiProgress = this.phase == AptitudePhase.INTRO
                ? AnimationUtility.easeOutBack(AnimationUtility.progress(
                        phaseElapsed - TAIJI_APPEAR_DELAY,
                        TAIJI_APPEAR_DURATION
                ))
                : 1.0F;
        if (taijiProgress > 0.0F) {
            float centerX = this.width * 0.5F;
            float centerY = getDiagramCenterY();
            float rotationDuration = Mth.lerp(yinYangBalanceStrength, 45000.0F, 10500.0F);
            float rotation = (currentTime % (long) rotationDuration) * 360.0F / rotationDuration;
            float balanceScale = 1.0F + yinYangBalanceStrength * 0.045F * balancePulse;
            AptitudeRenderUtility.drawTaiji(
                    guiGraphics,
                    centerX,
                    centerY,
                    getTaijiRadius(),
                    rotation,
                    this.taijiScale * balanceScale * (0.78F + 0.22F * taijiProgress),
                    selectionAlpha * Mth.clamp(taijiProgress, 0.0F, 1.0F)
            );
        }

        if (this.phase != AptitudePhase.INTRO) {
            int hintColor = this.selectedElements.isEmpty() ? 0xA9A7A0 : 0xE8DFC8;
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.selectionHint,
                    this.width * 0.5F,
                    this.height * 0.91F,
                    getHintScale(),
                    ColorUtility.withAlpha(hintColor, selectionAlpha * 0.92F),
                    0x111318,
                    selectionAlpha * 0.45F
            );
        }
    }

    private void renderElementRelationEffects(
            GuiGraphics guiGraphics,
            List<ScreenPoint> elementPositions,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F || SpiritualRootUtility.isChaos(this.selectedElements)) {
            return;
        }
        float nodeRadius = getElementNodeRadius();
        float formationRadius = getFormationRadius();
        int selectedFiveElementCount = 0;
        for (ElementType element : SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER) {
            if (this.selectedElements.contains(element)) {
                selectedFiveElementCount++;
            }
        }
        float crowdAlpha = Mth.clamp(1.06F - selectedFiveElementCount * 0.1F, 0.58F, 1.0F);
        for (int sourceIndex = 0; sourceIndex < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); sourceIndex++) {
            ElementType sourceElement = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(sourceIndex);
            float sourceStrength = getElementEffectStrength(sourceElement);
            if (sourceStrength <= 0.01F) {
                continue;
            }
            ElementRelationRenderUtility.drawSourceAura(
                    guiGraphics,
                    sourceElement,
                    elementPositions.get(sourceIndex),
                    formationRadius,
                    nodeRadius,
                    currentTime,
                    alpha * sourceStrength * crowdAlpha
            );
        }
        for (int sourceIndex = 0; sourceIndex < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); sourceIndex++) {
            ElementType sourceElement = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(sourceIndex);
            float sourceStrength = getElementEffectStrength(sourceElement);
            if (sourceStrength <= 0.01F) {
                continue;
            }
            for (int targetIndex = 0; targetIndex < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); targetIndex++) {
                if (sourceIndex == targetIndex) {
                    continue;
                }
                ElementType targetElement = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(targetIndex);
                ElementRelation relation = sourceElement.getRelationTo(targetElement);
                if (relation != ElementRelation.GENERATES && relation != ElementRelation.OVERCOMES) {
                    continue;
                }
                ElementRelationRenderUtility.drawRelation(
                        guiGraphics,
                        sourceElement,
                        targetElement,
                        elementPositions.get(sourceIndex),
                        elementPositions.get(targetIndex),
                        formationRadius,
                        nodeRadius,
                        currentTime,
                        alpha * sourceStrength * crowdAlpha
                );
            }
        }
        float yinYangStrength = getElementEffectStrength(ElementType.YIN_YANG);
        if (yinYangStrength > 0.01F) {
            ElementRelationRenderUtility.drawYinYangBalance(
                    guiGraphics,
                    new ScreenPoint(this.width * 0.5F, getDiagramCenterY()),
                    getTaijiRadius(),
                    elementPositions,
                    this.selectedElements,
                    nodeRadius,
                    currentTime,
                    alpha * yinYangStrength
            );
        }
    }

    private void renderSpiritualRootName(GuiGraphics guiGraphics, long currentTime) {
        long elapsed = currentTime - this.phaseStartedAt;
        float alpha;
        float appearanceProgress;
        if (elapsed < ROOT_FADE_IN_DURATION) {
            appearanceProgress = AnimationUtility.progress(elapsed, ROOT_FADE_IN_DURATION);
            alpha = AnimationUtility.easeOutCubic(appearanceProgress);
        } else if (elapsed < ROOT_FADE_IN_DURATION + ROOT_HOLD_DURATION) {
            appearanceProgress = 1.0F;
            alpha = 1.0F;
        } else {
            appearanceProgress = 1.0F;
            alpha = 1.0F - AnimationUtility.easeInOutCubic(AnimationUtility.progress(
                    elapsed - ROOT_FADE_IN_DURATION - ROOT_HOLD_DURATION,
                    ROOT_FADE_OUT_DURATION
            ));
        }
        float scale = getSpiritualRootNameScale()
                * (0.86F + 0.14F * AnimationUtility.easeOutBack(appearanceProgress));
        int outlineColor = this.spiritualRootColor == 0x000000
                ? 0xEEEAE0
                : ColorUtility.brighten(this.spiritualRootColor, 0.62F);
        AptitudeRenderUtility.drawCenteredText(
                guiGraphics,
                this.font,
                this.spiritualRootName,
                this.width * 0.5F,
                this.height * 0.48F,
                scale,
                ColorUtility.withAlpha(this.spiritualRootColor, alpha),
                outlineColor,
                alpha * (this.spiritualRootColor == 0x000000 ? 0.72F : 0.38F)
        );
    }

    private void renderSproutStage(GuiGraphics guiGraphics, long currentTime) {
        long elapsed = currentTime - this.phaseStartedAt;
        float stageAlpha = this.phase == AptitudePhase.SPROUT_FADE
                ? 1.0F - AnimationUtility.easeInOutCubic(
                        AnimationUtility.progress(elapsed, SPROUT_FADE_DURATION)
                )
                : 1.0F;
        float bottomY = getSproutBottomY();
        float topY;
        float hintAlpha;
        if (this.phase == AptitudePhase.SPROUT_GROW) {
            float growthProgress = AnimationUtility.easeOutCubic(
                    AnimationUtility.progress(elapsed, SPROUT_GROW_DURATION)
            );
            topY = bottomY - getMinimumStemHeight() * growthProgress;
            hintAlpha = AnimationUtility.progress(elapsed - 620L, 300L);
        } else {
            topY = getSproutTopY(this.spiritualRootLength);
            hintAlpha = 1.0F;
        }
        ScreenPoint rootMarker = AptitudeRenderUtility.drawLivingSpiritualRoot(
                guiGraphics,
                this.width * 0.5F,
                bottomY,
                topY,
                getMaximumStemTopY(),
                this.spiritualRootColor,
                currentTime,
                stageAlpha,
                this.phase == AptitudePhase.LENGTH_SELECTION
        );

        float hintScale = getHintScale();
        int readableColor = this.spiritualRootColor == 0x000000
                ? 0x111111
                : ColorUtility.brighten(this.spiritualRootColor, 0.2F);
        int outlineColor = this.spiritualRootColor == 0x000000 ? 0xEEEAE0 : 0x111318;
        if (this.phase != AptitudePhase.SPROUT_GROW || hintAlpha > 0.0F) {
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.lengthHint,
                    this.width * 0.5F,
                    this.height * 0.1F,
                    hintScale,
                    ColorUtility.withAlpha(readableColor, stageAlpha * hintAlpha * 0.92F),
                    outlineColor,
                    stageAlpha * hintAlpha * 0.5F
            );
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.maximumLengthHint,
                    this.width * 0.5F + 38.0F,
                    getMaximumStemTopY() + 4.0F,
                    hintScale * 0.62F,
                    ColorUtility.withAlpha(0xE7C87A, stageAlpha * hintAlpha * 0.7F),
                    0x111318,
                    stageAlpha * hintAlpha * 0.42F
            );
        }
        if (this.phase == AptitudePhase.LENGTH_SELECTION || this.phase == AptitudePhase.SPROUT_FADE) {
            Component lengthText = customFontText(formatLength(this.spiritualRootLength));
            float lengthLabelY = rootMarker.y() - 18.0F;
            if (lengthLabelY < this.height * 0.145F) {
                lengthLabelY = rootMarker.y() + 19.0F;
            }
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    lengthText,
                    rootMarker.x(),
                    lengthLabelY,
                    hintScale * 1.14F,
                    ColorUtility.withAlpha(readableColor, stageAlpha),
                    outlineColor,
                    stageAlpha * 0.58F
            );
        }
        if (this.phase == AptitudePhase.LENGTH_SELECTION) {
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.lengthSelectionHint,
                    this.width * 0.5F,
                    this.height * 0.91F,
                    getHintScale() * 0.78F,
                    ColorUtility.withAlpha(0xD8D4CA, stageAlpha * 0.82F),
                    0x111318,
                    stageAlpha * 0.42F
            );
        }
    }

    private void renderSoulStage(GuiGraphics guiGraphics, long currentTime) {
        long elapsed = currentTime - this.phaseStartedAt;
        float introProgress = this.phase == AptitudePhase.SOUL_INTRO
                ? AnimationUtility.easeOutCubic(AnimationUtility.progress(elapsed, SOUL_INTRO_DURATION))
                : 1.0F;
        float bloomProgress;
        if (this.phase == AptitudePhase.SOUL_BLOOM) {
            bloomProgress = AnimationUtility.progress(elapsed, SOUL_BLOOM_DURATION);
        } else if (this.phase == AptitudePhase.COMPLETE_FADE
                && this.illuminatedSoulStarMask == SoulConstellationRenderUtility.ALL_STARS_MASK) {
            bloomProgress = 1.0F;
        } else {
            bloomProgress = 0.0F;
        }
        float stageAlpha = this.phase == AptitudePhase.COMPLETE_FADE
                ? 1.0F - AnimationUtility.easeInOutCubic(
                        AnimationUtility.progress(elapsed, COMPLETE_FADE_DURATION)
                )
                : 1.0F;
        SoulConstellationRenderUtility.drawConstellation(
                guiGraphics,
                this.width * 0.5F,
                getSoulConstellationCenterY(),
                getSoulConstellationRadius(),
                currentTime,
                this.illuminatedSoulStarMask,
                introProgress,
                bloomProgress,
                stageAlpha
        );

        AptitudeRenderUtility.drawCenteredText(
                guiGraphics,
                this.font,
                this.soulStrengthHint,
                this.width * 0.5F,
                this.height * 0.1F,
                getHintScale() * 1.08F,
                ColorUtility.withAlpha(0xD8F1FF, stageAlpha * introProgress * 0.94F),
                0x111827,
                stageAlpha * introProgress * 0.52F
        );
        if (this.phase != AptitudePhase.SOUL_INTRO) {
            int illuminatedStarCount = getIlluminatedSoulStarCount();
            String starCountText = ChineseNumberUtility.toChineseInteger(illuminatedStarCount) + "星";
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    customFontText(starCountText),
                    this.width * 0.5F,
                    this.height * 0.78F,
                    getHintScale() * 0.9F,
                    ColorUtility.withAlpha(
                            illuminatedStarCount == SoulStrength.MAXIMUM_ILLUMINATED_STARS
                                    ? 0xF4FCFF
                                    : 0xA9D7EA,
                            stageAlpha * 0.9F
                    ),
                    0x101722,
                    stageAlpha * 0.48F
            );
        }
        if (this.phase == AptitudePhase.SOUL_SELECTION) {
            boolean canConfirm = getIlluminatedSoulStarCount() >= SoulStrength.MINIMUM_ILLUMINATED_STARS;
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.soulSelectionHint,
                    this.width * 0.5F,
                    this.height * 0.91F,
                    getHintScale() * 0.78F,
                    ColorUtility.withAlpha(canConfirm ? 0xDCEEF5 : 0x89939E, stageAlpha * 0.86F),
                    0x111318,
                    stageAlpha * 0.44F
            );
        }
    }

    private void confirmElementSelection() {
        this.confirmedElements = Set.copyOf(this.selectedElements);
        this.spiritualRootName = displayFontText(
                SpiritualRootUtility.getSpiritualRootName(this.confirmedElements)
        );
        this.spiritualRootColor = SpiritualRootUtility.getSpiritualRootColor(this.confirmedElements);
        playUiSound(0.84F);
        setPhase(AptitudePhase.SELECTION_FADE);
    }

    private void confirmSpiritualRootLength() {
        this.draggingSprout = false;
        playUiSound(0.96F);
        setPhase(AptitudePhase.SPROUT_FADE);
    }

    private void submitAptitude() {
        int illuminatedSoulStars = getIlluminatedSoulStarCount();
        if (this.submitted
                || this.confirmedElements.isEmpty()
                || illuminatedSoulStars < SoulStrength.MINIMUM_ILLUMINATED_STARS) {
            return;
        }
        this.submitted = true;
        boolean allStarsIlluminated = illuminatedSoulStars == SoulStrength.MAXIMUM_ILLUMINATED_STARS;
        playUiSound(allStarsIlluminated ? 1.42F : 1.18F);
        ModNetwork.sendToServer(new SubmitAptitudePacket(
                SpiritualRoot.createElementMask(this.confirmedElements),
                this.spiritualRootLength,
                illuminatedSoulStars
        ));
        setPhase(allStarsIlluminated ? AptitudePhase.SOUL_BLOOM : AptitudePhase.COMPLETE_FADE);
    }

    private void toggleElement(ElementType element) {
        if (this.selectedElements.contains(element)) {
            this.selectedElements.remove(element);
            playUiSound(0.78F);
        } else {
            this.selectedElements.add(element);
            playUiSound(1.04F);
        }
    }

    private void toggleSoulStar(int starIndex) {
        int starBit = 1 << starIndex;
        if ((this.illuminatedSoulStarMask & starBit) != 0) {
            this.illuminatedSoulStarMask &= ~starBit;
            playUiSound(0.82F);
        } else {
            this.illuminatedSoulStarMask |= starBit;
            playUiSound(0.98F + starIndex * 0.045F);
        }
    }

    @Nullable
    private ElementType findClickedElement(double mouseX, double mouseY) {
        List<ScreenPoint> elementPositions = getElementPositions();
        float textScale = getElementTextScale();
        for (int index = 0; index < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); index++) {
            ElementType element = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            ScreenPoint position = elementPositions.get(index);
            float scale = textScale * this.selectionScales.get(element);
            float halfWidth = this.font.width(this.elementComponents.get(element)) * scale * 0.5F + 8.0F;
            float halfHeight = this.font.lineHeight * scale * 0.5F + 7.0F;
            if (Math.abs(mouseX - position.x()) <= halfWidth
                    && Math.abs(mouseY - position.y()) <= halfHeight) {
                return element;
            }
        }
        float deltaX = (float) mouseX - this.width * 0.5F;
        float deltaY = (float) mouseY - getDiagramCenterY();
        float taijiHitRadius = getTaijiRadius() * this.taijiScale + 5.0F;
        if (deltaX * deltaX + deltaY * deltaY <= taijiHitRadius * taijiHitRadius) {
            return ElementType.YIN_YANG;
        }
        return null;
    }

    private int findClickedSoulStar(double mouseX, double mouseY) {
        List<ScreenPoint> starPositions = SoulConstellationRenderUtility.getStarPositions(
                this.width * 0.5F,
                getSoulConstellationCenterY(),
                getSoulConstellationRadius()
        );
        float hitRadius = Mth.clamp(getSoulConstellationRadius() * 0.06F, 8.0F, 17.0F);
        for (int starIndex = 0; starIndex < starPositions.size(); starIndex++) {
            ScreenPoint starPosition = starPositions.get(starIndex);
            float deltaX = (float) mouseX - starPosition.x();
            float deltaY = (float) mouseY - starPosition.y();
            if (deltaX * deltaX + deltaY * deltaY <= hitRadius * hitRadius) {
                return starIndex;
            }
        }
        return -1;
    }

    private int getIlluminatedSoulStarCount() {
        return Integer.bitCount(this.illuminatedSoulStarMask & SoulConstellationRenderUtility.ALL_STARS_MASK);
    }

    private boolean isMouseOverSprout(double mouseX, double mouseY) {
        float topY = getSproutTopY(this.spiritualRootLength);
        float rootHeight = getSproutBottomY() - topY;
        float horizontalReach = Mth.clamp(rootHeight * 0.13F, 24.0F, 52.0F);
        return Math.abs(mouseX - this.width * 0.5F) <= horizontalReach
                && mouseY >= topY - 13.0F
                && mouseY <= getSproutBottomY() + 18.0F;
    }

    private void updateSpiritualRootLength(double mouseY) {
        float minimumTopY = getSproutBottomY() - getMinimumStemHeight();
        float maximumTopY = getMaximumStemTopY();
        float clampedTopY = Mth.clamp((float) mouseY, maximumTopY, minimumTopY);
        float qualityProgress = (minimumTopY - clampedTopY) / (minimumTopY - maximumTopY);
        float length = Mth.lerp(
                qualityProgress,
                SpiritualRoot.MINIMUM_LENGTH_INCHES,
                SpiritualRoot.MAXIMUM_LENGTH_INCHES
        );
        this.spiritualRootLength = Math.round(length * 10.0F) / 10.0F;
    }

    private boolean advancePhase(long currentTime) {
        long elapsed = currentTime - this.phaseStartedAt;
        switch (this.phase) {
            case INTRO -> {
                if (elapsed >= INTRO_DURATION) {
                    setPhase(AptitudePhase.SELECTING);
                }
            }
            case SELECTION_FADE -> {
                if (elapsed >= SELECTION_FADE_DURATION) {
                    setPhase(AptitudePhase.ROOT_DISPLAY);
                }
            }
            case ROOT_DISPLAY -> {
                if (elapsed >= ROOT_DISPLAY_DURATION) {
                    setPhase(AptitudePhase.SPROUT_GROW);
                }
            }
            case SPROUT_GROW -> {
                if (elapsed >= SPROUT_GROW_DURATION) {
                    setPhase(AptitudePhase.LENGTH_SELECTION);
                }
            }
            case SPROUT_FADE -> {
                if (elapsed >= SPROUT_FADE_DURATION) {
                    setPhase(AptitudePhase.SOUL_INTRO);
                }
            }
            case SOUL_INTRO -> {
                if (elapsed >= SOUL_INTRO_DURATION) {
                    setPhase(AptitudePhase.SOUL_SELECTION);
                }
            }
            case SOUL_BLOOM -> {
                if (elapsed >= SOUL_BLOOM_DURATION) {
                    setPhase(AptitudePhase.COMPLETE_FADE);
                }
            }
            case COMPLETE_FADE -> {
                if (elapsed >= COMPLETE_FADE_DURATION && this.minecraft != null) {
                    this.minecraft.setScreen(null);
                    return true;
                }
            }
            case SELECTING, LENGTH_SELECTION, SOUL_SELECTION -> {
            }
        }
        return false;
    }

    private void setPhase(AptitudePhase newPhase) {
        this.phase = newPhase;
        this.phaseStartedAt = Util.getMillis();
    }

    private void updateSelectionScales(long currentTime) {
        float deltaSeconds = Mth.clamp((currentTime - this.lastFrameAt) / 1000.0F, 0.0F, 0.1F);
        this.lastFrameAt = currentTime;
        for (ElementType element : SpiritualRoot.SELECTABLE_ELEMENTS) {
            if (element == ElementType.YIN_YANG) {
                continue;
            }
            float targetScale = this.selectedElements.contains(element) ? SELECTED_ELEMENT_SCALE : 1.0F;
            this.selectionScales.put(
                    element,
                    AnimationUtility.approach(this.selectionScales.get(element), targetScale, deltaSeconds, 14.0F)
            );
        }
        float taijiTargetScale = this.selectedElements.contains(ElementType.YIN_YANG)
                ? SELECTED_TAIJI_SCALE
                : 1.0F;
        this.taijiScale = AnimationUtility.approach(
                this.taijiScale,
                taijiTargetScale,
                deltaSeconds,
                14.0F
        );
    }

    private List<ScreenPoint> getElementPositions() {
        float centerX = this.width * 0.5F;
        float centerY = getDiagramCenterY();
        float radius = getDiagramRadius();
        return SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.stream()
                .map(element -> {
                    int index = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.indexOf(element);
                    float angle = (float) Math.toRadians(-90.0F + index * 72.0F);
                    return new ScreenPoint(
                            centerX + Mth.cos(angle) * radius,
                            centerY + Mth.sin(angle) * radius
                    );
                })
                .toList();
    }

    private float getElementIntroProgress(int elementIndex, long phaseElapsed) {
        if (this.phase != AptitudePhase.INTRO) {
            return 1.0F;
        }
        long elementElapsed = phaseElapsed - elementIndex * ELEMENT_APPEAR_INTERVAL;
        return AnimationUtility.easeOutCubic(
                AnimationUtility.progress(elementElapsed, ELEMENT_APPEAR_DURATION)
        );
    }

    private float getDiagramCenterY() {
        return this.height * 0.43F;
    }

    private float getDiagramRadius() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.31F, 56.0F, 172.0F);
    }

    private float getFormationRadius() {
        return getDiagramRadius() + getElementNodeRadius() * 2.05F;
    }

    private float getTaijiRadius() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.072F, 13.0F, 36.0F);
    }

    private float getElementNodeRadius() {
        return this.font.lineHeight * getElementTextScale() * 0.68F + 7.0F;
    }

    private float getElementEffectStrength(ElementType element) {
        if (element == ElementType.YIN_YANG) {
            return Mth.clamp((this.taijiScale - 1.0F) / (SELECTED_TAIJI_SCALE - 1.0F), 0.0F, 1.0F);
        }
        Float selectionScale = this.selectionScales.get(element);
        if (selectionScale == null) {
            return 0.0F;
        }
        return Mth.clamp(
                (selectionScale - 1.0F) / (SELECTED_ELEMENT_SCALE - 1.0F),
                0.0F,
                1.0F
        );
    }

    private boolean isYinYangBalanceFocus(ElementType targetElement) {
        if (this.selectedElements.contains(targetElement)) {
            return true;
        }
        for (ElementType selectedElement : this.selectedElements) {
            if (!selectedElement.isFiveElement()) {
                continue;
            }
            ElementRelation relation = selectedElement.getRelationTo(targetElement);
            if (relation == ElementRelation.GENERATES || relation == ElementRelation.GENERATED_BY) {
                return true;
            }
        }
        return false;
    }

    private float getElementTextScale() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.014F, 2.35F, 4.2F);
    }

    private static float getElementTextOpticalOffsetX(ElementType element) {
        return switch (element) {
            case WOOD -> 1.2F;
            case WATER -> 1.45F;
            case FIRE -> 0.85F;
            case EARTH -> 0.65F;
            case METAL -> 0.25F;
            default -> 0.0F;
        };
    }

    private float getHintScale() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.0065F, 1.0F, 1.65F);
    }

    private float getSpiritualRootNameScale() {
        float textWidth = Math.max(1.0F, this.font.width(this.spiritualRootName));
        return Mth.clamp(this.width * 0.76F / textWidth, 2.0F, 6.2F);
    }

    private float getSoulConstellationCenterY() {
        return this.height * 0.46F;
    }

    private float getSoulConstellationRadius() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.36F, 72.0F, 210.0F);
    }

    private float getSproutBottomY() {
        return this.height - Math.max(24.0F, this.height * 0.08F);
    }

    private float getMinimumStemHeight() {
        float fullStemHeight = getSproutBottomY() - getMaximumStemTopY();
        return fullStemHeight * SpiritualRoot.MINIMUM_LENGTH_INCHES
                / SpiritualRoot.MAXIMUM_LENGTH_INCHES;
    }

    private float getMaximumStemTopY() {
        return Math.max(24.0F, this.height * 0.13F);
    }

    private float getSproutTopY(float lengthInches) {
        float lengthProgress = Mth.clamp(
                lengthInches / SpiritualRoot.MAXIMUM_LENGTH_INCHES,
                0.0F,
                1.0F
        );
        float fullStemHeight = getSproutBottomY() - getMaximumStemTopY();
        return getSproutBottomY() - fullStemHeight * lengthProgress;
    }

    private void playUiSound(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK,
                    pitch
            ));
        }
    }

    private void installBlurEffect() {
        if (this.minecraft == null || this.blurInstalled) {
            return;
        }
        PostChain currentEffect = this.minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && !BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            this.previousPostEffectName = currentEffect.getName();
        }
        this.minecraft.gameRenderer.loadEffect(BLUR_EFFECT);
        PostChain installedEffect = this.minecraft.gameRenderer.currentEffect();
        this.blurInstalled = installedEffect != null && BLUR_EFFECT_NAME.equals(installedEffect.getName());
        this.blurLoadFailed = !this.blurInstalled;
        if (!this.blurInstalled) {
            restorePreviousPostEffect();
        }
    }

    private void ensureBlurEffect() {
        if (this.minecraft == null || this.blurLoadFailed) {
            return;
        }
        PostChain currentEffect = this.minecraft.gameRenderer.currentEffect();
        if (currentEffect == null || !BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            this.blurInstalled = false;
            installBlurEffect();
        }
    }

    private void restorePreviousPostEffect() {
        if (this.minecraft == null) {
            return;
        }
        PostChain currentEffect = this.minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            this.minecraft.gameRenderer.shutdownEffect();
        }
        this.blurInstalled = false;
        if (this.previousPostEffectName != null) {
            ResourceLocation previousEffect = ResourceLocation.tryParse(this.previousPostEffectName);
            this.previousPostEffectName = null;
            if (previousEffect != null) {
                this.minecraft.gameRenderer.loadEffect(previousEffect);
            }
        }
    }

    private static String formatLength(float lengthInches) {
        return ChineseNumberUtility.formatTenths(lengthInches, "寸", "分");
    }

    private static Component customFontText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(CustomFontConstants.SHAN_BEI_FONT_ID));
    }

    private static Component displayFontText(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.SHAN_BEI_DISPLAY_FONT_ID)
        );
    }

    private enum AptitudePhase {
        INTRO,
        SELECTING,
        SELECTION_FADE,
        ROOT_DISPLAY,
        SPROUT_GROW,
        LENGTH_SELECTION,
        SPROUT_FADE,
        SOUL_INTRO,
        SOUL_SELECTION,
        SOUL_BLOOM,
        COMPLETE_FADE
    }
}
