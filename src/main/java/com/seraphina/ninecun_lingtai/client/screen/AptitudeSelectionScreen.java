package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.utility.AnimationUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
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
import java.util.Locale;
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
    private static final long COMPLETE_FADE_DURATION = 720L;
    private static final float SELECTED_ELEMENT_SCALE = 1.14F;
    private static final float SELECTED_TAIJI_SCALE = 1.09F;

    private final EnumSet<ElementType> selectedElements = EnumSet.noneOf(ElementType.class);
    private final EnumMap<ElementType, Float> selectionScales = new EnumMap<>(ElementType.class);
    private final EnumMap<ElementType, Component> elementComponents = new EnumMap<>(ElementType.class);
    private final Component selectionHint = customFontText("点击五行与阴阳，按 Enter 确定");
    private final Component lengthHint = customFontText("拖动灵苗决定长度，松开确定");

    private AptitudePhase phase = AptitudePhase.INTRO;
    private long phaseStartedAt;
    private long lastFrameAt;
    private Set<ElementType> confirmedElements = Set.of();
    private Component spiritualRootName = Component.empty();
    private int spiritualRootColor = 0xFFFFFF;
    private float spiritualRootLength = SpiritualRoot.MINIMUM_LENGTH_INCHES;
    private float taijiScale = 1.0F;
    private boolean draggingSprout;
    private boolean submitted;
    private boolean blurInstalled;
    private boolean blurLoadFailed;
    @Nullable
    private String previousPostEffectName;

    public AptitudeSelectionScreen() {
        super(Component.literal("资质选择"));
        for (ElementType element : SpiritualRoot.SELECTABLE_ELEMENTS) {
            this.selectionScales.put(element, 1.0F);
            this.elementComponents.put(element, customFontText(SpiritualRootUtility.getElementName(element)));
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
        float screenAlpha = getScreenAlpha(currentTime);
        guiGraphics.fill(
                0,
                0,
                this.width,
                this.height,
                ColorUtility.withAlpha(0x10131A, 0.56F * screenAlpha)
        );

        switch (this.phase) {
            case INTRO, SELECTING, SELECTION_FADE -> renderElementSelection(guiGraphics, currentTime);
            case ROOT_DISPLAY -> renderSpiritualRootName(guiGraphics, currentTime);
            case SPROUT_GROW, LENGTH_SELECTION, COMPLETE_FADE -> renderSproutStage(guiGraphics, currentTime);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && this.phase == AptitudePhase.SELECTING
                && !this.selectedElements.isEmpty()) {
            confirmElementSelection();
            return true;
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
            submitSpiritualRoot();
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
        AptitudeRenderUtility.drawFiveElementConnections(
                guiGraphics,
                elementPositions,
                selectionAlpha * connectionProgress * 0.62F
        );

        float elementScale = getElementTextScale();
        for (int index = 0; index < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); index++) {
            ElementType element = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            float introProgress = getElementIntroProgress(index, phaseElapsed);
            if (introProgress <= 0.0F) {
                continue;
            }
            boolean selected = this.selectedElements.contains(element);
            int baseColor = SpiritualRootUtility.getElementColor(element);
            int displayColor = selected
                    ? ColorUtility.brighten(baseColor, 0.28F)
                    : ColorUtility.darken(baseColor, 0.42F);
            float alpha = selectionAlpha * introProgress * (selected ? 1.0F : 0.82F);
            float scale = elementScale
                    * this.selectionScales.get(element)
                    * (0.82F + 0.18F * AnimationUtility.easeOutBack(introProgress));
            ScreenPoint position = elementPositions.get(index);
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    this.elementComponents.get(element),
                    position.x(),
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
            float rotation = (currentTime % 45000L) * 360.0F / 45000.0F;
            AptitudeRenderUtility.drawTaiji(
                    guiGraphics,
                    centerX,
                    centerY,
                    getTaijiRadius(),
                    rotation,
                    this.taijiScale * (0.78F + 0.22F * taijiProgress),
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
        float stageAlpha = this.phase == AptitudePhase.COMPLETE_FADE
                ? 1.0F - AnimationUtility.easeInOutCubic(
                        AnimationUtility.progress(elapsed, COMPLETE_FADE_DURATION)
                )
                : 1.0F;
        float bottomY = getSproutBottomY();
        float topY;
        float leafProgress;
        float hintAlpha;
        if (this.phase == AptitudePhase.SPROUT_GROW) {
            float growthProgress = AnimationUtility.easeOutCubic(
                    AnimationUtility.progress(elapsed, SPROUT_GROW_DURATION)
            );
            topY = bottomY - getMinimumStemHeight() * growthProgress;
            leafProgress = AnimationUtility.progress(
                    elapsed - Math.round(SPROUT_GROW_DURATION * 0.55F),
                    Math.round(SPROUT_GROW_DURATION * 0.45F)
            );
            hintAlpha = AnimationUtility.progress(elapsed - 620L, 300L);
        } else {
            topY = getSproutTopY(this.spiritualRootLength);
            leafProgress = 1.0F;
            hintAlpha = 1.0F;
        }
        AptitudeRenderUtility.drawSprout(
                guiGraphics,
                this.width * 0.5F,
                bottomY,
                topY,
                this.spiritualRootColor,
                stageAlpha,
                leafProgress
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
        }
        if (this.phase == AptitudePhase.LENGTH_SELECTION || this.phase == AptitudePhase.COMPLETE_FADE) {
            Component lengthText = customFontText(formatLength(this.spiritualRootLength));
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    this.font,
                    lengthText,
                    this.width * 0.5F,
                    Math.max(this.height * 0.16F, topY - 18.0F),
                    hintScale * 1.14F,
                    ColorUtility.withAlpha(readableColor, stageAlpha),
                    outlineColor,
                    stageAlpha * 0.58F
            );
        }
    }

    private void confirmElementSelection() {
        this.confirmedElements = Set.copyOf(this.selectedElements);
        this.spiritualRootName = customFontText(
                SpiritualRootUtility.getSpiritualRootName(this.confirmedElements)
        );
        this.spiritualRootColor = SpiritualRootUtility.getSpiritualRootColor(this.confirmedElements);
        playUiSound(0.84F);
        setPhase(AptitudePhase.SELECTION_FADE);
    }

    private void submitSpiritualRoot() {
        if (this.submitted || this.confirmedElements.isEmpty()) {
            return;
        }
        this.submitted = true;
        playUiSound(1.18F);
        ModNetwork.sendToServer(new SubmitAptitudePacket(
                SpiritualRoot.createElementMask(this.confirmedElements),
                this.spiritualRootLength
        ));
        setPhase(AptitudePhase.COMPLETE_FADE);
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

    private boolean isMouseOverSprout(double mouseX, double mouseY) {
        float topY = getSproutTopY(this.spiritualRootLength);
        return Math.abs(mouseX - this.width * 0.5F) <= 22.0F
                && mouseY >= topY - 13.0F
                && mouseY <= getSproutBottomY() + 7.0F;
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
            case COMPLETE_FADE -> {
                if (elapsed >= COMPLETE_FADE_DURATION && this.minecraft != null) {
                    this.minecraft.setScreen(null);
                    return true;
                }
            }
            case SELECTING, LENGTH_SELECTION -> {
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

    private float getScreenAlpha(long currentTime) {
        if (this.phase != AptitudePhase.COMPLETE_FADE) {
            return 1.0F;
        }
        return 1.0F - AnimationUtility.easeInOutCubic(AnimationUtility.progress(
                currentTime - this.phaseStartedAt,
                COMPLETE_FADE_DURATION
        ));
    }

    private float getDiagramCenterY() {
        return this.height * 0.43F;
    }

    private float getDiagramRadius() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.31F, 56.0F, 172.0F);
    }

    private float getTaijiRadius() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.072F, 13.0F, 36.0F);
    }

    private float getElementTextScale() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.014F, 2.35F, 4.2F);
    }

    private float getHintScale() {
        return Mth.clamp(Math.min(this.width, this.height) * 0.0065F, 1.0F, 1.65F);
    }

    private float getSpiritualRootNameScale() {
        float textWidth = Math.max(1.0F, this.font.width(this.spiritualRootName));
        return Mth.clamp(this.width * 0.76F / textWidth, 2.0F, 6.2F);
    }

    private float getSproutBottomY() {
        return this.height - Math.max(14.0F, this.height * 0.065F);
    }

    private float getMinimumStemHeight() {
        return Math.max(24.0F, this.height * 0.13F);
    }

    private float getMaximumStemTopY() {
        return Math.max(20.0F, this.height * 0.14F);
    }

    private float getSproutTopY(float lengthInches) {
        float qualityProgress = (lengthInches - SpiritualRoot.MINIMUM_LENGTH_INCHES)
                / (SpiritualRoot.MAXIMUM_LENGTH_INCHES - SpiritualRoot.MINIMUM_LENGTH_INCHES);
        float minimumHeight = getMinimumStemHeight();
        float maximumHeight = getSproutBottomY() - getMaximumStemTopY();
        return getSproutBottomY() - Mth.lerp(qualityProgress, minimumHeight, maximumHeight);
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
        if (Math.abs(lengthInches - Math.round(lengthInches)) < 0.001F) {
            return Math.round(lengthInches) + "寸";
        }
        return String.format(Locale.ROOT, "%.1f寸", lengthInches);
    }

    private static Component customFontText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(CustomFontConstants.SHAN_BEI_FONT_ID));
    }

    private enum AptitudePhase {
        INTRO,
        SELECTING,
        SELECTION_FADE,
        ROOT_DISPLAY,
        SPROUT_GROW,
        LENGTH_SELECTION,
        COMPLETE_FADE
    }
}
