package net.seraphina.ninecun_lingtai.client.animation;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.client.render.BreakthroughAnimationShader;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.ScreenShaderEffects;
import net.seraphina.ninecun_lingtai.client.utility.BezierUtility;
import net.seraphina.ninecun_lingtai.client.utility.BreakthroughAnimationRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.BreakthroughAnimationSoundUtility;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;

import java.util.Optional;

public final class BreakthroughAnimationManager {
    private static final long DEFAULT_ANIMATION_DURATION_MILLIS = 5_200L;
    private static final long FOUNDATION_TO_GOLDEN_CORE_ANIMATION_DURATION_MILLIS = 7_500L;
    private static final long GOLDEN_CORE_HOLD_DURATION_MILLIS = 2_000L;
    private static final float GOLDEN_CORE_HOLD_START_PROGRESS = 0.84F;
    private static final long FOUNDATION_TO_GOLDEN_CORE_BREAK_START_MILLIS = 2_000L;
    private static final long FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_DURATION_MILLIS = 1_000L;
    private static final long FOUNDATION_TO_GOLDEN_CORE_SETTLE_DURATION_MILLIS = 1_500L;
    private static final long GOLDEN_CORE_TO_NASCENT_SOUL_ANIMATION_DURATION_MILLIS = 8_400L;
    private static final long NASCENT_SOUL_HOLD_DURATION_MILLIS = 1_800L;
    private static final float NASCENT_SOUL_HOLD_START_PROGRESS = 0.82F;
    private static final long GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START_MILLIS = 1_680L;
    private static final long GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_DURATION_MILLIS = 800L;
    private static final long GOLDEN_CORE_TO_NASCENT_SOUL_SETTLE_DURATION_MILLIS = 1_100L;
    private static final long MORTAL_TO_QI_WATER_DROP_IMPACT_MILLIS = 1_612L;
    private static final long MORTAL_TO_QI_VORTEX_START_MILLIS = 1_872L;
    private static final long BLUR_IN_DURATION_MILLIS = 700L;
    private static final long BLUR_OUT_DURATION_MILLIS = 1_000L;
    private static final int DEFAULT_PRIMARY_COLOR = 0x5C7FFF;
    private static final int DEFAULT_SECONDARY_COLOR = 0xB7E6FF;

    private static BreakthroughAnimationType animationType;
    private static long startTimeMillis = -1L;
    private static int primaryColor = DEFAULT_PRIMARY_COLOR;
    private static int secondaryColor = DEFAULT_SECONDARY_COLOR;
    private static int accentColor = 0xFFF3C4;
    private static long lastRenderLogMillis = -1L;
    private static boolean waterDropSoundPlayed;
    private static boolean vortexSoundPlayed;

    private BreakthroughAnimationManager() {
    }

    public static void start(BreakthroughAnimationType type, int elementMask) {
        if (type == null) {
            return;
        }
        Optional<SpiritualRoot> spiritualRoot = SpiritualRoot.fromElementMask(
                elementMask,
                SpiritualRoot.MAXIMUM_LENGTH_INCHES
        );
        if (spiritualRoot.isPresent()) {
            primaryColor = SpiritualRootUtility.getSpiritualRootColor(spiritualRoot.get().elements());
            secondaryColor = SpiritualRootUtility.getSpiritualRootGlowColor(spiritualRoot.get().elements());
            if (primaryColor == 0x000000) {
                primaryColor = SpiritualRootUtility.getElementColor(
                        net.seraphina.ninecun_lingtai.api.element.ElementType.CHAOS
                );
            }
            accentColor = ColorUtility.brighten(secondaryColor, 0.48F);
        } else {
            primaryColor = DEFAULT_PRIMARY_COLOR;
            secondaryColor = DEFAULT_SECONDARY_COLOR;
            accentColor = 0xFFF3C4;
        }
        animationType = type;
        startTimeMillis = Util.getMillis();
        lastRenderLogMillis = -1L;
        waterDropSoundPlayed = false;
        vortexSoundPlayed = false;
        BreakthroughAnimationSoundUtility.stopAll();
        ModMain.LOGGER.info(
                "Started breakthrough animation: type={}, elementMask={}",
                type,
                elementMask
        );
        ScreenShaderEffects.setBreakthroughBlurActive(true);
    }

    public static void tick() {
        if (!isActive()) {
            return;
        }
        long elapsedMillis = getElapsedMillis();
        if (animationType == BreakthroughAnimationType.MORTAL_TO_QI
                && !waterDropSoundPlayed
                && elapsedMillis >= MORTAL_TO_QI_WATER_DROP_IMPACT_MILLIS) {
            BreakthroughAnimationSoundUtility.playWaterDrop();
            waterDropSoundPlayed = true;
        }
        if (animationType == BreakthroughAnimationType.MORTAL_TO_QI
                && !vortexSoundPlayed
                && elapsedMillis >= MORTAL_TO_QI_VORTEX_START_MILLIS) {
            BreakthroughAnimationSoundUtility.playVortex();
            vortexSoundPlayed = true;
        }
        if (elapsedMillis >= getAnimationDurationMillis()) {
            reset();
        }
    }

    public static void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!isActive()) {
            return;
        }
        float elapsedMillis = getElapsedMillis() + partialTicks * 50.0F;
        if (lastRenderLogMillis < 0L || elapsedMillis - lastRenderLogMillis >= 1_000.0F) {
            lastRenderLogMillis = (long) elapsedMillis;
            ModMain.LOGGER.info(
                    "Rendering breakthrough animation: type={}, elapsedMs={}, shaderLoaded={}",
                    animationType,
                    (long) elapsedMillis,
                    BreakthroughAnimationShader.getBreakthroughAnimationShader() != null
            );
        }
        float progress = getAnimationProgress(elapsedMillis);
        BreakthroughAnimationRenderUtility.render(
                guiGraphics,
                animationType,
                progress,
                elapsedMillis / 1000.0F,
                getScreenShakeStrength(),
                primaryColor,
                secondaryColor,
                accentColor
        );
    }

    public static float getBlurStrength() {
        if (!isActive()) {
            return 0.0F;
        }
        long elapsedMillis = getElapsedMillis();
        if (elapsedMillis < BLUR_IN_DURATION_MILLIS) {
            return BezierUtility.cubic(
                    elapsedMillis / (float) BLUR_IN_DURATION_MILLIS,
                    0.0F,
                    0.08F,
                    0.82F,
                    1.0F
            );
        }
        long blurOutStartMillis = getAnimationDurationMillis() - BLUR_OUT_DURATION_MILLIS;
        if (elapsedMillis < blurOutStartMillis) {
            return 1.0F;
        }
        return 1.0F - BezierUtility.cubic(
                (elapsedMillis - blurOutStartMillis) / (float) BLUR_OUT_DURATION_MILLIS,
                0.0F,
                0.18F,
                0.92F,
                1.0F
        );
    }

    public static void reset() {
        if (isActive()) {
            ModMain.LOGGER.info("Finished breakthrough animation: type={}", animationType);
        }
        animationType = null;
        startTimeMillis = -1L;
        lastRenderLogMillis = -1L;
        waterDropSoundPlayed = false;
        vortexSoundPlayed = false;
        BreakthroughAnimationSoundUtility.stopAll();
        ScreenShaderEffects.setBreakthroughBlurActive(false);
    }

    public static boolean isActive() {
        return animationType != null && startTimeMillis >= 0L;
    }

    public static float getScreenShakeStrength() {
        if (!isActive()) {
            return 0.0F;
        }

        long elapsedMillis = getElapsedMillis();
        if (animationType == BreakthroughAnimationType.GOLDEN_CORE_TO_NASCENT_SOUL) {
            return getGoldenCoreToNascentSoulShakeStrength(elapsedMillis);
        }
        if (animationType != BreakthroughAnimationType.FOUNDATION_TO_GOLDEN_CORE) {
            return 0.0F;
        }
        if (elapsedMillis < FOUNDATION_TO_GOLDEN_CORE_BREAK_START_MILLIS) {
            return 0.0F;
        }

        long violentShakeEndMillis = FOUNDATION_TO_GOLDEN_CORE_BREAK_START_MILLIS
                + FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_DURATION_MILLIS;
        if (elapsedMillis < violentShakeEndMillis) {
            float violentProgress = Mth.clamp(
                    (elapsedMillis - FOUNDATION_TO_GOLDEN_CORE_BREAK_START_MILLIS)
                            / (float) FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_DURATION_MILLIS,
                    0.0F,
                    1.0F
            );
            return Mth.lerp(violentProgress, 1.0F, 0.82F);
        }

        float settleProgress = Mth.clamp(
                (elapsedMillis - violentShakeEndMillis)
                        / (float) FOUNDATION_TO_GOLDEN_CORE_SETTLE_DURATION_MILLIS,
                0.0F,
                1.0F
        );
        return 0.82F * (
                1.0F - BezierUtility.cubic(
                        settleProgress,
                        0.0F,
                        0.10F,
                        0.72F,
                        1.0F
                )
        );
    }

    private static float getGoldenCoreToNascentSoulShakeStrength(long elapsedMillis) {
        if (elapsedMillis < GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START_MILLIS) {
            float trembleProgress = Mth.clamp(
                    elapsedMillis / (float) GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START_MILLIS,
                    0.0F,
                    1.0F
            );
            return Mth.lerp(trembleProgress, 0.0F, 0.08F);
        }

        long violentShakeEndMillis = GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START_MILLIS
                + GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_DURATION_MILLIS;
        if (elapsedMillis < violentShakeEndMillis) {
            float violentProgress = Mth.clamp(
                    (elapsedMillis - GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START_MILLIS)
                            / (float) GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_DURATION_MILLIS,
                    0.0F,
                    1.0F
            );
            return Mth.lerp(violentProgress, 0.58F, 0.46F);
        }

        float settleProgress = Mth.clamp(
                (elapsedMillis - violentShakeEndMillis)
                        / (float) GOLDEN_CORE_TO_NASCENT_SOUL_SETTLE_DURATION_MILLIS,
                0.0F,
                1.0F
        );
        return 0.46F * (
                1.0F - BezierUtility.cubic(
                        settleProgress,
                        0.0F,
                        0.12F,
                        0.78F,
                        1.0F
                )
        );
    }

    public static float getScreenShakeTime() {
        if (!isActive()) {
            return 0.0F;
        }
        return getElapsedMillis() / 1000.0F;
    }

    private static long getElapsedMillis() {
        return Math.max(0L, Util.getMillis() - startTimeMillis);
    }

    private static long getAnimationDurationMillis() {
        if (animationType == BreakthroughAnimationType.FOUNDATION_TO_GOLDEN_CORE) {
            return FOUNDATION_TO_GOLDEN_CORE_ANIMATION_DURATION_MILLIS
                    + GOLDEN_CORE_HOLD_DURATION_MILLIS;
        }
        if (animationType == BreakthroughAnimationType.GOLDEN_CORE_TO_NASCENT_SOUL) {
            return GOLDEN_CORE_TO_NASCENT_SOUL_ANIMATION_DURATION_MILLIS
                    + NASCENT_SOUL_HOLD_DURATION_MILLIS;
        }
        return DEFAULT_ANIMATION_DURATION_MILLIS;
    }

    private static float getAnimationProgress(float elapsedMillis) {
        if (animationType != BreakthroughAnimationType.FOUNDATION_TO_GOLDEN_CORE
                && animationType != BreakthroughAnimationType.GOLDEN_CORE_TO_NASCENT_SOUL) {
            return Mth.clamp(
                    elapsedMillis / DEFAULT_ANIMATION_DURATION_MILLIS,
                    0.0F,
                    1.0F
            );
        }

        long animationDurationMillis = animationType == BreakthroughAnimationType.FOUNDATION_TO_GOLDEN_CORE
                ? FOUNDATION_TO_GOLDEN_CORE_ANIMATION_DURATION_MILLIS
                : GOLDEN_CORE_TO_NASCENT_SOUL_ANIMATION_DURATION_MILLIS;
        float holdStartProgress = animationType == BreakthroughAnimationType.FOUNDATION_TO_GOLDEN_CORE
                ? GOLDEN_CORE_HOLD_START_PROGRESS
                : NASCENT_SOUL_HOLD_START_PROGRESS;
        long holdStartMillis = (long) (animationDurationMillis * holdStartProgress);
        float holdEndMillis = holdStartMillis + GOLDEN_CORE_HOLD_DURATION_MILLIS;
        if (animationType == BreakthroughAnimationType.GOLDEN_CORE_TO_NASCENT_SOUL) {
            holdEndMillis = holdStartMillis + NASCENT_SOUL_HOLD_DURATION_MILLIS;
        }
        if (elapsedMillis <= holdStartMillis) {
            return Mth.clamp(
                    elapsedMillis / (float) animationDurationMillis,
                    0.0F,
                    1.0F
            );
        }
        if (elapsedMillis <= holdEndMillis) {
            return holdStartProgress;
        }

        float fadeProgress = Mth.clamp(
                (elapsedMillis - holdEndMillis)
                        / (animationDurationMillis - holdStartMillis),
                0.0F,
                1.0F
        );
        return Mth.lerp(fadeProgress, holdStartProgress, 1.0F);
    }
}
