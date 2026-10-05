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
    private static final int MAXIMUM_TRAIL_POINTS = 640;
    private static final double MINIMUM_TRAIL_POINT_DISTANCE = 0.08D;

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
    private Vec3 lastObservedPosition = Vec3.ZERO;
    private boolean hasObservedPosition;
    private int stationaryTicks;
    private double motionSpeed;
    private float previousTrailVisibility;
    private float trailVisibility;
    private final Deque<Vec3> trailPositions = new ArrayDeque<>();
    private final Deque<FlightMote> flightMotes = new ArrayDeque<>();
    private int visualTicks;
    private int emissionSequence;

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
        this.visualTicks++;
        this.flightMotes.removeIf(mote -> this.visualTicks - mote.birthTick() > mote.lifetime());
        this.previousVisibility = this.visibility;
        this.previousBoostVisibility = this.boostVisibility;
        this.previousTrailVisibility = this.trailVisibility;
        this.trailVisibility = approach(this.trailVisibility, this.moving && this.flying ? 1.0F : 0.0F,
                this.moving && this.flying ? 0.12F : 0.5F);
        if (this.previousTrailVisibility == 0.0F && this.trailVisibility == 0.0F && !this.moving) {
            this.trailPositions.clear();
            this.hasTrailPosition = false;
        }
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

    public boolean observePosition(Vec3 position) {
        if (!this.hasObservedPosition) {
            this.lastObservedPosition = position;
            this.hasObservedPosition = true;
            this.stationaryTicks = 0;
            this.recordTrailPosition(position);
            return false;
        }
        double distance = position.distanceTo(this.lastObservedPosition);
        this.lastObservedPosition = position;
        boolean stopped = false;
        if (distance > 0.0015D) {
            this.stationaryTicks = 0;
            this.setMoving(true);
        } else {
            this.stationaryTicks++;
            if (this.stationaryTicks >= 3 && this.moving) {
                this.setMoving(false);
                stopped = true;
            }
        }
        this.recordTrailPosition(position);
        return stopped;
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
        if (distance > 32.0D) {
            this.trailPositions.clear();
            this.flightMotes.clear();
        } else if (this.flying && this.moving && this.style == LightFlightStyle.LIGHT && distance > 0.0015D) {
            Vec3 forward = position.subtract(this.lastTrailPosition).normalize();
            Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            side = side.normalize();
            Vec3 up = side.cross(forward).normalize();
            int count = Mth.clamp((int) Math.ceil(distance / 0.16D), 1, 8);
            for (int index = 0; index < count; index++) {
                int seed = ++this.emissionSequence * 0x9E3779B9;
                seed = (seed ^ seed >>> 16) * 0x85EBCA6B;
                seed ^= seed >>> 13;
                float phase = (seed & 65535) / 65535.0F * Mth.TWO_PI;
                float variation = (seed >>> 16 & 255) / 255.0F;
                Vec3 radial = side.scale(Mth.cos(phase)).add(up.scale(Mth.sin(phase)));
                Vec3 origin = this.lastTrailPosition.lerp(position, (index + 0.5D) / count)
                        .add(radial.scale(0.16D + variation * 0.3D));
                Vec3 velocity = radial.scale(0.016D + variation * 0.034D)
                        .add(forward.scale(-0.075D - variation * 0.07D));
                this.flightMotes.addLast(new FlightMote(origin, velocity, this.visualTicks,
                        13 + (seed >>> 24 & 15), phase, 0.018F + variation * 0.018F));
                while (this.flightMotes.size() > 160) {
                    this.flightMotes.removeFirst();
                }
            }
        }
        if (this.trailPositions.isEmpty()
                || position.distanceTo(this.trailPositions.peekFirst()) >= MINIMUM_TRAIL_POINT_DISTANCE) {
            this.trailPositions.addFirst(position);
            while (this.trailPositions.size() > MAXIMUM_TRAIL_POINTS) {
                this.trailPositions.removeLast();
            }
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
        return !this.flying && this.visibility <= 0.0F && this.boostVisibility <= 0.0F
                && this.flightMotes.isEmpty();
    }

    public Deque<FlightMote> flightMotes() {
        return new ArrayDeque<>(this.flightMotes);
    }

    public float visualAge(float partialTick) {
        return this.visualTicks - 1 + partialTick;
    }

    public record FlightMote(Vec3 position, Vec3 velocity, int birthTick, int lifetime, float phase, float size) {
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

    public float trailVisibility(float partialTick) {
        return Mth.lerp(partialTick, this.previousTrailVisibility, this.trailVisibility);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }
}
