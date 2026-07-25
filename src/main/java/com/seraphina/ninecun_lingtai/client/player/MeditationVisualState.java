package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.minecraft.util.Mth;

public final class MeditationVisualState {
    private static final float ENTER_DURATION_TICKS = 24.0F;
    private static final float EXIT_DURATION_TICKS = 20.0F;

    private FiveElementValues environmentValues;
    private FiveElementValues movementAffinities;
    private boolean meditating = true;
    private float visibility;

    public MeditationVisualState(
            FiveElementValues environmentValues,
            FiveElementValues movementAffinities
    ) {
        this.environmentValues = environmentValues;
        this.movementAffinities = movementAffinities;
    }

    public void update(
            FiveElementValues environmentValues,
            FiveElementValues movementAffinities
    ) {
        this.environmentValues = environmentValues;
        this.movementAffinities = movementAffinities;
        this.meditating = true;
    }

    public void stopMeditating() {
        this.meditating = false;
    }

    public void tick() {
        float visibilityChange = this.meditating
                ? 1.0F / ENTER_DURATION_TICKS
                : -1.0F / EXIT_DURATION_TICKS;
        this.visibility = Mth.clamp(this.visibility + visibilityChange, 0.0F, 1.0F);
    }

    public FiveElementValues environmentValues() {
        return this.environmentValues;
    }

    public FiveElementValues movementAffinities() {
        return this.movementAffinities;
    }

    public boolean isMeditating() {
        return this.meditating;
    }

    public float visibility() {
        return this.visibility;
    }

    public boolean hasFinishedExiting() {
        return !this.meditating && this.visibility <= 0.0F;
    }
}
