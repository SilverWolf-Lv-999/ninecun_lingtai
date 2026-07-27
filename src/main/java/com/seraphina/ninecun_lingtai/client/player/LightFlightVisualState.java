package com.seraphina.ninecun_lingtai.client.player;

import net.minecraft.util.Mth;

public final class LightFlightVisualState {
    private static final float VISIBILITY_STEP = 0.18F;
    private static final float BOOST_FADE_IN_STEP = 0.35F;
    private static final float BOOST_FADE_OUT_STEP = 0.20F;

    private int color;
    private boolean flying;
    private boolean boosting;
    private float previousVisibility;
    private float visibility;
    private float previousBoostVisibility;
    private float boostVisibility;

    public LightFlightVisualState(int color, boolean boosting) {
        this.color = color;
        this.flying = true;
        this.boosting = boosting;
        this.visibility = 0.35F;
        this.previousVisibility = this.visibility;
        if (boosting) {
            this.boostVisibility = 1.0F;
            this.previousBoostVisibility = this.boostVisibility;
        }
    }

    public void update(int color, boolean boosting) {
        this.color = color;
        this.flying = true;
        this.boosting = boosting;
        this.visibility = Math.max(this.visibility, 0.35F);
    }

    public void stopFlying() {
        this.flying = false;
        this.boosting = false;
    }

    public void tick() {
        this.previousVisibility = this.visibility;
        this.previousBoostVisibility = this.boostVisibility;
        this.visibility = approach(this.visibility, this.flying ? 1.0F : 0.0F, VISIBILITY_STEP);
        this.boostVisibility = approach(
                this.boostVisibility,
                this.boosting ? 1.0F : 0.0F,
                this.boosting ? BOOST_FADE_IN_STEP : BOOST_FADE_OUT_STEP
        );
    }

    public boolean isFlying() {
        return this.flying;
    }

    public boolean isBoosting() {
        return this.boosting;
    }

    public boolean hasFinishedExiting() {
        return !this.flying && this.visibility <= 0.0F && this.boostVisibility <= 0.0F;
    }

    public int color() {
        return this.color;
    }

    public float visibility(float partialTick) {
        return Mth.lerp(partialTick, this.previousVisibility, this.visibility);
    }

    public float boostVisibility(float partialTick) {
        return Mth.lerp(partialTick, this.previousBoostVisibility, this.boostVisibility);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }
}
