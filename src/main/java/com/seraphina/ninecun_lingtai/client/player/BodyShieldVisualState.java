package com.seraphina.ninecun_lingtai.client.player;

import net.minecraft.util.Mth;

public final class BodyShieldVisualState {
    private static final int DURATION_TICKS = 22;
    private static final float FADE_IN_TICKS = 3.0F;
    private static final float FADE_OUT_START_TICKS = 6.0F;

    private int ageTicks;

    public void restart() {
        this.ageTicks = 0;
    }

    public void tick() {
        this.ageTicks++;
    }

    public boolean isFinished() {
        return this.ageTicks >= DURATION_TICKS;
    }

    public float visibility(float partialTick) {
        float age = this.ageTicks + partialTick;
        float fadeIn = Mth.clamp(age / FADE_IN_TICKS, 0.0F, 1.0F);
        float fadeOut = 1.0F - Mth.clamp(
                (age - FADE_OUT_START_TICKS) / (DURATION_TICKS - FADE_OUT_START_TICKS),
                0.0F,
                1.0F
        );
        float visibility = fadeIn * fadeOut;
        return visibility * visibility * (3.0F - 2.0F * visibility);
    }
}
