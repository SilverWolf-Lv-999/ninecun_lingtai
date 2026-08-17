package net.seraphina.ninecun_lingtai.common.ascension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class AscensionSession {
    private final UUID thisPlayerId;
    private final Vec3 thisCenter;
    private final ResourceKey<Level> thisDimension;
    private final List<AscensionQuestion> thisQuestions;
    private AscensionPhase thisPhase;
    private int thisPhaseRemainingTicks;
    private int thisCompletedStrikes;
    private int thisQuestionIndex;
    private boolean thisEvaluationPending;

    public AscensionSession(
            UUID playerId,
            Vec3 center,
            ResourceKey<Level> dimension,
            List<AscensionQuestion> questions,
            int preparationTicks
    ) {
        this.thisPlayerId = playerId;
        this.thisCenter = center;
        this.thisDimension = dimension;
        this.thisQuestions = List.copyOf(questions);
        this.thisPhase = AscensionPhase.PREPARING;
        this.thisPhaseRemainingTicks = Math.max(0, preparationTicks);
    }

    public UUID playerId() {
        return this.thisPlayerId;
    }

    public Vec3 center() {
        return this.thisCenter;
    }

    public ResourceKey<Level> dimension() {
        return this.thisDimension;
    }

    public List<AscensionQuestion> questions() {
        return this.thisQuestions;
    }

    public AscensionPhase phase() {
        return this.thisPhase;
    }

    public int phaseRemainingTicks() {
        return this.thisPhaseRemainingTicks;
    }

    public int completedStrikes() {
        return this.thisCompletedStrikes;
    }

    public int questionIndex() {
        return this.thisQuestionIndex;
    }

    public boolean isEvaluationPending() {
        return this.thisEvaluationPending;
    }

    public AscensionQuestion currentQuestion() {
        return this.thisQuestions.get(this.thisQuestionIndex);
    }

    public void tickPhase() {
        this.thisPhaseRemainingTicks = Math.max(0, this.thisPhaseRemainingTicks - 1);
    }

    public void startStriking(int intervalTicks) {
        this.thisPhase = AscensionPhase.STRIKING;
        this.thisPhaseRemainingTicks = Math.max(0, intervalTicks);
    }

    public void startQuestions() {
        this.thisPhase = AscensionPhase.QUESTIONS;
        this.thisPhaseRemainingTicks = 0;
        this.thisEvaluationPending = false;
    }

    public void completeStrike() {
        this.thisCompletedStrikes++;
    }

    public void setEvaluationPending(boolean evaluationPending) {
        this.thisEvaluationPending = evaluationPending;
    }

    public boolean advanceQuestion() {
        this.thisQuestionIndex++;
        this.thisEvaluationPending = false;
        return this.thisQuestionIndex < this.thisQuestions.size();
    }
}
