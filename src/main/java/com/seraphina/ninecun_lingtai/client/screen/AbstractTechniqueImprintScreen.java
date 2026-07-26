package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.client.utility.AnimationUtility;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public abstract class AbstractTechniqueImprintScreen extends Screen {
    private static final long CLOUD_EXPAND_DURATION_MILLIS = 920L;
    private static final long FIRST_ELEMENT_DELAY_MILLIS = 310L;
    private static final long ELEMENT_FADE_IN_DURATION_MILLIS = 390L;
    private static final long ELEMENT_FADE_IN_STAGGER_MILLIS = 105L;
    private static final long ELEMENT_FADE_OUT_DURATION_MILLIS = 280L;
    private static final long ELEMENT_FADE_OUT_STAGGER_MILLIS = 75L;
    private static final long CLOUD_RETRACT_DELAY_MILLIS = 850L;
    private static final long CLOUD_RETRACT_DURATION_MILLIS = 760L;

    private long openedAtMillis = Long.MIN_VALUE;
    private long closingAtMillis = Long.MIN_VALUE;

    protected AbstractTechniqueImprintScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        if (this.openedAtMillis == Long.MIN_VALUE) {
            this.openedAtMillis = Util.getMillis();
        }
    }

    @Override
    public void tick() {
        if (isClosing()
                && getCurrentTime() - this.closingAtMillis
                >= CLOUD_RETRACT_DELAY_MILLIS + CLOUD_RETRACT_DURATION_MILLIS) {
            finishClosing();
        }
    }

    @Override
    public void onClose() {
        beginClosing();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected final void renderAnimatedBackground(GuiGraphics guiGraphics, long currentTime) {
        this.renderBackground(guiGraphics);
        TechniqueImprintRenderUtility.drawCloudVeil(
                guiGraphics,
                this.width,
                this.height,
                currentTime,
                getCloudProgress(currentTime)
        );
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics) {}

    protected final float getElementAlpha(
            int openingOrder,
            int closingOrder,
            long currentTime
    ) {
        if (isClosing()) {
            long elapsed = currentTime - this.closingAtMillis
                    - closingOrder * ELEMENT_FADE_OUT_STAGGER_MILLIS;
            return 1.0F - AnimationUtility.easeInOutCubic(
                    AnimationUtility.progress(elapsed, ELEMENT_FADE_OUT_DURATION_MILLIS)
            );
        }
        long elapsed = currentTime - this.openedAtMillis
                - FIRST_ELEMENT_DELAY_MILLIS
                - openingOrder * ELEMENT_FADE_IN_STAGGER_MILLIS;
        return AnimationUtility.easeOutCubic(
                AnimationUtility.progress(elapsed, ELEMENT_FADE_IN_DURATION_MILLIS)
        );
    }

    protected final boolean acceptsInput() {
        return !isClosing() && getCurrentTime() - this.openedAtMillis >= 720L;
    }

    protected final boolean isClosing() {
        return this.closingAtMillis != Long.MIN_VALUE;
    }

    protected final void beginClosing() {
        if (!isClosing()) {
            this.closingAtMillis = getCurrentTime();
        }
    }

    protected final long getCurrentTime() {
        return Util.getMillis();
    }

    @Nullable
    protected Screen getParentScreen() {
        return null;
    }

    private float getCloudProgress(long currentTime) {
        if (!isClosing()) {
            return AnimationUtility.easeOutCubic(
                    AnimationUtility.progress(
                            currentTime - this.openedAtMillis,
                            CLOUD_EXPAND_DURATION_MILLIS
                    )
            );
        }
        long retractElapsed = currentTime - this.closingAtMillis - CLOUD_RETRACT_DELAY_MILLIS;
        if (retractElapsed <= 0L) {
            return 1.0F;
        }
        return 1.0F - AnimationUtility.easeInOutCubic(
                AnimationUtility.progress(retractElapsed, CLOUD_RETRACT_DURATION_MILLIS)
        );
    }

    private void finishClosing() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(getParentScreen());
        }
    }
}
