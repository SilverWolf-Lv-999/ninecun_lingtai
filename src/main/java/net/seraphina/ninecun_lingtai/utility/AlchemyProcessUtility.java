package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;

import java.util.List;

public final class AlchemyProcessUtility {
    public static final double FIRE_OPERATION_WEIGHT = 0.65D;
    public static final double FEED_OPERATION_WEIGHT = 0.35D;
    private static final double STAGE_BOUNDARY_EPSILON = 1.0E-12D;

    public static StageProgress locateStage(List<HeatStage> stages, double overallProgress) {
        if (stages.isEmpty()) {
            return new StageProgress(-1, clamp01(overallProgress));
        }
        if (!Double.isFinite(overallProgress) || overallProgress <= 0.0D) {
            return new StageProgress(0, 0.0D);
        }
        if (overallProgress >= 1.0D) {
            return new StageProgress(stages.size(), 1.0D);
        }
        double totalPortion = stages.stream().mapToDouble(HeatStage::portion).sum();
        double scaledProgress = overallProgress * totalPortion;
        double cumulative = 0.0D;
        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            double stagePortion = stages.get(stageIndex).portion();
            double next = cumulative + stagePortion;
            if (scaledProgress + STAGE_BOUNDARY_EPSILON < next
                    || stageIndex == stages.size() - 1) {
                double localProgress = (scaledProgress - cumulative)
                        / Math.max(1.0E-9D, stagePortion);
                return new StageProgress(stageIndex, clamp01(localProgress));
            }
            cumulative = next;
        }
        return new StageProgress(stages.size(), 1.0D);
    }

    public static FeedWindowState getFeedWindowState(
            List<AlchemyFeedStep> feedPlan,
            int fedMask,
            StageProgress stageProgress
    ) {
        int openMask = 0;
        int missedMask = 0;
        for (AlchemyFeedStep step : feedPlan) {
            int slotMask = step.slotMask();
            if ((fedMask & slotMask) != 0) {
                continue;
            }
            if (stageProgress.stageIndex() > step.stageIndex()) {
                missedMask |= slotMask;
            } else if (stageProgress.stageIndex() == step.stageIndex()
                    && stageProgress.progressWithinStage() >= step.windowStart()) {
                openMask |= slotMask;
            }
        }
        return new FeedWindowState(openMask, missedMask);
    }

    public static double calculateFeedAccuracy(
            AlchemyFeedStep step,
            StageProgress stageProgress
    ) {
        if (stageProgress.stageIndex() != step.stageIndex()
                || stageProgress.progressWithinStage() < step.windowStart()) {
            return 0.0D;
        }
        double progress = stageProgress.progressWithinStage();
        if (progress <= step.idealPoint()) {
            return interpolateScore(
                    progress,
                    step.windowStart(),
                    step.idealPoint(),
                    0.70D,
                    1.0D
            );
        }
        if (progress <= step.windowEnd()) {
            return interpolateScore(
                    progress,
                    step.idealPoint(),
                    step.windowEnd(),
                    1.0D,
                    0.70D
            );
        }
        return interpolateScore(progress, step.windowEnd(), 1.0D, 0.70D, 0.25D);
    }

    public static double combineOperationScore(
            double fireScore,
            double feedScore,
            boolean hasFeedPlan
    ) {
        if (!hasFeedPlan) {
            return clamp01(fireScore);
        }
        return clamp01(
                clamp01(fireScore) * FIRE_OPERATION_WEIGHT
                        + clamp01(feedScore) * FEED_OPERATION_WEIGHT
        );
    }

    public static int getRequiredFeedMask(List<AlchemyFeedStep> feedPlan) {
        int requiredMask = 0;
        for (AlchemyFeedStep step : feedPlan) {
            requiredMask |= step.slotMask();
        }
        return requiredMask;
    }

    private static double interpolateScore(
            double value,
            double start,
            double end,
            double startScore,
            double endScore
    ) {
        if (end <= start) {
            return endScore;
        }
        double ratio = clamp01((value - start) / (end - start));
        return startScore + (endScore - startScore) * ratio;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public record StageProgress(int stageIndex, double progressWithinStage) {
    }

    public record FeedWindowState(int openMask, int missedMask) {
    }
}
