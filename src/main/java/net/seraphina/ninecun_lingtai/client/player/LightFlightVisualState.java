package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LightFlightVisualState {
    private static final float VISIBILITY_STEP = 0.18F;
    private static final float BOOST_FADE_IN_STEP = 0.35F;
    private static final float BOOST_FADE_OUT_STEP = 0.20F;
    private static final int MAXIMUM_TRAIL_POINTS = 24;
    private static final double MINIMUM_TRAIL_POINT_DISTANCE = 0.025D;

    private LightFlightStyle style;
    private int color;
    private boolean flying;
    private boolean moving;
    private boolean boosting;
    private float previousVisibility;
    private float visibility;
    private float previousBoostVisibility;
    private float boostVisibility;
    private Vec3 lastTrailPosition = Vec3.ZERO;
    private boolean hasTrailPosition;
    private double motionSpeed;
    private final Deque<Vec3> trailPositions = new ArrayDeque<>();

    public LightFlightVisualState(LightFlightStyle style, int color, boolean boosting) {
        this.style = style == null ? LightFlightStyle.LIGHT : style;
        this.color = color;
        this.flying = true;
        this.moving = false;
        this.boosting = boosting;
        this.visibility = 0.35F;
        this.previousVisibility = this.visibility;
        if (boosting) {
            this.boostVisibility = 1.0F;
            this.previousBoostVisibility = this.boostVisibility;
        }
    }

    public void update(LightFlightStyle style, int color, boolean boosting) {
        this.style = style == null ? LightFlightStyle.LIGHT : style;
        this.color = color;
        this.flying = true;
        this.boosting = boosting;
        this.visibility = Math.max(this.visibility, 0.35F);
    }

    public void stopFlying() {
        this.flying = false;
        this.moving = false;
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
        this.motionSpeed *= 0.78D;
    }

    public boolean isFlying() {
        return this.flying;
    }

    public boolean isBoosting() {
        return this.boosting;
    }

    public boolean isMoving() {
        return this.moving;
    }

    public void setMoving(boolean moving) {
        if (moving && !this.moving) {
            this.trailPositions.clear();
            this.hasTrailPosition = false;
            this.motionSpeed = 0.0D;
        }
        this.moving = moving;
        if (!moving) {
            this.motionSpeed = 0.0D;
        }
    }

    public void recordTrailPosition(Vec3 position) {
        if (!this.hasTrailPosition) {
            this.lastTrailPosition = position;
            this.hasTrailPosition = true;
            this.trailPositions.addFirst(position);
            return;
        }
        double distance = position.distanceTo(this.lastTrailPosition);
        this.motionSpeed = distance;
        if (distance >= MINIMUM_TRAIL_POINT_DISTANCE) {
            this.trailPositions.addFirst(position);
            while (this.trailPositions.size() > MAXIMUM_TRAIL_POINTS) {
                this.trailPositions.removeLast();
            }
        } else if (!this.trailPositions.isEmpty()) {
            this.trailPositions.removeFirst();
            this.trailPositions.addFirst(position);
        }
        this.lastTrailPosition = position;
    }

    public double motionSpeed() {
        return this.motionSpeed;
    }

    public Deque<Vec3> trailPositions() {
        return new ArrayDeque<>(this.trailPositions);
    }

    public boolean hasFinishedExiting() {
        return !this.flying && this.visibility <= 0.0F && this.boostVisibility <= 0.0F;
    }

    public int color() {
        return this.color;
    }

    public LightFlightStyle style() {
        return this.style;
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
