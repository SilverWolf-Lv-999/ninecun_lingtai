package com.seraphina.ninecun_lingtai.common.tribulation;

import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class TribulationSession {
    private final UUID playerId;
    private final TribulationType type;
    private final Vec3 center;
    private final ResourceKey<Level> dimension;
    private final float aptitudeScore;
    private final double radius;
    private TribulationPhase phase;
    private int phaseRemainingTicks;
    private int completedStrikes;

    public TribulationSession(
            UUID playerId,
            TribulationType type,
            Vec3 center,
            ResourceKey<Level> dimension,
            float aptitudeScore,
            double radius,
            int preparationTicks
    ) {
        this.playerId = playerId;
        this.type = type;
        this.center = center;
        this.dimension = dimension;
        this.aptitudeScore = aptitudeScore;
        this.radius = radius;
        this.phase = TribulationPhase.PREPARING;
        this.phaseRemainingTicks = preparationTicks;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public TribulationType type() {
        return this.type;
    }

    public Vec3 center() {
        return this.center;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public float aptitudeScore() {
        return this.aptitudeScore;
    }

    public double radius() {
        return this.radius;
    }

    public TribulationPhase phase() {
        return this.phase;
    }

    public int phaseRemainingTicks() {
        return this.phaseRemainingTicks;
    }

    public int completedStrikes() {
        return this.completedStrikes;
    }

    public void tickPhase() {
        this.phaseRemainingTicks = Math.max(0, this.phaseRemainingTicks - 1);
    }

    public void startStriking(int intervalTicks) {
        this.phase = TribulationPhase.STRIKING;
        this.phaseRemainingTicks = intervalTicks;
    }

    public void startAftermath(int aftermathTicks) {
        this.phase = TribulationPhase.AFTERMATH;
        this.phaseRemainingTicks = aftermathTicks;
    }

    public void startDissipating(int dissipatingTicks) {
        this.phase = TribulationPhase.DISSIPATING;
        this.phaseRemainingTicks = dissipatingTicks;
    }

    public void completeStrike() {
        this.completedStrikes++;
    }
}
